package com.surelogic._flashlight.rewriter.runtime.frame;

/**
 * Information about a local variable in the frame model.  Right now just the 
 * name and description (type) of the variable.
 */
abstract class LocalVariable extends DescribedName {
  public static final LocalVariable UNKNOWN = new LocalVariable("unknown", "unknown") {
    @Override
    public String toString() {
      return "Unknown";
    }
  };
  
  private LocalVariable(final String n, final String d) {
    super(n, d);
  }
  
  /* Protected access, only call from Frame class */
  static LocalVariable create(final String n, final String d) {
    return new LocalVariable(n, d) {
      @Override
      public String toString() {
        return name + " " + desciption;
      }
    };
  }
  
  /* Force subclasses to reimplement */
  @Override
  public abstract String toString();
}
