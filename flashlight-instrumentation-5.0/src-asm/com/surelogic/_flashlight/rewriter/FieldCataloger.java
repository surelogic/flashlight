package com.surelogic._flashlight.rewriter;

import java.io.File;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;


/**
 * Class visitor that implements the first pass of the classfile instrumentation.
 * Builds a {@link ClassAndFieldModel} to model the class hierarchy and field
 * declarations, and to maintain the unique identifiers of the fields.  Writes
 * a catalog of the all the field declarations to a {@code PrintWriter}.  This 
 * catalog includes
 * <ul>
 * <li>The unique integer id for the field.
 * <li>The fully qualified name of the class that declares the field.
 * <li>The field name.
 * <li>Whether the field is <code>final</code>.
 * <li>Whether the field is <code>volatile</code>.
 * </ul>
 * 
 * @see ClassAndFieldModel
 * @see ClassAndFieldModel.Clazz
 */
final class FieldCataloger implements ClassVisitor {
  /** 
   * The classpath entry that contains the class being catalogged.
   */
  private final File where;
  
  /**
   * The pathname of the classfile relative to the root of the classpath entry
   * that contains it. 
   */
  private final String relativePath;
  
  /**
   * Is the scanned class going to be instrumented?  Classes that are 
   * not instrumented are in the class model only for supertype information
   * needed to maintain stack map frames.
   * Class that are not instrumented do not have field information, and do
   * not contribute to the field catalog.
   */
  private final boolean isInstrumented;
  
  /**
   * The class and field model we are building.
   */
  private final ClassAndFieldModel classModel;
  
  private final RewriteMessenger messenger;

  /**
   * The class model object for this class. Set by the
   * {@link #visit(int, int, String, String, String, String[])} method.
   */
  private ClassAndFieldModel.Clazz clazz = null;
  
  /**
   * Output field: May be set to true by {@link #visit} to indicated 
   * that the class was not included in the model.
   */
  private boolean classIsBogus = false;
  
  /**
   * Output field: May be set to non-{@code null} to indicate that the
   * class duplicates a class already found in the model.  Refers to the
   * class record of the original class.
   */
  private ClassAndFieldModel.Clazz duplicateOf = null;
  
  
  
  public FieldCataloger(final File where, final String relativePath,
      final boolean isInstrumented, final ClassAndFieldModel model,
      RewriteMessenger messenger) {
    this.where = where;
    this.relativePath = relativePath;
    this.isInstrumented = isInstrumented;
    this.classModel = model;
    this.messenger = messenger;
  }

  public boolean isClassBogus() {
    return classIsBogus;
  }
  
  public ClassAndFieldModel.Clazz getDuplicateOf() {
    return duplicateOf;
  }
  
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    /* We check to see if the name of the class matches the name of the classfile,
     * and whether the class is located in the correct location.  The RewriteManager
     * guarantees that the relativePath ends with ".class" and that the 
     * path separator is '/'.
     */
    if (name.equals(relativePath.substring(0, relativePath.length() - 6))) {
      final boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
      clazz = classModel.addClass(where, name,
          isInterface, isInstrumented, superName, interfaces);
      if (clazz == null) {
        messenger.warning("Skipping class " + ClassNameUtil.internal2FullyQualified(name) + " because we already saw another copy of it");
        try {
          duplicateOf = classModel.getClass(name);
        } catch (ClassNotFoundException e) {
          /* Cannot happen because we already know from addClass that the
           * class exists. */
        }
      }
    } else {
      messenger.warning("Skipping class " + ClassNameUtil.internal2FullyQualified(name) + " because it is not located in the proper directory or has the wrong classfile name");
      classIsBogus = true;
    }
  }

  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    if (clazz != null) clazz.addField(name, access);
    return null;
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    // Don't care about
    return null;
  }

  public void visitAttribute(Attribute attr) {
    // Don't care about
  }

  public void visitEnd() {
    // Don't care about
  }

  public void visitInnerClass(String name, String outerName, String innerName,
      int access) {
    // Don't care about
  }

  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    // Don't care about
    return null;
  }

  public void visitOuterClass(String owner, String name, String desc) {
    // Don't care about
  }

  public void visitSource(String source, String debug) {
    // Don't care about
  }
}
