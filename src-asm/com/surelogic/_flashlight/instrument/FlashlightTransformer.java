package com.surelogic._flashlight.instrument;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.StringTokenizer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.surelogic._flashlight.rewriter.FlashlightClassRewriter;
import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.runtime.Log;

final class FlashlightTransformer implements ClassFileTransformer {
  private static final String FLASHLIGHT_PACKAGE_PREFIX = "com/surelogic/_flashlight/";
  
  private final Log theLog;
  private final File rewriteCache;
  private final Configuration rewriterConfig;
  
  public FlashlightTransformer(final Log log, final String rcn, final Configuration rp) {
    theLog = log;
    rewriteCache = (rcn == null) ? null : new File(rcn);
    rewriterConfig = rp;
  }
  
  public byte[] transform(final ClassLoader loader, final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain, final byte[] classfileBuffer)
      throws IllegalClassFormatException {
    if (!checkLoader(loader)) {
      theLog.log("Skipping " + className + " (loaded by " + getLoaderName(loader) + ")");
      return null;
    }
    if (isTransformable(className)) {
      theLog.log("Transforming class " + className + " (loaded by " + getLoaderName(loader) + ")");
      final ClassReader input = new ClassReader(classfileBuffer);
      final ClassWriter output = new ClassWriter(input, 0);
      final FlashlightClassRewriter xformer = new FlashlightClassRewriter(rewriterConfig, output);
      input.accept(xformer, 0);
      final byte[] newClass = output.toByteArray();    
      if (rewriteCache != null) {
        cacheClassfile(className, newClass);
      }
      return newClass;
    } else {
      theLog.log("Skipping class " + className + " (loaded by " + loader.getClass().getName() + ")");
      return null;
    }
  }
  
  private static String getLoaderName(final ClassLoader loader) {
    if (loader == null) {
      return "Bootstrap Class Loader";
    } else {
      return loader.getClass().getName();
    }
  }
  
  private static boolean checkLoader(final ClassLoader loader) {
    if (loader == null) {
      return false;
    }
    return !getLoaderName(loader).startsWith("sun.reflect");
  }

  private static boolean isTransformable(final String className) {
    return !className.startsWith(FLASHLIGHT_PACKAGE_PREFIX)
        && !className.startsWith("java/") && !className.startsWith("javax/")
        && !className.startsWith("sun/reflect/");
  }
  
  private void cacheClassfile(final String className, final byte[] classBytes) {
    File outputClass = rewriteCache;
    final StringTokenizer tokenizer = new StringTokenizer(className, "/");
    while (tokenizer.hasMoreTokens()) {
      final String segment = tokenizer.nextToken();
      if (tokenizer.hasMoreTokens()) {
        outputClass = new File(outputClass, segment);
      } else {
        // create directories
        outputClass.mkdirs();
        outputClass = new File(outputClass, segment + ".class");
      }
    }
    theLog.log("Caching " + outputClass);
    BufferedOutputStream bos = null;
    try {
      bos = new BufferedOutputStream(new FileOutputStream(outputClass));
      bos.write(classBytes);
      bos.close();
    } catch (final IOException e) {
      try {
        bos.close();
      } catch (final IOException e2) {
        // cannot do anything about it here
      }
    }
  }
}
