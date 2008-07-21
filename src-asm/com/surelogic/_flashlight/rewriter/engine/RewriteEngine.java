package com.surelogic._flashlight.rewriter.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.FlashlightClassRewriter;

public final class RewriteEngine {
  private final Configuration config;
  private final EngineMessenger messenger;
  
  
  
  public RewriteEngine(final Configuration c, final EngineMessenger m) {
    config = c;
    messenger = m;
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
        if (name.endsWith(".class")) {
          rewriteClass(nextIn, nextOut);
        }
      }
    }
  }

  private void rewriteClass(final File inName, final File outName)
      throws IOException {
    messenger.message("Rewriting classfile " + inName);
    final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inName));
    final ClassReader input = new ClassReader(bis);
    final ClassWriter output = new ClassWriter(input, 0);
    final FlashlightClassRewriter xformer = new FlashlightClassRewriter(config, output);
    input.accept(xformer, 0);
    bis.close();
    
    final byte[] newClass = output.toByteArray();    

    final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outName));
    bos.write(newClass);
    bos.close();
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
