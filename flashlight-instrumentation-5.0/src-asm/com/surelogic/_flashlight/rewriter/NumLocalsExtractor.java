package com.surelogic._flashlight.rewriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Class visitor makes a scan over the methods in the class to extract the debug
 * information for local variables and logs the number of local variables
 * allocated for each method.
 */
final class NumLocalsExtractor extends ClassVisitor {
	private final Map<String, Integer> method2numLocals = new HashMap<String, Integer>();

	public NumLocalsExtractor() {
		super(Opcodes.ASM4);
	}

	public Map<String, Integer> getNumLocalsMap() {
		return Collections.unmodifiableMap(method2numLocals);
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		// don't care
	}

	@Override
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		// don't care
		return null;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		// Don't care about
		return null;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		// Don't care about
	}

	@Override
	public void visitEnd() {
		// Don't care about
	}

	@Override
	public void visitInnerClass(final String name, final String outerName,
			final String innerName, final int access) {
		// Don't care about
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		final String key = name + desc;

		return new MethodVisitor(Opcodes.ASM4) {
			@Override
			public AnnotationVisitor visitAnnotation(final String desc,
					final boolean visible) {
				// don't care
				return null;
			}

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				// don't care
				return null;
			}

			@Override
			public void visitAttribute(final Attribute attr) {
				// don't care
			}

			@Override
			public void visitCode() {
				// don't care
			}

			@Override
			public void visitEnd() {
				// don't care
			}

			@Override
			public void visitFieldInsn(final int opcode, final String owner,
					final String name, final String desc) {
				// don't care
			}

			@Override
			public void visitFrame(final int type, final int local,
					final Object[] local2, final int stack,
					final Object[] stack2) {
				// don't care
			}

			@Override
			public void visitIincInsn(final int var, final int increment) {
				// don't care
			}

			@Override
			public void visitInsn(final int opcode) {
				// don't care
			}

			@Override
			public void visitIntInsn(final int opcode, final int operand) {
				// don't care
			}

			@Override
			public void visitJumpInsn(final int opcode, final Label label) {
				// don't care
			}

			@Override
			public void visitLabel(final Label label) {
				// don't care
			}

			@Override
			public void visitLdcInsn(final Object cst) {
				// don't care
			}

			@Override
			public void visitLineNumber(final int line, final Label start) {
				// don't care
			}

			@Override
			public void visitLocalVariable(final String name,
					final String desc, final String signature,
					final Label start, final Label end, final int index) {
				// don't care
			}

			@Override
			public void visitLookupSwitchInsn(final Label dflt,
					final int[] keys, final Label[] labels) {
				// don't care
			}

			@Override
			public void visitMaxs(final int maxStack, final int maxLocals) {
				method2numLocals.put(key, Integer.valueOf(maxLocals));
			}

			@Override
			public void visitMethodInsn(final int opcode, final String owner,
					final String name, final String desc) {
				// don't care
			}

			@Override
			public void visitMultiANewArrayInsn(final String desc,
					final int dims) {
				// don't care
			}

			@Override
			public AnnotationVisitor visitParameterAnnotation(
					final int parameter, final String desc,
					final boolean visible) {
				// don't care
				return null;
			}

			@Override
			public void visitTableSwitchInsn(final int min, final int max,
					final Label dflt, final Label... labels) {
				// don't care
			}

			@Override
			public void visitTryCatchBlock(final Label start, final Label end,
					final Label handler, final String type) {
				// don't care
			}

			@Override
			public void visitTypeInsn(final int opcode, final String type) {
				// don't care
			}

			@Override
			public void visitVarInsn(final int opcode, final int var) {
				// don't care
			}
		};
	}

	@Override
	public void visitOuterClass(final String owner, final String name,
			final String desc) {
		// Don't care about
	}

	@Override
	public void visitSource(final String source, final String debug) {
		// Don't care about
	}
}
