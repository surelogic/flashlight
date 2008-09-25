package com.surelogic._flashlight;

import java.io.*;

import static com.surelogic._flashlight.common.EventType.*;
import static com.surelogic._flashlight.common.FlagType.*;

public class OutputStrategyBinary extends EventVisitor {	
	private final IdPhantomReferenceVisitor refVisitor = new DefinitionVisitor();
	private final ObjectOutputStream f_out;
	private final byte[] buf = new byte[9];
	
	public OutputStrategyBinary(ObjectOutputStream stream) {
		f_out = stream;
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
		try {
			writeTraceEvent(Before_Trace.getByte(), e);
			f_out.writeUTF(e.getDeclaringTypeName());
			f_out.writeUTF(e.getLocationName());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}	
	}
	@Override
	void visit(BeforeUtilConcurrentLockAcquisitionAttempt e) {
		writeLockEvent(Before_UtilConcurrentLockAcquisitionAttempt.getByte(), e);
	}
	@Override
	void visit(final FieldDefinition e) {
		try {
			f_out.writeByte(Field_Definition.getByte());
			writeCompressedLong(e.getId());
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
		writeLong(Final_Event.getByte(), e.getNanoTime());
	}
	@Override
	void visit(GarbageCollectedObject e) {
		writeLong(GarbageCollected_Object.getByte(), e.getObjectId());
	}
	
	private class DefinitionVisitor extends IdPhantomReferenceVisitor {
		@Override
		void visit(final ClassPhantomReference r) {
			try {
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
	void visit(final ReadWriteLockDefinition e) {
		try {
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
		writeLong(SingleThreadedField_Static.getByte(), e.getFieldId());
	}
	@Override
	void visit(final Time e) {
		writeLong(Time_Event.getByte(), e.getNanoTime());
	}

	// Common code
	
	private void writeLong(byte header, long l) {
		try {
			f_out.writeByte(header);
			writeCompressedLong(l);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeTwoLongs(byte header, long l1, long l2) {
		try {
			f_out.writeByte(header);
			writeCompressedLong(l1);
			writeCompressedLong(l2);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeCommon(byte header, WithinThreadEvent e) throws IOException {
		f_out.writeByte(header);
		writeCompressedLong(e.getNanoTime());
		writeCompressedLong(e.getWithinThread().getId());
		writeCompressedLong(e.getWithinClassId());
	}
	
	private void writeFieldAccess_unsafe(byte header, FieldAccess e) throws IOException {
		writeCommon(header, e);
		writeCompressedLong(e.getFieldId());
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
		f_out.write(buf, 0, len);
	}
	
	private void writeCompressedLong(long l) throws IOException {
		final byte len;
		if (l == (l & 0xffffffff)) {
			writeCompressedInt((int) l);
		} 
		else {
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
			f_out.write(buf, 0, len);
		}
	}
}
