package com.surelogic._flashlight.rewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * State machine that eats JVM operations and triggers when the super
 * constructor has been called.  Client class needs to forward events from
 * <ul>
 * <li><code>visitFieldInsn</code>
 * <li><code>visitInsn</code>
 * <li><code>visitIntInsn</code>
 * <li><code>visitJumpInsn</code>
 * <li><code>visitLabel</code>
 * <li><code>visitLdcInsn</code>
 * <li><code>visitLookupSwitchInsn</code>
 * <li><code>visitMethodInsn</code>
 * <li><code>visitMultiANewArrayInsn</code>
 * <li><code>visitTableSwitchInsn</code>
 * <li><code>visitTypeInsn</code>
 * <li><code>visitVarInsn</code>
 * </ul>
 * 
 * <p>This class is based on the source code in
 * {@link org.objectweb.asm.commons.AdviceAdapter}. I am not using that class
 * directly because I am not interested in the additional overhead from the
 * label reordering and the stack frame correction.
 * 
 * <p>The branch management works because the JVM specification requires that no 
 * <em>backwards</em> branches are taken when an uninitialized object is on the
 * stack.
 * 
 * <p>This could be extended to track movement of objects into and out of 
 * local variables, but I don't think it is worthwhile.
 */
final class ConstructorInitStateMachine implements MethodVisitor {
  private static enum AbstractObjects { THIS, OTHER }
  
  public static interface Callback {
    public void superConstructorCalled();
  }
  
  

  private List<AbstractObjects> stackFrame =
    new ArrayList<AbstractObjects>();

  private final Map<Label, List<AbstractObjects>> branchMap =
    new HashMap<Label, List<AbstractObjects>>();
  
  private boolean triggered = false;
  private final Callback callback; 
  
  
  
  public ConstructorInitStateMachine(final Callback callback) {
    this.callback = callback;
  }
  
  
  
  // ==========================================================================
  // == Stack management methods
  // ==========================================================================
  
  private AbstractObjects popValue() {
    return stackFrame.remove(stackFrame.size() - 1);
  }

  private AbstractObjects peekValue() {
    return stackFrame.get(stackFrame.size() - 1);
  }

  private void pushValue(final AbstractObjects o) {
    stackFrame.add(o);
  }

  
  
  // ==========================================================================
  // == Branch management methods
  // ==========================================================================

  private void addBranches(final Label dflt, final Label[] labels) {
    addBranch(dflt);
    for (int i = 0; i < labels.length; i++) {
      addBranch(labels[i]);
    } 
  }

  private void addBranch(final Label label) {
    if (branchMap.containsKey(label)) {
      return;
    }
    branchMap.put(label, new ArrayList(stackFrame));
  }


  
  // ==========================================================================
  // == MethodVisitor methods
  // ==========================================================================
  
  public AnnotationVisitor visitAnnotation(
      final String desc, final boolean visible) {
    // Not interesting
    return null;
  }

  public AnnotationVisitor visitAnnotationDefault() {
    // Not interesting
    return null;
  }

  public void visitAttribute(final Attribute attr) {
    // Not interesting
  }

  public void visitCode() {
    // Not interesting
  }

  public void visitEnd() {
    // Not interesting
  }

  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    if (!triggered) {
      final char c = desc.charAt(0);
      final boolean longOrDouble = c == 'J' || c == 'D';
      switch (opcode) {
      case Opcodes.GETSTATIC:
        pushValue(AbstractObjects.OTHER);
        if (longOrDouble) {
          pushValue(AbstractObjects.OTHER);
        }
        break;
      case Opcodes.PUTSTATIC:
        popValue();
        if (longOrDouble) {
          popValue();
        }
        break;
      case Opcodes.PUTFIELD:
        popValue();
        if (longOrDouble) {
          popValue();
          popValue();
        }
        break;
      // case Opcodes.GETFIELD:
      default:
        if (longOrDouble) {
          pushValue(AbstractObjects.OTHER);
        }
      }
    }
  }

  public void visitFrame(
      final int type, final int local, final Object[] local2,
      final int stack, final Object[] stack2) {
    // Not interesting

  }
  public void visitIincInsn(final int var, final int increment) {
    // Not interesting
  }

  public void visitInsn(final int opcode) {
    if (!triggered) {
      int s;
      switch (opcode) {
      case Opcodes.RETURN: // empty stack
        break;
  
      case Opcodes.IRETURN: // 1 before n/a after
      case Opcodes.FRETURN: // 1 before n/a after
      case Opcodes.ARETURN: // 1 before n/a after
      case Opcodes.ATHROW: // 1 before n/a after
        popValue();
        break;
  
      case Opcodes.LRETURN: // 2 before n/a after
      case Opcodes.DRETURN: // 2 before n/a after
        popValue();
        popValue();
        break;
  
      case Opcodes.NOP:
      case Opcodes.LALOAD: // remove 2 add 2
      case Opcodes.DALOAD: // remove 2 add 2
      case Opcodes.LNEG:
      case Opcodes.DNEG:
      case Opcodes.FNEG:
      case Opcodes.INEG:
      case Opcodes.L2D:
      case Opcodes.D2L:
      case Opcodes.F2I:
      case Opcodes.I2B:
      case Opcodes.I2C:
      case Opcodes.I2S:
      case Opcodes.I2F:
      case Opcodes.ARRAYLENGTH:
        break;
  
      case Opcodes.ACONST_NULL:
      case Opcodes.ICONST_M1:
      case Opcodes.ICONST_0:
      case Opcodes.ICONST_1:
      case Opcodes.ICONST_2:
      case Opcodes.ICONST_3:
      case Opcodes.ICONST_4:
      case Opcodes.ICONST_5:
      case Opcodes.FCONST_0:
      case Opcodes.FCONST_1:
      case Opcodes.FCONST_2:
      case Opcodes.F2L: // 1 before 2 after
      case Opcodes.F2D:
      case Opcodes.I2L:
      case Opcodes.I2D:
        pushValue(AbstractObjects.OTHER);
        break;
  
      case Opcodes.LCONST_0:
      case Opcodes.LCONST_1:
      case Opcodes.DCONST_0:
      case Opcodes.DCONST_1:
        pushValue(AbstractObjects.OTHER);
        pushValue(AbstractObjects.OTHER);
        break;
  
      case Opcodes.IALOAD: // remove 2 add 1
      case Opcodes.FALOAD: // remove 2 add 1
      case Opcodes.AALOAD: // remove 2 add 1
      case Opcodes.BALOAD: // remove 2 add 1
      case Opcodes.CALOAD: // remove 2 add 1
      case Opcodes.SALOAD: // remove 2 add 1
      case Opcodes.POP:
      case Opcodes.IADD:
      case Opcodes.FADD:
      case Opcodes.ISUB:
      case Opcodes.LSHL: // 3 before 2 after
      case Opcodes.LSHR: // 3 before 2 after
      case Opcodes.LUSHR: // 3 before 2 after
      case Opcodes.L2I: // 2 before 1 after
      case Opcodes.L2F: // 2 before 1 after
      case Opcodes.D2I: // 2 before 1 after
      case Opcodes.D2F: // 2 before 1 after
      case Opcodes.FSUB:
      case Opcodes.FMUL:
      case Opcodes.FDIV:
      case Opcodes.FREM:
      case Opcodes.FCMPL: // 2 before 1 after
      case Opcodes.FCMPG: // 2 before 1 after
      case Opcodes.IMUL:
      case Opcodes.IDIV:
      case Opcodes.IREM:
      case Opcodes.ISHL:
      case Opcodes.ISHR:
      case Opcodes.IUSHR:
      case Opcodes.IAND:
      case Opcodes.IOR:
      case Opcodes.IXOR:
      case Opcodes.MONITORENTER:
      case Opcodes.MONITOREXIT:
        popValue();
        break;
  
      case Opcodes.POP2:
      case Opcodes.LSUB:
      case Opcodes.LMUL:
      case Opcodes.LDIV:
      case Opcodes.LREM:
      case Opcodes.LADD:
      case Opcodes.LAND:
      case Opcodes.LOR:
      case Opcodes.LXOR:
      case Opcodes.DADD:
      case Opcodes.DMUL:
      case Opcodes.DSUB:
      case Opcodes.DDIV:
      case Opcodes.DREM:
        popValue();
        popValue();
        break;
  
      case Opcodes.IASTORE:
      case Opcodes.FASTORE:
      case Opcodes.AASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.SASTORE:
      case Opcodes.LCMP: // 4 before 1 after
      case Opcodes.DCMPL:
      case Opcodes.DCMPG:
        popValue();
        popValue();
        popValue();
        break;
  
      case Opcodes.LASTORE:
      case Opcodes.DASTORE:
        popValue();
        popValue();
        popValue();
        popValue();
        break;
  
      case Opcodes.DUP:
        pushValue(peekValue());
        break;
  
      case Opcodes.DUP_X1:
        s = stackFrame.size();
        stackFrame.add(s - 2, stackFrame.get(s - 1));
        break;
  
      case Opcodes.DUP_X2:
        s = stackFrame.size();
        stackFrame.add(s - 3, stackFrame.get(s - 1));
        break;
  
      case Opcodes.DUP2:
        s = stackFrame.size();
        stackFrame.add(s - 2, stackFrame.get(s - 1));
        stackFrame.add(s - 2, stackFrame.get(s - 1));
        break;
  
      case Opcodes.DUP2_X1:
        s = stackFrame.size();
        stackFrame.add(s - 3, stackFrame.get(s - 1));
        stackFrame.add(s - 3, stackFrame.get(s - 1));
        break;
  
      case Opcodes.DUP2_X2:
        s = stackFrame.size();
        stackFrame.add(s - 4, stackFrame.get(s - 1));
        stackFrame.add(s - 4, stackFrame.get(s - 1));
        break;
  
      case Opcodes.SWAP:
        s = stackFrame.size();
        stackFrame.add(s - 2, stackFrame.get(s - 1));
        stackFrame.remove(s);
        break;
      }
    }
  }

  public void visitIntInsn(final int opcode, final int operand) {
    if (!triggered) {
      if (opcode != Opcodes.NEWARRAY) {
        pushValue(AbstractObjects.OTHER);
      }
    }
  }

  public void visitJumpInsn(final int opcode, final Label label) {
    if (!triggered) {
      switch (opcode) {
      case Opcodes.IFEQ:
      case Opcodes.IFNE:
      case Opcodes.IFLT:
      case Opcodes.IFGE:
      case Opcodes.IFGT:
      case Opcodes.IFLE:
      case Opcodes.IFNULL:
      case Opcodes.IFNONNULL:
        popValue();
        break;
  
      case Opcodes.IF_ICMPEQ:
      case Opcodes.IF_ICMPNE:
      case Opcodes.IF_ICMPLT:
      case Opcodes.IF_ICMPGE:
      case Opcodes.IF_ICMPGT:
      case Opcodes.IF_ICMPLE:
      case Opcodes.IF_ACMPEQ:
      case Opcodes.IF_ACMPNE:
        popValue();
        popValue();
        break;
  
      case Opcodes.JSR:
        pushValue(AbstractObjects.OTHER);
        break;
      }
      addBranch(label);
    }
  }
  
  public void visitLabel(final Label label) {
    if (!triggered) {
      final List<AbstractObjects> frame = branchMap.get(label);
      if (frame != null) {
        stackFrame = frame;
        branchMap.remove(label);
      }
    }
  }

  public void visitLdcInsn(final Object cst) {
    if (!triggered) {
      pushValue(AbstractObjects.OTHER);
      if (cst instanceof Double || cst instanceof Long) {
        pushValue(AbstractObjects.OTHER);
      }
    }
  }

  public void visitLineNumber(final int line, final Label start) {
    // Not interesting
  }

  public void visitLocalVariable(
      final String name, final String desc, final String signature,
      final Label start, final Label end, final int index) {
    // Not interesting
  }

  public void visitLookupSwitchInsn(
      final Label dflt, final int[] keys, final Label[] labels) {
    if (!triggered) {
      popValue();
      addBranches(dflt, labels);
    }
  }

  public void visitMaxs(final int maxStack, final int maxLocals) {
    // Not interesting
  }

  public void visitMethodInsn(
      final int opcode, final String owner, final String name, final String desc) {
    if (!triggered) {
      final Type[] types = Type.getArgumentTypes(desc);
      for (int i = 0; i < types.length; i++) {
        popValue();
        if (types[i].getSize() == 2) {
          popValue();
        }
      }
      switch (opcode) {
      // case INVOKESTATIC:
      // break;
  
      case Opcodes.INVOKEINTERFACE:
      case Opcodes.INVOKEVIRTUAL:
        popValue(); // objectref
        break;
  
      case Opcodes.INVOKESPECIAL:
        Object type = popValue(); // objectref
        if (type == AbstractObjects.THIS) {
          triggered = true;
          callback.superConstructorCalled();
        }
        break;
      }
  
      final Type returnType = Type.getReturnType(desc);
      if (returnType != Type.VOID_TYPE) {
        pushValue(AbstractObjects.OTHER);
        if (returnType.getSize() == 2) {
          pushValue(AbstractObjects.OTHER);
        }
      }
    }
  }
  
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    if (!triggered) {
      for (int i = 0; i < dims; i++) {
        popValue();
      }
      pushValue(AbstractObjects.OTHER);
    }
  }

  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String desc, final boolean visible) {
    // Not interesting
    return null;
  }

  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label[] labels) {
    if (!triggered) {
      popValue();
      addBranches(dflt, labels);
    }
  }

  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler,
      final String type) {
    // Not interesting
  }

  public void visitTypeInsn(final int opcode, final String type) {
    if (!triggered) {
      // ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack
      if (opcode == Opcodes.NEW) {
        pushValue(AbstractObjects.OTHER);
      }
    }
  }

  public void visitVarInsn(final int opcode, final int var) {
    if (!triggered) {
      switch (opcode) {
      case Opcodes.ILOAD:
      case Opcodes.FLOAD:
        pushValue(AbstractObjects.OTHER);
        break;
      case Opcodes.LLOAD:
      case Opcodes.DLOAD:
        pushValue(AbstractObjects.OTHER);
        pushValue(AbstractObjects.OTHER);
        break;
      case Opcodes.ALOAD:
        pushValue(var == 0 ? AbstractObjects.THIS : AbstractObjects.OTHER);
        break;
      case Opcodes.ASTORE:
      case Opcodes.ISTORE:
      case Opcodes.FSTORE:
        popValue();
        break;
      case Opcodes.LSTORE:
      case Opcodes.DSTORE:
        popValue();
        popValue();
        break;
      }
    }
  }
}
