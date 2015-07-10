package com.surelogic._flashlight.common;

public enum FlagType implements IAttributeType {
  UNDER_CONSTRUCTION("under-construction"),

  THIS_LOCK("lock-is-this"),

  CLASS_LOCK("lock-is-class"),

  RELEASED_LOCK("released-the-lock"),

  GOT_LOCK("got-the-lock"),

  IS_STATIC("static"),

  IS_FINAL("final"),

  IS_VOLATILE("volatile");

  private final int mask;
  private final String label;

  private FlagType(String l) {
    label = l;
    mask = 1 << this.ordinal();
  }

  @Override
  public final int base() {
    return 100;
  }

  @Override
  public final String label() {
    return label;
  }

  public int mask() {
    return mask;
  }

  public boolean matches(String name) {
    return label.equals(name);
  }
}
