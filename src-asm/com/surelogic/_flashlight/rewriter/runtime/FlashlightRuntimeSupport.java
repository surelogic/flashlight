package com.surelogic._flashlight.rewriter.runtime;

import java.lang.reflect.Field;

/**
 * Helper methods to support Flashlight transformations at runtime.
 * 
 * @author aarong
 */
public final class FlashlightRuntimeSupport {
  private static Log theLog = new Log() {
    public void log(final String message) {
      System.err.println(message);
    }
    
    public void log(final Throwable throwable) {
      throwable.printStackTrace(System.err);
    }
    
    public void shutdown() {
      // nothing to do
    }
  };
  
  
  
  /** Private method to prevent instantiation. */
  private FlashlightRuntimeSupport() {
    // Do nothing
  }
  
  
  
  public static synchronized void setLog(final Log newLog) {
    theLog = newLog;
  }
  
  
  
  /**
   * A fatal error was encountered.
   * @param e The exception reporting the error.
   */
  public static synchronized void reportFatalError(final Exception e) {
    if (theLog != null) {
      theLog.log(e);
    }
  }
  
  
  
//  private static Map<String, Field> fieldCache = new HashMap<String, Field>();
  
  /**
   * Get the Field object for the named field, starting the search with the
   * given class Object.
   */
  public static synchronized Field getField(final Class root, final String fname) 
  throws NoSuchFieldException {
//    final String key = root.getCanonicalName() + "#" + fname;
//    Field f = fieldCache.get(key);
//    if (f != null) {
//      return f;
//    }
    
    try {
      /* Hopefully the field is local. */
      final Field f = root.getDeclaredField(fname);
      // Won't get here if the field is not found
//      fieldCache.put(key, f);
      return f;
    } catch(final NoSuchFieldException e) {
      // Fall through to try super class and interfaces 
    }
    
    final Class superClass = root.getSuperclass();
    if (superClass != null) {
      try {
        return getField(superClass, fname);
      } catch (final NoSuchFieldException e) {
        // fall through to check interfaces
      }
    }
      
    final Class[] interfaces = root.getInterfaces();
    for (final Class i : interfaces) {
      try {
        return getField(i, fname);
      } catch (final NoSuchFieldException e) {
        // try next interface
      }
    }
    
    // Couldn't find the field
    throw new NoSuchFieldException("Couldn't find field \"" + fname
        + "\" in class " + root.getCanonicalName() + " or any of its ancestors");
  }
}
