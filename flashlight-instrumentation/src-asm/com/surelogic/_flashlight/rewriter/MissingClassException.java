package com.surelogic._flashlight.rewriter;

public final class MissingClassException extends RuntimeException {
  private String missingClassName;
  private MissingClassReference record;
  
  public MissingClassException(final String missingClassName) {
    this.missingClassName = missingClassName;
    this.record = null;
  }
  
  public void completeException(final String referringClassName,
      final String methodName, final String methodDesc) {
    record = new MissingClassReference(missingClassName, referringClassName, methodName, methodDesc);
    missingClassName = null;
  }
  
  public MissingClassReference getRecord() {
    return record;
  }
}
