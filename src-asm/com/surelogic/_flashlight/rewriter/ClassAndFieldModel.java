package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
   * A Field.
   */
  public final class Field {
    /** The class that declares the field. */
    public final Clazz clazz;
    /** The name of the field. */
    public final String name;
    /** The access modifiers of the field. */
    public final int access;
    /** The globally unique id of the field. */
    public final int id;
    
    /** Whether the field is actually referenced by instrumented code. */
    private boolean isReferenced;
    
    
    
    public Field(final Clazz c, final String n, final int a, final int i) {
      clazz = c;
      name = n;
      access = a;
      id = i;
      isReferenced = false;
    }
    
    public boolean isReferenced() {
      return isReferenced;
    }
    
    public void setReferenced() {
      isReferenced = true;
    }
    
    public void writeFieldInfo(final PrintWriter out) {
//      final boolean isFinal = (access & Opcodes.ACC_FINAL) != 0;
//      final boolean isVolatile = (access & Opcodes.ACC_VOLATILE) != 0;
//      final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
//      final int viz = access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE); 
//      out.print(id);
//      out.print(' ');
//      out.print(ClassNameUtil.internal2FullyQualified(clazz.name));
//      out.print(' ');
//      out.print(name);
//      out.print(' ');
//      out.print(isStatic);
//      out.print(' ');
//      out.print(isFinal);
//      out.print(' ');
//      out.print(isVolatile);
//      out.print(' ');
//      out.print(viz);
//      out.println();
      
      // Simplified
      out.print(id);
      out.print(' ');
      out.print(ClassNameUtil.internal2FullyQualified(clazz.name));
      out.print(' ');
      out.print(name);
      out.print(' ');
      out.print(Integer.toString(access, 16).toUpperCase());
      out.println();
    }
  }
  
  
  
  /**
   * A class in the system being instrumented.  Contains the names of the 
   * superclass and any implemented interfaces.  Contains a map from all the 
   * fields directly declared in the class to unique identifiers. 
   */
  public final class Clazz {
    /** The classpath entry that contains this class */
    private final File where;
    
    /** The internal class name */
    private final String name;
    
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
     * Map from field names to fields.
     */
    private final Map<String, Field> fields = new HashMap<String, Field>();
    
    
    
    public Clazz(final File where, final String name, final boolean isInterface,
        final boolean isInstrumented, final String superClass, final String[] interfaces) {
      this.where = where;
      this.name = name;
      this.isInterface = isInterface;
      this.isInstrumented = isInstrumented;
      this.superClass = superClass;
      this.interfaces = new String[interfaces.length];
      System.arraycopy(interfaces, 0, this.interfaces, 0, interfaces.length);
    }
    
    public File getClasspathEntry() {
      return where;
    }
    
    public String getName() {
      return name;
    }
    
    public String getPackage() {
      return ClassAndFieldModel.getPackage(name);
    }
    
    public boolean isInstrumented() {
      return isInstrumented;
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
    
    public Field addField(final String fieldName, final int access) {
      final int fid = nextID++;
      final Field f = new Field(this, fieldName, access, fid);
      fields.put(fieldName, f);
      return f;
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
    
    /**
     * Test if the class directly implements an interface
     */
    public boolean implementsInterface(final String interfaceName) {
      for (final String i : interfaces) {
        if (i.equals(interfaceName)) {
          return true;
        }
      }
      return false;
    }
    
//    public void writeReferencedFields(final PrintWriter out) {
//      final String fqName = ClassNameUtil.internal2FullyQualified(name);
//      for (final Field f : fields.values()) {
//        if (f.isReferenced()) {
//          f.writeFieldInfo(out, fqName);
//        }
//      }
//    }
    
    public void collectReferencedFields(final List<Field> referencedFields) {
      for (final Field f : fields.values()) {
        if (f.isReferenced()) {
          referencedFields.add(f);
        }
      }
    }
  }

  
  /**
   * Get the internal package package name from a class name.
   * 
   * @return The package name. This is the empty string for the default package.
   */
  public static String getPackage(final String className) {
    final int lastSlash = className.lastIndexOf('/');
    // deal with default package
    if (lastSlash == -1) {
      return "";
    } else {
      return className.substring(0, lastSlash);
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
   * @return The class model object for the class, or {@value null} if the
   * class is already in the model.
   */
  public Clazz addClass(final File where,
      final String name, final boolean isInterface, 
      final boolean isInstrumented,
      final String superClass, final String[] interfaces) {
    if (classes.containsKey(name)) {
      return null;
    } else {
      final Clazz c = new Clazz(where, name, isInterface, isInstrumented, superClass, interfaces);
      classes.put(name, c);
      return c;
    }
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
   * @return The field.
   * @exception ClassNotFoundException
   *              Thrown if the class or one of its ancestors is missing from
   *              the model.
   * @exception FieldNotFoundException
   *              Thrown if the field is not found.
   */
  public Field getFieldID(final String className, final String fieldName)
      throws ClassNotFoundException, FieldNotFoundException {
    // Try the class itself first, then the superclass, then any interfaces
    
    final Clazz c = getClass(className);
    final Field f = c.fields.get(fieldName);
    if (f != null) {
      return f;
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
  
  /**
   * Test if a class or one of its ancestors implements a given interface.
   * 
   * @param className
   *          The internal name of the class to test.
   * @param interfaceName
   *          The name of the interface to test for.
   * @return Whether the class, or one of its ancestors, implements the given
   *         interface.
   * @exception ClassNotFoundException
   *              Thrown if the class or one of its ancestors is missing from
   *              the model.
   */
  public boolean implementsInterface(final String className, final String interfaceName)
      throws ClassNotFoundException {
    // Try the class itself first, then the superclass, then any interfaces
    
    final Clazz c = getClass(className);
    if (c.implementsInterface(interfaceName)) {
      return true;
    } else {
      if ((c.superClass == null) ? false : implementsInterface(c.superClass, interfaceName)) {
        return true;
      } else {
        // Try the interfaces
        for (final String i : c.getInterfaces()) {
          if (implementsInterface(i, interfaceName)) {
            return true;
          }
        }
        return false;
      }
    }
  }

  /**
   * Write the field information for all the fields that are referenced 
   * by instrumented classes.
   */
  public void writeReferencedFields(final PrintWriter out) {
    final List<Field> referencedFields = new ArrayList<Field>();
    for (final Clazz c : classes.values()) {
      c.collectReferencedFields(referencedFields);
    }
    
    Collections.sort(referencedFields, new Comparator<Field>() {
      public int compare(final Field f1, final Field f2) {
        final int id1 = f1.id;
        final int id2 = f2.id;
        return (id1 < id2 ? -1 : (id1 == id2 ? 0 : 1));
      }
    });
    
    for (final Field f : referencedFields) {
      f.writeFieldInfo(out);
    }
  }
}
