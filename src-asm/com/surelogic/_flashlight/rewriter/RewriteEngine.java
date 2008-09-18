package com.surelogic._flashlight.rewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel;
import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.FieldCataloger;
import com.surelogic._flashlight.rewriter.FlashlightClassRewriter;
import com.surelogic._flashlight.rewriter.MethodIdentifier;

/**
 * Class that encapsulates the ability to instrument sets of classfiles.  Supports
 * <ul>
 * <li>{@link #rewriteDirectoryToDirectory(File, File) Rewriting a directory of classfiles to a new directory.}
 * <li>{@link #rewriteJarToJar(File, File) Rewriting a jar file of classfiles to a new jar file.}
 * <li>{@link #rewriteJarToDirectory(File, File) Rewriting a jar file of classfiles to a directory.}
 * </ul>
 * 
 * <p>Supports both one- and two-pass processing.  Two-pass processing is the
 * preferred way of doing things: The first pass builds a model of the class 
 * hierarchy from all the classfiles to be instrumented and assigns unique
 * identifiers to all the declared fields in those classfiles.  The second pass
 * rewrites the classfiles and inserts instrumentation.  This cuts down on the
 * amount of reflection required during runtime: only fields that are not declared
 * in the instrumented classes requires reflection.
 * 
 * <p>One-pass processing skips the first scanning phase and just rewrites the
 * code.  In this case, all field accesses require reflective lookups.  It is 
 * very expensive at runtime.
 * 
 * <p>The first scanning pass is performed using the methods
 * {@link #scanDirectory(File)} and {@link #scanJar(File)}.
 * 
 * <p>The second instrumentation pass is performed using the methods
 * {@link #rewriteDirectoryToDirectory(File, File)}, {@link #rewriteJarToJar(File, File)},
 * and {@link #rewriteJarToDirectory(File, File)}. 
 * 
 * <p>You must not invoke a scan method after invoking a rewrite method.
 */
public final class RewriteEngine {
  public static final String DEFAULT_FLASHLIGHT_RUNTIME_JAR = "flashlight-runtime.jar";
  private static final String ZIP_FILE_NAME_SEPERATOR = "/";
  private static final char SPACE = ' ';
  private static final int BUFSIZE = 10240;
  
  private final byte[] buffer = new byte[BUFSIZE];

  private final Configuration config;
  private final EngineMessenger messenger;
  
  private final ClassAndFieldModel classModel = new ClassAndFieldModel();
  private final PrintWriter fieldOutput;
  
  
  /**
   * Create a new rewrite engine based on the given configuration and that
   * reports messages to the given messenger.
   */
  public RewriteEngine(final Configuration c, final EngineMessenger m, final PrintWriter pw) {
    config = c;
    messenger = m;
    fieldOutput = pw;
  }

  
  
  /**
   * Is the given file name a class file?
   */
  private static boolean isClassfileName(final String name) {
    return name.endsWith(".class");
  }

  
  
  // =======================================================================
  // == Dir functionality
  // =======================================================================

  /**
   * Process the files in the given directory {@code inDir}, writing to the
   * directory {@code outDir}. Class files are instrumented when they are
   * copied. Other files are copied verbatim. Nested directories are processed
   * recursively.
   * 
   * @throws IOException
   *           Thrown when there is a problem with any of the directories or
   *           files.
   */
  public void rewriteDirectoryToDirectory(final File inDir, final File outDir)
      throws IOException {
    final String[] files = inDir.list();
    if (files == null) {
      throw new IOException("Source directory " + inDir + " is bad");
    }
    // Make sure the output directory exists
    if (!outDir.exists()) outDir.mkdirs();
    for (final String name : files) {
      final File nextIn = new File(inDir, name);
      final File nextOut = new File(outDir, name);
      if (nextIn.isDirectory()) {
        rewriteDirectoryToDirectory(nextIn, nextOut);
      } else {
        final BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(nextOut));
        try {
          rewriteFileStream(new RewriteHelper() {
            public InputStream getInputStream() throws IOException {
              return new BufferedInputStream(new FileInputStream(nextIn));
            }
          }, nextIn.getPath(), bos);
        } finally {
          try {
            bos.close();
          } catch (final IOException e) {
            // Doesn't want to close, what can we do?
          }
        }
      }
    }
  }
  
  /**
   * Process the files in the given directory {@code inDir}, writing to the
   * jar file {@code outJar}. Class files are instrumented when they are
   * stored. Other files are stored verbatim. Nested directories are processed
   * recursively.  The jar file is given a simple manifest file that
   * contains a "Manifest-Version" entry, and a "Class-Path" entry pointing 
   * to the flashlight runtime jar.
   * 
   * @throws IOException
   *           Thrown when there is a problem with any of the directories or
   *           files.
   */
  public void rewriteDirectoryToJar(final File inDir, final File outJarFile,
      final String runtimeJarName)
      throws IOException {
    /* Create the manifest object */
    final Manifest outManifest = new Manifest();
    outManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (runtimeJarName != null) {
      outManifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, runtimeJarName);
    }
    
    final FileOutputStream fos = new FileOutputStream(outJarFile);
    final BufferedOutputStream bos = new BufferedOutputStream(fos);
    JarOutputStream jarOut = null;
    try {
      jarOut = new JarOutputStream(bos, outManifest);
      rewriteDirectoryToJarHelper(inDir, jarOut, "");
    } finally {
      final OutputStream streamToClose = (jarOut != null) ? jarOut : bos;
      try {
        streamToClose.close();
      } catch (final IOException e) {
        // Doesn't want to close, what can we do?
      }
    } 
  }

  private void rewriteDirectoryToJarHelper(
      final File inDir, final JarOutputStream jarOut, final String jarPathPrefix) 
      throws IOException {
    final String[] files = inDir.list();
    if (files == null) {
      throw new IOException("Source directory " + inDir + " is bad");
    }
    for (final String name : files) {
      final File nextIn = new File(inDir, name);
      if (nextIn.isDirectory()) {
        final String entryName = jarPathPrefix + name + "/";
        final JarEntry jarEntryOut = new JarEntry(entryName);
        jarOut.putNextEntry(jarEntryOut);
        rewriteDirectoryToJarHelper(nextIn, jarOut, entryName);
      } else {
        final String entryName = jarPathPrefix + name;
        final JarEntry jarEntryOut = new JarEntry(entryName);
        jarOut.putNextEntry(jarEntryOut);
        rewriteFileStream(new RewriteHelper() {
          public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new FileInputStream(nextIn));
          }
        }, nextIn.getPath(), jarOut);
      }
    }
  }
  
  /**
   * Process the files in the given directory {@code inDir} and build a class and
   * field model from them to be used in the second rewrite pass of the 
   * instrumentation.
   */
  public void scanDirectory(final File inDir) throws IOException {
    final String[] files = inDir.list();
    if (files == null) {
      throw new IOException("Source directory " + inDir + " is bad");
    }
    for (final String name : files) {
      final File nextIn = new File(inDir, name);
      if (nextIn.isDirectory()) {
        scanDirectory(nextIn);
      } else {
        scanFileStream(new RewriteHelper() {
          public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new FileInputStream(nextIn));
          }
        }, nextIn.getPath());
      }
    }
  }  

  
  
  // =======================================================================
  // == Jar functionality
  // =======================================================================

  /**
   * Process the files in the given JAR file {@code inJarFile}, writing to the
   * new JAR file {@code outJarFile}. Class files are instrumented when they
   * are copied. Other files are copied verbatim. If {@code runtimeJarName} is
   * not {@code null}, the manifest file of {@code outJarFile} is the same as
   * the input manifest file except that {@code runtimeJarName} is added to the
   * {@code Class-Path} attribute. If {@code runtimeJarName} is {@code null},
   * the manifest file of {@code outJarFile} is identical to the that of
   * {@code inJarFile}.
   * 
   * @throws IOException
   *           Thrown when there is a problem with any of the files.
   */
  public void rewriteJarToJar(final File inJarFile, final File outJarFile,
      final String runtimeJarName) throws IOException {
    final JarFile jarFile = new JarFile(inJarFile);
    try {
      final Manifest inManifest = jarFile.getManifest();
      final Manifest outManifest;
      if (runtimeJarName != null) {
        outManifest = updateManifest(inManifest, runtimeJarName);
      } else {
        outManifest = inManifest;
      }
      
      final FileOutputStream fos = new FileOutputStream(outJarFile);
      final BufferedOutputStream bos = new BufferedOutputStream(fos);
      JarOutputStream jarOut = null;
      try {
        jarOut = new JarOutputStream(bos, outManifest);
        final Enumeration jarEnum = jarFile.entries(); 
        while (jarEnum.hasMoreElements()) {
          final JarEntry jarEntryIn = (JarEntry) jarEnum.nextElement();
          final String entryName = jarEntryIn.getName();
          
          /* Skip the manifest file, it has already been written by the
           * JarOutputStream constructor
           */
          if (!entryName.equals(JarFile.MANIFEST_NAME)) {
            final JarEntry jarEntryOut = copyJarEntry(jarEntryIn);
            jarOut.putNextEntry(jarEntryOut);
            rewriteFileStream(new RewriteHelper() {
              public InputStream getInputStream() throws IOException {
                return new BufferedInputStream(jarFile.getInputStream(jarEntryIn));
              }
            }, entryName, jarOut);
          }
        }
      } finally {
        final OutputStream streamToClose = (jarOut != null) ? jarOut : bos;
        try {
          streamToClose.close();
        } catch (final IOException e) {
          // Doesn't want to close, what can we do?
        }
      }  
    } finally {
      // Close the jarFile to be tidy
      try {
        jarFile.close();
      } catch (final IOException e) {
        // Doesn't want to close, what can we do?
      }
    }
  }

  /**
   * Process the files in the given JAR file {@code inJarFile}, writing them to
   * the directory {@code outDir}. Class files are instrumented when they are
   * copied. Other files are copied verbatim. If {@code runtimeJarName} is not
   * {@code null}, the manifest file is copied but has {@code runtimeJarName}
   * added to the {@code Class-Path} attribute. If {@code runtimeJarName} is
   * {@code null}, the manifest file is copied verbatim.
   * 
   * @throws IOException
   *           Thrown when there is a problem with any of the files.
   */
  public void rewriteJarToDirectory(final File inJarFile, final File outDir,
      final String runtimeJarName) throws IOException {
    final JarFile jarFile = new JarFile(inJarFile);
    try {
      final Manifest inManifest = jarFile.getManifest();
      final Manifest outManifest;
      if (runtimeJarName != null) {
        outManifest = updateManifest(inManifest, runtimeJarName);
      } else {
        outManifest = inManifest;
      }
      
      final File outManifestFile = composeFile(outDir, JarFile.MANIFEST_NAME);
      outManifestFile.getParentFile().mkdirs();
      final BufferedOutputStream manifestOutputStream =
        new BufferedOutputStream(new FileOutputStream(outManifestFile));
      try {
        outManifest.write(manifestOutputStream);
      } finally {
        try {
          manifestOutputStream.close();
        } catch (final IOException e) {
          // Doesn't want to close, what can we do?
        }
      }
      
      final Enumeration jarEnum = jarFile.entries(); 
      while (jarEnum.hasMoreElements()) {
        final JarEntry jarEntryIn = (JarEntry) jarEnum.nextElement();
        final String entryName = jarEntryIn.getName();
        
        /* Skip the manifest file, it has already been written above */
        if (!entryName.equals(JarFile.MANIFEST_NAME)) {
          final File outFile = composeFile(outDir, entryName);
          if (jarEntryIn.isDirectory()) {
            if (!outFile.exists()) outFile.mkdirs();
          } else {
            final File outFileParentDirectory = outFile.getParentFile();
            if (!outFileParentDirectory.exists()) {
              outFileParentDirectory.mkdirs();
            }
            final BufferedOutputStream bos = 
              new BufferedOutputStream(new FileOutputStream(outFile));
            try {
              rewriteFileStream(new RewriteHelper() {
                public InputStream getInputStream() throws IOException {
                  return new BufferedInputStream(jarFile.getInputStream(jarEntryIn));
                }
              }, entryName, bos);
            } finally {
              // Close the output stream if possible
              try {
                bos.close();
              } catch (final IOException e) {
                // Doesn't want to close, what can we do?
              }
            }
          }
        }
      }
    } finally {
      // Close the jarFile to be tidy
      try {
        jarFile.close();
      } catch (final IOException e) {
        // Doesn't want to close, what can we do?
      }
    }
  }

  /**
   * Process the files in the given JAR file {@code inJarFile} and build a class
   * and field model from them to be used in the second rewrite pass of the
   * instrumentation.
   */
  public void scanJar(final File inJarFile) throws IOException {
    final JarFile jarFile = new JarFile(inJarFile);
    try {
      final Enumeration jarEnum = jarFile.entries(); 
      while (jarEnum.hasMoreElements()) {
        final JarEntry jarEntryIn = (JarEntry) jarEnum.nextElement();
        final String entryName = jarEntryIn.getName();
        
        if (!jarEntryIn.isDirectory()) {
          scanFileStream(new RewriteHelper() {
            public InputStream getInputStream() throws IOException {
              return new BufferedInputStream(jarFile.getInputStream(jarEntryIn));
            }
          }, entryName);
        }
      }
    } finally {
      // Close the jarFile to be tidy
      try {
        jarFile.close();
      } catch (final IOException e) {
        // Doesn't want to close, what can we do?
      }
    }
  }

  
  
  // =======================================================================
  // == Rewrite helpers
  // =======================================================================

  public static interface RewriteHelper {
    public InputStream getInputStream() throws IOException;
  }
  
  private static final class OversizedMethodsException extends Exception {
    private Set<MethodIdentifier> oversizedMethods;
    
    public OversizedMethodsException(
        final String message, final Set<MethodIdentifier> methods) {
      super(message);
      oversizedMethods = methods;
    }
    
    public Set<MethodIdentifier> getOversizedMethods() {
      return oversizedMethods;
    }
  }

  
  public void rewriteFileStream(final RewriteHelper helper, 
      final String fname, final OutputStream outFile)
      throws IOException {
    if (isClassfileName(fname)) {
      messenger.info("Rewriting classfile " + fname);
      try {
        messenger.increaseNesting();
        rewriteClassfileStream(fname, helper, outFile);
      } finally {
        messenger.decreaseNesting();
      }
    } else {
      messenger.info("Copying file unchanged " + fname);
      final InputStream inStream = helper.getInputStream();
      try {
        messenger.increaseNesting();
        copyStream(inStream, outFile);
      } finally {
        messenger.decreaseNesting();
        try {
          inStream.close();
        } catch (final IOException e) {
          // Doesn't want to close, what can we do?
        }
      }
    }          
  }

  public void rewriteClassfileStream(final String classname,
      final RewriteHelper helper, final OutputStream outClassfile)
      throws IOException {
    EngineMessenger msgr = messenger;
    boolean done = false;
    Set<MethodIdentifier> badMethods =
      Collections.<MethodIdentifier>emptySet();
    while (!done) {
      final InputStream inClassfile = helper.getInputStream();
      try {
        tryToRewriteClassfileStream(inClassfile, outClassfile, msgr, badMethods);
        done = true;
      } catch (final OversizedMethodsException e) {
        badMethods = e.getOversizedMethods();
        /* Set the messenger to be silent because it is going to output all
         * the same warning messages all over again.
         */
        msgr = NullMessenger.prototype;
      } finally {
        try {
          inClassfile.close();
        } catch (final IOException e) {
          // Doesn't want to close, what can we do?
        }
      }
    }
  }
  
  /**
   * Given an input stream for a classfile, write an instrumented version of the
   * classfile to the given output stream, ignoring the given methods.
   * 
   * @param inClassfile
   *          The stream to read the input class file from.
   * @param outClassfile
   *          The stream to write the instrumented class file to.
   * @param msgr The status messenger to use.
   * @param ignoreMethods
   *          The set of methods that should not be instrumented.
   * @throws IOException
   *           Thrown if there is a problem with any of the streams.
   * @throws OversizedMethodsException
   *           Thrown if any of the methods would become oversized were the
   *           instrumentation applied. When this is thrown, the instrumented
   *           version of the class is not written to the output stream.
   *           Specifically, nothing is written to the output stream when this
   *           exception is thrown.
   */
  private void tryToRewriteClassfileStream(
      final InputStream inClassfile, final OutputStream outClassfile,
      final EngineMessenger msgr, final Set<MethodIdentifier> ignoreMethods)
      throws IOException, OversizedMethodsException {
    final ClassReader input = new ClassReader(inClassfile);
    final ClassWriter output = new ClassWriter(input, 0);
    final FlashlightClassRewriter xformer =
      new FlashlightClassRewriter(config, msgr, output, classModel, ignoreMethods);
    input.accept(xformer, 0);
    final Set<MethodIdentifier> badMethods = xformer.getOversizedMethods();
    if (!badMethods.isEmpty()) {
      throw new OversizedMethodsException(
          "Instrumentation generates oversized methods", badMethods);
    } else {
      outClassfile.write(output.toByteArray());
    }
  }

  
  
  // =======================================================================
  // == Scan helpers
  // =======================================================================

  public void scanFileStream(final RewriteHelper helper, final String fname)
      throws IOException {
    if (isClassfileName(fname)) {
      messenger.info("Scanning classfile " + fname);
      try {
        messenger.increaseNesting();
        scanClassfileStream(fname, helper);
      } finally {
        messenger.decreaseNesting();
      }
    }          
  }

  public void scanClassfileStream(final String classname, final RewriteHelper helper)
      throws IOException {
    final InputStream inClassfile = helper.getInputStream();
    try {
      final ClassReader input = new ClassReader(inClassfile);
      final FieldCataloger cataloger =
        new FieldCataloger(fieldOutput, classModel);
      input.accept(cataloger, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    } finally {
      try {
        inClassfile.close();
      } catch (final IOException e) {
        // Doesn't want to close, what can we do?
      }
    }
  }


  
  // =======================================================================
  // == Copy helpers
  // =======================================================================

  /**
   * Copy the contents of an input stream to an output stream.
   * 
   * @throws IOException
   *           Thrown if there is a problem with any of the streams.
   */
  private void copyStream(
      final InputStream inStream, final OutputStream outStream)
      throws IOException {
    int count;
    while ((count = inStream.read(buffer, 0, BUFSIZE)) != -1) {
      outStream.write(buffer, 0, count);
    }
  }
  
  /**
   * Create a {@link JarEntry} suitable for use with a {@link JarOutputStream}
   * from a JarEntry obtained from a {@link JarFile}. In particular, if the
   * given entry is deflated, we leave the sizes and checksums of the return
   * entry unset so that JarOutputStream will compute them. If the entry is
   * stored, we set them explicitly by copying from the original.
   */
  private static JarEntry copyJarEntry(final JarEntry original) {
    final JarEntry result = new JarEntry(original.getName());
    result.setMethod(original.getMethod());
    result.setExtra(original.getExtra());
    result.setComment(original.getComment());
    
    if (original.getMethod() == ZipEntry.STORED) {
      // Make the zip output stream do all the work
      result.setSize(original.getSize());
      result.setCompressedSize(original.getCompressedSize());
      result.setCrc(original.getCrc());
    }
    return result;
  }
  


  // =======================================================================
  // == Other Helpers
  // =======================================================================

  /**
   * Given a File naming an output directory and a file name from a {@link JarEntry},
   * return a new File object that names the jar entry in the output directory.
   * (Basically prepends the output directory to the file name.)
   */
  private static File composeFile(final File outDir, final String zipName) {
    File result = outDir;
    final StringTokenizer tokenizer =
      new StringTokenizer(zipName, ZIP_FILE_NAME_SEPERATOR);
    while (tokenizer.hasMoreTokens()) {
      final String name = tokenizer.nextToken();
      result = new File(result, name);
    }
    return result;
  }

  /**
   * Given a JAR file manifest, update the <tt>Class-Path</tt> attribute to
   * ensure that it contains a reference to the Flashlight runtime JAR.
   * 
   * @param manifest
   *          The original JAR manifest. This object is not modified by this
   *          method.
   * @param runtimeJarName
   *          The name of the flashlight runtime JAR.
   * @return A new manifest object identical to {@code manifest} except that the
   *         <tt>Class-Path</tt> attribute is guaranteed to contain a
   *         reference to {@code runtimeJarName}.
   */
  private static Manifest updateManifest(
      final Manifest manifest, final String runtimeJarName) {
    final Manifest newManifest = new Manifest(manifest);
    final Attributes mainAttrs = newManifest.getMainAttributes();
    final String classpath = mainAttrs.getValue(Attributes.Name.CLASS_PATH);
    final StringBuilder newClasspath = new StringBuilder();
    
    if (classpath == null) {
      newClasspath.append(runtimeJarName);
    } else {
      newClasspath.append(classpath);
      if (classpath.indexOf(runtimeJarName) == -1) {
        newClasspath.append(SPACE);
        newClasspath.append(runtimeJarName);
      }
    }
    mainAttrs.put(Attributes.Name.CLASS_PATH, newClasspath.toString());
    return newManifest;
  }

  
  
  // =======================================================================
  // == Main
  // =======================================================================
  
  public static void main(final String[] args) throws IOException {
    final File userHome = new File(System.getProperty("user.home"));
    final File propFile = new File(userHome, "flashlight-rewriter.properties");
    final Properties properties = new Properties(loadPropertiesFromFile(propFile));
    
    final PrintWriter writer = new PrintWriter(System.out);
    final RewriteEngine engine =
      new RewriteEngine(new Configuration(properties), ConsoleMessenger.prototype, writer);
    engine.scanDirectory(new File(args[0]));
    engine.rewriteDirectoryToDirectory(new File(args[0]), new File(args[1]));
    writer.close();
    
    System.out.println("done");
  }
  
  private static Properties loadPropertiesFromFile(final File propFile) {
    final Properties properties = new Properties(System.getProperties());
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(propFile);
      properties.load(fis);
      fis.close();
    } catch (final IOException e) {
      System.err.println("Problem reading properties file: " + e.getMessage());
      if (fis != null) {
      try {
        fis.close();
        } catch (final IOException e2) {
          // eat it, what else can we do?
        }
      }
    }
    return properties;
  }
}
