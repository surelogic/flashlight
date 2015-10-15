package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;

/**
 * A simple class model built during the first pass of the classfile
 * instrumentation. Used to model which fields are in which classes, and to
 * assign each field a unique integer identifier.
 * 
 * <p>
 * Instances of this class are not thread safe.
 */
final class ClassAndFieldModel {
  /**
   * Map from internal class names to class model objects.
   */
  final Map<String, Clazz> classes = new HashMap<String, Clazz>();

  /**
   * The next unique id to assign to a field.
   */
  int nextID = 0;

  /**
   * Thrown when a class lookup fails.
   */
  public final class ClassNotFoundException extends Exception {
    private static final long serialVersionUID = 4489685259043029901L;
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
    private static final long serialVersionUID = -3504285528216855852L;
    private final String internalClassName;
    private final String fieldName;

    public FieldNotFoundException(final String internalClassName, final String fieldName) {
      super("Could not find field " + fieldName + " in class " + internalClassName);
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

  private static final String OBJECT = "java/lang/Object";
  private static final String CLONEABLE = "java/lang/Cloneable";
  private static final String SERIALIZABLE = "java/io/Serializable";

  private static final String LENGTH = "length";
  private static final int LENGTH_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;

  private static final String CLONE = "clone";
  private static final String CLONE_DESC = "()L{0};";
  private static final int CLONE_ACCESS = Opcodes.ACC_PUBLIC;

  /**
   * A class in the system being instrumented. Contains the names of the
   * superclass and any implemented interfaces. Contains a map from all the
   * fields directly declared in the class to unique identifiers.
   */
  public final class Clazz {
    /** The classpath entry that contains this class */
    final File where;

    /** The internal class name */
    final String name;

    /**
     * Does this represent a class that is meant to be instrumented by
     * Flashlight?
     */
    final boolean isInstrumented;

    /**
     * Is it an interface?
     */
    final boolean isInterface;

    /**
     * The internal class name of the superclass of this class. May only be
     * {@code null} if the class represents "java/lang/Object".
     */
    final String superClass;

    /**
     * The internal class names of any interfaces implemented by this class. May
     * not be {@code null}; use a zero-length array instead.
     */
    final String[] interfaces;

    /**
     * Map from field names to fields.
     */
    final Map<String, Field> fields = new HashMap<String, Field>();

    /**
     * Map from method names to method access bits. The key is the concatenation
     * of the method name with the method description.
     */
    final Map<String, Integer> methods = new HashMap<String, Integer>();

    public Clazz(final File where, final String name, final boolean isInterface, final boolean isInstrumented,
        final String superClass, final String[] interfaces) {
      this.where = where;
      this.name = name;
      this.isInterface = isInterface;
      this.isInstrumented = isInstrumented;
      this.superClass = superClass;
      this.interfaces = new String[interfaces.length];
      System.arraycopy(interfaces, 0, this.interfaces, 0, interfaces.length);
    }

    /**
     * Create a new array class.
     */
    public Clazz(final String name) {
      this.where = null;
      this.name = name;
      this.isInterface = false;
      this.isInstrumented = false;
      this.superClass = OBJECT;
      this.interfaces = new String[] { CLONEABLE, SERIALIZABLE };

      addField(LENGTH, LENGTH_ACCESS);
      addMethod(CLONE, MessageFormat.format(CLONE_DESC, name), CLONE_ACCESS);
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append(name);
      sb.append(" extends ");
      sb.append(superClass);
      if (interfaces.length > 0) {
        sb.append(" implements ");
        for (int i = 0; i < interfaces.length - 1; i++) {
          sb.append(interfaces[i]);
          sb.append(", ");
        }
        sb.append(interfaces[interfaces.length - 1]);
      }
      return sb.toString();
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

    public void addMethod(final String name, final String desc, final int access) {
      methods.put(name + desc, access);
    }

    public int getMethodAccess(final String name, final String desc) {
      final Integer access = methods.get(name + desc);
      if (access == null) {
        final Clazz zuper = ClassAndFieldModel.this.classes.get(this.superClass);
        if (zuper == null) {
          return 0; // XXX: should do something better than this
        } else {
          return zuper.getMethodAccess(name, desc);
        }
      } else {
        return access;
      }
    }

    /**
     * Determines if the class or interface represented by this
     * <code>Clazz</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>.
     * 
     * <p>
     * Specifically, this method tests whether the type represented by the
     * specified <code>Clazz</code> parameter can be converted to the type
     * represented by this <code>Clazz</code> object via an identity conversion.
     * See <em>The Java Language Specification</em>, sections 5.1.1 and 5.1.4 ,
     * for details.
     * 
     * @param otherName
     *          The internal class name of the other class. THis must be a real
     *          class name, not the name of an array class, e.g.,
     *          "[[Ljava/lang/Object;" (Object[][]).
     * @return the <code>boolean</code> value indicating whether objects of the
     *         type <code>other</code> can be assigned to objects of this class
     * @exception ClassNotFoundException
     *              Thrown if one of the ancestor classes of
     *              <code>otherName</code> is not in the class model.
     */
    public boolean isAssignableFrom(final String otherName) throws ClassNotFoundException {
      /*
       * We keep a list of ancestors of other to be tested. We start with other
       * itself as the seed.
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
     * Determines if the class represented by this <code>Clazz</code> object is
     * a proper subclass of the class represented by the specified class. It
     * returns <code>true</code> if so; otherwise it returns <code>false</code>.
     * 
     * @param otherName
     *          The internal class name of the other class.
     * @return the <code>boolean</code> value indicating whether objects of the
     *         type <code>other</code> can be assigned to objects of this class
     * @exception ClassNotFoundException
     *              Thrown if one of the ancestor classes of
     *              <code>otherName</code> is not in the class model.
     */
    public boolean isProperSubclassOf(final String otherName) throws ClassNotFoundException {
      final Clazz otherClass = ClassAndFieldModel.this.getClass(otherName);
      String testMe = this.superClass;

      while (testMe != null) {
        final Clazz testClass = ClassAndFieldModel.this.getClass(testMe);
        if (testClass == otherClass) {
          return true;
        }
        testMe = testClass.getSuperClass();
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

    public void collectReferencedFields(final List<Field> referencedFields) {
      for (final Field f : fields.values()) {
        if (f.isReferenced()) {
          referencedFields.add(f);
        }
      }
    }

    void writeToFile(final PrintWriter pw) {
      pw.print(ClassNameUtil.internal2FullyQualified(name));
      final int numParents = (superClass == null ? 0 : 1) + interfaces.length;
      pw.print(' ');
      pw.print(numParents);
      if (superClass != null) {
        pw.print(' ');
        pw.print(ClassNameUtil.internal2FullyQualified(superClass));
      }
      for (final String iName : interfaces) {
        pw.print(' ');
        pw.print(ClassNameUtil.internal2FullyQualified(iName));
      }
      pw.println();
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
   *          The internal names of the interfaces implemented by the class. May
   *          not be {@code null}; use a zero-length array instead.
   * @return The class model object for the class, or {@value null} if the class
   *         is already in the model.
   */
  public AddWrapper addClass(final File where, final String name, final boolean isInterface, final boolean isInstrumented,
      final String superClass, final String[] interfaces) {
    Clazz c = classes.get(name);
    if (c != null) {
      return new AddWrapper(false, c);
    } else {
      c = new Clazz(where, name, isInterface, isInstrumented, superClass, interfaces);
      classes.put(name, c);
      return new AddWrapper(true, c);
    }
  }

  public static final class AddWrapper {
    private final boolean successful;
    private final Clazz clazz;

    AddWrapper(final boolean s, final Clazz c) {
      successful = s;
      clazz = c;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public Clazz getClazz() {
      return clazz;
    }
  }

  /**
   * Lookup a class in the model.
   * 
   * @param name
   *          The internal name of the class.
   * @return The class. Never {@value null}.
   * @exception ClassNotFoundException
   *              Thrown if the class is not in the model
   */
  public Clazz getClass(final String name) throws ClassNotFoundException {
    final Clazz clazz = classes.get(name);
    if (clazz == null) {
      /*
       * Does the class name an array? If so, we need to create a new Clazz
       * object for it.
       */
      if (name.charAt(0) == '[') {
        final Clazz arrayClass = new Clazz(name);
        classes.put(name, arrayClass);
        return arrayClass;
      } else {
        throw new ClassNotFoundException(name);
      }
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
    /*
     * Don't care here if the class is not in the model. If it is not in the
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
  public Field getFieldID(final String className, final String fieldName) throws ClassNotFoundException, FieldNotFoundException {
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
  public boolean implementsInterface(final String className, final String interfaceName) throws ClassNotFoundException {
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
   * Write the field information for all the fields that are referenced by
   * instrumented classes.
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

  /**
   * Write the class hierarchy to print writer
   */
  public void writeClassHierarchy(final PrintWriter out) {
    // Output the number of types
    out.println(classes.size());
    // Output the types
    for (final Clazz c : classes.values()) {
      c.writeToFile(out);
    }
  }
}
