package com.surelogic._flashlight.rewriter;

import java.util.Arrays;
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
   * Map from internal class names to class model objects.
   */
  private final Map<String, Clazz> classes = new HashMap<String, Clazz>();
  
  /**
   * The next unique id to assign to a field. 
   */
  private int nextID = 0;
  
  
  
  /**
   * Thrown when a class lookup fails.
   */
  public final class ClassNotFoundException extends Exception {
    private final String internalClassName; 
    public ClassNotFoundException(final String internalClassName) {
      super("Could not find class " + internalClassName);
      this.internalClassName = internalClassName;
    }
    
    public String getMissingClass() {
      return internalClassName;
    }
  }
  
  /**
   * Thrown when a field lookup fails.
   */
  public final class FieldNotFoundException extends Exception {
    private final String internalClassName;
    private final String fieldName;
    
    public FieldNotFoundException(
        final String internalClassName, final String fieldName) {
      super("Could not find field " + fieldName + " in class "
          + internalClassName);
      this.internalClassName = internalClassName;
      this.fieldName = fieldName;
    }
    
    public String getClassName() {
      return internalClassName;
    }
    
    public String getMissingField() {
      return fieldName;
    }
  }
  
  
  
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
     * @exception ClassNotFoundException Thrown if one of the ancestor classes
     * of <code>otherName</code> is not in the class model.
     */
    public boolean isAssignableFrom(final String otherName)
        throws ClassNotFoundException {
      /* We keep a list of ancestors of other to be tested.
       * We start with other itself as the seed.
       */
      final LinkedList<String> toBeTested = new LinkedList<String>();
      toBeTested.addLast(otherName);
      
      while (!toBeTested.isEmpty()) {
        final String testName = toBeTested.removeFirst();
        final Clazz testClass = ClassAndFieldModel.this.getClass(testName);
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
   * @return The class.  Never {@value null}.
   * @exception ClassNotFoundException Thrown if the class is not in the model
   */
  public Clazz getClass(final String name) throws ClassNotFoundException {
    final Clazz clazz = classes.get(name);
    if (clazz == null) {
      throw new ClassNotFoundException(name);
    } else {
      return clazz;
    }
  }
  
  /**
   * Is the named class part of the set of classes being instrumented.
   * 
   * @param name
   *          The internal name of the class.
   */
  public boolean isInstrumentedClass(final String name) {
    /* Don't care here if the class is not in the model.  If it is not in the
     * model, it is not going to be instrumented, so we just return false in
     * that case.
     */
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
   *          The internal name of the class in which to search for the field.
   * @param fieldName
   *          The name of the field.
   * @return The unique identifier of the field.
   * @exception ClassNotFoundException
   *              Thrown if the class or one of its ancestors is missing from
   *              the model.
   * @exception FieldNotFoundException
   *              Thrown if the field is not found.
   */
  public Integer getFieldID(final String className, final String fieldName)
      throws ClassNotFoundException, FieldNotFoundException {
    // Try the class itself first, then the superclass, then any interfaces
    
    final Clazz c = getClass(className);
    final Integer id = c.fields.get(fieldName);
    if (id != null) {
      return id;
    } else {
      // Try the superclass, if it exists
      if (c.superClass != null) {
        try {
          return getFieldID(c.superClass, fieldName);
        } catch (final FieldNotFoundException e) {
          // Eat it and try the interfaces
        }
      }
      
      // try the interfaces
      for (final String iface : c.getInterfaces()) {
        try {
          return getFieldID(iface, fieldName);
        } catch (final FieldNotFoundException e2) {
          // Eat it, and try the next interface
        }
      }
      
      // Fail!
      throw new FieldNotFoundException(className, fieldName);
    }
  }
}
