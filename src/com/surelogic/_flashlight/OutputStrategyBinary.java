package com.surelogic._flashlight;

import java.io.*;

import static com.surelogic._flashlight.common.EventType.*;
import static com.surelogic._flashlight.common.FlagType.*;

public class OutputStrategyBinary extends EventVisitor {	
	private final IdPhantomReferenceVisitor refVisitor = new DefinitionVisitor();
	private final ObjectOutputStream f_out;
	
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
		writeTraceEvent(Before_Trace.getByte(), e);
	}
	@Override
	void visit(BeforeUtilConcurrentLockAcquisitionAttempt e) {
		writeLockEvent(Before_UtilConcurrentLockAcquisitionAttempt.getByte(), e);
	}
	@Override
	void visit(final FieldDefinition e) {
		try {
			f_out.writeByte(Field_Definition.getByte());
			f_out.writeLong(e.getId());
			f_out.writeLong(e.getTypeId());
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
				f_out.writeLong(r.getId());
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
				f_out.writeLong(r.getId());
				f_out.writeLong(r.getType().getId());
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
			f_out.writeLong(e.getReadWriteLockId());
			f_out.writeLong(e.getReadLockId());
			f_out.writeLong(e.getWriteLockId());
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
			f_out.writeLong(l);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeTwoLongs(byte header, long l1, long l2) {
		try {
			f_out.writeByte(header);
			f_out.writeLong(l1);
			f_out.writeLong(l2);
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeCommon(byte header, WithinThreadEvent e) throws IOException {
		f_out.writeByte(header);
		f_out.writeLong(e.getNanoTime());
		f_out.writeLong(e.getWithinThread().getId());
		f_out.writeLong(e.getWithinClassId());
	}
	
	private void writeFieldAccess_unsafe(byte header, FieldAccess e) throws IOException {
		writeCommon(header, e);
		f_out.writeLong(e.getFieldId());
		f_out.writeInt(e.getLine());
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
			f_out.writeInt(e.receiverUnderConstruction() ? UNDER_CONSTRUCTION.mask() : 0);
			f_out.writeLong(e.getReceiver().getId());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeLockEvent(byte header, Lock e) {
		try {
			writeCommon(header, e);
			f_out.writeLong(e.getLockObject().getId());
			f_out.writeInt(e.getLine());		
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void writeTraceEvent(byte header, Trace e) {
		try {
			writeCommon(header, e);
			f_out.writeInt(e.getLine());
		} catch (IOException ioe) {
			handleIOException(ioe);
		}
	}
	
	private void handleIOException(IOException e) {
		e.printStackTrace();
	}
}
