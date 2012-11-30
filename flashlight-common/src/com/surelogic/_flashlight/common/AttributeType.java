package com.surelogic._flashlight.common;

public enum AttributeType implements IAttributeType {
  TIME("nano-time"), THREAD("thread"), SITE_ID("site"), IN_CLASS("in-class"), LINE("line"), FILE("file"), LOCK("lock"), FIELD(
      "field"), RECEIVER("receiver"), LOCATION("location"), ID("id"), READ_LOCK_ID("read-lock-id"), WRITE_LOCK_ID("write-lock-id"), TYPE(
      "type"), CLASS_NAME("class-name"), THREAD_NAME("thread-name"), FLAGS("flags"), VERSION("version"), RUN("run"), CPUS(
      "processors"), MEMORY_MB("max-memory-mb"), USER_NAME("user-name"), JAVA_VERSION("java-version"), JAVA_VENDOR("java-vendor"), OS_NAME(
      "os-name"), OS_ARCH("os-arch"), OS_VERSION("os-version"), WALL_CLOCK("wall-clock-time"), PARENT_ID("parent-id"), TRACE(
      "trace"), START_TIME("start-time"), PACKAGE("package"), VALUE("value"), VISIBILITY("visibility"), MODIFIER("mod"), HOSTNAME(
      "hostname"), METHODCALLNAME("method-call-name"), METHODCALLOWNER("method-call-owner"), METHODCALLDESC("method-call-desc"), SOURCE(
      "source"), TARGET("target"), OBJECT("object"), ISSOURCE("is-source"), ANDROID("android");

  private final String label;

  private AttributeType(final String l) {
    label = l;
  }

  @Override
  public final int base() {
    return 0;
  }

  @Override
  public final String label() {
    return label;
  }

  public boolean matches(final String name) {
    return label.equals(name);
  }

  public static AttributeType getType(String name) {
    name = name.replace('.', '-');
    for (final AttributeType t : values()) {
      if (t.label().equals(name)) {
        return t;
      }
    }
    return null;
  }
}
