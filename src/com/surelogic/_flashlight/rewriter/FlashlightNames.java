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
  public static final String BEFORE_INTRINSIC_LOCK_ACQUISITION = "beforeIntrinsicLockAcquisition";
  public static final String BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;ZZLjava/lang/Class;I)V";
  public static final String FIELD_ACCESS = "fieldAccess";
  public static final String FIELD_ACCESS_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/reflect/Field;Ljava/lang/Class;I)V";
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
  
  public static final String JAVA_LANG_CLASS = "java/lang/Class";
  public static final String FOR_NAME = "forName";
  public static final String FOR_NAME_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";
  public static final String GET_DECLARED_FIELD = "getDeclaredField";
  public static final String GET_DECLARED_FIELD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/reflect/Field;";

  public static final String JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";
  
  public static final String JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";

  
  
  // Generated fields and methods
  public static final String IN_CLASS = "flashlight$inClass";
  public static final int IN_CLASS_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final String IN_CLASS_DESC = "Ljava/lang/Class;";

  /** Generated wrapper methods are <code>private static</code> and synthetic. */
  public static final int WRAPPER_METHOD_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
  
  
  
  // Prevent instantiation
  private FlashlightNames() {
    // do nothing
  }
}
