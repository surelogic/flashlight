package com.surelogic._flashlight.io;

public class StringBuilderOutput implements IPrintOutput {
  final StringBuilder b = new StringBuilder();

  @Override
  public final String toString() {
    return b.toString();
  }

  public IPrintOutput append(char c) {
    b.append(c);
    return this;
  }

  public IPrintOutput append(String s) {
    b.append(s);
    return this;
  }
}
