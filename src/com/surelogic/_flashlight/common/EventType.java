package com.surelogic._flashlight.common;

public enum EventType {	
	After_IntrinsicLockAcquisition("after-intrinsic-lock-acquisition"),
	After_IntrinsicLockRelease("after-intrinsic-lock-release"),
	After_IntrinsicLockWait("after-intrinsic-lock-wait"),
	After_Trace("after-trace"),
	After_UtilConcurrentLockAcquisitionAttempt("after-util-concurrent-lock-acquisition-attempt"),
	After_UtilConcurrentLockReleaseAttempt("after-util-concurrent-lock-release-attempt"),
	Before_IntrinsicLockAcquisition("before-intrinsic-lock-acquisition"),
	Before_IntrinsicLockWait("before-intrinsic-lock-wait"),
	Before_Trace("before-trace"),
	Before_UtilConcurrentLockAcquisitionAttempt("before-util-concurrent-lock-acquisition-attempt"),
	Field_Definition("field-definition"),
	FieldRead_Instance("field-read"),
	FieldRead_Static("field-read"),	
	FieldWrite_Instance("field-write"),
	FieldWrite_Static("field-write"),
	Final_Event("final"),
	GarbageCollected_Object("garbage-collected-object"),
	Object_Definition("object-definition"),
	ReadWriteLock_Definition("read-write-lock-definition"),
	SingleThreadedField_Instance("single-threaded-field"),
	SingleThreadedField_Static("single-threaded-field"),
	Time_Event("time");
	
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
