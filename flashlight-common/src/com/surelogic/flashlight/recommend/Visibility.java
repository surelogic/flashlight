package com.surelogic.flashlight.recommend;

import java.lang.reflect.Modifier;

public enum Visibility {
  PUBLIC("public"), PROTECTED("protected"), DEFAULT("default"), PRIVATE("private");

  private final String prettyPrint;

  private Visibility(final String pp) {
    prettyPrint = pp;
  }

  public static int toFlag(final int mod) {
    if (Modifier.isProtected(mod)) {
      return 4;
    } else if (Modifier.isPrivate(mod)) {
      return 2;
    } else if (Modifier.isPublic(mod)) {
      return 1;
    } else {
      return 0;
    }
  }

  public static Visibility fromFlag(final int f) {
    switch (f) {
    case 0:
      return DEFAULT;
    case 1:
      return PUBLIC;
    case 2:
      return PRIVATE;
    case 4:
      return PROTECTED;
    default:
      throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    return prettyPrint;
  }
}
