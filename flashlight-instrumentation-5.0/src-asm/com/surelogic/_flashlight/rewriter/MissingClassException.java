package com.surelogic._flashlight.rewriter;

public final class MissingClassException extends RuntimeException {
  private final String referringClassName;
  private final String missingClassName;
  
  public MissingClassException(final String missingClassName) {
    this(null, missingClassName);
  }
  
  private MissingClassException(
      final String referringClassName, final String missingClassName) {
    this.referringClassName = referringClassName;
    this.missingClassName = missingClassName;
  }

  public MissingClassException setReferringClassName(final String referringClassName) {
    return new MissingClassException(referringClassName, missingClassName);
  }
  
  public String getReferringClassName() {
    return referringClassName;
  }
  
  public String getMissingClassName() {
    return missingClassName;
  }
}
