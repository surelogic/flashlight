package com.surelogic._flashlight.common;

import java.lang.reflect.Modifier;

/**
 * ClassType represents what kind of class a particular class is, and provides
 * some utility methods for displaying class codes used by the ad hoc query
 * viewer.
 */
public enum ClassType {
  ANONYMOUS("CL"),

  INTERFACE("IN"),

  ARRAY("CL"),

  ENUM("EN"),

  ABSTRACT("CL"),

  CLASS("CL"),

  ANNOTATION("AN");

  private static final int SYNTHETIC = 0x00001000;

  private String declCode;

  ClassType(String declCode) {
    this.declCode = declCode;
  }

  public String getXmlName() {
    return toString().toLowerCase();
  }

  public static ClassType fromXmlName(String name) {
    return valueOf(name.toUpperCase());
  }

  public String getCode(int mod) {
    return "@" + declCode + ":" + getVisibilityCode(mod) + ":" + getModifierCodes(mod);
  }

  public String getDeclarationCode() {
    return declCode;
  }

  public String getVisibilityCode(int mod) {
    if (this == ANONYMOUS) {
      return "AN";
    }
    if (Modifier.isPublic(mod)) {
      return "PU";
    }
    if (Modifier.isProtected(mod)) {
      return "PO";
    }
    if (Modifier.isPrivate(mod)) {
      return "PR";
    }
    return "DE";
  }

  public String getModifierCodes(int mod) {
    StringBuilder mods = new StringBuilder();
    if (this == ABSTRACT) {
      mods.append("A");
    }
    if (Modifier.isFinal(mod)) {
      mods.append("F");
    }
    if ((mod & SYNTHETIC) != 0) {
      mods.append("I");
    }
    if (Modifier.isStatic(mod)) {
      mods.append("S");
    }
    if (Modifier.isVolatile(mod)) {
      mods.append("V");
    }
    return mods.toString();
  }

  public static ClassType detectType(Class<?> referent) {
    if (referent.isAnonymousClass()) {
      return ClassType.ANONYMOUS;
    }
    if (referent.isInterface()) {
      return ClassType.INTERFACE;
    }
    if (referent.isArray()) {
      return ARRAY;
    }
    if (referent.isEnum()) {
      return ENUM;
    }
    if (referent.isAnnotation()) {
      return ANNOTATION;
    }
    if (Modifier.isAbstract(referent.getModifiers())) {
      return ABSTRACT;
    }
    return ClassType.CLASS;
  }

}