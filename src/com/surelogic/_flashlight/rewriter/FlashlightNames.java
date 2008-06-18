package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

/**
 * Constants for names introduced into class files by the Flashlight classfile
 * rewriter.
 * @author aarong
 */
final class FlashlightNames {
  // Constants for accessing the special Flashlight Store class
  public static final String FLASHLIGHT_STORE = "com/surelogic/_flashlight/rewriter/test/DebugStore";
  
  public static final String AFTER_INTRINSIC_LOCK_ACQUISITION = "afterIntrinsicLockAcquisition";
  public static final String AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String AFTER_INTRINSIC_LOCK_RELEASE = "afterIntrinsicLockRelease";
  public static final String AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT = "afterUtilConcurrentLockAcquisitionAttempt";
  public static final String AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT = "afterUtilConcurrentLockReleaseAttempt";
  public static final String AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String BEFORE_INTRINSIC_LOCK_ACQUISITION = "beforeIntrinsicLockAcquisition";
  public static final String BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;ZZLjava/lang/Class;I)V";
  
  public static final String BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT = "beforeUtilConcurrentLockAcquisitionAttempt";
  public static final String BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String CONSTRUCTOR_CALL = "constructorCall";
  public static final String CONSTRUCTOR_CALL_SIGNATURE = "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/Class;I)V";

  public static final String CONSTRUCTOR_EXECUTION = "constructorExecution";
  public static final String CONSTRUCTOR_EXECUTION_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String FIELD_ACCESS = "fieldAccess";
  public static final String FIELD_ACCESS_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/reflect/Field;Ljava/lang/Class;I)V";
  
  public static final String INTRINSIC_LOCK_WAIT = "intrinsicLockWait";
  public static final String INTRINSIC_LOCK_WAIT_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/Class;I)V";
  
  public static final String METHOD_CALL = "methodCall";
  public static final String METHOD_CALL_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;I)V";
  
  // Flashlight classes and methods
  public static final String FLASHLIGHT_RUNTIME_SUPPORT = "com/surelogic/_flashlight/rewriter/runtime/FlashlightRuntimeSupport";
  public static final String REPORT_FATAL_ERROR = "reportFatalError";
  public static final String REPORT_FATAL_ERROR_SIGNATURE = "(Ljava/lang/Exception;)V";
  
  public static final String FLASHLIGHT_EXCEPTION = "com/surelogic/_flashlight/rewriter/runtime/FlashlightRuntimeException";
  public static final String FLASHLIGHT_EXCEPTION_SIGNATURE = "(Ljava/lang/Exception;)V";
  
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
  public static final String GET_DECLARED_FIELD = "getDeclaredField";
  public static final String GET_DECLARED_FIELD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/reflect/Field;";

  public static final String JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";
  
  public static final String JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";

  public static final String JAVA_UTIL_CONCURRENT_LOCKS_LOCK = "java/util/concurrent/locks/Lock";
  public static final String LOCK = "lock";
  public static final String LOCK_INTERRUPTIBLY = "lockInterruptibly";
  public static final String TRY_LOCK = "tryLock";
  public static final String UNLOCK = "unlock";
  
  
  
  /* We add the static final field "flashlight$inClass" to store the Class
   * object of the class for use in logging calls.  We prefer this field to
   * be private, but for interfaces we have to make the field public.  The
   * field cannot be accessed from Java code though because of the '$' in the
   * field's name.
   */ 
  public static final String IN_CLASS = "flashlight$inClass";
  private static final int IN_CLASS_ACCESS_BASE = 
    Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final int IN_CLASS_ACCESS_CLASS = Opcodes.ACC_PRIVATE | IN_CLASS_ACCESS_BASE;
  public static final int IN_CLASS_ACCESS_INTERFACE = Opcodes.ACC_PUBLIC | IN_CLASS_ACCESS_BASE;
  public static final String IN_CLASS_DESC = "Ljava/lang/Class;";
  
  
  
  // Prevent instantiation
  private FlashlightNames() {
    // do nothing
  }
}
