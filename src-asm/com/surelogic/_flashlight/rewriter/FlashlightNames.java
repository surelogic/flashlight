package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

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
  
  public static final String AFTER_INTRINSIC_LOCK_ACQUISITION = "afterIntrinsicLockAcquisition";
  public static final String AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String AFTER_INTRINSIC_LOCK_RELEASE = "afterIntrinsicLockRelease";
  public static final String AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE = "(Ljava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT = "afterUtilConcurrentLockAcquisitionAttempt";
  public static final String AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE = "(ZLjava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT = "afterUtilConcurrentLockReleaseAttempt";
  public static final String AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT_SIGNATURE = "(ZLjava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String BEFORE_INTRINSIC_LOCK_ACQUISITION = "beforeIntrinsicLockAcquisition";
  public static final String BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;ZZLcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT = "beforeUtilConcurrentLockAcquisitionAttempt";
  public static final String BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE = "(Ljava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String CONSTRUCTOR_CALL = "constructorCall";
  public static final String CONSTRUCTOR_CALL_SIGNATURE = "(ZLjava/lang/String;Ljava/lang/String;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";

  public static final String CONSTRUCTOR_EXECUTION = "constructorExecution";
  public static final String CONSTRUCTOR_EXECUTION_SIGNATURE = "(ZLjava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String INSTANCE_FIELD_ACCESS = "instanceFieldAccess";
  public static final String INSTANCE_FIELD_ACCESS_SIGNATURE = "(ZLjava/lang/Object;ILcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String STATIC_FIELD_ACCESS = "staticFieldAccess";
  public static final String STATIC_FIELD_ACCESS_SIGNATURE = "(ZLcom/surelogic/_flashlight/ClassPhantomReference;ILcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String INSTANCE_FIELD_ACCESS_LOOKUP = "instanceFieldAccessLookup";
  public static final String INSTANCE_FIELD_ACCESS_LOOKUP_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String STATIC_FIELD_ACCESS_LOOKUP = "staticFieldAccessLookup";
  public static final String STATIC_FIELD_ACCESS_LOOKUP_SIGNATURE = "(ZLjava/lang/Class;Ljava/lang/String;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String INTRINSIC_LOCK_WAIT = "intrinsicLockWait";
  public static final String INTRINSIC_LOCK_WAIT_SIGNATURE = "(ZLjava/lang/Object;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String METHOD_CALL = "methodCall";
  public static final String METHOD_CALL_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/String;Ljava/lang/String;Lcom/surelogic/_flashlight/ClassPhantomReference;I)V";
  
  public static final String GET_CLASS_PHANTOM = "getClassPhantom";
  public static final String GET_CLASS_PHANTOM_SIGNATURE = "(Ljava/lang/Class;)Lcom/surelogic/_flashlight/ClassPhantomReference;";

  // Flashlight classes and methods  
  public static final String FLASHLIGHT_RUNTIME_SUPPORT = "com/surelogic/_flashlight/rewriter/runtime/FlashlightRuntimeSupport";
  public static final String GET_CLASSLOADER_INFO = "getClassLoaderInfo";
  public static final String GET_CLASSLOADER_INFO_SIGNATURE = "(Ljava/lang/Class;)Lcom/surelogic/_flashlight/rewriter/runtime/ClassLoaderInfo;";
  
  public static final String CLASS_LOADER_INFO = "com/surelogic/_flashlight/rewriter/runtime/ClassLoaderInfo";
  public static final String GET_CLASS = "getClass";
  public static final String GET_CLASS_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";
  
  // Other Java classes and methods
  public static final String CONSTRUCTOR = "<init>";
  
  public static final String JAVA_LANG_OBJECT = "java/lang/Object";
  public static final String WAIT = "wait";
  public static final String WAIT_SIGNATURE_0_ARGS = "()V";
  public static final String WAIT_SIGNATURE_1_ARG = "(J)V";
  public static final String WAIT_SIGNATURE_2_ARGS = "(JI)V";
  
  public static final String JAVA_LANG_CLASS = "java/lang/Class";
  public static final String FOR_NAME = "forName";
  public static final String FOR_NAME_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";

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
}
