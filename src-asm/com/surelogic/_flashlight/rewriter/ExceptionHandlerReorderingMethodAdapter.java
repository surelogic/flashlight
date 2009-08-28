package com.surelogic._flashlight.rewriter;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * This class buffers all the "visit" calls until {@link #visitEnd()} is called,
 * at which point all the calls are forwarded to the underlying {@link MethodVisitor}
 * in the order they were received, with the exception of calls to
 * {@link #visitTryCatchBlock(Label, Label, Label, String)}.  All calls to 
 * {code visitTryCatchBlock} are made immediately after the call to
 * {@link #visitCode()} is forwarded.  
 * 
 * <p>The purpose of this class is to allow more control over the order of 
 * exception handlers in the generated method bytecode.  The order of handlers
 * is significant at runtime, but ASM doesn't allow control over there order: 
 * they are output in the order they are seen via calls to {@code visitTryCatchBlock}.
 * The problem is that we often generate an exception handler that must come before
 * another handler <em>after</em> the second one has already been output.  So 
 * this class allows the generation of handlers that come at the start of the
 * list or that go at the end of the list.  Specifically an exception handler
 * can be output using three methods:
 * 
 * <dl>
 *   <dt>{@link #visitTryCatchBlock(Label, Label, Label, String)}
 *   <dd>This is the normal {@code MethodVisitor} method, and the exception 
 *   handler is output in the order it was received with respect to other
 *   calls to {@code visitTryCatchBlock}.
 *   
 *   <dt>{@link #prependTryCatchBlock(Label, Label, Label, String)}
 *   <dd>Adds an exception handler to the front of the list of exception handlers.
 *   
 *   <dt>{@link #appendTryCatchBlock(Label, Label, Label, String)}
 *   <dd>Adds an exception handler to the end of the list of exception handlers.
 * </dl>
 * 
 * <p>Exception handlers are output to the underlying {@code MethodVisitor} with
 * the following guarantees:
 * 
 * <ul>
 *   <li>All exception handlers are output after {@code visitCode} is called,
 *   but before any other "visit" method is called.
 *   
 *   <li>All handlers added using {@code prependTryCatchBlock} are output
 *   before any other exception handlers.  The relative order of handlers 
 *   added using {@code prependTryCatchBlock} is not guaranteed.
 *   
 *   <li>All handlers added using {@code visitTryCatchBlock} are output
 *   after the handlers added using {@code prependTryCatchBlock} and before
 *   any handlers added using {@code appendTryCatchBlock}.  The handlers are
 *   output in the order they are received.
 *   
 *   <li>All handlers added using {@code appendTryCatchBlock} are output
 *   after the handlers added using {@code visitTryCatchBlock}.
 *   The relative order of handlers 
 *   added using {@code appendTryCatchBlock} is not guaranteed.
 * </ul>
 *
 * <p>This class supports the needs of the Flashlight instrumentation in the 
 * following ways.  We only add exception handlers to support try&mdash;finally
 * blocks.  We only either add these blocks around a single opcode, or around
 * the entire method body.  In the case of surrounding a single opcode, we
 * need to prepend the exception handler to the list of exceptions.  In the case
 * of surrounding the entire method body we need to append the exception handler
 * to the list of exceptions. 
 */
final class ExceptionHandlerReorderingMethodAdapter implements MethodVisitor {
  /**
   * The visitor to adapt.
   */
  private final MethodVisitor mv;
  
  /**
   * The list of memoized method calls.  Does not contain try catch blocks.
   */
  private final List<MethodMemo> memoizedCalls = new ArrayList<MethodMemo>();
  
  /**
   * The list of exception handlers we get from calls to 
   * {@link #visitTryCatchBlock(Label, Label, Label, String)}.
   */
  private final List<TryCatchBlockMemo> exceptionHandlers = new ArrayList<TryCatchBlockMemo>();
  
  /**
   * The exception handlers to prepend to the front of the list
   * of handlers.
   */
  private final List<TryCatchBlockMemo> prefix = new ArrayList<TryCatchBlockMemo>();
  
  /**
   * The exception handlers to append to the end of the list
   * of handlers.
   */
  private final List<TryCatchBlockMemo> postfix = new ArrayList<TryCatchBlockMemo>();
  
  
  
  public ExceptionHandlerReorderingMethodAdapter(final MethodVisitor mv) {
    this.mv = mv;
  }

  
  

  public void prependTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    prefix.add(new TryCatchBlockMemo(start, end, handler, type));
  }

  public void appendTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    postfix.add(new TryCatchBlockMemo(start, end, handler, type));
  }

  
  
  public AnnotationVisitor visitAnnotation(
      final String desc, final boolean visible) {
    return mv.visitAnnotation(desc, visible);
  }

  public AnnotationVisitor visitAnnotationDefault() {
    return mv.visitAnnotationDefault();
  }

  public void visitAttribute(final Attribute attr) {
    mv.visitAttribute(attr);    
  }

  public void visitCode() {
    mv.visitCode();
  }

  public void visitEnd() {
    // Output all the exception handlers first
    for (final TryCatchBlockMemo tcb : prefix) {
      tcb.forward(mv);
    }
    for (final TryCatchBlockMemo tcb : exceptionHandlers) {
      tcb.forward(mv);
    }
    for (final TryCatchBlockMemo tcb : postfix) {
      tcb.forward(mv);
    }
    
    // Output all the buffered instructions
    for (final MethodMemo memo : memoizedCalls) {
      memo.forward(mv);
    }
    
    // Done
    mv.visitEnd();
  }

  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String desc) {
    memoizedCalls.add(new FieldInsnMemo(opcode, owner, name, desc));
  }

  public void visitFrame(
      final int type, final int nLocal, final Object[] local, final int nStack,
      final Object[] stack) {
    memoizedCalls.add(new FrameMemo(type, nLocal, local, nStack, stack));
  }

  public void visitIincInsn(final int var, final int increment) {
    memoizedCalls.add(new IncInsnMemo(var, increment));
  }

  public void visitInsn(final int opcode) {
    memoizedCalls.add(new InsnMemo(opcode));
  }

  public void visitIntInsn(final int opcode, final int operand) {
    memoizedCalls.add(new IntInsnMemo(opcode, operand));
  }

  public void visitJumpInsn(final int opcode, final Label label) {
    memoizedCalls.add(new JumpInsnMemo(opcode, label));
  }

  public void visitLabel(final Label label) {
    memoizedCalls.add(new LabelMemo(label));
  }

  public void visitLdcInsn(final Object cst) {
    memoizedCalls.add(new LdcInsnMemo(cst));
  }

  public void visitLineNumber(final int line, final Label start) {
    memoizedCalls.add(new LineNumberMemo(line, start));
  }

  public void visitLocalVariable(
      final String name, final String desc, final String signature,
      final Label start, final Label end, final int index) {
    memoizedCalls.add(
        new LocalVariableMemo(name, desc, signature, start, end, index));
  }

  public void visitLookupSwitchInsn(
      final Label dflt, final int[] keys, final Label[] labels) {
    memoizedCalls.add(new LookupSwitchInsn(dflt, keys, labels));
  }

  public void visitMaxs(final int maxStack, final int maxLocals) {
    memoizedCalls.add(new MaxsMemo(maxStack, maxLocals));
  }

  public void visitMethodInsn(
      final int opcode, final String owner, final String name, final String desc) {
    memoizedCalls.add(new MethodInsnMemo(opcode, owner, name, desc));
  }

  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    memoizedCalls.add(new MultiANewArrayInsnMemo(desc, dims));
  }

  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String desc, final boolean visible) {
    return mv.visitParameterAnnotation(parameter, desc, visible);
  }

  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label[] labels) {
    memoizedCalls.add(new TableSwitchInsnMemo(min, max, dflt, labels));
  }

  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    //System.out.println("***** normal handler: " + start + " " + end + " " + handler + " " + type);
    exceptionHandlers.add(new TryCatchBlockMemo(start, end, handler, type));
  }

  public void visitTypeInsn(final int opcode, final String type) {
    memoizedCalls.add(new TypeInsnMemo(opcode, type));
  }

  public void visitVarInsn(final int opcode, final int var) {
    memoizedCalls.add(new VarInsnMemo(opcode, var));
  }

  
  
  private static interface MethodMemo {
    public void forward(MethodVisitor mv);
  }
  
  private final class FieldInsnMemo implements MethodMemo {
    private final int opcode;
    private final String owner;
    private final String name;
    private final String desc;
    
    public FieldInsnMemo(
        final int opcode, final String owner, final String name, final String desc) {
      this.opcode = opcode;
      this.owner = owner;
      this.name = name;
      this.desc = desc;      
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitFieldInsn(opcode, owner, name, desc);
    }
  }
  
  private final class FrameMemo implements MethodMemo {
    private final int type;
    private final int nLocal;
    private final Object[] local;
    private final int nStack;
    private final Object[] stack;
    
    public FrameMemo(final int type, final int nLocal, final Object[] local,
        final int nStack, final Object[] stack) {
      this.type = type;
      this.nLocal = nLocal;
      this.local = local;
      this.nStack = nStack;
      this.stack = stack;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitFrame(type, nLocal, local, nStack, stack);
    }
  }
  
  private final class IncInsnMemo implements MethodMemo {
    private final int var;
    private final int increment;
    
    public IncInsnMemo(final int var, final int increment) {
      this.var = var;
      this.increment = increment;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitIincInsn(var, increment);
    }
  }
  
  private final class InsnMemo implements MethodMemo {
    private final  int opcode;
    
    public InsnMemo(final int opcode) {
      this.opcode = opcode;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitInsn(opcode);
    }
  }
  
  private final class IntInsnMemo implements MethodMemo {
    private final  int opcode;
    private final  int operand;
    
    public IntInsnMemo(final int opcode, final int operand) {
      this.opcode = opcode;
      this.operand = operand;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitIntInsn(opcode, operand);
    }
  }
  
  private final class JumpInsnMemo implements MethodMemo {
    private final  int opcode;
    private final Label label;
    
    public JumpInsnMemo(final int opcode, final Label label) {
      this.opcode = opcode;
      this.label = label;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitJumpInsn(opcode, label);
    }
  }
  
  private final class LabelMemo implements MethodMemo {
    private final Label label;
    
    public LabelMemo(final Label label) {
      this.label = label;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitLabel(label);
    }
  }
  
  private final class LdcInsnMemo implements MethodMemo {
    private final Object cst;
    
    public LdcInsnMemo(final Object cst) {
      this.cst = cst;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitLdcInsn(cst);
    }
  }
  
  private final class LineNumberMemo implements MethodMemo {
    private final int line;
    private final Label start;
    
    public LineNumberMemo(final int line, final Label start) {
      this.line = line;
      this.start = start;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitLineNumber(line, start);
    }
  }
  
  private final class LocalVariableMemo implements MethodMemo {
    private final String name;
    private final String desc;
    private final String signature;
    private final Label start;
    private final Label end;
    private final int index;
    
    public LocalVariableMemo(final String name, final String desc,
        final String signature, final Label start, final Label end, final int index) {
      this.name = name;
      this.desc = desc;
      this.signature = signature;
      this.start = start;
      this.end = end;
      this.index = index;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitLocalVariable(name, desc, signature, start, end, index);
    }
  }
  
  private final class LookupSwitchInsn implements MethodMemo {
    private final Label dflt;
    private final int[] keys;
    private final Label[] labels;
    
    public LookupSwitchInsn(
        final Label dflt, final int[] keys, final Label[] labels) {
      this.dflt = dflt;
      this.keys = keys;
      this.labels = labels;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitLookupSwitchInsn(dflt, keys, labels);
    }
  }
  
  private final class MaxsMemo implements MethodMemo {
    private final int maxStack;
    private final int maxLocals;
    
    public MaxsMemo(final int maxStack, final int maxLocals) {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitMaxs(maxStack, maxLocals);
    }
  }
  
  private final class MethodInsnMemo implements MethodMemo {
    private final int opcode;
    private final String owner;
    private final String name;
    private final String desc;
    
    public MethodInsnMemo(
        final int opcode, final String owner, final String name, final String desc) {
      this.opcode = opcode;
      this.owner = owner;
      this.name = name;
      this.desc = desc;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitMethodInsn(opcode, owner, name, desc);
    }
  }
  
  private final class MultiANewArrayInsnMemo implements MethodMemo {
    private final String desc;
    private final int dims;
    
    public MultiANewArrayInsnMemo(final String desc, final int dims) {
      this.desc = desc;
      this.dims = dims;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitMultiANewArrayInsn(desc, dims);
    }
  }
  
  private final class TableSwitchInsnMemo implements MethodMemo {
    private final int min;
    private final int max;
    private final Label dflt;
    private final Label[] labels;
    
    public TableSwitchInsnMemo(
        final int min, final int max, final Label dflt, final Label[] labels) {
      this.min = min;
      this.max = max;
      this.dflt = dflt;
      this.labels = labels;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitTableSwitchInsn(min, max, dflt, labels);
    }
  }
  
  private final class TryCatchBlockMemo implements MethodMemo {
    private final  Label start;
    private final  Label end;
    private final  Label handler;
    private final  String type;
    
    public TryCatchBlockMemo(final Label start, final Label end, final Label handler, final String type) {
      this.start = start;
      this.end = end;
      this.handler = handler;
      this.type = type;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitTryCatchBlock(start, end, handler, type);
    }
  }
  
  private final class TypeInsnMemo implements MethodMemo {
    private final  int opcode;
    private final  String type;
    
    public TypeInsnMemo(final int opcode, final String type) {
      this.opcode = opcode;
      this.type = type;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitTypeInsn(opcode, type);
    }
  }
  
  private final class VarInsnMemo implements MethodMemo {
    private final int opcode;
    private final  int var;
    
    public VarInsnMemo(final int opcode, final int var) {
      this.opcode = opcode;
      this.var = var;
    }
    
    public void forward(final MethodVisitor mv) {
      mv.visitVarInsn(opcode, var);
    }
  }
}
