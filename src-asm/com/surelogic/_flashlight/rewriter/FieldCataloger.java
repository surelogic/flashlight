package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


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
  
  /**
   * The class model object for this class. Set by the
   * {@link #visit(int, int, String, String, String, String[])} method.
   */
  private ClassAndFieldModel.Clazz clazz = null;
  
  
  
  public FieldCataloger(
      final boolean isInstrumented, final ClassAndFieldModel model) {
    this.isInstrumented = isInstrumented;
    classModel = model;
  }

  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    final boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    clazz = classModel.addClass(name,
        isInterface, isInstrumented, superName, interfaces);
  }

  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    clazz.addField(name, access);
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
