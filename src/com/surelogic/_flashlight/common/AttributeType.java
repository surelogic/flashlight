package com.surelogic._flashlight.common;

public enum AttributeType implements IAttributeType {
	TIME("nano-time"),
	THREAD("thread"),
	IN_CLASS("in-class"),
	LINE("line"),
	FILE("file"),
	LOCK("lock"),
	FIELD("field"),
	RECEIVER("receiver"),
	LOCATION("location"),
	ID("id"),
	READ_LOCK_ID("read-lock-id"),
	WRITE_LOCK_ID("write-lock-id"),
	TYPE("type"),
	CLASS_NAME("class-name"),
	THREAD_NAME("thread-name"),
	FLAGS("flags"),
	VERSION("version"),
	RUN("run"),
	CPUS("processors"),
	MEMORY_MB("max-memory-mb"),
	USER_NAME("user-name"),
	JAVA_VERSION("java-version"),
	JAVA_VENDOR("java-vendor"),
	OS_NAME("os-name"),
	OS_ARCH("os-arch"),
	OS_VERSION("os-version"),
	WALL_CLOCK("wall-clock-time"),
	PARENT_ID("parent-id");
	
	private final String label;
	
	private AttributeType(String l) {
		label = l;
	}
	
	public final int base() {
		return 0;
	}
	
	public final String label() {
		return label;
	}
	
	public boolean matches(String name) {
		return label.equals(name);
	}

	public static AttributeType getType(String name) {		
		name = name.replace('.', '-');
		for(AttributeType t : values()) {
			if (t.label().equals(name)) {
				return t;
			}
		}
		return null;
	}
}
