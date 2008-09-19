package com.surelogic._flashlight.rewriter.runtime;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods to support Flashlight transformations at runtime.
 */
public final class FlashlightRuntimeSupport {
  private static Map<ClassLoader,ClassLoaderInfo> loaderToInfoMap =
	  new HashMap<ClassLoader,ClassLoaderInfo>();
  
  /** Private method to prevent instantiation. */
  private FlashlightRuntimeSupport() {
    // Do nothing
  }
   
  public static synchronized ClassLoaderInfo getClassLoaderInfo(final Class c) {
	  final ClassLoader cl       = c.getClassLoader();
	  ClassLoaderInfo info = loaderToInfoMap.get(cl);
	  if (info == null) {
		  info = new ClassLoaderInfo(cl);
		  loaderToInfoMap.put(cl, info);
	  }
	  return info;
  }
   
  
  @SuppressWarnings("unused")
  private static Field getFieldInternal(final Class root, final String fname) 
      throws NoSuchFieldException {
    try {
      /* Hopefully the field is local. */
      final Field f = root.getDeclaredField(fname);
      // Won't get here if the field is not found
      return f;
    } catch(final NoSuchFieldException e) {
      // Fall through to try super class and interfaces 
    }
    
    final Class superClass = root.getSuperclass();
    if (superClass != null) {
      try {
        return getFieldInternal(superClass, fname);
      } catch (final NoSuchFieldException e) {
        // fall through to check interfaces
      }
    }
      
    final Class[] interfaces = root.getInterfaces();
    for (final Class i : interfaces) {
      try {
        return getFieldInternal(i, fname);
      } catch (final NoSuchFieldException e) {
        // try next interface
      }
    }
    
    // Couldn't find the field
    throw new NoSuchFieldException("Couldn't find field \"" + fname
        + "\" in class " + root.getCanonicalName() + " or any of its ancestors");
  }
}
