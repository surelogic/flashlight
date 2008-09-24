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
	FLAGS("flags");
	
	private final String label;
	
	private AttributeType(String l) {
		label = l;
	}
	
	public String label() {
		return label;
	}
	
	public boolean matches(String name) {
		return label.equals(name);
	}
}
