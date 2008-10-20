package com.surelogic._flashlight.common;

import java.io.*;
import java.util.*;

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
			final int flags = readCompressedInt(in);
			readFlag(flags, GOT_LOCK, attrs);	
		}
	},
	After_UtilConcurrentLockReleaseAttempt("after-util-concurrent-lock-release-attempt") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
			final int flags = readCompressedInt(in);
			readFlag(flags, RELEASED_LOCK, attrs);	
		}
	},
	Before_IntrinsicLockAcquisition("before-intrinsic-lock-acquisition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
			final int flags = readCompressedInt(in);
			readFlag(flags, THIS_LOCK, attrs);	
			readFlag(flags, CLASS_LOCK, attrs);
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
			/*
			attrs.put(FILE, in.readUTF());
			attrs.put(LOCATION, in.readUTF());
			*/
		}
	},
	Before_UtilConcurrentLockAcquisitionAttempt("before-util-concurrent-lock-acquisition-attempt") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readLockEvent(in, attrs);
		}
	},
	Class_Definition("class-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
			attrs.put(CLASS_NAME, in.readUTF());
		}
	},
	Environment("environment") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(MEMORY_MB, in.readLong());
			attrs.put(CPUS, in.readInt());
			final byte numProps = in.readByte();
			for(int i=0; i<numProps; i++) {
				attrs.put(AttributeType.getType(in.readUTF()), in.readUTF());
			}			
		}
	},
	Field_Definition("field-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
			attrs.put(TYPE, readCompressedLong(in));
			attrs.put(FIELD, in.readUTF());
			int flags = readCompressedInt(in);
			readFlag(flags, IS_STATIC, attrs);
			readFlag(flags, IS_FINAL, attrs);
			readFlag(flags, IS_VOLATILE, attrs);
		}
	},
	//////////////////////////////////////////////	
	
	FieldRead_Instance("field-read") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccessInstance(in, attrs);
		}
	},	
	FieldRead_Instance_UnderConstruction("field-read") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccessInstance(in, attrs);
			attrs.put(UNDER_CONSTRUCTION, Boolean.TRUE);
		}
	},
	FieldRead_Static("field-read") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccess(in, attrs);
			attrs.put(RECEIVER, IdConstants.ILLEGAL_RECEIVER_ID);
		}
	},	
	FieldWrite_Instance("field-write") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccessInstance(in, attrs);
		}
	},
	FieldWrite_Instance_UnderConstruction("field-write") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccessInstance(in, attrs);
			attrs.put(UNDER_CONSTRUCTION, Boolean.TRUE);
		}
	},
	FieldWrite_Static("field-write") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			readFieldAccess(in, attrs);
			attrs.put(RECEIVER, IdConstants.ILLEGAL_RECEIVER_ID);
		}
	},
	Final_Event("final") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(TIME, in.readLong());
		}
	},
	First_Event("flashlight") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(VERSION, in.readUTF());
			attrs.put(RUN, in.readUTF());
		}
	},
	GarbageCollected_Object("garbage-collected-object") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
		}
	},
	Object_Definition("object-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
			attrs.put(TYPE, readCompressedLong(in));
		}
	},
	Observed_CallLocation("call-location") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(SITE_ID, readCompressedLong(in));
			attrs.put(IN_CLASS, readCompressedLong(in));
			attrs.put(LINE, readCompressedInt(in));
		}
	},
	ReadWriteLock_Definition("read-write-lock-definition") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
			attrs.put(READ_LOCK_ID, readCompressedLong(in));
			attrs.put(WRITE_LOCK_ID, readCompressedLong(in));
		}
	},
	Receiver("receiver") {
	    @Override
	    void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
	        attrs.put(RECEIVER, readCompressedLong(in));
	    }
		@Override
		IAttributeType getPersistentAttribute() {
			return RECEIVER;
		}
	},
	SingleThreadedField_Instance("single-threaded-field") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(FIELD, readCompressedLong(in));
			attrs.put(RECEIVER, readCompressedLong(in));
		}
	},
	SingleThreadedField_Static("single-threaded-field") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(FIELD, readCompressedLong(in));
		}
	},
	Static_CallLocation("static-call-location") {
	    @Override
	    void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
			attrs.put(IN_CLASS, readCompressedLong(in));
			attrs.put(LINE, readCompressedInt(in));
			attrs.put(FILE, in.readUTF());
			attrs.put(LOCATION, in.readUTF());
	    }
	},
	Thread("thread") {
	    @Override
	    void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
	        attrs.put(THREAD, readCompressedLong(in));
	    }
		@Override
		IAttributeType getPersistentAttribute() {
			return THREAD;
		}
	},
	Thread_Definition("thread-definition") {		
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			attrs.put(ID, readCompressedLong(in));
			attrs.put(TYPE, readCompressedLong(in));
			attrs.put(THREAD_NAME, in.readUTF());
		}
	},	
	Time_Event("time") {
		@Override
		void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
			Long time = in.readLong();
			attrs.put(TIME, time);
			attrs.put(START_TIME, time);
			attrs.put(WALL_CLOCK, in.readUTF());
		}
		@Override
		IAttributeType getPersistentAttribute() {
			return START_TIME;
		}
	},
	Trace("trace") {
	    @Override
	    void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
	        attrs.put(TRACE, readCompressedLong(in));
	    }
		@Override
		IAttributeType getPersistentAttribute() {
			return TRACE;
		}
	},
	Trace_Node("trace-node") {
	    @Override
	    void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
	    	Long id = readCompressedLong(in);
	        attrs.put(ID, id);
	        attrs.put(TRACE, id);
	        attrs.put(PARENT_ID, readCompressedLong(in));
            attrs.put(SITE_ID, readCompressedLong(in));
	    }
		@Override
		IAttributeType getPersistentAttribute() {
			return TRACE;
		}
	}
	;
	public static final int NumEvents = values().length;
	public static final byte MINUS_ONE = Byte.MIN_VALUE; 
	private static final byte[] buf = new byte[9];
	private static final Map<String,EventType> byLabel = new HashMap<String,EventType>();
	static {
		for(EventType e : values()) {
			byLabel.put(e.label, e);
		}
	}
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
	
	public static EventType getEvent(int i) {
		return values()[i];
	}
	
	abstract void read(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException;
	
	static void readFlag(int flags, FlagType flag, Map<IAttributeType,Object> attrs) {
		attrs.put(flag, Boolean.valueOf(((flags & flag.mask()) != 0)));
	}
	
	static void readCommon(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		//attrs.put(TIME, in.readLong());
		// Added to avoid NPE when converting to raw XML
		Long startT = (Long) attrs.get(START_TIME);		
		long start = startT == null ? 0 : startT.longValue();
		attrs.put(TIME, start + readCompressedLong(in));
		if (!IdConstants.factorOutThread) {
			attrs.put(THREAD, readCompressedLong(in));
		}
	}
	
	static void readTracedEvent(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		readCommon(in, attrs);
		attrs.put(TRACE, readCompressedLong(in));
	}
	
	static void readFieldAccess(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		readTracedEvent(in, attrs);
		/*
		if (((Long)attrs.get(TIME)).longValue() == 1654719095825181L) {
			System.out.println("Here.");
		}
		*/
		attrs.put(FIELD, readCompressedLong(in));
	}
	
	static void readFieldAccessInstance(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		readFieldAccess(in, attrs);
		/*
		final int flags = readCompressedInt(in);
		readFlag(flags, UNDER_CONSTRUCTION, attrs);
		*/
		//attrs.put(RECEIVER, readCompressedLong(in));
	}
	
	static void readLockEvent(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		readTracedEvent(in, attrs);
		attrs.put(LOCK, readCompressedLong(in));
	}
	
	static void readTraceEvent(ObjectInputStream in, Map<IAttributeType,Object> attrs) throws IOException {
		readCommon(in, attrs);
		attrs.put(SITE_ID, readCompressedLong(in));
	}

	static int readCompressedInt(ObjectInputStream in) throws IOException {
		byte moreBytes = in.readByte();
		int contents;
		if (moreBytes == MINUS_ONE) {
			return -1;
		}
		if (moreBytes < 0) {
			moreBytes = (byte) -moreBytes;
			contents  = 0xffffffff << (moreBytes << 3);
		} else {	
			contents = 0;
		}
		if (moreBytes > 0) {
			readIntoBuffer(in, moreBytes);
			contents += (buf[0] & 0xff);
			if (moreBytes > 1) {
				contents += ((buf[1] & 0xff) << 8);
			}
			if (moreBytes > 2) {
				contents += ((buf[2] & 0xff) << 16);
			}
			if (moreBytes > 3) {
				contents += ((buf[3] & 0xff) << 24);
			}
		}
		/*
		if (contents < 0) {
			System.out.println("Negative");
		}
		*/
		return contents;
	}
	
	static void readIntoBuffer(ObjectInputStream in, int numBytes) throws IOException {
		int offset = 0;
		while (offset < numBytes) {
			final int read = in.read(buf, offset, numBytes - offset);
			if (read < 0) {
				throw new IOException("Couldn't read "+numBytes+" bytes: "+offset);
			}
			offset += read;
		}
	}
	
	static long readCompressedLong(ObjectInputStream in) throws IOException {
		byte moreBytes = in.readByte();
		long contents;
		if (moreBytes == MINUS_ONE) {
			return -1L;
		}
		if (moreBytes < 0) {
			moreBytes = (byte) -moreBytes;
			contents  = 0xffffffffffffffffL << (moreBytes << 3);
		} else {	
			contents = 0;
		}
		if (moreBytes > 0) {
			readIntoBuffer(in, moreBytes);
			contents += (buf[0] & 0xffL);
			if (moreBytes > 1) {
				contents += ((buf[1] & 0xffL) << 8);
			}
			if (moreBytes > 2) {
				contents += ((buf[2] & 0xffL) << 16);
			}
			if (moreBytes > 3) {
				contents += ((buf[3] & 0xffL) << 24);
			}
			if (moreBytes > 4) {
				contents += ((buf[4] & 0xffL) << 32);
			}
			if (moreBytes > 5) {
				contents += ((buf[5] & 0xffL) << 40);
			}
			if (moreBytes > 6) {
				contents += ((buf[6] & 0xffL) << 48);
			}
			if (moreBytes > 7) {
				contents += ((buf[7] & 0xffL) << 56);
			}
		}
		return contents;
	}
	
	IAttributeType getPersistentAttribute() {
		return null;
	}
	
	public static EventType findByLabel(String label) {
		EventType e = byLabel.get(label);
		if (e == null) {
			throw new IllegalArgumentException("No constant: "+label);
		}
		return e;
	}	
}
