package com.surelogic._flashlight.rewriter;

public final class MethodIdentifier {
  public final String name;
  public final String desc;
  private final int hashCode;
  
  public MethodIdentifier(final String name, final String desc) {
    this.name = name;
    this.desc = desc;
    this.hashCode = computeHashCode();
  }
  
  @Override
  public String toString() {
    return name + desc;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof MethodIdentifier) {
      final MethodIdentifier other = (MethodIdentifier) o;
      return name.equals(other.name) && desc.equals(other.desc);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return hashCode;
  }
  
  private int computeHashCode() {
    int result = 17;
    result = 31 * result + name.hashCode();
    result = 31 * result + desc.hashCode();
    return result;
  }
}
