package com.surelogic._flashlight.common;

import java.io.*;
import java.util.Map;

import static com.surelogic._flashlight.common.AttributeType.*;
import static com.surelogic._flashlight.common.FlagType.*;

public enum EventType {	
	After_IntrinsicLockAcquisition("after-intrinsic-lock-acquisition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	After_IntrinsicLockRelease("after-intrinsic-lock-release") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	After_IntrinsicLockWait("after-intrinsic-lock-wait") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	After_Trace("after-trace") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readTraceEvent(in, attrs);
		}
	},
	After_UtilConcurrentLockAcquisitionAttempt("after-util-concurrent-lock-acquisition-attempt") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	After_UtilConcurrentLockReleaseAttempt("after-util-concurrent-lock-release-attempt") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	Before_IntrinsicLockAcquisition("before-intrinsic-lock-acquisition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	Before_IntrinsicLockWait("before-intrinsic-lock-wait") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	Before_Trace("before-trace") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readTraceEvent(in, attrs);
			attrs.put(FILE, in.readUTF());
			attrs.put(LOCATION, in.readUTF());
		}
	},
	Before_UtilConcurrentLockAcquisitionAttempt("before-util-concurrent-lock-acquisition-attempt") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	Field_Definition("field-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, in.readLong());
			attrs.put(TYPE, in.readLong());
			attrs.put(FIELD, in.readUTF());
			int flags = in.readInt();
			readFlag(flags, IS_STATIC, attrs);
			readFlag(flags, IS_FINAL, attrs);
			readFlag(flags, IS_VOLATILE, attrs);
		}
	},
	//////////////////////////////////////////////	
	
	FieldRead_Instance("field-read") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccess(in, attrs);
			attrs.put(RECEIVER, in.readLong());
		}
	},	
	FieldRead_Static("field-read") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccess(in, attrs);
		}
	},	
	FieldWrite_Instance("field-write") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccess(in, attrs);
			attrs.put(RECEIVER, in.readLong());
		}
	},
	FieldWrite_Static("field-write") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccess(in, attrs);
		}
	},
	Final_Event("final") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(TIME, in.readLong());
		}
	},
	GarbageCollected_Object("garbage-collected-object") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, in.readLong());
		}
	},
	Object_Definition("object-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, in.readLong());
			attrs.put(TYPE, in.readLong());
		}
	},
	ReadWriteLock_Definition("read-write-lock-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, in.readLong());
			attrs.put(READ_LOCK_ID, in.readLong());
			attrs.put(WRITE_LOCK_ID, in.readLong());
		}
	},
	SingleThreadedField_Instance("single-threaded-field") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(FIELD, in.readLong());
			attrs.put(RECEIVER, in.readLong());
		}
	},
	SingleThreadedField_Static("single-threaded-field") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(FIELD, in.readLong());
		}
	},
	Time_Event("time") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(TIME, in.readLong());
		}
	}
	;
	private final String label;
	
	private EventType(String l) {
		label = l;
	}
	String getLabel() {
		return label;
	}
	byte getByte() {
		return (byte) this.ordinal();
	}
	static EventType getEvent(int i) {
		return values()[i];
	}
	
	abstract void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException;
	
	static void readFlag(int flags, FlagType flag, Map<IAttributeType,Object> attrs) {
		attrs.put(flag, Boolean.valueOf(((flags & flag.mask()) != 0)));
	}
	
	static void readFieldAccess(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		attrs.put(TIME, in.readLong());
		attrs.put(THREAD, in.readLong());
		attrs.put(IN_CLASS, in.readLong());
		attrs.put(FIELD, in.readLong());
		attrs.put(LINE, in.readInt());
		final int flags = in.readInt();
		readFlag(flags, UNDER_CONSTRUCTION, attrs);
	}
	
	static void readLockEvent(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		attrs.put(TIME, in.readLong());
		attrs.put(THREAD, in.readLong());
		attrs.put(IN_CLASS, in.readLong());
		attrs.put(LOCK, in.readLong());
		attrs.put(LINE, in.readInt());
		final int flags = in.readInt();
		readFlag(flags, THIS_LOCK, attrs);
		readFlag(flags, CLASS_LOCK, attrs);
		readFlag(flags, RELEASED_LOCK, attrs);
		readFlag(flags, GOT_LOCK, attrs);
	}
	
	static void readTraceEvent(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		attrs.put(TIME, in.readLong());
		attrs.put(THREAD, in.readLong());
		attrs.put(IN_CLASS, in.readLong());
		attrs.put(LINE, in.readInt());		
	}
}
