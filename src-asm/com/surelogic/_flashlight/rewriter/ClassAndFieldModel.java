package com.surelogic._flashlight.rewriter;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple class model built during the first pass of the classfile
 * instrumentation.  Used to model which fields are in which classes, and to
 * assign each field a unique integer identifier.
 * 
 * <p>Instances of this class are not thread safe.
 */
final class ClassAndFieldModel {
  /**
   * Return value for {@link #getFieldID(String, String)} indicating that a
   * field with the given name could not be found in the class.
   */
  public static final Integer FIELD_NOT_FOUND = null;

  /**
   * Map from fully qualified class names to class model objects.
   */
  private final Map<String, Clazz> classes = new HashMap<String, Clazz>();
  
  /**
   * The next unique id to assign to a field. 
   */
  private int nextID = 0;
  
  
  
  /**
   * A class in the system being instrumented.  Contains the names of the 
   * superclass and any implemented interfaces.  Contains a map from all the 
   * fields directly declared in the class to unique identifiers. 
   */
  public final class Clazz {
    /**
     * The fully qualified name of the superclass of this class.  May only
     * be {@code null} if the class represent "java.lang.Object".
     */
    private final String superClass;
    
    /**
     * The fully qualified names of any interfaces implemented by this class.
     * May not be {@code null}; use a zero-length array instead.
     */
    private final String[] interfaces;
    
    /**
     * Map from field names to unique identifiers.
     */
    private final Map<String, Integer> fields = new HashMap<String, Integer>();
    
    
    
    public Clazz(final String superClass, final String[] interfaces) {
      this.superClass = superClass;
      this.interfaces = interfaces;
    }
    
    public String getSuperClass() {
      return superClass;
    }
    
    public String[] getInterfaces() {
      return interfaces;
    }
    
    public Integer addField(final String fieldName) {
      final Integer id = new Integer(nextID++);
      fields.put(fieldName, id);
      return id;
    }
    
    public Integer getField(final String fieldName) {
      return fields.get(fieldName);
    }
  }

  

  /**
   * Add a new class to the model. This should be called when a new classfile is
   * encountered.
   * 
   * @param name
   *          The fully qualified name of the class.
   * @param superClass
   *          The fully qualified name of the class's superclass.
   * @param interfaces
   *          The fully qualified names of the interfaces implemented by the
   *          class. May not be {@code null}; use a zero-length array instead.
   * @return The class model object for the class.
   */
  public Clazz addClass(
      final String name, final String superClass, final String[] interfaces) {
    final Clazz c = new Clazz(superClass, interfaces);
    classes.put(name, c);
    return c;
  }
  
  /**
   * Does the class model contain an entry for this class?  In other words, is
   * the named class part of the set of classes being instrumented.
   * 
   * @param name
   *          The fully qualified name of the class.
   * @return {@code true} if and only if the class is part of the model.
   */
  public boolean isInstrumentedClass(final String name) {
    return classes.containsKey(name);
  }
  
  /**
   * Get the unique identifier for the given field in the given class.
   * 
   * @param className
   *          The fully qualified name of the class in which to search for the
   *          field.
   * @param fieldName
   *          The name of the field.
   * @return The unique identifier of the field or {@link #FIELD_NOT_FOUND} if
   *         the field could not be found.
   */
  public Integer getFieldID(final String className, final String fieldName) {
    // Try the class itself first, then the superclass, then any interfaces
    
    final Clazz c = classes.get(className);
    if (c == null) {
      return FIELD_NOT_FOUND;
    } else {
      Integer id = c.getField(fieldName);
      if (id != null) {
        return id;
      } else {
        // try superclass
        id = getFieldID(c.superClass, fieldName);
        if (id != FIELD_NOT_FOUND) {
          return id;
        } else {
          // try interfaces
          for (String iface : c.getInterfaces()) {
            id = getFieldID(iface, fieldName);
            if (id != FIELD_NOT_FOUND) {
              return id;
            }
          }
          
          // Field not found
          return FIELD_NOT_FOUND;
        }
      }
    }
  }
}
