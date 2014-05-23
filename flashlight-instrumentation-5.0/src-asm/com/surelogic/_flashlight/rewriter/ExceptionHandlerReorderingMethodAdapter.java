package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class buffers all the "visit" calls until {@link #visitEnd()} is called,
 * at which point all the calls are forwarded to the underlying
 * {@link MethodVisitor} in the order they were received, with the exception of
 * calls to {@link #visitTryCatchBlock(Label, Label, Label, String)}. All calls
 * to {code visitTryCatchBlock} are made immediately after the call to
 * {@link #visitCode()} is forwarded.
 * 
 * <p>
 * The purpose of this class is to allow more control over the order of
 * exception handlers in the generated method bytecode. The order of handlers is
 * significant at runtime, but ASM doesn't allow control over their order: they
 * are output in the order they are seen via calls to {@code visitTryCatchBlock}
 * . The problem is that we often generate an exception handler that must come
 * before another handler <em>after</em> the second one has already been output.
 * So this class allows the generation of handlers that come at the start of the
 * list or that go at the end of the list. Specifically an exception handler can
 * be output using three methods:
 * 
 * <dl>
 * <dt>{@link #visitTryCatchBlock(Label, Label, Label, String)}
 * <dd>This is the normal {@code MethodVisitor} method, and the exception
 * handler is output in the order it was received with respect to other calls to
 * {@code visitTryCatchBlock}.
 * 
 * <dt>{@link #prependTryCatchBlock(Label, Label, Label, String)}
 * <dd>Adds an exception handler to the front of the list of exception handlers.
 * 
 * <dt>{@link #appendTryCatchBlock(Label, Label, Label, String)}
 * <dd>Adds an exception handler to the end of the list of exception handlers.
 * </dl>
 * 
 * <p>
 * Exception handlers are output to the underlying {@code MethodVisitor} with
 * the following guarantees:
 * 
 * <ul>
 * <li>All exception handlers are output after {@code visitCode} is called, but
 * before any other "visit" method is called.
 * 
 * <li>All handlers added using {@code prependTryCatchBlock} are output before
 * any other exception handlers. The relative order of handlers added using
 * {@code prependTryCatchBlock} is not guaranteed.
 * 
 * <li>All handlers added using {@code visitTryCatchBlock} are output after the
 * handlers added using {@code prependTryCatchBlock} and before any handlers
 * added using {@code appendTryCatchBlock}. The handlers are output in the order
 * they are received.
 * 
 * <li>All handlers added using {@code appendTryCatchBlock} are output after the
 * handlers added using {@code visitTryCatchBlock}. The relative order of
 * handlers added using {@code appendTryCatchBlock} is not guaranteed.
 * </ul>
 * 
 * <p>
 * This class supports the needs of the Flashlight instrumentation in the
 * following ways. We only add exception handlers to support try&mdash;finally
 * blocks. We only either add these blocks around a single opcode, or around the
 * entire method body. In the case of surrounding a single opcode, we need to
 * prepend the exception handler to the list of exceptions. In the case of
 * surrounding the entire method body we need to append the exception handler to
 * the list of exceptions.
 */
final class ExceptionHandlerReorderingMethodAdapter extends MethodVisitor {
	private static final String[] OPCODES = new String[] { "NOP",
			"ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2",
			"ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1",
			"FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1",
			"BIPUSH", "SIPUSH", "LDC", "LDC_W", "LDC2_W", "ILOAD", "LLOAD",
			"FLOAD", "DLOAD", "ALOAD", "ILOAD_0", "ILOAD_1", "ILOAD_2",
			"ILOAD_3", "LLOAD_0", "LLOAD_1", "LLOAD_2", "LLOAD_3", "FLOAD_0",
			"FLOAD_1", "FLOAD_2", "FLOAD_3", "DLOAD_0", "DLOAD_1", "DLOAD_2",
			"DLOAD_3", "ALOAD_0", "ALOAD_1", "ALOAD_2", "ALOAD_3", "IALOAD",
			"LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD",
			"SALOAD", "ISTORE", "LSTORE", "FSTORE", "DSTORE", "ASTORE",
			"ISTORE_0", "ISTORE_1", "ISTORE_2", "ISTORE_3", "LSTORE_0",
			"LSTORE_1", "LSTORE_2", "LSTORE_3", "FSTORE_0", "FSTORE_1",
			"FSTORE_2", "FSTORE_3", "DSTORE_0", "DSTORE_1", "DSTORE_2",
			"DSTORE_3", "ASTORE_0", "ASTORE_1", "ASTORE_2", "ASTORE_3",
			"IASTORE", "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE",
			"CASTORE", "SASTORE", "POP", "POP2", "DUP", "DUP_X1", "DUP_X2",
			"DUP2", "DUP2_X1", "DUP2_X2", "SWAP", "IADD", "LADD", "FADD",
			"DADD", "ISUB", "LSUB", "FSUB", "DSUB", "IMUL", "LMUL", "FMUL",
			"DMUL", "IDIV", "LDIV", "FDIV", "DDIV", "IREM", "LREM", "FREM",
			"DREM", "INEG", "LNEG", "FNEG", "DNEG", "ISHL", "LSHL", "ISHR",
			"LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR", "IXOR",
			"LXOR", "IINC", "I2L", "I2F", "I2D", "L2I", "L2F", "L2D", "F2I",
			"F2L", "F2D", "D2I", "D2L", "D2F", "I2B", "I2C", "I2S", "LCMP",
			"FCMPL", "FCMPG", "DCMPL", "DCMPG", "IFEQ", "IFNE", "IFLT", "IFGE",
			"IFGT", "IFLE", "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE",
			"IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR",
			"RET", "TABLESWITCH", "LOOKUPSWITCH", "IRETURN", "LRETURN",
			"FRETURN", "DRETURN", "ARETURN", "RETURN", "GETSTATIC",
			"PUTSTATIC", "GETFIELD", "PUTFIELD", "INVOKEVIRTUAL",
			"INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE",
			"INVOKEDYNAMIC", "NEW", "NEWARRAY", "ANEWARRAY", "ARRAYLENGTH",
			"ATHROW", "CHECKCAST", "INSTANCEOF", "MONITORENTER", "MONITOREXIT",
			"WIDE", "MULTIANEWARRAY", "IFNULL", "IFNONNULL", "GOTO_W", "JSR_W" };

	/**
	 * The list of memoized method calls. Does not contain try catch blocks.
	 */
	private final List<Memo> memoizedCalls = new ArrayList<Memo>();

	/**
	 * The list of exception handlers we get from calls to
	 * {@link #visitTryCatchBlock(Label, Label, Label, String)}.
	 */
	private final List<TryCatchBlockMemo> exceptionHandlers = new ArrayList<TryCatchBlockMemo>();

	/**
	 * The exception handlers to prepend to the front of the list of handlers.
	 */
	private final List<TryCatchBlockMemo> prefix = new ArrayList<TryCatchBlockMemo>();

	/**
	 * The exception handlers to append to the end of the list of handlers.
	 */
	private final List<TryCatchBlockMemo> postfix = new ArrayList<TryCatchBlockMemo>();

	public ExceptionHandlerReorderingMethodAdapter(final MethodVisitor mv) {
		super(Opcodes.ASM4, mv);
	}

	public void prependTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		prefix.add(new TryCatchBlockMemo(start, end, handler, type));
	}

	public void appendTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		postfix.add(new TryCatchBlockMemo(start, end, handler, type));
	}

	@Override
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
		for (final Memo memo : memoizedCalls) {
			memo.forward(mv);
		}

		// Done
		mv.visitEnd();
	}

	public void dump(final PrintWriter pw) {
		// Output all the exception handlers first
		for (final TryCatchBlockMemo tcb : prefix) {
			pw.println(tcb);
		}
		for (final TryCatchBlockMemo tcb : exceptionHandlers) {
			pw.println(tcb);
		}
		for (final TryCatchBlockMemo tcb : postfix) {
			pw.println(tcb);
		}

		// Output all the buffered instructions
		for (final Memo memo : memoizedCalls) {
			pw.println(memo);
		}
		pw.flush();
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		memoizedCalls.add(new FieldInsnMemo(opcode, owner, name, desc));
	}

	@Override
	public void visitFrame(final int type, final int nLocal,
			final Object[] local, final int nStack, final Object[] stack) {
		memoizedCalls.add(new FrameMemo(type, nLocal, local, nStack, stack));
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		memoizedCalls.add(new IncInsnMemo(var, increment));
	}

	@Override
	public void visitInsn(final int opcode) {
		memoizedCalls.add(new InsnMemo(opcode));
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		memoizedCalls.add(new IntInsnMemo(opcode, operand));
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		memoizedCalls.add(new JumpInsnMemo(opcode, label));
	}

	@Override
	public void visitLabel(final Label label) {
		memoizedCalls.add(new LabelMemo(label));
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		memoizedCalls.add(new LdcInsnMemo(cst));
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		memoizedCalls.add(new LineNumberMemo(line, start));
	}

	@Override
	public void visitLocalVariable(final String name, final String desc,
			final String signature, final Label start, final Label end,
			final int index) {
		memoizedCalls.add(new LocalVariableMemo(name, desc, signature, start,
				end, index));
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		memoizedCalls.add(new LookupSwitchInsn(dflt, keys, labels));
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		memoizedCalls.add(new MaxsMemo(maxStack, maxLocals));
	}

  @Override
  public void visitInvokeDynamicInsn(final String name, final String desc,
      final Handle bsm, final Object... bsmArgs) {
    memoizedCalls.add(new InvokeDynamicMemo(name, desc, bsm, bsmArgs));
  }

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc) {
		memoizedCalls.add(new MethodInsnMemo(opcode, owner, name, desc));
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		memoizedCalls.add(new MultiANewArrayInsnMemo(desc, dims));
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(final int parameter,
			final String desc, final boolean visible) {
		return mv.visitParameterAnnotation(parameter, desc, visible);
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max,
			final Label dflt, final Label... labels) {
		memoizedCalls.add(new TableSwitchInsnMemo(min, max, dflt, labels));
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		exceptionHandlers.add(new TryCatchBlockMemo(start, end, handler, type));
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		memoizedCalls.add(new TypeInsnMemo(opcode, type));
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		memoizedCalls.add(new VarInsnMemo(opcode, var));
	}

	
	
	private static interface Memo {
		public void forward(MethodVisitor mv);
	}
	
	private static abstract class OpcodeMemo implements Memo {
	  protected final int opcode;
	  
	  protected OpcodeMemo(final int opcode) {
	    this.opcode = opcode;
	  }
	  
	  @Override
	  public final String toString() {
	    return OPCODES[opcode] + restOfToString();
	  }
	  
	  protected String restOfToString() { return ""; }
	}

	private static final class FieldInsnMemo extends OpcodeMemo {
		private final String owner;
		private final String name;
		private final String desc;

		public FieldInsnMemo(final int opcode, final String owner,
				final String name, final String desc) {
		  super(opcode);
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		protected String restOfToString() {
			return " " + owner + " " + name + " " + desc;
		}
	}

	private static final class FrameMemo implements Memo {
		private final int type;
		private final int nLocal;
		private final Object[] local;
		private final int nStack;
		private final Object[] stack;

		public FrameMemo(final int type, final int nLocal,
				final Object[] local, final int nStack, final Object[] stack) {
			this.type = type;
			this.nLocal = nLocal;
			this.local = local;
			this.nStack = nStack;
			this.stack = stack;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitFrame(type, nLocal, local, nStack, stack);
		}

		@Override
		public String toString() {
			return "frame";
		}
	}

	private static final class IncInsnMemo implements Memo {
		private final int var;
		private final int increment;

		public IncInsnMemo(final int var, final int increment) {
			this.var = var;
			this.increment = increment;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitIincInsn(var, increment);
		}

		@Override
		public String toString() {
			return "INC " + var + " " + increment;
		}
	}

	private static final class InsnMemo extends OpcodeMemo {
		public InsnMemo(final int opcode) {
		  super(opcode);
		}

		public void forward(final MethodVisitor mv) {
			mv.visitInsn(opcode);
		}
	}

	private static final class IntInsnMemo extends OpcodeMemo {
		private final int operand;

		public IntInsnMemo(final int opcode, final int operand) {
			super(opcode);
			this.operand = operand;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitIntInsn(opcode, operand);
		}

		@Override
		protected String restOfToString() {
			return " " + operand;
		}
	}

	private static final class JumpInsnMemo extends OpcodeMemo {
		private final Label label;

		public JumpInsnMemo(final int opcode, final Label label) {
			super(opcode);
			this.label = label;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitJumpInsn(opcode, label);
		}

		@Override
		protected String restOfToString() {
			return " " + label;
		}
	}

	private static final class LabelMemo implements Memo {
		private final Label label;

		public LabelMemo(final Label label) {
			this.label = label;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitLabel(label);
		}

		@Override
		public String toString() {
			return label.toString() + ":";
		}
	}

	private static final class LdcInsnMemo implements Memo {
		private final Object cst;

		public LdcInsnMemo(final Object cst) {
			this.cst = cst;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitLdcInsn(cst);
		}

		@Override
		public String toString() {
			return "LDC " + cst;
		}
	}

	private static final class LineNumberMemo implements Memo {
		private final int line;
		private final Label start;

		public LineNumberMemo(final int line, final Label start) {
			this.line = line;
			this.start = start;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitLineNumber(line, start);
		}

		@Override
		public String toString() {
			return "Line " + line;
		}
	}

	private static final class LocalVariableMemo implements Memo {
		private final String name;
		private final String desc;
		private final String signature;
		private final Label start;
		private final Label end;
		private final int index;

		public LocalVariableMemo(final String name, final String desc,
				final String signature, final Label start, final Label end,
				final int index) {
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

		@Override
		public String toString() {
			return "var";
		}
	}

	private static final class LookupSwitchInsn implements Memo {
		private final Label dflt;
		private final int[] keys;
		private final Label[] labels;

		public LookupSwitchInsn(final Label dflt, final int[] keys,
				final Label[] labels) {
			this.dflt = dflt;
			this.keys = keys;
			this.labels = labels;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitLookupSwitchInsn(dflt, keys, labels);
		}

		@Override
		public String toString() {
			return "LOOKUPSWITCH " + dflt + " " + keys + " " + labels;
		}
	}

	private static final class MaxsMemo implements Memo {
		private final int maxStack;
		private final int maxLocals;

		public MaxsMemo(final int maxStack, final int maxLocals) {
			this.maxStack = maxStack;
			this.maxLocals = maxLocals;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitMaxs(maxStack, maxLocals);
		}

		@Override
		public String toString() {
			return "maxs";
		}
	}

	private static final class InvokeDynamicMemo implements Memo {
	  private final String name;
	  private final String desc;
	  private final Handle bsm;
	  private final Object[] bsmArgs;
	  
	  public InvokeDynamicMemo(final String name, final String desc,
	      final Handle bsm, final Object... bsmArgs) {
	    this.name = name;
	    this.desc = desc;
	    this.bsm = bsm;
	    this.bsmArgs = bsmArgs;
	  }
	  
	  public void forward(final MethodVisitor mv) {
	    mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	  }
	  
	  @Override
	  public String toString() {
	    return "INVOKEDYNAMIC " + name + " " + desc + " " + bsm + " " + bsmArgs;
	  }
	}
	

//  @Override
//  public void visitInvokeDynamicInsn(final String name, final String desc,
//      final Handle bsm, final Object... bsmArgs) {
//    // TODO: Something special here.  For now, just forward
//    mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
//  }

	private static final class MethodInsnMemo extends OpcodeMemo {
		private final String owner;
		private final String name;
		private final String desc;

		public MethodInsnMemo(final int opcode, final String owner,
				final String name, final String desc) {
		  super(opcode);
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitMethodInsn(opcode, owner, name, desc);
		}

		@Override
		protected String restOfToString() {
			return " " + owner + " " + name + " " + desc;
		}
	}

	private static final class MultiANewArrayInsnMemo implements Memo {
		private final String desc;
		private final int dims;

		public MultiANewArrayInsnMemo(final String desc, final int dims) {
			this.desc = desc;
			this.dims = dims;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitMultiANewArrayInsn(desc, dims);
		}

		@Override
		public String toString() {
			return "MULTIANEWARRAY " + desc + " " + dims;
		}
	}

	private static final class TableSwitchInsnMemo implements Memo {
		private final int min;
		private final int max;
		private final Label dflt;
		private final Label[] labels;

		public TableSwitchInsnMemo(final int min, final int max,
				final Label dflt, final Label[] labels) {
			this.min = min;
			this.max = max;
			this.dflt = dflt;
			this.labels = labels;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitTableSwitchInsn(min, max, dflt, labels);
		}

		@Override
		public String toString() {
			return "TABLESWITCH " + min + " " + max + " " + dflt + " " + labels;
		}
	}

	private static final class TryCatchBlockMemo implements Memo {
		private final Label start;
		private final Label end;
		private final Label handler;
		private final String type;

		public TryCatchBlockMemo(final Label start, final Label end,
				final Label handler, final String type) {
			this.start = start;
			this.end = end;
			this.handler = handler;
			this.type = type;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitTryCatchBlock(start, end, handler, type);
		}

		@Override
		public String toString() {
			return "TRY" + start + " " + end + " " + handler + " " + type;
		}
	}

	private static final class TypeInsnMemo extends OpcodeMemo {
		private final String type;

		public TypeInsnMemo(final int opcode, final String type) {
		  super(opcode);
			this.type = type;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitTypeInsn(opcode, type);
		}

		@Override
		protected String restOfToString() {
			return " " + type;
		}
	}

	private static final class VarInsnMemo extends OpcodeMemo {
		private final int var;

		public VarInsnMemo(final int opcode, final int var) {
			super(opcode);
			this.var = var;
		}

		public void forward(final MethodVisitor mv) {
			mv.visitVarInsn(opcode, var);
		}

		@Override
		protected String restOfToString() {
			return " " + var;
		}
	}
}
