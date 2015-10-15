package com.surelogic._flashlight.rewriter;

public final class MissingClassReference extends RuntimeException {
  private static final long serialVersionUID = 7597024396531038574L;
  private String referringClassName;
  private MethodIdentifier referringMethod;
  private final String missingClassName;
  
  public MissingClassReference(
      final String missingClassName, final String referringClassName,
      final String methodName, final String methodDesc) {
    this.missingClassName = missingClassName;
    this.referringClassName = referringClassName;
    this.referringMethod = new MethodIdentifier(methodName, methodDesc);
  }
  
  public String getReferringClassName() {
    return referringClassName;
  }
  
  public MethodIdentifier getReferringMethod() {
    return referringMethod;
  }
  
  public String getMissingClassName() {
    return missingClassName;
  }
}
