package com.surelogic._flashlight.rewriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.config.Configuration;

/**
 * Visits a classfile and rewrites it to contain Flashlight instrumentation.
 * This is the second pass of the instrumentation process. The first pass is
 * implemented by {@link FieldCataloger}, and the results of the first pass are
 * provided to this pass as a {@link ClassAndFieldModel} instance. This class
 * can be used as the first and only pass by providing an empty class model. In
 * this case, all the field accesses will use reflection at runtime, which has
 * significant runtime costs.
 * 
 * <p>
 * Most of the real work is performed by instances of
 * {@link FlashlightMethodRewriter}.
 * 
 * @see FieldCataloger
 * @see ClassAndFieldModel
 * @see FlashlightMethodRewriter
 * @see Configuration
 */
final class FlashlightClassRewriter extends ClassVisitor {
	private static final String UNKNOWN_SOURCE_FILE = "<unknown>";

	private static final String CLASS_INITIALIZER = "<clinit>";
	private static final String CLASS_INITIALIZER_DESC = "()V";

	/**
	 * The maximum size in bytes that a method code section is allowed to be.
	 * Methods that end up larger than this after instrumentation are not
	 * instrumented at all.
	 */
	private static final int MAX_CODE_SIZE = 64 * 1024;

	
	
	/** Properties to control rewriting and instrumentation. */
	private final Configuration config;

	/** Messenger for status reports. */
	private final RewriteMessenger messenger;

	/** Is the current class file an interface? */
	private boolean isInterface;

	/** The name of the source file that contains the class being rewritten. */
	private String sourceFileName = UNKNOWN_SOURCE_FILE;

	/** The internal name of the class being rewritten. */
	private String classNameInternal;

	/** The fully qualified name of the class being rewritten. */
	private String classNameFullyQualified;

	/** The internal name of the superclass of the class being rewritten. */
	private String superClassInternal;

	/**
	 * Do we need to add a class initializer? If the class already had one, we
	 * modify it. Otherwise we need to add one.
	 */
	private boolean needsClassInitializer = true;

	/**
	 * Do we need to implement the IIdObject interface? This is set by the
	 * {@link #visit} method.
	 */
	private boolean mustImplementIIdObject = false;

	/**
	 * Do we need to fix the serialization "read" methods?
	 */
	private boolean mustFixReadObject = false;

	/**
	 * Does the original class implementation have a readObject method?
	 */
	private boolean hasReadObjectMethod = false;

	/**
	 * The wrapper methods that we need to generate to instrument calls to
	 * instance methods.
	 */
	private final Set<MethodCallWrapper> wrapperMethods = new TreeSet<MethodCallWrapper>(
			MethodCallWrapper.comparator);

	/**
	 * Table from method names to CodeSizeEvaluators. Need to look at this after
	 * the class has been visited to find overly long methods. The key is the
	 * method name + method description.
	 */
	private final Map<MethodIdentifier, CodeSizeEvaluator> methodSizes = new HashMap<MethodIdentifier, CodeSizeEvaluator>();

	/**
	 * The set of methods that should not be rewritten.
	 */
	private final Set<MethodIdentifier> methodsToIgnore;

	/**
	 * After the entire class has been visited this contains the names of all
	 * the methods that are oversized after instrumentation.
	 */
	private final Set<MethodIdentifier> oversizedMethods = new HashSet<MethodIdentifier>();

	/**
	 * The class and field model built during the first pass. This is used to
	 * obtain unique field identifiers. For a field that does not have a unique
	 * identifier in this model, we must use reflection at runtime.
	 */
	private final ClassAndFieldModel classModel;

	/**
	 * The set of methods that indirectly access aggregated state.
	 */
	private final IndirectAccessMethods accessMethods;

	/** Factory for unique call site identifiers */
	private final SiteIdFactory callSiteIdFactory;

	/**
	 * Mapping from (name + desc) to maxLocals. The orignal number of local
	 * variables for each method in the class.
	 */
	private final Map<String, Integer> method2numLocals;

	/**
	 * Did we output the special Flashlight attribute yet?
	 */
	private boolean wroteFlashlightAttribute = false;

	/**
	 * Create a new class rewriter.
	 * 
	 * @param conf
	 *            The configuration information for instrumentation.
	 * @param cv
	 *            The class visitor to delegate to.
	 * @param model
	 *            The class and field model to use.
	 * @param ignore
	 *            The set of methods that <em>should not</em> be instrumented.
	 *            This are determined by a prior attempt at instrumentation, and
	 *            would be obtained by calling {@link #getOversizedMethods()}.
	 */
	public FlashlightClassRewriter(final Configuration conf,
			final SiteIdFactory csif, final RewriteMessenger msg,
			final ClassVisitor cv, final ClassAndFieldModel model,
			final IndirectAccessMethods am,
			final Map<String, Integer> m2locals,
			final Set<MethodIdentifier> ignore) {
		super(Opcodes.ASM4, cv);
		config = conf;
		callSiteIdFactory = csif;
		messenger = msg;
		classModel = model;
		accessMethods = am;
		method2numLocals = m2locals;
		methodsToIgnore = ignore;
	}

	/**
	 * Get the names of those methods whose code size has become too large after
	 * instrumentation.
	 */
	public Set<MethodIdentifier> getOversizedMethods() {
		return Collections.unmodifiableSet(oversizedMethods);
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		classNameInternal = name;
		classNameFullyQualified = ClassNameUtil.internal2FullyQualified(name);
		superClassInternal = superName;

		/*
		 * We have to modify root classes to insert object id information. We
		 * only care about those classes that extend object, or those whose
		 * superclass is a class not being instrumented.
		 */
		final String[] newInterfaces;
		if (isInterface) { // Interface, leave alone
			newInterfaces = interfaces;
			mustImplementIIdObject = false;
		} else if (hasNoInstrumentedAncestor(superName)) {
			/*
			 * Class extends Object or a class that is not being instrumented.
			 * Add the IIdObject interface, and we need to add the methods to
			 * implement it.
			 */
			newInterfaces = new String[interfaces.length + 1];
			newInterfaces[0] = FlashlightNames.I_ID_OBJECT;
			System.arraycopy(interfaces, 0, newInterfaces, 1, interfaces.length);
			mustImplementIIdObject = true;

			// Now test for serializable (but ignore externaliable classes)
			try {
				if (classModel.implementsInterface(name,
						FlashlightNames.JAVA_IO_SERIALIZABLE)
						&& !classModel.implementsInterface(name,
								FlashlightNames.JAVA_IO_EXTERNALIZABLE)) {
					mustFixReadObject = true;
				}
			} catch (final ClassNotFoundException e) {
				messenger.verbose("In class " + classNameFullyQualified
						+ ": Couldn't find class " + e.getMissingClass() + ".");
			}
		} else {
			/* Class already has a parent that implements IIdObject */
			newInterfaces = interfaces;
			mustImplementIIdObject = false;
		}

		/*
		 * If the class extends from java.lang.Object, we change it to extend
		 * com.surelogic._flashlight.rewriter.runtime.IdObject.
		 * 
		 * We also have to sanitize the access bits. Some very old classfiles
		 * have the SUPER bit set together with the INTERFACE bit. This is
		 * illegal, but passes in old JVMs, and even under Java 6 in some
		 * circumstances. What we do know is that if the classfile version is
		 * java 5, and the SUPER and INTERFACE bits are both set, then the Java
		 * 6 JVM rejects the classfile. Because we make sure the classfile is at
		 * least java 5 version, we must fix the access bits as well. Once
		 * again, we can thank the crazy classfiles in the dbBenchmark example
		 * for uncovering this problem.
		 */
		final int newAccess = isInterface ? access & ~Opcodes.ACC_SUPER
				: access;
		cv.visit(version, newAccess, name, signature, superName, newInterfaces);
	}

	@Override
	public void visitSource(final String source, final String debug) {
		if (source != null) {
			sourceFileName = source;
		}
		cv.visitSource(source, debug);
	}

	@Override
	public void visitInnerClass(final String name, final String outerName,
			final String innerName, final int access) {
		addFlashlightAttribute();
		cv.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		addFlashlightAttribute();
		return cv.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		addFlashlightAttribute();

		final boolean isClassInit = name.equals(CLASS_INITIALIZER);
		if (isClassInit) {
			needsClassInitializer = false;
		}

		final boolean isReadObject = name.equals(FlashlightNames.READ_OBJECT
				.getName())
				&& desc.equals(FlashlightNames.READ_OBJECT.getDescriptor());
		hasReadObjectMethod |= isReadObject;

		final MethodIdentifier methodId = new MethodIdentifier(name, desc);
		if (methodsToIgnore.contains(methodId)) {
			return cv.visitMethod(access, name, desc, signature, exceptions);
		} else {
			final int newAccess;
			if (config.rewriteSynchronizedMethod) {
				newAccess = access & ~Opcodes.ACC_SYNCHRONIZED;
			} else {
				newAccess = access;
			}
			final MethodVisitor original = cv.visitMethod(newAccess, name,
					desc, signature, exceptions);
			final CodeSizeEvaluator cse = new CodeSizeEvaluator(original);
			methodSizes.put(methodId, cse);
			/*
			 * Get the number of locals in the original method. If the method is
			 * not found in the map, then the method is abstract and thus as 0
			 * local variables.
			 */
			final Integer numLocalsInteger = method2numLocals.get(name + desc);
			final int numLocals = numLocalsInteger == null ? 0
					: numLocalsInteger.intValue();
			return FlashlightMethodRewriter
					.create(access, name, desc, numLocals, cse, config,
							callSiteIdFactory, messenger, classModel,
							accessMethods, isInterface, mustImplementIIdObject,
							sourceFileName, classNameInternal,
							classNameFullyQualified, superClassInternal,
							wrapperMethods);
		}
	}

	@Override
	public void visitEnd() {
		// Insert the flashlight$phantomClassObject field
		final FieldVisitor fv = cv.visitField(
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_ACCESS,
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC, null,
				null);
		fv.visitEnd();

		// Add the flashlight phantom class object getter method
		if (!isInterface) {
			addPhantomClassObjectGetter();
		}

		// Add the class initializer if needed
		if (needsClassInitializer) {
			addClassInitializer();
		}

		// Add readObject() if needed
		if (mustFixReadObject && !hasReadObjectMethod) {
			addReadObjectMethod();
		}

		// Add the wrapper methods
		for (final MethodCallWrapper wrapper : wrapperMethods) {
			addWrapperMethod(wrapper);
		}

		/*
		 * Implement the IIdObject interface, if necessary. The constructors
		 * have already been modified by the method rewriter to initialize the
		 * flashlight$phantomObject field.
		 */
		if (mustImplementIIdObject) {
			// insert methods
			addIIdObjectFieldsAndMethods();
		}

		// Find any oversized methods
		for (final Map.Entry<MethodIdentifier, CodeSizeEvaluator> entry : methodSizes
				.entrySet()) {
			if (entry.getValue().getMaxSize() > MAX_CODE_SIZE) {
				final MethodIdentifier mid = entry.getKey();
				oversizedMethods.add(mid);
				messenger.warning("Instrumentation causes method "
						+ classNameFullyQualified + "." + mid.name + mid.desc
						+ " to be too large.");
			}
		}

		// Now we are done
		addFlashlightAttribute();
		cv.visitEnd();
	}

	private boolean hasNoInstrumentedAncestor(final String superName) {
		String currentName = superName;
		while (true) {
			// If we hit java.lang.Object, we are done: there are no
			// instrumented ancestors
			if (currentName.equals(FlashlightNames.JAVA_LANG_OBJECT)) {
				return true;
			}
			// If the ancestor is instrumented, we are done
			if (classModel.isInstrumentedClass(currentName)) {
				return false;
			}
			try {
				currentName = classModel.getClass(currentName).getSuperClass();
			} catch (final ClassNotFoundException e) {
				// If we don't know about the class, it is not being
				// instrumented.
				return false;
			}
		}
	}

	private void addPhantomClassObjectGetter() {
		// Mark as synthetic because it does not appear in the original source
		// code
		final MethodVisitor mv = cv.visitMethod(
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_GETTER_ACCESS,
				FlashlightNames
						.getPhantomClassObjectGetterName(classNameInternal),
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_GETTER_DESC,
				null, null);

		mv.visitCode();

		// read the phantom class object for this class
		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameInternal,
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
				FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC);

		// return
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitMaxs(1, 0);
		mv.visitEnd();
	}

	private void addClassInitializer() {
		/* Create a new <clinit> method to visit */
		final MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC,
				CLASS_INITIALIZER, CLASS_INITIALIZER_DESC, null, null);
		/*
		 * Proceed as if visitMethod() were called on us, and simulate the
		 * method traversal through the rewriter visitor.
		 */
		final MethodVisitor rewriter_mv = FlashlightMethodRewriter.create(
				Opcodes.ACC_STATIC, CLASS_INITIALIZER, CLASS_INITIALIZER_DESC,
				0, mv, config, callSiteIdFactory, messenger, classModel,
				accessMethods, isInterface, mustImplementIIdObject,
				sourceFileName, classNameInternal, classNameFullyQualified,
				superClassInternal, wrapperMethods);
		rewriter_mv.visitCode(); // start code section
		rewriter_mv.visitInsn(Opcodes.RETURN); // empty method, just return
		rewriter_mv.visitMaxs(0, 0); // Don't need any stack or variables
		rewriter_mv.visitEnd(); // end of method
	}

	private void addReadObjectMethod() {
		// Mark as synthetic because it does not appear in the original source
		// code
		final MethodVisitor mv = cv.visitMethod(
				FlashlightNames.READ_OBJECT_ACCESS | Opcodes.ACC_SYNTHETIC,
				FlashlightNames.READ_OBJECT.getName(),
				FlashlightNames.READ_OBJECT.getDescriptor(), null,
				new String[] { FlashlightNames.JAVA_IO_IOEXCEPTION,
						FlashlightNames.JAVA_LANG_CLASSNOTFOUNDEXCEPTION });

		mv.visitCode();

		// Init the phantomObject field
		ByteCodeUtils.initializePhantomObject(mv, config, classNameInternal);

		// Call ObjectInputStream.defaultReadObject()
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/ObjectInputStream",
				"defaultReadObject", "()V");

		// Insert code to generate write events for the deserialized fields
		ByteCodeUtils.insertPostDeserializationFieldWrites(mv, config,
				classNameInternal, FlashlightNames.SYNTHETIC_METHOD_SITE_ID, 2,
				3, 4);

		// Return
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(6, 5);
		mv.visitEnd();
	}

	private void addIIdObjectFieldsAndMethods() {
		// Insert the flashlight$phantomObject field
		final FieldVisitor fv = cv.visitField(
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT_ACCESS,
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT,
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT_DESC, null, null);
		fv.visitEnd();

		final MethodVisitor identityHashCode = cv.visitMethod(
				FlashlightNames.IDENTITY_HASHCODE_ACCESS,
				FlashlightNames.IDENTITY_HASHCODE.getName(),
				FlashlightNames.IDENTITY_HASHCODE.getDescriptor(), null, null);
		identityHashCode.visitCode();
		identityHashCode.visitVarInsn(Opcodes.ALOAD, 0);
		identityHashCode.visitFieldInsn(Opcodes.GETFIELD, classNameInternal,
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT,
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT_DESC);
		identityHashCode.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
				FlashlightNames.OBJECT_PHANTOM_REFERENCE,
				FlashlightNames.GET_ID.getName(),
				FlashlightNames.GET_ID.getDescriptor());
		identityHashCode.visitInsn(Opcodes.L2I);
		identityHashCode.visitInsn(Opcodes.IRETURN);
		identityHashCode.visitMaxs(2, 1);
		identityHashCode.visitEnd();

		final MethodVisitor getPhantomReference = cv.visitMethod(
				FlashlightNames.GET_PHANTOM_REFERENCE_ACCESS,
				FlashlightNames.GET_PHANTOM_REFERENCE.getName(),
				FlashlightNames.GET_PHANTOM_REFERENCE.getDescriptor(), null,
				null);
		getPhantomReference.visitCode();
		getPhantomReference.visitVarInsn(Opcodes.ALOAD, 0);
		getPhantomReference.visitFieldInsn(Opcodes.GETFIELD, classNameInternal,
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT,
				FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT_DESC);
		getPhantomReference.visitInsn(Opcodes.ARETURN);
		getPhantomReference.visitMaxs(1, 1);
		getPhantomReference.visitEnd();
	}

	private void addWrapperMethod(final MethodCallWrapper wrapper) {
		/* Create the method header */
		final ExceptionHandlerReorderingMethodAdapter mv = new ExceptionHandlerReorderingMethodAdapter(
				wrapper.createMethodHeader(cv));
		mv.visitCode();

		// empty stack

		/* Instrument the method call */
		wrapper.instrumentMethodCall(mv, config);

		/* Return from method */
		wrapper.methodReturn(mv);

		final int numLocals = wrapper.getNumLocals();
		mv.visitMaxs(Math.max(6 + Math.max(1, wrapper.getMethodReturnSize()),
				numLocals), numLocals);
		mv.visitEnd();
	}

	/**
	 * Called from visitInnerClass, visitField, visitMethod, and visitEnd. See
	 * the method order constraints in ClassVisitor.
	 */
	private void addFlashlightAttribute() {
		if (!wroteFlashlightAttribute) {
			cv.visitAttribute(new FlashlightAttribute());
			wroteFlashlightAttribute = true;
		}
	}
}
