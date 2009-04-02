package com.surelogic._flashlight.rewriter;

import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;


/**
 * Class visitor makes a scan over the methods in the class to extract the
 * debug information for local variables and logs the number of local variables
 * and stack size for each method.
 */
final class DebugExtractor implements ClassVisitor {
  private final DebugInfo debugInfo = new DebugInfo();

  
  
  public DebugExtractor() {
    // do nothing
  }
  
  
  
  public Map<String, DebugInfo.MethodInfo> getDebugInfo() {
    return debugInfo.getMethodList();
  }
  
  

  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    System.out.println("Extracting from " + name);
    // don't care
  }

  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    // don't care
    return null;
  }

  public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
    // Don't care about
    return null;
  }

  public void visitAttribute(final Attribute attr) {
    // Don't care about
  }

  public void visitEnd() {
    // Don't care about
  }

  public void visitInnerClass(
      final String name, final String outerName, final String innerName,
      final int access) {
    // Don't care about
  }

  public MethodVisitor visitMethod(
      final int access, final String name, final String desc,
      final String signature, final String[] exceptions) {
    System.out.println("  method " + name);
    debugInfo.newMethod(name, desc);
    
    return new MethodVisitor() {
      public AnnotationVisitor visitAnnotation(
          final String desc, final boolean visible) {
        // don't care
        return null;
      }

      public AnnotationVisitor visitAnnotationDefault() {
        // don't care
        return null;
      }

      public void visitAttribute(final Attribute attr) {
        // don't care
      }

      public void visitCode() {
        // don't care
      }

      public void visitEnd() {
        // don't care
      }

      public void visitFieldInsn(
          final int opcode, final String owner, final String name,
          final String desc) {
        // don't care
      }

      public void visitFrame(
          final int type, final int local, final Object[] local2,
          final int stack, final Object[] stack2) {
        // don't care
      }

      public void visitIincInsn(final int var, final int increment) {
        // don't care
      }

      public void visitInsn(int opcode) {
        // don't care
      }

      public void visitIntInsn(int opcode, int operand) {
        // don't care
      }

      public void visitJumpInsn(int opcode, Label label) {
        // don't care
      }

      public void visitLabel(final Label label) {
        debugInfo.visitLabel(label);
      }

      public void visitLdcInsn(Object cst) {
        // don't care
      }

      public void visitLineNumber(int line, Label start) {
        // don't care
      }

      public void visitLocalVariable(
          final String name, final String desc,
          final String signature, final Label start, final Label end,
          final int index) {
        debugInfo.vistLocalVariable(index, name, desc, start, end);
      }

      public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        // don't care
      }

      public void visitMaxs(int maxStack, int maxLocals) {
        debugInfo.visitSizes(maxLocals, maxStack);
      }

      public void visitMethodInsn(
          int opcode, String owner, String name, String desc) {
        // don't care
      }

      public void visitMultiANewArrayInsn(String desc, int dims) {
        // don't care
      }

      public AnnotationVisitor visitParameterAnnotation(
          int parameter, String desc, boolean visible) {
        // don't care
        return null;
      }

      public void visitTableSwitchInsn(
          int min, int max, Label dflt, Label[] labels) {
        // don't care
      }

      public void visitTryCatchBlock(
          Label start, Label end, Label handler, String type) {
        // don't care
      }

      public void visitTypeInsn(int opcode, String type) {
        // don't care
      }

      public void visitVarInsn(int opcode, int var) {
        // don't care
      }      
    };
  }

  public void visitOuterClass(
      final String owner, final String name, final String desc) {
    // Don't care about
  }

  public void visitSource(final String source, final String debug) {
    // Don't care about
  }
}
