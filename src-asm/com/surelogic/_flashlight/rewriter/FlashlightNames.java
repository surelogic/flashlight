package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * Constants for names introduced into class files by the Flashlight classfile
 * rewriter.
 */
final class FlashlightNames {
  // Prevent instantiation
  private FlashlightNames() {
    // do nothing
  }

  public static final String FLASHLIGHT_STORE = "com/surelogic/_flashlight/Store";
  
  public static final Method AFTER_INTRINSIC_LOCK_ACQUISITION = 
    Method.getMethod("void afterIntrinsicLockAcquisition(Object, long)");
  
  public static final Method AFTER_INTRINSIC_LOCK_RELEASE =
    Method.getMethod("void afterIntrinsicLockRelease(Object, long)");
  
  public static final Method AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT =
    Method.getMethod("void afterUtilConcurrentLockAcquisitionAttempt(boolean, Object, long)");
  
  public static final Method AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT =
    Method.getMethod("void afterUtilConcurrentLockReleaseAttempt(boolean, Object, long)");
  
  public static final Method BEFORE_INTRINSIC_LOCK_ACQUISITION =
    Method.getMethod("void beforeIntrinsicLockAcquisition(Object, boolean, boolean, long)");
  
  public static final Method BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT =
    Method.getMethod("void beforeUtilConcurrentLockAcquisitionAttempt(Object, long)");
  
  public static final Method CONSTRUCTOR_CALL =
    Method.getMethod("void constructorCall(boolean, long)");

  public static final Method CONSTRUCTOR_EXECUTION =
    Method.getMethod("void constructorExecution(boolean, Object, long)");
  
  public static final Method INSTANCE_FIELD_ACCESS =
    Method.getMethod("void instanceFieldAccess(boolean, Object, int, long)");
  
  public static final Method STATIC_FIELD_ACCESS =
    Method.getMethod("void staticFieldAccess(boolean, com.surelogic._flashlight.ClassPhantomReference, int, long)");
  
  public static final Method INSTANCE_FIELD_ACCESS_LOOKUP =
    Method.getMethod("void instanceFieldAccessLookup(boolean, Object, Class, String, long)");
  
  public static final Method STATIC_FIELD_ACCESS_LOOKUP =
    Method.getMethod("void staticFieldAccessLookup(boolean, Class, String, long)");
  
  public static final Method INTRINSIC_LOCK_WAIT =
    Method.getMethod("void intrinsicLockWait(boolean, Object, long)");
  
  public static final Method METHOD_CALL =
    Method.getMethod("void methodCall(boolean, Object, long)");
  
  public static final Method GET_CLASS_PHANTOM =
    Method.getMethod("com.surelogic._flashlight.ClassPhantomReference getClassPhantom(Class)");
  
  public static final Method GET_OBJECT_PHANTOM =
    Method.getMethod("com.surelogic._flashlight.ObjectPhantomReference getObjectPhantom(Object, long)");

  // Flashlight IIdObject interface
  public static final String I_ID_OBJECT = "com/surelogic/_flashlight/rewriter/runtime/IIdObject";
  
  public static final Method IDENTITY_HASHCODE =
    Method.getMethod("int identity$HashCode()");
  public static final int    IDENTITY_HASHCODE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;

  public static final Method GET_PHANTOM_REFERENCE =
    Method.getMethod("com.surelogic._flashlight.ObjectPhantomReference getPhantom$Reference()");
  public static final int    GET_PHANTOM_REFERENCE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;

  // Flashlight IdObject class
  public static final String ID_OBJECT = "com/surelogic/_flashlight/rewriter/runtime/IdObject";

  public static final Method GET_NEW_ID = Method.getMethod("long getNewId()");
  
  // Flashlight ObjectPhantomReference class
  public static final String OBJECT_PHANTOM_REFERENCE = "com/surelogic/_flashlight/ObjectPhantomReference";
  public static final Method GET_ID = Method.getMethod("long getId()");
  
  // Flashlight classes and methods  
  public static final String FLASHLIGHT_RUNTIME_SUPPORT = "com/surelogic/_flashlight/rewriter/runtime/FlashlightRuntimeSupport";
  public static final Method GET_CLASSLOADER_INFO = 
    Method.getMethod("com.surelogic._flashlight.rewriter.runtime.ClassLoaderInfo getClassLoaderInfo(Class)");
  
  public static final String CLASS_LOADER_INFO = "com/surelogic/_flashlight/rewriter/runtime/ClassLoaderInfo";
  public static final Method GET_CLASS = Method.getMethod("Class getClass(String)");
  
    
  // Other Java classes and methods
  public static final String CONSTRUCTOR = "<init>";
  
  public static final String JAVA_LANG_OBJECT = "java/lang/Object";
  public static final Type JAVA_LANG_OBJECT_TYPE = Type.getObjectType(JAVA_LANG_OBJECT);
  
  public static final String WAIT = "wait";
  public static final String WAIT_SIGNATURE_0_ARGS = "()V";
  public static final String WAIT_SIGNATURE_1_ARG = "(J)V";
  public static final String WAIT_SIGNATURE_2_ARGS = "(JI)V";
  
  public static final String JAVA_LANG_CLASS = "java/lang/Class";
  public static final Method FOR_NAME = Method.getMethod("Class forName(String)");

  public static final String JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";
  
  public static final String JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";

  public static final String JAVA_UTIL_CONCURRENT_LOCKS_LOCK = "java/util/concurrent/locks/Lock";
  public static final String LOCK = "lock";
  public static final String LOCK_INTERRUPTIBLY = "lockInterruptibly";
  public static final String TRY_LOCK = "tryLock";
  public static final String UNLOCK = "unlock";
  
  
  
  /* constants for the frame class */
  public static final String FRAME = "com/surelogic/_flashlight/rewriter/runtime/frame/Frame";
  public static final Type FRAME_TYPE = Type.getObjectType(FRAME);
  
  public static final String FRAME_INIT_DESCRIPTION = "(II)V";
  
  public static final Method SET_CURRENT_SOURCE_LINE = Method.getMethod("void setCurrentSourceLine(int)");
  public static final Method CLEAR_LOCAL_VARIABLE = Method.getMethod("void clearLocalVariable(int)");
  public static final Method SET_LOCAL_VARIABLE = Method.getMethod("void setLocalVariable(int, String, String)");
  public static final Method INIT_RECEIVER = Method.getMethod("void initReceiver()");
  public static final Method INIT_PARAMETER = Method.getMethod("void initParameter(int, int)");
  
  public static final Method AALOAD = Method.getMethod("void aaload()");
  public static final Method ALOAD = Method.getMethod("void aload(int)");
  public static final Method ARRAYLENGTH = Method.getMethod("void arraylength()");
  public static final Method ASTORE = Method.getMethod("void astore(int)");
  public static final Method ATHROW = Method.getMethod("void athrow()");
  public static final Method DUP = Method.getMethod("void dup()");
  public static final Method DUP_X1 = Method.getMethod("void dup_x1()");
  public static final Method DUP_X2 = Method.getMethod("void dup_x2()");
  public static final Method DUP2 = Method.getMethod("void dup2()");
  public static final Method DUP2_X1 = Method.getMethod("void dup2_x1()");
  public static final Method DUP2_X2 = Method.getMethod("void dup2_x2()");
  public static final Method GETFIELD_OBJECT = Method.getMethod("void getfieldObject(String, String, String)");
  public static final Method GETFIELD_PRIMITIVE = Method.getMethod("void getfieldPrimitive()");
  public static final Method GETFIELD_PRIMITIVE2 = Method.getMethod("void getfieldPrimitive2()");
  public static final Method GETSTATIC_OBJECT = Method.getMethod("void getstaticObject(String, String, String)");
  public static final Method GETSTATIC_PRIMITIVE = Method.getMethod("void getstaticPrimitive()");
  public static final Method GETSTATIC_PRIMITIVE2 = Method.getMethod("void getstaticPrimitive2()");
  public static final Method INSTANCEOF = Method.getMethod("void instanceOf()");
  public static final Method LDC_STRING = Method.getMethod("void ldcString(String)");
  public static final Method LDC_CLASS = Method.getMethod("void ldcClass(String)");
  public static final Method MULTIANEWARRAY = Method.getMethod("void multianewarray(String, int)");
  public static final Method NEWOBJECT = Method.getMethod("void newObject(String)");
  public static final Method SWAP = Method.getMethod("void swap()");

  public static final Method EXCEPTION_HANDLER = Method.getMethod("void exceptionHandler(String)");
  public static final Method FINALLY_HANDLER = Method.getMethod("void finallyHandler()");
  public static final Method POP = Method.getMethod("void pop()");
  public static final Method POP2 = Method.getMethod("void pop2()");
  public static final Method POP3 = Method.getMethod("void pop3()");
  public static final Method POP4 = Method.getMethod("void pop4()");
  public static final Method PUSH_PRIMITIVE = Method.getMethod("void pushPrimitive()");
  public static final Method PUSH_PRIMITIVE2 = Method.getMethod("void pushPrimitive2()");
  public static final Method NEWARRAY = Method.getMethod("void newarray(String)");
  public static final Method PRIMITIVE_ARRAY_LOAD = Method.getMethod("void primitiveArrayLoad()");
  public static final Method PRIMITIVE_ARRAY_LOAD2 = Method.getMethod("void primitiveArrayLoad2()");
  public static final Method INVOKE_METHOD_RETURNS_OBJECT = Method.getMethod("void invokeMethodReturnsObject(int, int, String, String, String)");
  public static final Method INVOKE_METHOD_RETURNS_VOID = Method.getMethod("void invokeMethodReturnsVoid(int)");
  public static final Method INVOKE_METHOD_RETURNS_PRIMITIVE = Method.getMethod("void invokeMethodReturnsPrimitive(int)");
  public static final Method INVOKE_METHOD_RETURNS_PRIMITIVE2 = Method.getMethod("void invokeMethodReturnsPrimitive2(int)");
  public static final Method INVOKE_STATIC_METHOD_RETURNS_OBJECT = Method.getMethod("void invokeStaticMethodReturnsObject(int, String, String, String)");
  public static final Method INVOKE_STATIC_METHOD_RETURNS_VOID = Method.getMethod("void invokeStaticMethodReturnsVoid(int)");
  public static final Method INVOKE_STATIC_METHOD_RETURNS_PRIMITIVE = Method.getMethod("void invokeStaticMethodReturnsPrimitive(int)");
  public static final Method INVOKE_STATIC_METHOD_RETURNS_PRIMITIVE2 = Method.getMethod("void invokeStaticMethodReturnsPrimitive2(int)");
  
  
  /* We add the static final field "flashlight$classObject" to store the Class
   * object of the class for use in logging calls.  We make the field public so
   * that any class can look up the Class object of any other class.  The
   * field cannot be accessed from Java code though because of the '$' in the
   * field's name.
   */ 
  public static final String FLASHLIGHT_CLASS_OBJECT = "flashlight$classObject";
  public static final int FLASHLIGHT_CLASS_OBJECT_ACCESS = 
    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final String FLASHLIGHT_CLASS_OBJECT_DESC = "Ljava/lang/Class;";

  /* We add the static final field "flashlight$phantomClassObject" to store the Class
   * object of the class for use in logging calls.  We make the field public so
   * that any class can look up the phantom class object for any other class.  The
   * field cannot be accessed from Java code though because of the '$' in the
   * field's name.
   */ 
  public static final String FLASHLIGHT_PHANTOM_CLASS_OBJECT = "flashlight$phantomClassObject";
  public static final int FLASHLIGHT_PHANTOM_CLASS_OBJECT_ACCESS = 
    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final String FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC = "Lcom/surelogic/_flashlight/ClassPhantomReference;";
  
  /* We add a static final field "flashlight$classLoaderInfo" to store the
   * cache of class names to class objects used when dealing with field 
   * access that must be dynamically identified.
   */
  public static final String FLASHLIGHT_CLASS_LOADER_INFO = "flashlight$classLoaderInfo";
  public static final int FLASHLIGHT_CLASS_LOADER_INFO_ACCESS =
    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final String FLASHLIGHT_CLASS_LOADER_INFO_DESC = "Lcom/surelogic/_flashlight/rewriter/runtime/ClassLoaderInfo;";
  
  /* When implementing the IIdObject interface, we need to add a private final
   * field "flashlight$phantomObject" to store the phantom object referenced for
   * the object.  
   */
  public static final String FLASHLIGHT_PHANTOM_OBJECT = "flashlight$phantomObject";
  public static final int FLASHLIGHT_PHANTOM_OBJECT_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final String FLASHLIGHT_PHANTOM_OBJECT_DESC = "Lcom/surelogic/_flashlight/ObjectPhantomReference;";
}
