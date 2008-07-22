package com.surelogic._flashlight.rewriter.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.FlashlightClassRewriter;

public final class RewriteEngine {
  private static final String ZIP_FILE_NAME_SEPERATOR = "/";
  private final Configuration config;
  private final EngineMessenger messenger;
  
  
  
  public RewriteEngine(final Configuration c, final EngineMessenger m) {
    config = c;
    messenger = m;
  }

  
  
  private static boolean isClassfileName(final String name) {
    return name.endsWith(".class");
  }

  
  
  public void rewriteDirectory(final File inDir, final File outDir)
      throws IOException {
    final String[] files = inDir.list();
    if (files == null) {
      throw new IOException("Source directory " + inDir + " is bad");
    }
    for (final String name : files) {
      final File nextIn = new File(inDir, name);
      final File nextOut = new File(outDir, name);
      if (nextIn.isDirectory()) {
        nextOut.mkdirs();
        rewriteDirectory(nextIn, nextOut);
      } else {
        if (isClassfileName(name)) {
          rewriteClassfile(nextIn, nextOut);
        }
      }
    }
  }



  public void rewriteJarToJar(final File inJarFile, final File outJarFile) 
      throws IOException {
    final JarFile jarFile = new JarFile(inJarFile);
    try {
      final Manifest inManifest = jarFile.getManifest();
      final Manifest outManifest = updateManifest(inManifest);
      
      final FileOutputStream fos = new FileOutputStream(outJarFile);
      final BufferedOutputStream bos = new BufferedOutputStream(fos);
      JarOutputStream jarOut = null;
      try {
        jarOut = new JarOutputStream(bos, updateManifest(outManifest));
        final byte[] buffer = new byte[10240];
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
            final BufferedInputStream entryStream =
              new BufferedInputStream(jarFile.getInputStream(jarEntryIn));
            
            if (isClassfileName(entryName)) {
              messenger.message("Rewriting classfile " + entryName);
              rewriteClassfileStream(entryStream, jarOut);
            } else {
              // Just copy the file unchanged
              messenger.message("Copying " + entryName + " unchanged");
              int count;
              while ((count = entryStream.read(buffer, 0, buffer.length)) != -1) {
                jarOut.write(buffer, 0, count);
              }
            }
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



  public void rewriteJarToDirectory(final File inJarFile, final File outDir) 
      throws IOException {
    final JarFile jarFile = new JarFile(inJarFile);
    try {
      final Manifest inManifest = jarFile.getManifest();
      final Manifest outManifest = updateManifest(inManifest);
      
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
      
      final byte[] buffer = new byte[10240];
      final Enumeration jarEnum = jarFile.entries(); 
      while (jarEnum.hasMoreElements()) {
        final JarEntry jarEntryIn = (JarEntry) jarEnum.nextElement();
        final String entryName = jarEntryIn.getName();
        
        /* Skip the manifest file, it has already been written above */
        if (!entryName.equals(JarFile.MANIFEST_NAME)) {
          final BufferedInputStream entryStream =
            new BufferedInputStream(jarFile.getInputStream(jarEntryIn));
          
          final File outFile = composeFile(outDir, entryName);
          if (jarEntryIn.isDirectory()) {
            if (!outFile.exists()) outFile.mkdirs();
          } else {
            if (!outFile.getParentFile().exists()) {
              outFile.getParentFile().mkdirs();
            }
            final BufferedOutputStream bos = 
              new BufferedOutputStream(new FileOutputStream(outFile));
            try {
              if (isClassfileName(entryName)) {
                messenger.message("Rewriting classfile " + entryName);
                rewriteClassfileStream(entryStream, bos);
              } else {
                // Just copy the file unchanged
                messenger.message("Copying " + entryName + " unchanged");
                int count;
                while ((count = entryStream.read(buffer, 0, buffer.length)) != -1) {
                  bos.write(buffer, 0, count);
                }
              }
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
  
  private void rewriteClassfile(final File inName, final File outName)
      throws IOException {
    messenger.message("Rewriting classfile " + inName);
    final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inName));
    final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outName));
    try {
      rewriteClassfileStream(bis, bos);
    } finally {
      // Close the files to be tidy
      try {
        bis.close();
      } catch(final IOException e) {
        // Doesn't want to close, what can we do?
      }
      try {
        bos.close();
      } catch(final IOException e) {
        // Doesn't want to close, what can we do?
      }
    }
  }

  private void rewriteClassfileStream(
      final InputStream inClassfile, final OutputStream outClassfile)
      throws IOException {
    final ClassReader input = new ClassReader(inClassfile);
    final ClassWriter output = new ClassWriter(input, 0);
    final FlashlightClassRewriter xformer = new FlashlightClassRewriter(config, output);
    input.accept(xformer, 0);
    outClassfile.write(output.toByteArray());
  }
  
  
  private static JarEntry copyJarEntry(final JarEntry original) {
    final JarEntry result = new JarEntry(original.getName());
    result.setMethod(original.getMethod());
    result.setExtra(original.getExtra());
    result.setComment(original.getComment());
    
    /* If the entry is deflated, we leave the sizes and checksums unset so
     * that ZipOutputStream will compute them.  If the entry is stored, we 
     * must set them explicitly, so we copy them from the original.
     */
    if (original.getMethod() == ZipEntry.STORED) {
      // Make the zip output stream do all the work
      result.setSize(original.getSize());
      result.setCompressedSize(original.getCompressedSize());
      result.setCrc(original.getCrc());
    }
    return result;
  }
  
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
  
  private static Manifest updateManifest(final Manifest manifest) {
    final Manifest newManifest = new Manifest(manifest);
    final Attributes mainAttrs = newManifest.getMainAttributes();
    final String classpath = mainAttrs.getValue(Attributes.Name.CLASS_PATH);
    final StringBuilder newClasspath = new StringBuilder();
    if (classpath != null) {
      newClasspath.append(classpath);
      newClasspath.append(' ');
    }
    newClasspath.append("flashlight-asm-runtime.jar");
    mainAttrs.put(Attributes.Name.CLASS_PATH, newClasspath.toString());
    return newManifest;
  }


  
  public static void main(final String[] args) throws IOException {
    final File userHome = new File(System.getProperty("user.home"));
    final File propFile = new File(userHome, "flashlight-rewriter.properties");
    final Properties properties = new Properties(loadPropertiesFromFile(propFile));
    
    final RewriteEngine engine =
      new RewriteEngine(new Configuration(properties), ConsoleMessenger.prototype);
    engine.rewriteDirectory(new File(args[0]), new File(args[1]));
    
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
