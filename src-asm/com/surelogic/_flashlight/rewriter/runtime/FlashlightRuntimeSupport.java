package com.surelogic._flashlight.rewriter.runtime;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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

  private static Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

  private static Map<String, Map<String, Field>> classNameToFieldNameToField =
    new HashMap<String, Map<String, Field>>();


  
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
  
  
  
  /**
   * Get the Field object for the named field, starting the search with the
   * given named classt.
   */
  public static synchronized Field getField(
      final String className, final String fieldName)
      throws NoSuchFieldException, ClassNotFoundException {
    Map<String, Field> fieldName2Field = classNameToFieldNameToField.get(className);
    if (fieldName2Field == null) {
      /* Never seen this class before */
      final Class clazz = Class.forName(className);
      final Field field = getFieldInternal(clazz, fieldName);
      
      classes.put(className, clazz);
      fieldName2Field = new HashMap<String, Field>();
      fieldName2Field.put(fieldName, field);
      classNameToFieldNameToField.put(className, fieldName2Field);
      return field;
    } else {
      // Have we seen this field before?
      Field field = fieldName2Field.get(fieldName);
      if (field == null) {
        // We know that Class in the classes table
        final Class clazz = classes.get(className);
        field = getFieldInternal(clazz, fieldName);
        fieldName2Field.put(fieldName, field);
      }
      return field;
    }
  }
    
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
