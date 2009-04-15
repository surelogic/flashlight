package com.surelogic._flashlight.rewriter;

import java.util.HashMap;
import java.util.LinkedList;
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
   * Map from internal class names to class model objects.
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
     * Does this represent a class that is meant to be instrumented by
     * Flashlight?
     */
    private final boolean isInstrumented;
    
    /**
     * Is it an inteface?
     */
    private final boolean isInterface;
    
    /**
     * The internal class name of the superclass of this class.  May only
     * be {@code null} if the class represents "java/lang/Object".
     */
    private final String superClass;
    
    /**
     * The internal class names of any interfaces implemented by this class.
     * May not be {@code null}; use a zero-length array instead.
     */
    private final String[] interfaces;
    
    /**
     * Map from field names to unique identifiers.
     */
    private final Map<String, Integer> fields = new HashMap<String, Integer>();
    
    
    
    public Clazz(final boolean isInterface, final boolean isInstrumented,
        final String superClass, final String[] interfaces) {
      this.isInterface = isInterface;
      this.isInstrumented = isInstrumented;
      this.superClass = superClass;
      this.interfaces = new String[interfaces.length];
      System.arraycopy(interfaces, 0, this.interfaces, 0, interfaces.length);
    }
    
    public boolean isInterface() {
      return isInterface;
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
    
    /**
     * Determines if the class or interface represented by this
     * <code>Clazz</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>.
     * 
     * <p> Specifically, this method tests whether the type represented by the
     * specified <code>Clazz</code> parameter can be converted to the type
     * represented by this <code>Clazz</code> object via an identity
     * conversion. See <em>The Java Language Specification</em>, sections
     * 5.1.1 and 5.1.4 , for details.
     * 
     * @param otherName
     *          The internal class name of the other class.
     * @return the <code>boolean</code> value indicating whether objects of
     *         the type <code>other</code> can be assigned to objects of this
     *         class
     * @exception IllegalStateException Thrown if one of the ancestor classes
     * of <code>otherName</code> is not in the class model.
     */
    public boolean isAssignableFrom(final String otherName) {
      /* We keep a list of ancestors of other to be tested.
       * We start with other itself as the seed.
       */
      final LinkedList<String> toBeTested = new LinkedList<String>();
      toBeTested.addLast(otherName);
      
      while (!toBeTested.isEmpty()) {
        final String testName = toBeTested.removeFirst();
        final Clazz testClass = classes.get(testName);
        if (testClass == null) {
          throw new IllegalStateException("Couldn't find class " + testName);
        }
        if (this == testClass) {
          return true;
        }
        
        /* add the superclass and superinterfaces */
        if (testClass.superClass != null) {
          toBeTested.addLast(testClass.superClass);
        }
        for (final String superInterface : testClass.interfaces) {
          toBeTested.addLast(superInterface);
        }
      }
      return false;
    }
  }

  

  /**
   * Add a new class to the model. This should be called when a new classfile is
   * encountered.
   * 
   * @param name
   *          The internal name of the class.
   * @param superClass
   *          The internal name of the class's superclass.
   * @param interfaces
   *          The internal names of the interfaces implemented by the
   *          class. May not be {@code null}; use a zero-length array instead.
   * @return The class model object for the class.
   */
  public Clazz addClass(
      final String name, final boolean isInterface, 
      final boolean isInstrumented,
      final String superClass, final String[] interfaces) {
    final Clazz c = new Clazz(isInterface, isInstrumented, superClass, interfaces);
    classes.put(name, c);
    return c;
  }
  
  /**
   * Lookup a class in the model.
   * 
   * @param name
   *          The internal name of the class.
   * @return The class, or {@code null} if the class is not in the model.
   */
  public Clazz getClass(final String name) {
    return classes.get(name);
  }
  
  /**
   * Is the named class part of the set of classes being instrumented.
   * 
   * @param name
   *          The internal name of the class.
   */
  public boolean isInstrumentedClass(final String name) {
    final Clazz c = classes.get(name);
    if (c != null) {
      return c.isInstrumented;
    } else {
      return false;
    }
  }
  
  /**
   * Get the unique identifier for the given field in the given class.
   * 
   * @param className
   *          The internal name of the class in which to search for the
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
