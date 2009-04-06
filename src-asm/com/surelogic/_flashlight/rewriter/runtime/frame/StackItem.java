package com.surelogic._flashlight.rewriter.runtime.frame;

public interface StackItem {
  public enum Type {
    STRING_CONSTANT,
    CLASS_CONSTANT,
    LOCAL_VARIABLE,
    FIELD_REFERENCE,
    STATIC_FIELD_REFERENCE,
    ARRAY_REFERENCE,
    METHOD_CALL,
    STATIC_METHOD_CALL,
    NEW_ARRAY,
    NEW_OBJECT,
    EXCEPTION,
    PARAMETER,
    RECEIVER,
    PRIMITIVE }
  
  public Type getType();
}
