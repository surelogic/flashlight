package com.surelogic._flashlight;

import java.io.*;
import java.text.SimpleDateFormat;

import com.surelogic._flashlight.common.EventType;

import static com.surelogic._flashlight.common.EventType.*;
import static com.surelogic._flashlight.common.FlagType.*;

public class OutputStrategyBinary extends EventVisitor {	
	private static final boolean debug = false;
	private static final String version = "1.1";
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private final IdPhantomReferenceVisitor refVisitor = new DefinitionVisitor();
	private final ObjectOutputStream f_out;
	private final byte[] buf = new byte[9];
	
	public OutputStrategyBinary(ObjectOutputStream stream) {
		f_out = stream;
		try {
			f_out.writeByte(First_Event.getByte());
			f_out.writeUTF(version);
			f_out.writeUTF(Store.getRun());
			f_out.writeByte(Environment.getByte());
			f_out.writeLong(Runtime.getRuntime().maxMemory() / (1024L * 1024L)); // "max-memory-mb"
			f_out.writeInt(Runtime.getRuntime().availableProcessors());
			f_out.writeByte(6); // num of properties following
			addProperty("user.name", f_out);
			addProperty("java.version", f_out);
			addProperty("java.vendor", f_out);
			addProperty("os.name", f_out);
			addProperty("os.arch", f_out);
			addProperty("os.version", f_out);

		} catch (IOException ioe) {
			handleIOException(ioe);
		}	
	}

	private static void addProperty(String key, ObjectOutputStream f_out) throws IOException {
		String prop = System.getProperty(key);
		if (prop == null)
			prop = "UNKNOWN";
		f_out.writeUTF(key);
		f_out.writeUTF(prop);
	}

	@Override
	void flush() {
		try {
			f_out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	void visit(final AfterIntrinsicLockAcquisition e) {
		writeLockEvent(After_IntrinsicLockAcquisition.getByte(), e);
	}
	@Override
	void visit(final AfterIntrinsicLockRelease e) {
		writeLockEvent(After_IntrinsicLockRelease.getByte(), e);
	}
	@Override
	void visit(final AfterIntrinsicLockWait e) {
		writeLockEvent(After_IntrinsicLockWait.getByte(), e);
	}
	@Override
	void visit(final AfterTrace e) {
		writeTraceEvent(After_Trace.getByte(), e);
	}
	@Override
	void visit(AfterUtilConcurrentLockAcquisitionAttempt e) {
		writeLockEvent(After_UtilConcurrentLockAcquisitionAttempt.getByte(), e);
	}
	@Override
	void visit(AfterUtilConcurrentLockReleaseAttempt e) {
		writeLockEvent(After_UtilConcurrentLockReleaseAttempt.getByte(), e);
	}
	@Override
	void visit(final BeforeIntrinsicLockAcquisition e) {
		writeLockEvent(Before_IntrinsicLockAcquisition.getByte(), e);
	}
	@Override
	void visit(final BeforeIntrinsicLockWait e) {
		writeLockEvent(Before_IntrinsicLockWait.getByte(), e);
	}
	@Override
	void visit(final BeforeTrace e) {
		//try {
			writeTraceEvent(Before_Trace.getByte(), e);
			/*
			f_out.writeUTF(e.getDeclaringTypeName());
			f_out.writeUTF(e.getLocationName());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}	
		*/
	}
	@Override
	void visit(BeforeUtilConcurrentLockAcquisitionAttempt e) {
		writeLockEvent(Before_UtilConcurrentLockAcquisitionAttempt.getByte(), e);
	}
	@Override
	void visit(final FieldDefinition e) {
		try {
			if (debug) System.out.println("Writing event: "+Field_Definition.getLabel());
			f_out.writeByte(Field_Definition.getByte());
			writeCompressedMaybeNegativeLong(e.getId());
			writeCompressedLong(e.getTypeId());
			f_out.writeUTF(e.getName());
			int flags = 0;
			if (e.isStatic()) {
				flags |= IS_STATIC.mask();
			}
			if (e.isFinal()) {
				flags |= IS_FINAL.mask();
			}
			if (e.isVolatile()) {
				flags |= IS_VOLATILE.mask();
			}
			writeCompressedInt(flags);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}		
	}

	@Override
	void visit(final FieldReadInstance e) {	
		writeFieldAccessInstance(FieldRead_Instance.getByte(), e);
	}
	
	@Override	
	void visit(final FieldReadStatic e) {
		writeFieldAccess(FieldRead_Static.getByte(), e);
	}
	@Override
	void visit(final FieldWriteInstance e) {
		writeFieldAccessInstance(FieldWrite_Instance.getByte(), e);
	}
	@Override
	void visit(final FieldWriteStatic e) {
		writeFieldAccess(FieldWrite_Static.getByte(), e);
	}
	@Override
	void visit(final FinalEvent e) {
		writeLong(Final_Event.getByte(), e.getNanoTime(), false);
		try {
			f_out.close();
		} catch (IOException e1) {
			handleIOException(e1);
		}
	}
	@Override
	void visit(GarbageCollectedObject e) {
		writeLong(GarbageCollected_Object.getByte(), e.getObjectId(), true);
	}
	
	private class DefinitionVisitor extends IdPhantomReferenceVisitor {
		@Override
		void visit(final ClassPhantomReference r) {
			try {
				if (debug) System.out.println("Writing event: "+Class_Definition.getLabel());
				f_out.writeByte(Class_Definition.getByte());
				writeCompressedLong(r.getId());
				f_out.writeUTF(r.getName());
			} catch (IOException e) {
				handleIOException(e);
			}
		}
		@Override
		void visit(final ObjectPhantomReference r) {
			writeTwoLongs(Object_Definition.getByte(), r.getId(), r.getType().getId());
		}
		@Override
		void visit(final ThreadPhantomReference r) {
			try {
				if (debug) System.out.println("Writing event: "+Thread_Definition.getLabel());
				f_out.writeByte(Thread_Definition.getByte());
				writeCompressedLong(r.getId());
				writeCompressedLong(r.getType().getId());
				f_out.writeUTF(r.getName());
			} catch (IOException e) {
				handleIOException(e);
			}
		}
	}
	
	@Override
	void visit(final ObjectDefinition e) {
		e.getObject().accept(refVisitor);
	}
	
	@Override
	void visit(final ObservedCallLocation e) {
		try {
			if (debug) System.out.println("Writing event: "+Observed_CallLocation.getLabel());
			f_out.writeByte(Observed_CallLocation.getByte());
			writeCompressedLong(e.getWithinClassId());
			writeCompressedInt(e.getLine());
			f_out.writeUTF(e.getDeclaringTypeName());
			f_out.writeUTF(e.getLocationName());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}	
	}
	
	@Override
	void visit(final ReadWriteLockDefinition e) {
		try {
			if (debug) System.out.println("Writing event: "+ReadWriteLock_Definition.getLabel());
			f_out.writeByte(ReadWriteLock_Definition.getByte());
			writeCompressedLong(e.getReadWriteLockId());
			writeCompressedLong(e.getReadLockId());
			writeCompressedLong(e.getWriteLockId());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	@Override
	void visit(final SingleThreadedFieldInstance e) {
		writeTwoLongs(SingleThreadedField_Instance.getByte(), e.getFieldId(), e.getReceiver().getId());
	}
	@Override
	void visit(final SingleThreadedFieldStatic e) {
		writeLong(SingleThreadedField_Static.getByte(), e.getFieldId(), true);
	}
	@Override
	void visit(final Time e) {
		writeLong(Time_Event.getByte(), e.getNanoTime(), false);
		try {		
			f_out.writeUTF(dateFormat.format(e.getDate()));
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final TraceNode e) {
        try {
            if (debug) System.out.println("Writing event: "+Trace_Node.getLabel());
            f_out.writeByte(Trace_Node.getByte());
            writeCompressedLong(e.getId());    
            writeCompressedLong(e.getParentId());             
            writeCompressedLong(e.getWithinClassId());
            writeCompressedInt(e.getLine());
        } catch (IOException ioe) {
            handleIOException(ioe);
        }   
	}
	
	// Common code
	
	private void writeLong(byte header, long l, boolean compress) {
		try {
			if (debug) System.out.println("Writing event: "+EventType.getEvent(header));
			f_out.writeByte(header);
			if (compress) {
				writeCompressedMaybeNegativeLong(l);
			} else {
				if (debug) System.out.println("\tLong: "+l);
				f_out.writeLong(l);
			}
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeTwoLongs(byte header, long l1, long l2) {
		try {
			if (debug) System.out.println("Writing event: "+EventType.getEvent(header));
			f_out.writeByte(header);
			writeCompressedMaybeNegativeLong(l1);
			writeCompressedMaybeNegativeLong(l2);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeCommon(byte header, WithinThreadEvent e) throws IOException {
		if (debug) System.out.println("Writing event: "+EventType.getEvent(header));
		f_out.writeByte(header);
		if (debug) System.out.println("\tTime: "+e.getNanoTime());
		f_out.writeLong(e.getNanoTime());		
		writeCompressedLong(e.getWithinThread().getId());
		writeCompressedLong(e.getWithinClassId());
	}
	
	private void writeFieldAccess_unsafe(byte header, FieldAccess e) throws IOException {
		writeCommon(header, e);
		writeCompressedMaybeNegativeLong(e.getFieldId());
		writeCompressedInt(e.getLine());
	}
	
	private void writeFieldAccess(byte header, FieldAccess e)  {
		try {
			writeFieldAccess_unsafe(header, e);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeFieldAccessInstance(byte header, FieldAccessInstance e) {
		try {
			writeFieldAccess_unsafe(header, e);
			writeCompressedInt(e.receiverUnderConstruction() ? UNDER_CONSTRUCTION.mask() : 0);
			writeCompressedLong(e.getReceiver().getId());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeLockEvent(byte header, Lock e) {
		try {
			writeCommon(header, e);
			writeCompressedLong(e.getLockObject().getId());
			writeCompressedInt(e.getLine());		
			/* FIX
			 readFlag(flags, THIS_LOCK, attrs);
		     readFlag(flags, CLASS_LOCK, attrs);
		     readFlag(flags, RELEASED_LOCK, attrs);
		     readFlag(flags, GOT_LOCK, attrs);
			 */
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeTraceEvent(byte header, Trace e) {
		try {
			writeCommon(header, e);
			writeCompressedInt(e.getLine());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void handleIOException(IOException e) {
		e.printStackTrace();
	}
	
	private void writeCompressedInt(int i) throws IOException {
		if (debug) System.out.println("\tInt: "+i);
		final int len;
		buf[1] = (byte) i;
		if (i == (i & 0xffff)) {
			if (i == (i & 0xff)) {
				len = 2;
			} else {
				// Need only bottom 2 bytes
				len = 3;
				buf[2] = (byte) (i >>> 8);
			}
		} else {
			// Need at least bottom 3 bytes
			buf[3] = (byte) (i >>> 16);

			if (i == (i & 0xffffff)) {
				// Need only bottom 3 bytes
				len = 4;
			} else {
				// Need only bottom 4 bytes
				len = 5;
				buf[4] = (byte) (i >>> 24);
			}
		}				
		buf[0] = (byte) (len-1);
		if (debug) {
			for(int j=0; j<=4; j++) {
				System.out.println("buf["+j+"] = "+buf[j]);
			}
		}		
		f_out.write(buf, 0, len);
	}
	
	private void writeCompressedMaybeNegativeLong(long l) throws IOException {
		if (l >= 0) {
			writeCompressedLong(l);
			return;
		}
		final int len;
		long mask = ~0xffffffffL;
		long masked = l & mask;
		if (masked == mask) {
			if (debug) System.out.println("\t-? Long: "+l);
			// top 4 bytes are all ones
			buf[1] = (byte) l;
			buf[2] = (byte) (l >>> 8);
			
			mask = 0xffff0000L;
			masked = l & mask;					
			if (masked == mask) {
				// top 6 bytes are all ones
				len = 3;
				buf[0] = -2;
			} else {
				// top 4 bytes are all ones
				len = 5;
				buf[0] = -4;
				buf[3] = (byte) (l >>> 16);
				buf[4] = (byte) (l >>> 24);
			}
			f_out.write(buf, 0, len);
		} else {
			writeCompressedLong(l);
		}
	}
	
	private void writeCompressedLong(long l) throws IOException {
		final byte len;		
		long mask   = 0xffffffffL;
		long masked = l & mask;
		if (l == masked) {
			// top 4 bytes are all zeroes
			writeCompressedInt((int) l);
			return;
		} 
		else {
			if (debug) System.out.println("\tLong: "+l);
			buf[1] = (byte) l;
			buf[2] = (byte) (l >>> 8);
			buf[3] = (byte) (l >>> 16);
			buf[4] = (byte) (l >>> 24);
			buf[5] = (byte) (l >>> 32);
			buf[6] = (byte) (l >>> 40);
			
			if (l == (l & 0xffffffffffffL)) {
				// Need only bottom 6 bytes
				len = 7;
			}
			else {
				len = 9;
				buf[7] = (byte) (l >>> 48);
				buf[8] = (byte) (l >>> 56);
			}
			buf[0] = (byte) (len-1);
		}
		f_out.write(buf, 0, len);
	}
}
