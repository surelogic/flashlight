package com.surelogic._flashlight.rewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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


/**
 * Wrapper around a {@link RewriteEngine} that makes it easier to use for
 * the common cases.
 */
public abstract class RewriteManager {
  // ======================================================================
  // == Constants
  // ======================================================================

  public static final String DEFAULT_FLASHLIGHT_RUNTIME_JAR = "flashlight-runtime.jar";
  private static final String ZIP_FILE_NAME_SEPERATOR = "/";
  private static final String MANIFEST_DIR = 
    JarFile.MANIFEST_NAME.substring(0, JarFile.MANIFEST_NAME.indexOf('/')+1);
  private static final char SPACE = ' ';
  private static final int BUFSIZE = 10240;
  private static final int CLASSFILE_1_6 = 50;

  
  
  // ======================================================================
  // == Inner classes
  // ======================================================================

  /**
   * Enumeration whose elements represent the different ways to scan 
   * classpath entries to build the class and field model.
   */
  private static enum HowToScan {
    /**
     * Scan a directory of classfiles whose members are not going to be
     * instrumented.  They are only used for supertype information, and
     * their fields are not added to the model.
     */
    DIR_SCAN_ONLY {
      @Override
      public void scan(final Scanner scanner, final File src)
          throws IOException {
        scanner.scanDirectory(src, false);
      }
    },

    /**
     * Scan a directory of classfiles whose members are going to be
     * instrumented.  Supertype and field information are used.
     */
    DIR {
      @Override
      public void scan(final Scanner scanner, final File src)
          throws IOException {
        scanner.scanDirectory(src, true);
      }
    },
    
    /**
     * Scan a JAR file whose members are not going to be
     * instrumented.  They are only used for supertype information, and
     * their fields are not added to the model.
     */
    JAR_SCAN_ONLY {
      @Override
      public void scan(final Scanner scanner, final File src)
          throws IOException {
        scanner.scanJar(src, false);
      }
    },
    
    /**
     * Scan a JAR file whose members are going to be
     * instrumented.  Supertype and field information are used.
     */
    JAR {
      @Override
      public void scan(final Scanner scanner, final File src)
          throws IOException {
        scanner.scanJar(src, true);
      }
    };    
    
    public abstract void scan(
        Scanner scanner, File src) throws IOException;
  }

  
  
  /**
   * Enumeration whose elements represent the different ways to
   * handle instrumentation of classfiles.
   * 
   */
  private static enum HowToInstrument {
    /**
     * Instrumented a directory of classfiles and store the instrumented
     * classfiles in a new directory.
     */
    DIR_TO_DIR(HowToScan.DIR) {
      @Override
      public void instrument(final Instrumenter instrumenter,
          final File src, final File dest, final String runtime)
          throws IOException {
        instrumenter.rewriteDirectoryToDirectory(src, dest);
      }
    },
    
    /**
     * Instrumented a directory of classfiles and store the instrumented
     * classfiles in a new JAR file.
     */
    DIR_TO_JAR(HowToScan.DIR) {
      @Override
      public void instrument(final Instrumenter instrumenter,
          final File src, final File dest, final String runtime)
          throws IOException {
        instrumenter.rewriteDirectoryToJar(src, dest, runtime);
      }
    },
    
    /**
     * Instrumented a JAR file and store the instrumented
     * classfiles in a new directory.
     */
    JAR_TO_DIR(HowToScan.JAR) {
      @Override
      public void instrument(final Instrumenter instrumenter,
          final File src, final File dest, final String runtime)
          throws IOException {
        instrumenter.rewriteJarToDirectory(src, dest, runtime);
      }
    },
    
    /**
     * Instrumented a JAR file and store the instrumented
     * classfiles in a new JAR file.
     */
    JAR_TO_JAR(HowToScan.JAR) {
      @Override
      public void instrument(final Instrumenter instrumenter,
          final File src, final File dest, final String runtime)
          throws IOException {
        instrumenter.rewriteJarToJar(src, dest, runtime);
      }
    };
    
    private HowToInstrument(final HowToScan scan) {
      scanHow = scan;
    }
    
    public final HowToScan scanHow;
    
    public abstract void instrument(
        Instrumenter instrumenter, File src, File dest, String runtime)
        throws IOException;
  }
  
  
  
  /**
   * A classpath element to be scanned.
   */
  private static class ScannableEntry {
    public final HowToScan scanHow;
    public final File src;
    
    public ScannableEntry(final HowToScan h, final File s) {
      scanHow = h;
      src = s;
    }
    
    public final void scan(final Scanner scanner) throws IOException {
      scanHow.scan(scanner, src);
    }
  }
  
  
  
  /**
   * A classpath element to be instrumented; is also a scannable element.
   */
  private static final class InstrumentableEntry extends ScannableEntry {
    public final HowToInstrument instrumentHow;
    public final File dest;
    public final String runtime;
    
    public InstrumentableEntry(final HowToInstrument instr,
        final File s, final File d, final String r) {
      super(instr.scanHow, s);
      instrumentHow = instr;
      dest = d;
      runtime = r;
    }
    
    public void instrument(final Instrumenter instrumenter) throws IOException {
      instrumentHow.instrument(instrumenter, src, dest, runtime);
    }
  }
  
  
  
  /**
   * Exception thrown during instrumentation indicating that the 
   * instrumented classfile contains oversized methods.  Contains a list
   * of those methods that are too large.
   */
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


  
  /**
   * Interface for defining strategies to provide buffered input streams
   * to the scanner and instrumenter.  Main purpose is to hide the differences
   * between getting files from a directory versus getting files from a JAR.
   */
  private static interface StreamProvider {
    public BufferedInputStream getInputStream() throws IOException;
  }



  /**
   * Class that encapsulates the methods for scanning the classfiles to
   * produce the field and class model.  This class is primary a wrapper 
   * around the {@link PrintWriter} used to output the field identifier
   * database.
   */
  private final class Scanner {
    private final PrintWriter fieldOutput;
    
    
    
    public Scanner(final PrintWriter fo) {
      fieldOutput = fo;
    }
    
    
    
    /**
     * Process the files in the given directory {@code inDir} and build a class
     * and field model from them to be used in the second rewrite pass of the
     * instrumentation.
     * 
     * @param inDir
     *          the directory to be scanned
     * @param willBeInstrumented
     *          Whether the classes in the directory are also going to be
     *          instrumented. If {@code false}, the fields of the classes are
     *          not added to the model; only the supertype information is used.
     */
    public void scanDirectory(
        final File inDir, final boolean willBeInstrumented)
        throws IOException {
      final String[] files = inDir.list();
      if (files == null) {
        throw new IOException("Source directory " + inDir + " is bad");
      }
      for (final String name : files) {
        final File nextIn = new File(inDir, name);
        if (nextIn.isDirectory()) {
          scanDirectory(nextIn, willBeInstrumented);
        } else {
          scanFileStream(new StreamProvider() {
            public BufferedInputStream getInputStream() throws IOException {
              return new BufferedInputStream(new FileInputStream(nextIn));
            }
          }, nextIn.getPath(), willBeInstrumented);
        }
      }
    }  
  
    /**
     * Process the files in the given JAR file {@code inJarFile} and build a
     * class and field model from them to be used in the second rewrite pass of
     * the instrumentation.
     * 
     * @param inJarFile
     *          the JAR file to be scanned
     * @param willBeInstrumented
     *          Whether the classes in the JAR are also going to be
     *          instrumented. If {@code false}, the fields of the classes are
     *          not added to the model; only the supertype information is used.
     */
    public void scanJar(final File inJarFile, final boolean willBeInstrumented)
        throws IOException {
      final JarFile jarFile = new JarFile(inJarFile);
      try {
        final Enumeration jarEnum = jarFile.entries(); 
        while (jarEnum.hasMoreElements()) {
          final JarEntry jarEntryIn = (JarEntry) jarEnum.nextElement();
          final String entryName = jarEntryIn.getName();
          
          if (!jarEntryIn.isDirectory()) {
            scanFileStream(new StreamProvider() {
              public BufferedInputStream getInputStream() throws IOException {
                return new BufferedInputStream(jarFile.getInputStream(jarEntryIn));
              }
            }, entryName, willBeInstrumented);
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
     * Given StreamProvider, scan the file represented by its stream contents.
     * If the given file is a classfile it is scanned, otherwise it is ignored.
     * 
     * @param provider
     *          The stream provider.
     * @param fname
     *          The name of the file whose contents are being provided.
     * @param willBeInstrumented
     *          Whether the classfile is also going to be instrumented. If
     *          {@code false}, the fields of the classes are not added to the
     *          model; only the supertype information is used.
     * @throws IOException
     *           Thrown if an IO error occurs.
     */
    private void scanFileStream(final StreamProvider provider, final String fname,
        final boolean willBeInstrumented) throws IOException {
      if (isClassfileName(fname)) {
        messenger.verbose("Scanning classfile " + fname);
        try {
          messenger.increaseNesting();
          scanClassfileStream(provider, fname, willBeInstrumented);
        } finally {
          messenger.decreaseNesting();
        }
      }          
    }
  
    /**
     * Give a StreamProvider for a classfile, scan classfile represented by
     * the stream's contents.
     * 
     * @param provider
     *          The stream provider.
     * @param fname
     *          The name of the file whose contents are being provided.
     * @param willBeInstrumented
     *          Whether the classfile is also going to be instrumented. If
     *          {@code false}, the fields of the classes are not added to the
     *          model; only the supertype information is used.
     * @throws IOException
     *           Thrown if an IO error occurs.
     */
    private void scanClassfileStream(final StreamProvider provider,
        final String classname, final boolean willBeInstrumented)
        throws IOException {
      final InputStream inClassfile = provider.getInputStream();
      try {
        final ClassReader input = new ClassReader(inClassfile);
        final FieldCataloger cataloger =
          new FieldCataloger(willBeInstrumented, fieldOutput, classModel);
        input.accept(cataloger, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES
            | ClassReader.SKIP_DEBUG);
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
   * Class that encapsulates the methods for instrumenting the classfiles. This
   * class is primary a wrapper around the {@link SiteIdFactory} used to
   * generate and maintain method call sites.
   */
  private final class Instrumenter {
    private final SiteIdFactory callSiteIdFactory;
    
    public Instrumenter(final SiteIdFactory idFactory) {
      callSiteIdFactory = idFactory;
    }
    
    
    
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
            rewriteFileStream(new StreamProvider() {
              public BufferedInputStream getInputStream() throws IOException {
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
    /* This method sets up the various file streams, but delegates the real
     * work to the recursive method rewriteDirectoryJarHelper().
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
      
      outJarFile.getParentFile().mkdirs();
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
          rewriteFileStream(new StreamProvider() {
            public BufferedInputStream getInputStream() throws IOException {
              return new BufferedInputStream(new FileInputStream(nextIn));
            }
          }, nextIn.getPath(), jarOut);
        }
      }
    }
  
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
        
        outJarFile.getParentFile().mkdirs();
        final FileOutputStream fos = new FileOutputStream(outJarFile);
        final BufferedOutputStream bos = new BufferedOutputStream(fos);
        JarOutputStream jarOut = null;
        try {
        if (outManifest == null) {
            jarOut = new JarOutputStream(bos);
        } else {
            jarOut = new JarOutputStream(bos, outManifest);
        }
          final Enumeration jarEnum = jarFile.entries(); 
          while (jarEnum.hasMoreElements()) {
            final JarEntry jarEntryIn = (JarEntry) jarEnum.nextElement();
            final String entryName = jarEntryIn.getName();
            
            /* Skip the manifest file, it has already been written by the
             * JarOutputStream constructor
             */
            if (!entryName.equals(MANIFEST_DIR) && 
              !entryName.equals(JarFile.MANIFEST_NAME)) {
              final JarEntry jarEntryOut = copyJarEntry(jarEntryIn);
              jarOut.putNextEntry(jarEntryOut);
              rewriteFileStream(new StreamProvider() {
                public BufferedInputStream getInputStream() throws IOException {
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
                rewriteFileStream(new StreamProvider() {
                  public BufferedInputStream getInputStream() throws IOException {
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
     * Rewrite the file whose contents are provided by the given StreamProvider.
     * If the file is a classfile it is instrumented. Otherwise the file is
     * copied verbatim.
     * 
     * @param provider
     *          The stream provider.
     * @param fname
     *          The name of the file whose contents are being provided.
     * @param outFile
     *          The output stream to use.
     * @throws IOException
     *           Thrown if an IO error occurs.
     */
    private void rewriteFileStream(final StreamProvider provider, 
        final String fname, final OutputStream outFile)
        throws IOException {
      if (isClassfileName(fname)) {
        messenger.verbose("Rewriting classfile " + fname);
        try {
          messenger.increaseNesting();
          rewriteClassfileStream(provider, outFile);
        } finally {
          messenger.decreaseNesting();
        }
      } else {
        messenger.verbose("Copying file unchanged " + fname);
        final InputStream inStream = provider.getInputStream();
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

    /**
     * Rewrite a classfile by instrumenting it.
     * 
     * @param provider
     *          The stream provider for the classfile.
     * @param outClassfile
     *          The output stream to write the new classfile to.
     * @throws IOException
     *           Thrown if an IO error occurs.
     */
    /* The real work is done by tryToWriteClassfileSTream().  This method
     * wraps around it to set up the streams and deal with the possibility of
     * oversized methods.
     */
    private void rewriteClassfileStream(
        final StreamProvider provider, final OutputStream outClassfile)
        throws IOException {
      RewriteMessenger msgr = messenger;
      boolean done = false;
      Set<MethodIdentifier> badMethods =
        Collections.<MethodIdentifier>emptySet();
      while (!done) {
        try {
          tryToRewriteClassfileStream(provider, outClassfile, msgr, badMethods);
          done = true;
        } catch (final OversizedMethodsException e) {
          badMethods = e.getOversizedMethods();
          /* Set the messenger to be silent because it is going to output all
           * the same warning messages all over again.
           */
          msgr = NullMessenger.prototype;
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
        final StreamProvider provider, final OutputStream outClassfile,
        final RewriteMessenger msgr, final Set<MethodIdentifier> ignoreMethods)
        throws IOException, OversizedMethodsException {
      /* First extract the debug information */
      BufferedInputStream inClassfile = provider.getInputStream();
      Map<String, DebugInfo.MethodInfo> methodInfos = null;
      try {
        final ClassReader input = new ClassReader(inClassfile);
        final DebugExtractor debugExtractor = new DebugExtractor(accessMethods);
        input.accept(debugExtractor, ClassReader.SKIP_FRAMES);
        methodInfos = debugExtractor.getDebugInfo();
      } finally {
        try {
          inClassfile.close();
        } catch (final IOException e) {
          // doesn't want to close, what can we do?
        }
      }
      
      /* Get the classfile version first so we can fine tune the ASM flags.
       * ASM is stupid: if we tell it to compute stack frames, it will do it
       * even if the classfile is from before 1.6.  So we only tell it to
       * compute stack frames if the classfile is 1.6 or higher.
       */
      inClassfile = provider.getInputStream();
      try {
        final int classfileMajorVersion = getMajorVersion(inClassfile);
        final int classWriterFlags =
          (classfileMajorVersion >= CLASSFILE_1_6) ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
        final ClassReader input = new ClassReader(inClassfile);
        final ClassWriter output = new FlashlightClassWriter(input, classWriterFlags, classModel);
        final FlashlightClassRewriter xformer =
          new FlashlightClassRewriter(config, callSiteIdFactory, msgr, output, classModel, accessMethods, methodInfos, ignoreMethods);
        // Skip stack map frames: Either the classfiles don't have them, or we will recompute them
        input.accept(xformer, ClassReader.SKIP_FRAMES);
        final Set<MethodIdentifier> badMethods = xformer.getOversizedMethods();
        if (!badMethods.isEmpty()) {
          throw new OversizedMethodsException(
              "Instrumentation generates oversized methods", badMethods);
        } else {
          outClassfile.write(output.toByteArray());
        }
      } finally {
        try {
          inClassfile.close();
        } catch (final IOException e) {
          // doesn't want to close, what can we do?
        }
      }
    }

    /**
     * Get the classfile major version number.
     * 
     * @param inClassfile
     *          The classfile. Assumes nothing has been read from the classfile
     *          yet. When this method returns the read index is reset to the start
     *          of the classfile.
     * @return The major version number
     * @throws IOException
     *           If there is a problem getting the version number
     */
    private int getMajorVersion(final BufferedInputStream inClassfile) throws IOException {
      // Remember the start of the classfile
      inClassfile.mark(10);
      
      /* Read the first 8 bytes.  We only care about bytes 7 and 8.
       * Bytes 1 to 4 == 0xCAFEBABE
       * Bytes 5 & 6 == minor version number
       * Bytes 7 & 8 == major version number
       */
      int totalRead = 0;
      while (totalRead < 8) {
        totalRead += inClassfile.read(buffer, totalRead, 8 - totalRead);
      }
      
      // Reset the stream
      inClassfile.reset();
      
      // Return the major version number;
      return (buffer[6] << 8) + buffer[7];
    }

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
    private JarEntry copyJarEntry(final JarEntry original) {
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
  
    /**
     * Given a File naming an output directory and a file name from a {@link JarEntry},
     * return a new File object that names the jar entry in the output directory.
     * (Basically prepends the output directory to the file name.)
     */
    private File composeFile(final File outDir, final String zipName) {
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
    private Manifest updateManifest(
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
  }

  
  
  // ======================================================================
  // == Fields
  // ======================================================================

  private final byte[] buffer = new byte[BUFSIZE];
  private final ClassAndFieldModel classModel = new ClassAndFieldModel();
  private final IndirectAccessMethods accessMethods = new IndirectAccessMethods();
  private final Configuration config;
  private final RewriteMessenger messenger;
  private final File fieldsFile;
  private final File sitesFile;
  private final List<ScannableEntry> otherClasspathEntries =
    new LinkedList<ScannableEntry>();
  private final List<InstrumentableEntry> entriesToInstrument =
    new LinkedList<InstrumentableEntry>();
  
  
  
  // ======================================================================
  // == Constructor
  // ======================================================================



  public RewriteManager(final Configuration c, final RewriteMessenger m,
      final File ff, final File sf) {
    config = c;
    messenger = m;
    fieldsFile = ff;
    sitesFile = sf;
  }
  
  
  
  // ======================================================================
  // == Methods to set up the manager behavior
  // ======================================================================

  /**
   * Add a directory whose contents are to be scanned but not instrumented.
   */
  public final void addClasspathDir(final File src) {
    otherClasspathEntries.add(new ScannableEntry(HowToScan.DIR_SCAN_ONLY, src));
  }
  
  /**
   * Add a JAR file whose contents are to be scanned but not instrumented.
   */
  public final void addClasspathJar(final File src) {
    otherClasspathEntries.add(new ScannableEntry(HowToScan.JAR_SCAN_ONLY, src));
  }

  /**
   * Add a directory whose contents are to be scanned and instrumented, where
   * the instrumented files are placed in the given directory. 
   */
  public final void addDirToDir(final File src, final File dest) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.DIR_TO_DIR, src, dest, null));
  }
  
  /**
   * Add a directory whose contents are to be scanned and instrumented, where
   * the instrumented files are placed in the given JAR file. 
   */
  public final void addDirToJar(final File src, final File dest, final String runtime) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.DIR_TO_JAR, src, dest, runtime));
  }
  
  /**
   * Add a JAR file whose contents are to be scanned and instrumented, where
   * the instrumented files are placed in the given JAR file. 
   */
  public final void addJarToJar(final File src, final File dest, final String runtime) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.JAR_TO_JAR, src, dest, runtime));
  }
  
  /**
   * Add a JAR file whose contents are to be scanned and instrumented, where
   * the instrumented files are placed in the given directory. 
   */
  public final void addJarToDir(final File src, final File dest, final String runtime) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.JAR_TO_DIR, src, dest, runtime));
  }


  
  // ======================================================================
  // == The main run method
  // ======================================================================
  
  /**
   * Execute the rewrite.
   */
  public final void execute() {
    PrintWriter fieldsOut = null;
    try {
      /* First pass: Scan all the classfiles to build the class
       * and field model.  Record the field identifiers in the fields
       * file.
       */
      fieldsFile.getParentFile().mkdirs();
      fieldsOut = new PrintWriter(fieldsFile);
      final Scanner scanner = new Scanner(fieldsOut);
      for (final ScannableEntry entry : otherClasspathEntries) {
        scan(entry, scanner);
      }
      for (final ScannableEntry entry : entriesToInstrument) {
        scan(entry, scanner);
      }
      
      PrintWriter sitesOut = null;
      try {
        sitesFile.getParentFile().mkdirs();
        sitesOut = new PrintWriter(sitesFile);
        final SiteIdFactory callSiteIdFactory = new SiteIdFactory(sitesOut);
        final Instrumenter instrumenter = new Instrumenter(callSiteIdFactory);
        
        for (final InstrumentableEntry entry : entriesToInstrument) {
          final String srcPath = entry.src.getAbsolutePath();
          final String destPath = entry.dest.getAbsolutePath();
          messenger.info("Instrumenting classfiles in " + srcPath + " to " + destPath);
          messenger.increaseNesting();
          preInstrument(srcPath, destPath);
          try {
            entry.instrument(instrumenter);
          } catch (final IOException e) {
            exceptionInstrument(srcPath, destPath, e);
          } finally {
            messenger.decreaseNesting();
            postInstrument(srcPath, destPath);
          }
        }
      } catch (final FileNotFoundException e) {
        exceptionCreatingSitesFile(sitesFile, e);
      } finally {
        if (sitesOut != null) {
          sitesOut.close();
        }
      }
    } catch(final FileNotFoundException e) {
      exceptionCreatingFieldsFile(fieldsFile, e);
    } finally {
      if (fieldsOut != null) {
        fieldsOut.close();
      }
    }
  }
  
  private void scan(final ScannableEntry entry, final Scanner scanner) {
    final String srcPath = entry.src.getAbsolutePath();
    messenger.info("Scanning classfiles in " + srcPath);
    messenger.increaseNesting();
    preScan(srcPath);
    try {
      entry.scan(scanner);
    } catch (final IOException e) {
      exceptionScan(srcPath, e);
    } finally {
      messenger.decreaseNesting();
      postScan(srcPath);
    }
  }
  
  
  
  // ======================================================================
  // == Helper method for the scanner/instrument classes
  // ======================================================================

  /**
   * Is the given file name a class file?
   */
  private static boolean isClassfileName(final String name) {
    return name.endsWith(".class");
  }
  
  
  
  // ======================================================================
  // == Methods for logging status
  // ======================================================================

  /**
   * Called immediately before a file is scanned.
   */
  protected void preScan(final String srcPath) {
    // do nothing
  }
  
  /**
   * Called immediately after a file is scanned.
   */
  protected void postScan(final String srcPath) {
    // do nothing
  }
  
  /**
   * Called if there is an exception while scanning a file.
   */
  protected abstract void exceptionScan(String srcPath, IOException e);
  
  /**
   * Called immediately before instrumenting a file.
   */
  protected void preInstrument(final String srcPath, final String destPath) {
    // do nothing
  }

  /**
   * Called immediately after instrumenting a file.
   */
  protected void postInstrument(final String srcPath, final String destPath) {
    // do nothing
  }
  
  /**
   * Called if there is an exception while instrumenting a file.
   */
  protected abstract void exceptionInstrument(String srcPath, String destPath, IOException e);

  /**
   * Called if there is an exception trying to create the fields database file.
   */
  protected abstract void exceptionCreatingFieldsFile(File fieldsFile, FileNotFoundException e);

  /**
   * Called if there is an exception trying to create the sites database file.
   */
  protected abstract void exceptionCreatingSitesFile(File sitesFile, FileNotFoundException e);
}
