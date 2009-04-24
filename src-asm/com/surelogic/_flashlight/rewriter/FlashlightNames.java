package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * Constants for names introduced into class files by the Flashlight classfile
 * rewriter.
 */
public final class FlashlightNames {
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
  
  public static final Method INDIRECT_ACCESS =
    Method.getMethod("void indirectAccess(Object, long)");
  
  public static final Method ARRAY_ACCESS =
    Method.getMethod("void arrayAccess(boolean, Object, int, long)");
  
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
