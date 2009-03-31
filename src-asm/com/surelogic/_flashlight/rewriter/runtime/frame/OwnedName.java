package com.surelogic._flashlight.rewriter.runtime.frame;

public final class OwnedName extends DescribedName {
  public final String owner;
  
  public OwnedName(final String o, final String n, final String d) {
    super(n, d);
    owner = o;
  }
  
  @Override
  public String toString() {
    return owner + " " + name + " " + desciption;
  }
}
