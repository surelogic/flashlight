package com.surelogic._flashlight.common;

public enum EventType {	
	AfterIntrinsicLockAcquisition("after-intrinsic-lock-acquisition"),
	AfterIntrinsicLockRelease("after-intrinsic-lock-release"),
	AfterIntrinsicLockWait("after-intrinsic-lock-wait"),
	AfterTrace("after-trace"),
	AfterUtilConcurrentLockAcquisitionAttempt("after-util-concurrent-lock-acquisition-attempt"),
	AfterUtilConcurrentLockReleaseAttempt("after-util-concurrent-lock-release-attempt"),
	BeforeIntrinsicLockAcquisition("before-intrinsic-lock-acquisition"),
	BeforeIntrinsicLockWait("before-intrinsic-lock-wait"),
	BeforeTrace("before-trace"),
	BeforeUtilConcurrentLockAcquisitionAttempt("before-util-concurrent-lock-acquisition-attempt"),
	FieldDefinition("field-definition"),
	FieldReadInstance("field-read"),
	FieldReadStatic("field-read"),	
	FieldWriteInstance("field-write"),
	FieldWriteStatic("field-write"),
	FinalEvent("final"),
	GarbageCollectedObject("garbage-collected-object"),
	ObjectDefinition("object-definition"),
	ReadWriteLockDefinition("read-write-lock-definition"),
	SingleThreadedFieldInstance("single-threaded-field"),
	SingleThreadedFieldStatic("single-threaded-field"),
	Time("time");
	
	private final String label;
	
	private EventType(String l) {
		label = l;
	}
	public String getLabel() {
		return label;
	}
	public byte getByte() {
		return (byte) this.ordinal();
	}
	public static EventType getEvent(byte i) {
		return values()[i];
	}
}
