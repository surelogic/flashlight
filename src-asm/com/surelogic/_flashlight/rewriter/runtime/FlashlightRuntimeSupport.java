package com.surelogic._flashlight.rewriter.runtime;

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
}
