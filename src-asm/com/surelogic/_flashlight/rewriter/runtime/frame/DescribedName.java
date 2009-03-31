package com.surelogic._flashlight.rewriter.runtime.frame;

/**
 * Information about a local variable in the frame model.  Right now just the 
 * name and description (type) of the variable.
 */
abstract class DescribedName {
  public final String name;
  public final String desciption;
  
  public DescribedName(final String n, final String d) {
    name = n;
    desciption = d;
  }
}
