package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class FlashlightMethodRewriter extends MethodAdapter {
  private final String classBeingAnalyzed;
  private Integer currentSrcLine = Integer.valueOf(-1);
  
  
  public FlashlightMethodRewriter(
      final String className, final MethodVisitor mv) {
    super(mv);
    classBeingAnalyzed = className;
  }
  
  
  
  @Override
  public void visitLineNumber(final int line, final Label start) {
    mv.visitLineNumber(line, start);
    currentSrcLine = Integer.valueOf(line);
  }
  
  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    final String ownerAsDottedClass = owner.replace('/', '.');
    if (opcode == Opcodes.PUTFIELD) {
      /* We need to manipulate the stack to make a copy of the object being
       * accessed so that we can have it for the call to the Store.
       */
      // At the start the stack is "... objectref, value"
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "... value, objectref"
      mv.visitInsn(Opcodes.DUP_X1);
      // Stack is "... objectref, value, objectref"
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "... objectref, objectref, value"
      
      // Execute the original PUTFIELD instruction
      mv.visitFieldInsn(opcode, owner, name, desc);
      // Stack is "... objectref"
      
      /* Again manipulate the stack so that we can set up the first two
       * arguments to the Store.fieldAccess() call.  The first argument
       * is a boolean "isRead" flag.  The second argument is the object being
       * accessed.
       */
      mv.visitInsn(Opcodes.ICONST_0); // Push "false"
      // Stack is "... objectref, 0"
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "... 0, objectref"
      
      /* We have to create try-catch blocks to deal with the exceptions that
       * the inserted reflection methods might throw.  We do this on a per-call
       * basis here in the bytecode, nested within the creation of the argument
       * list for the ultimate call to Store.fieldAccess().  This would not be
       * possible in the Java source code unless we used local variables.  But
       * the bytecode is more flexible.
       */
      final Label try1Start = new Label();
      final Label try1End_try2Start = new Label();
      final Label try2End_try3Start = new Label();
      final Label try3End = new Label();
      final Label catchClassNotFound1 = new Label();
      final Label catchNoSuchField = new Label();
      final Label catchClassNotFound2 = new Label();
      mv.visitTryCatchBlock(try1Start, try1End_try2Start, catchClassNotFound1, "java/lang/ClassNotFoundException");
      mv.visitTryCatchBlock(try1End_try2Start, try2End_try3Start, catchNoSuchField, "java/lang/NoSuchFieldException");
      mv.visitTryCatchBlock(try2End_try3Start, try3End, catchClassNotFound2, "java/lang/ClassNotFoundException");
      
      /* We need to insert the expression
       * "Class.forName(<owner>).getDeclaredField(<name>)" into the code.  This puts
       * the java.lang.reflect.Field object for the accessed field on the stack.
       * We have a try-catch for the call to forName() and a separate try-catch
       * for the call to getDeclaredField().
       */
      mv.visitLabel(try1Start);
      mv.visitLdcInsn(ownerAsDottedClass);
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
      mv.visitLabel(try1End_try2Start);
      mv.visitLdcInsn(name);
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
      mv.visitLabel(try2End_try3Start);
      // Stack is "... 0, objectref, Field"
      
      /* We need to insert the expression "Class.forName(<current_class>)"
       * to push the java.lang.Class object of the referencing class onto the 
       * stack.
       */
      mv.visitLdcInsn(classBeingAnalyzed);
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
      mv.visitLabel(try3End);
      // Stack is "... 0, objectref, Field, Class"
      
      /* We need to push the line number of the field access.  We could be smart
       * about how we do this based on whether the line number fits into an 8-,
       * 16-, or 32-bit value.  Right now we are dumb.
       */
      mv.visitLdcInsn(currentSrcLine);
      // Stack is "... 0, objectref, Field, Class, LineNumber"
      
      /* We can now call Store.fieldAccess() */
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, "test/Store", "fieldAccess", "(ZLjava/lang/Object;Ljava/lang/reflect/Field;Ljava/lang/Class;I)V");
      
      /* Insert catch blocks.  We also have to insert a jump around them. */
      final Label noExceptions = new Label();
      mv.visitJumpInsn(Opcodes.GOTO, noExceptions);
      
      mv.visitLabel(catchClassNotFound1); // catch ClassNotFoundException
      mv.visitTypeInsn(Opcodes.NEW, "java/lang/Error");
      mv.visitInsn(Opcodes.DUP);
      mv.visitLdcInsn("Failed to find Class object for " + ownerAsDottedClass);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V");
      mv.visitInsn(Opcodes.ATHROW);

      mv.visitLabel(catchClassNotFound2); // catch ClassNotFoundException
      mv.visitTypeInsn(Opcodes.NEW, "java/lang/Error");
      mv.visitInsn(Opcodes.DUP);
      mv.visitLdcInsn("Failed to find Class object for " + classBeingAnalyzed);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V");
      mv.visitInsn(Opcodes.ATHROW);

      mv.visitLabel(catchNoSuchField); // catch NoSuchFieldException
      mv.visitTypeInsn(Opcodes.NEW, "java/lang/Error");
      mv.visitInsn(Opcodes.DUP);
      mv.visitLdcInsn("Failed to Field object for " + name + " in class " + ownerAsDottedClass);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V");
      mv.visitInsn(Opcodes.ATHROW);

      /* Resume original instruction stream */
      mv.visitLabel(noExceptions);
    } else {
      mv.visitFieldInsn(opcode, owner, name, desc);
    }
  }
  
  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    mv.visitMaxs(maxStack+5, maxLocals);
  }
  
}
