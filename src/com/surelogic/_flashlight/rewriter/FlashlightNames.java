package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

/**
 * Constants for names introduced into classfiles by the Flashlight classfile
 * rewriter.
 * @author aarong
 */
public final class FlashlightNames {
  public static final String IN_CLASS = "flashlight$inClass";
  public static final int IN_CLASS_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  public static final String IN_CLASS_DESC = "Ljava/lang/Class;";
  
  // Prevent instantiation
  private FlashlightNames() {
    // do nothing
  }
}
