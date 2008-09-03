package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public final class FieldCataloger implements ClassVisitor {
  private String classNameInternal = null;
  
  private final PrintWriter out;
  private final FieldIDs fieldIDs;
  
  private String classNameFullyQualified = null;
  
  public FieldCataloger(final PrintWriter pw, final FieldIDs fids) {
    out = pw;
    fieldIDs = fids;
  }

  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    classNameInternal = name;
    classNameFullyQualified = ByteCodeUtils.internal2FullyQualified(name);
  }

  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    final Integer id = fieldIDs.addField(classNameFullyQualified, name);
    final boolean isFinal = (access & Opcodes.ACC_FINAL) != 0;
    final boolean isVolatile = (access & Opcodes.ACC_VOLATILE) != 0;
    out.print(id.intValue());
    out.print(' ');
    out.print(classNameFullyQualified);
    out.print(' ');
    out.print(name);
    out.print(' ');
    out.print(isFinal);
    out.print(' ');
    out.println(isVolatile);
    return null;
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return null;
  }

  public void visitAttribute(Attribute attr) {
  }

  public void visitEnd() {
  }

  public void visitInnerClass(String name, String outerName, String innerName,
      int access) {
  }

  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    return null;
  }

  public void visitOuterClass(String owner, String name, String desc) {
  }

  public void visitSource(String source, String debug) {
  }
}
