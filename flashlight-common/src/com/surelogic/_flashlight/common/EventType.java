package com.surelogic._flashlight.common;

import static com.surelogic._flashlight.common.AttributeType.CLASS_NAME;
import static com.surelogic._flashlight.common.AttributeType.CPUS;
import static com.surelogic._flashlight.common.AttributeType.FIELD;
import static com.surelogic._flashlight.common.AttributeType.FILE;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.IN_CLASS;
import static com.surelogic._flashlight.common.AttributeType.LINE;
import static com.surelogic._flashlight.common.AttributeType.LOCATION;
import static com.surelogic._flashlight.common.AttributeType.MEMORY_MB;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLDESC;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLNAME;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLOWNER;
import static com.surelogic._flashlight.common.AttributeType.MODIFIER;
import static com.surelogic._flashlight.common.AttributeType.PACKAGE;
import static com.surelogic._flashlight.common.AttributeType.PARENT_ID;
import static com.surelogic._flashlight.common.AttributeType.READ_LOCK_ID;
import static com.surelogic._flashlight.common.AttributeType.RECEIVER;
import static com.surelogic._flashlight.common.AttributeType.RUN;
import static com.surelogic._flashlight.common.AttributeType.SITE_ID;
import static com.surelogic._flashlight.common.AttributeType.THREAD_NAME;
import static com.surelogic._flashlight.common.AttributeType.TIME;
import static com.surelogic._flashlight.common.AttributeType.TYPE;
import static com.surelogic._flashlight.common.AttributeType.VALUE;
import static com.surelogic._flashlight.common.AttributeType.VERSION;
import static com.surelogic._flashlight.common.AttributeType.WALL_CLOCK;
import static com.surelogic._flashlight.common.AttributeType.WRITE_LOCK_ID;
import static com.surelogic._flashlight.common.FlagType.CLASS_LOCK;
import static com.surelogic._flashlight.common.FlagType.GOT_LOCK;
import static com.surelogic._flashlight.common.FlagType.RELEASED_LOCK;
import static com.surelogic._flashlight.common.FlagType.THIS_LOCK;
import static com.surelogic._flashlight.common.FlagType.UNDER_CONSTRUCTION;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The set of possible events recognized by the binary file format. Flashlight
 * is now in the hands of users, so any new entries to this <tt>enum</tt> must
 * be at the end of the file.
 * 
 * 
 * 
 */
public enum EventType {
    After_IntrinsicLockAcquisition("after-intrinsic-lock-acquisition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
        }
    },
    After_IntrinsicLockRelease("after-intrinsic-lock-release") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
        }
    },
    After_IntrinsicLockWait("after-intrinsic-lock-wait") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
        }
    },
    After_Trace("after-trace") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readTraceEvent(in, attrs);
        }
    },
    After_UtilConcurrentLockAcquisitionAttempt(
            "after-util-concurrent-lock-acquisition-attempt") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
            final int flags = readCompressedInt(in);
            readFlag(flags, GOT_LOCK, attrs);
        }
    },
    After_UtilConcurrentLockReleaseAttempt(
            "after-util-concurrent-lock-release-attempt") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
            final int flags = readCompressedInt(in);
            readFlag(flags, RELEASED_LOCK, attrs);
        }
    },
    Before_IntrinsicLockAcquisition("before-intrinsic-lock-acquisition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
            final int flags = readCompressedInt(in);
            readFlag(flags, THIS_LOCK, attrs);
            readFlag(flags, CLASS_LOCK, attrs);
        }
    },
    Before_IntrinsicLockWait("before-intrinsic-lock-wait") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
        }
    },
    Before_Trace("before-trace") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readTraceEvent(in, attrs);
            /*
             * attrs.put(FILE, in.readUTF()); attrs.put(LOCATION, in.readUTF());
             */
        }
    },
    Before_UtilConcurrentLockAcquisitionAttempt(
            "before-util-concurrent-lock-acquisition-attempt") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readLockEvent(in, attrs);
        }
    },
    Class_Definition("class-definition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
            attrs.put(CLASS_NAME, in.readUTF());
        }
    },
    Environment("environment") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(MEMORY_MB, in.readLong());
            attrs.put(CPUS, in.readInt());
            final byte numProps = in.readByte();
            for (int i = 0; i < numProps; i++) {
                attrs.put(AttributeType.getType(in.readUTF()), in.readUTF());
            }
        }
    },
    Field_Definition("field-definition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
            attrs.put(TYPE, readCompressedLong(in));
            attrs.put(FIELD, in.readUTF());
            attrs.put(MODIFIER, readCompressedInt(in));
        }
    },
    // ////////////////////////////////////////////

    FieldRead_Instance("field-read") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readFieldAccessInstance(in, attrs, false);
        }
    },
    FieldRead_Instance_WithReceiver("field-read") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readFieldAccessInstance(in, attrs, true);
        }

        @Override
        IAttributeType getPersistentAttribute() {
            return RECEIVER;
        }
    },
    FieldRead_Static("field-read") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readFieldAccess(in, attrs);
            attrs.put(RECEIVER, IdConstants.ILLEGAL_RECEIVER_ID);
            attrs.put(UNDER_CONSTRUCTION, Boolean.FALSE);
        }
    },
    FieldWrite_Instance("field-write") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readFieldAccessInstance(in, attrs, false);
        }
    },
    FieldWrite_Instance_WithReceiver("field-write") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readFieldAccessInstance(in, attrs, true);
        }

        @Override
        IAttributeType getPersistentAttribute() {
            return RECEIVER;
        }
    },
    FieldWrite_Static("field-write") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readFieldAccess(in, attrs);
            attrs.put(RECEIVER, IdConstants.ILLEGAL_RECEIVER_ID);
            attrs.put(UNDER_CONSTRUCTION, Boolean.FALSE);
        }
    },
    Final_Event("final") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.setEventTime(in.readLong());
        }
    },
    First_Event("flashlight") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(VERSION, in.readUTF());
            attrs.put(RUN, in.readUTF());
        }
    },
    GarbageCollected_Object("garbage-collected-object") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
        }
    },
    IndirectAccess("indirect-access") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            readTracedEvent(in, attrs);
            attrs.put(RECEIVER, readCompressedLong(in));
        }
    },
    Lock("lock", false) {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.setLockId(readCompressedLong(in));
        }
        /*
         * @Override IAttributeType getPersistentAttribute() { return LOCK; }
         */
    },
    Not_Under_Construction("not-under-construction", false) {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(UNDER_CONSTRUCTION, Boolean.FALSE);
        }

        @Override
        IAttributeType getPersistentAttribute() {
            return UNDER_CONSTRUCTION;
        }
    },
    Object_Definition("object-definition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
            attrs.put(TYPE, readCompressedLong(in));
        }
    },
    Observed_CallLocation("call-location") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(SITE_ID, readCompressedLong(in));
            attrs.put(IN_CLASS, readCompressedLong(in));
            attrs.put(LINE, readCompressedInt(in));
        }
    },
    ReadWriteLock_Definition("read-write-lock-definition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
            attrs.put(READ_LOCK_ID, readCompressedLong(in));
            attrs.put(WRITE_LOCK_ID, readCompressedLong(in));
        }
    },
    Receiver("receiver", false) {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(RECEIVER, readCompressedLong(in));
        }

        @Override
        IAttributeType getPersistentAttribute() {
            return RECEIVER;
        }
    },
    SelectedPackage("selected-package") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.setEventTime(in.readLong());
            attrs.put(PACKAGE, in.readUTF());
        }
    },
    SingleThreadedField_Instance("single-threaded-field") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(FIELD, readCompressedLong(in));
            attrs.put(RECEIVER, readCompressedLong(in));
        }
    },
    SingleThreadedField_Static("single-threaded-field") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(FIELD, readCompressedLong(in));
        }
    },
    Static_CallLocation("static-call-location") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
            attrs.put(IN_CLASS, readCompressedLong(in));
            attrs.put(LINE, readCompressedInt(in));
            attrs.put(FILE, in.readUTF());
            attrs.put(LOCATION, in.readUTF());
            int hasMore = readCompressedInt(in);
            if (hasMore == 1) {
                attrs.put(METHODCALLOWNER, in.readUTF());
                attrs.put(METHODCALLNAME, in.readUTF());
                attrs.put(METHODCALLDESC, in.readUTF());
            }
        }
    },
    Thread("thread", false) {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.setThreadId(readCompressedLong(in));
        }
        /*
         * @Override IAttributeType getPersistentAttribute() { return THREAD; }
         */
    },
    Thread_Definition("thread-definition") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(ID, readCompressedLong(in));
            attrs.put(TYPE, readCompressedLong(in));
            attrs.put(THREAD_NAME, in.readUTF());
        }
    },
    Time_Event("time") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            final long time = in.readLong();
            attrs.setEventTime(time);
            attrs.setStartTime(time);
            attrs.put(TIME, time); // Needed for unmodified code
            // attrs.put(START_TIME, time);
            attrs.put(WALL_CLOCK, in.readUTF());
        }
        /*
         * @Override IAttributeType getPersistentAttribute() { return
         * START_TIME; }
         */
    },
    @Deprecated
    Trace("trace") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.setTraceId(readCompressedLong(in));
        }
        /*
         * @Override IAttributeType getPersistentAttribute() { return TRACE; }
         */
    },
    Trace_Node("trace-node") {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            final Long id = readCompressedLong(in);
            // attrs.put(ID, id);
            attrs.setTraceId(id);
            attrs.put(PARENT_ID, readCompressedLong(in));
            attrs.put(SITE_ID, readCompressedLong(in));
        }
        /*
         * No longer factoring out trace ids
         * 
         * @Override IAttributeType getPersistentAttribute() { return TRACE; }
         */
    },
    Under_Construction("under-construction", false) {
        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            attrs.put(UNDER_CONSTRUCTION, Boolean.TRUE);
        }

        @Override
        IAttributeType getPersistentAttribute() {
            return UNDER_CONSTRUCTION;
        }
    },
    FieldAssignment_Instance("field-assignment") {
        @Override
        public void read(final ObjectInputStream in,
                final BinaryAttributes attrs) throws IOException {
            attrs.put(FIELD, in.readLong());
            attrs.put(VALUE, in.readLong());
            attrs.put(RECEIVER, in.readLong());
        }
    },
    FieldAssignment_Static("field-assignment") {
        @Override
        public void read(final ObjectInputStream in,
                final BinaryAttributes attrs) throws IOException {
            attrs.put(FIELD, in.readLong());
            attrs.put(VALUE, in.readLong());
        }
    },
    Checkpoint("checkpoint") {

        @Override
        void read(final ObjectInputStream in, final BinaryAttributes attrs)
                throws IOException {
            final long time = in.readLong();
            attrs.setEventTime(time);
            attrs.setStartTime(time);
            attrs.put(TIME, time); // Needed for unmodified code
            // attrs.put(START_TIME, time);
        }

    };

    public static final int NumEvents = values().length;
    public static final byte MINUS_ONE = Byte.MIN_VALUE;
    private static final byte[] buf = new byte[9];
    private static final Map<String, EventType> byLabel = new HashMap<String, EventType>();
    private static final EventType[] values;

    static {
        values = values();
        for (final EventType e : values()) {
            byLabel.put(e.label, e);
        }
    }
    private final String label;
    /**
     * Whether this event should be processed by XML handlers
     */
    private final boolean process;

    private EventType(final String l) {
        this(l, true);
    }

    private EventType(final String l, final boolean process) {
        label = l;
        this.process = process;
    }

    /**
     * @return true if this event should be processed by XML handlers
     */
    public boolean processEvent() {
        return process;
    }

    public String getLabel() {
        return label;
    }

    public byte getByte() {
        return (byte) ordinal();
    }

    public static EventType getEvent(final int i) {
        return values[i];
    }

    abstract void read(ObjectInputStream in, BinaryAttributes attrs)
            throws IOException;

    static void readFlag(final int flags, final FlagType flag,
            final BinaryAttributes attrs) {
        final boolean value = (flags & flag.mask()) != 0;
        if (value) {
            attrs.put(flag, Boolean.TRUE);
        } else {
            attrs.remove(flag);
        }
    }

    static void readCommon(final ObjectInputStream in,
            final BinaryAttributes attrs) throws IOException {
        // attrs.put(TIME, in.readLong());
        // Added to avoid NPE when converting to raw XML
        final long start = attrs.getStartTime();
        attrs.setEventTime(start + readCompressedLong(in));
        if (!IdConstants.factorOutThread) {
            attrs.setThreadId(readCompressedLong(in));
        }
    }

    static void readTracedEvent(final ObjectInputStream in,
            final BinaryAttributes attrs) throws IOException {
        readCommon(in, attrs);
        attrs.setTraceId(readCompressedLong(in));
    }

    static void readFieldAccess(final ObjectInputStream in,
            final BinaryAttributes attrs) throws IOException {
        readTracedEvent(in, attrs);
        /*
         * if (((Long)attrs.get(TIME)).longValue() == 1654719095825181L) {
         * System.out.println("Here."); }
         */
        attrs.put(FIELD, readCompressedLong(in));
    }

    static void readFieldAccessInstance(final ObjectInputStream in,
            final BinaryAttributes attrs, final boolean withReceiver)
            throws IOException {
        readFieldAccess(in, attrs);
        /*
         * final int flags = readCompressedInt(in); readFlag(flags,
         * UNDER_CONSTRUCTION, attrs);
         */
        if (withReceiver) {
            attrs.put(RECEIVER, readCompressedLong(in));
        }
    }

    static void readLockEvent(final ObjectInputStream in,
            final BinaryAttributes attrs) throws IOException {
        readTracedEvent(in, attrs);
        if (!IdConstants.factorOutLock) {
            attrs.setLockId(readCompressedLong(in));
        }
    }

    @Deprecated
    static void readTraceEvent(final ObjectInputStream in,
            final BinaryAttributes attrs) throws IOException {
        readCommon(in, attrs);
        attrs.put(SITE_ID, readCompressedLong(in));
    }

    static int readCompressedInt(final ObjectInputStream in) throws IOException {
        byte moreBytes = in.readByte();
        int contents;
        if (moreBytes == MINUS_ONE) {
            return -1;
        }
        if (moreBytes < 0) {
            moreBytes = (byte) -moreBytes;
            contents = 0xffffffff << (moreBytes << 3);
        } else {
            contents = 0;
        }
        if (moreBytes > 0) {
            readIntoBuffer(in, moreBytes);
            contents += buf[0] & 0xff;
            if (moreBytes > 1) {
                contents += (buf[1] & 0xff) << 8;
            }
            if (moreBytes > 2) {
                contents += (buf[2] & 0xff) << 16;
            }
            if (moreBytes > 3) {
                contents += (buf[3] & 0xff) << 24;
            }
        }
        /*
         * if (contents < 0) { System.out.println("Negative"); }
         */
        return contents;
    }

    static void readIntoBuffer(final ObjectInputStream in, final int numBytes)
            throws IOException {
        /*
         * if (numBytes > buf.length) { throw new
         * IllegalArgumentException("Too many bytes for buffer: "); }
         */
        int offset = 0;
        while (offset < numBytes) {
            final int read = in.read(buf, offset, numBytes - offset);
            if (read < 0) {
                throw new IOException("Couldn't read " + numBytes + " bytes: "
                        + offset);
            }
            offset += read;
        }
    }

    static long readCompressedLong(final ObjectInputStream in)
            throws IOException {
        byte moreBytes = in.readByte();
        long contents;
        if (moreBytes == MINUS_ONE) {
            return -1L;
        }
        if (moreBytes < 0) {
            moreBytes = (byte) -moreBytes;
            contents = 0xffffffffffffffffL << (moreBytes << 3);
        } else {
            contents = 0;
        }
        if (moreBytes > 0) {
            readIntoBuffer(in, moreBytes);
            contents += buf[0] & 0xffL;
            if (moreBytes > 1) {
                contents += (buf[1] & 0xffL) << 8;
            }
            if (moreBytes > 2) {
                contents += (buf[2] & 0xffL) << 16;
            }
            if (moreBytes > 3) {
                contents += (buf[3] & 0xffL) << 24;
            }
            if (moreBytes > 4) {
                contents += (buf[4] & 0xffL) << 32;
            }
            if (moreBytes > 5) {
                contents += (buf[5] & 0xffL) << 40;
            }
            if (moreBytes > 6) {
                contents += (buf[6] & 0xffL) << 48;
            }
            if (moreBytes > 7) {
                contents += (buf[7] & 0xffL) << 56;
            }
        }
        return contents;
    }

    IAttributeType getPersistentAttribute() {
        return null;
    }

    public static EventType findByLabel(final String label) {
        final EventType e = byLabel.get(label);
        if (e == null) {
            throw new IllegalArgumentException("No constant: " + label);
        }
        return e;
    }
}
