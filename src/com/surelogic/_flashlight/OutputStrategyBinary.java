package com.surelogic._flashlight;

import static com.surelogic._flashlight.common.EventType.After_IntrinsicLockAcquisition;
import static com.surelogic._flashlight.common.EventType.After_IntrinsicLockRelease;
import static com.surelogic._flashlight.common.EventType.After_IntrinsicLockWait;
import static com.surelogic._flashlight.common.EventType.After_UtilConcurrentLockAcquisitionAttempt;
import static com.surelogic._flashlight.common.EventType.After_UtilConcurrentLockReleaseAttempt;
import static com.surelogic._flashlight.common.EventType.Before_IntrinsicLockAcquisition;
import static com.surelogic._flashlight.common.EventType.Before_IntrinsicLockWait;
import static com.surelogic._flashlight.common.EventType.Before_UtilConcurrentLockAcquisitionAttempt;
import static com.surelogic._flashlight.common.EventType.Class_Definition;
import static com.surelogic._flashlight.common.EventType.Environment;
import static com.surelogic._flashlight.common.EventType.FieldAssignment_Instance;
import static com.surelogic._flashlight.common.EventType.FieldAssignment_Static;
import static com.surelogic._flashlight.common.EventType.FieldRead_Instance;
import static com.surelogic._flashlight.common.EventType.FieldRead_Instance_WithReceiver;
import static com.surelogic._flashlight.common.EventType.FieldRead_Static;
import static com.surelogic._flashlight.common.EventType.FieldWrite_Instance;
import static com.surelogic._flashlight.common.EventType.FieldWrite_Instance_WithReceiver;
import static com.surelogic._flashlight.common.EventType.FieldWrite_Static;
import static com.surelogic._flashlight.common.EventType.Field_Definition;
import static com.surelogic._flashlight.common.EventType.Final_Event;
import static com.surelogic._flashlight.common.EventType.First_Event;
import static com.surelogic._flashlight.common.EventType.GarbageCollected_Object;
import static com.surelogic._flashlight.common.EventType.IndirectAccess;
import static com.surelogic._flashlight.common.EventType.Lock;
import static com.surelogic._flashlight.common.EventType.Not_Under_Construction;
import static com.surelogic._flashlight.common.EventType.Object_Definition;
import static com.surelogic._flashlight.common.EventType.Observed_CallLocation;
import static com.surelogic._flashlight.common.EventType.ReadWriteLock_Definition;
import static com.surelogic._flashlight.common.EventType.Receiver;
import static com.surelogic._flashlight.common.EventType.SelectedPackage;
import static com.surelogic._flashlight.common.EventType.SingleThreadedField_Instance;
import static com.surelogic._flashlight.common.EventType.SingleThreadedField_Static;
import static com.surelogic._flashlight.common.EventType.Static_CallLocation;
import static com.surelogic._flashlight.common.EventType.Thread;
import static com.surelogic._flashlight.common.EventType.Thread_Definition;
import static com.surelogic._flashlight.common.EventType.Time_Event;
import static com.surelogic._flashlight.common.EventType.Trace;
import static com.surelogic._flashlight.common.EventType.Trace_Node;
import static com.surelogic._flashlight.common.EventType.Under_Construction;
import static com.surelogic._flashlight.common.FlagType.CLASS_LOCK;
import static com.surelogic._flashlight.common.FlagType.GOT_LOCK;
import static com.surelogic._flashlight.common.FlagType.IS_FINAL;
import static com.surelogic._flashlight.common.FlagType.IS_STATIC;
import static com.surelogic._flashlight.common.FlagType.IS_VOLATILE;
import static com.surelogic._flashlight.common.FlagType.RELEASED_LOCK;
import static com.surelogic._flashlight.common.FlagType.THIS_LOCK;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import com.surelogic._flashlight.common.EventType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.trace.TraceNode;

public class OutputStrategyBinary extends EventVisitor {
	private static final boolean debug = false;
	static final String version = "1.1";
	private static final long NO_VALUE = Long.MIN_VALUE;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");
	private final IdPhantomReferenceVisitor refVisitor = new DefinitionVisitor();
	private final ObjectOutputStream f_out;
	private final byte[] buf = new byte[9];
	private final long start;
	private long lastThread = NO_VALUE;
	private long lastTrace = NO_VALUE;
	private long lastReceiver = NO_VALUE;
	private long lastLock = NO_VALUE;
	private Boolean lastUnderConstruction = null;
	private final int[] stats = new int[EventType.values().length];

	/*
	 * private boolean lastWasTraceNode = false; private int total = 0, same =
	 * 0; private final int[] counts = new int[EventType.NumEvents]; private int
	 * total = 0; private int traceBytes = 0; private int commonBytes = 0;
	 * private int tracedBytes = 0; private int fieldBytes = 0; private int
	 * totalInts = 0, compressedInts = 0;
	 */
	static final Factory factory = new Factory() {
		public EventVisitor create(final OutputStream stream,
				final String encoding, final Time time) throws IOException {
			return new OutputStrategyBinary(stream, time);
		}
	};

	OutputStrategyBinary(final OutputStream stream, final Time time)
			throws IOException {
		f_out = new ObjectOutputStream(stream);
		start = time.getNanoTime();
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

		} catch (final IOException ioe) {
			handleIOException(ioe);
		}

	}

	private static void addProperty(final String key,
			final ObjectOutputStream f_out) throws IOException {
		String prop = System.getProperty(key);
		if (prop == null) {
			prop = "UNKNOWN";
		}
		f_out.writeUTF(key);
		f_out.writeUTF(prop);
	}

	@Override
	void printStats() {
		System.out.println("Event stats:");
		for (int i = 0; i < stats.length; i++) {
			if (stats[i] > 0) {
				System.out.println(stats[i] + "\t : " + EventType.getEvent(i));
			}
		}
	}

	@Override
	void flush() {
		try {
			f_out.flush();
		} catch (final IOException e) {
			Store.logAProblem("Unable to flush output", e);
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

	/*
	 * @Override void visit(final AfterTrace e) {
	 * writeTraceEvent(After_Trace.getByte(), e); }
	 */
	@Override
	void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
		final int flag = e.gotTheLock() ? GOT_LOCK.mask() : 0;
		writeLockEvent(After_UtilConcurrentLockAcquisitionAttempt.getByte(), e,
				flag);
	}

	@Override
	void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
		final int flag = e.releasedTheLock() ? RELEASED_LOCK.mask() : 0;
		writeLockEvent(After_UtilConcurrentLockReleaseAttempt.getByte(), e,
				flag);
	}

	@Override
	void visit(final BeforeIntrinsicLockAcquisition e) {
		int flags = 0;
		if (e.isLockThis()) {
			flags |= THIS_LOCK.mask();
		} else if (e.isLockClass()) {
			flags |= CLASS_LOCK.mask();
		}
		writeLockEvent(Before_IntrinsicLockAcquisition.getByte(), e, flags);
	}

	@Override
	void visit(final BeforeIntrinsicLockWait e) {
		writeLockEvent(Before_IntrinsicLockWait.getByte(), e);
	}

	/*
	 * @Override void visit(final BeforeTrace e) { //try {
	 * writeTraceEvent(Before_Trace.getByte(), e);
	 * writeUTF(e.getDeclaringTypeName()); writeUTF(e.getLocationName()); }
	 * catch (IOException ioe) { handleIOException(ioe); } }
	 */
	@Override
	void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
		writeLockEvent(Before_UtilConcurrentLockAcquisitionAttempt.getByte(), e);
	}

	@Override
	void visit(final FieldAssignment e) {
		try {
			if (e.hasReceiver()) {
				writeHeader(FieldAssignment_Instance.getByte());
			} else {
				writeHeader(FieldAssignment_Static.getByte());
			}
			f_out.writeLong(e.getFieldId());
			f_out.writeLong(e.getValueId());
			if (e.hasReceiver()) {
				f_out.writeLong(e.getReceiverId());
			}
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final FieldDefinition e) {
		try {
			writeHeader(Field_Definition.getByte());
			writeCompressedMaybeNegativeLong(e.getId());
			writeCompressedLong(e.getTypeId());
			writeUTF(e.getName());
			writeCompressedInt(e.getVisibility());
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
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final FieldReadInstance e) {
		writeFieldAccessInstance(FieldRead_Instance.getByte(), e);
	}

	@Override
	void visit(final FieldReadStatic e) {
		writeFieldAccessStatic(FieldRead_Static.getByte(), e);
	}

	@Override
	void visit(final FieldWriteInstance e) {
		writeFieldAccessInstance(FieldWrite_Instance.getByte(), e);
	}

	@Override
	void visit(final FieldWriteStatic e) {
		writeFieldAccessStatic(FieldWrite_Static.getByte(), e);
	}

	@Override
	void visit(final FinalEvent e) {
		writeLong(Final_Event.getByte(), e.getNanoTime(), false);
		try {
			f_out.close();
		} catch (final IOException e1) {
			handleIOException(e1);
		}
	}

	@Override
	void visit(final GarbageCollectedObject e) {
		writeLong(GarbageCollected_Object.getByte(), e.getObjectId(), true);
	}

	@Override
	void visit(final IndirectAccess e) {
		try {
			writeTracedEvent(IndirectAccess.getByte(), e);
			final long id = e.getReceiver().getId();
			// final boolean same = isSameReceiver(id);
			// if (!same) {
			writeCompressedLong(id);
			// }
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	private class DefinitionVisitor extends IdPhantomReferenceVisitor {
		@Override
		void visit(final ClassPhantomReference r) {
			try {
				writeHeader(Class_Definition.getByte());
				writeCompressedLong(r.getId());
				writeUTF(r.getName());
			} catch (final IOException e) {
				handleIOException(e);
			}
		}

		@Override
		void visit(final ObjectDefinition defn, final ObjectPhantomReference r) {
			writeTwoLongs(Object_Definition.getByte(), r.getId(), defn
					.getType().getId());
		}

		@Override
		void visit(final ObjectDefinition defn, final ThreadPhantomReference r) {
			try {
				writeHeader(Thread_Definition.getByte());
				writeCompressedLong(r.getId());
				writeCompressedLong(defn.getType().getId());
				writeUTF(r.getName());
				// System.out.println("Thread "+r.getId()+" of type "+defn.getType().getId());
			} catch (final IOException e) {
				handleIOException(e);
			}
		}
	}

	@Override
	void visit(final ObjectDefinition e) {
		e.getObject().accept(e, refVisitor);
	}

	@Override
	void visit(final ObservedCallLocation e) {
		try {
			writeHeader(Observed_CallLocation.getByte());
			writeCompressedMaybeNegativeLong(e.getSiteId());
			writeCompressedLong(e.getWithinClassId());
			writeCompressedInt(e.getLine());
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final ReadWriteLockDefinition e) {
		try {
			writeHeader(ReadWriteLock_Definition.getByte());
			writeCompressedLong(e.getReadWriteLockId());
			writeCompressedLong(e.getReadLockId());
			writeCompressedLong(e.getWriteLockId());
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final SelectedPackage e) {
		try {
			writeLong_unsafe(SelectedPackage.getByte(), e.getNanoTime(), false);
			writeUTF(e.name);
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final SingleThreadedFieldInstance e) {
		writeTwoLongs(SingleThreadedField_Instance.getByte(), e.getFieldId(), e
				.getReceiver().getId());
	}

	@Override
	void visit(final SingleThreadedFieldStatic e) {
		writeLong(SingleThreadedField_Static.getByte(), e.getFieldId(), true);
	}

	@Override
	void visit(final StaticCallLocation e) {
		try {
			writeLong_unsafe(Static_CallLocation.getByte(), e.getSiteId(), true);
			writeCompressedLong(e.getWithinClassId());
			writeCompressedMaybeNegativeInt(e.getLine());
			writeUTF(e.getFileName());
			writeUTF(e.getLocationName());
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	void visit(final Time e) {
		try {
			writeLong_unsafe(Time_Event.getByte(), e.getNanoTime(), false);
			writeUTF(dateFormat.format(e.getDate()));
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	@Override
	public void visit(final TraceNode e) {
		try {
			int bytes = 1;
			writeHeader(Trace_Node.getByte());
			bytes += writeCompressedLong(e.getId());
			bytes += writeCompressedLong(e.getParentId());
			bytes += writeCompressedLong(e.getSiteId());
			// traceBytes += bytes;

			lastTrace = e.getId();
			// lastWasTraceNode = true;
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	// Common code
	private void writeUTF(final String s) throws IOException {
		if (IdConstants.writeOutput) {
			f_out.writeUTF(s);
		}
	}

	private void writeHeader(final byte header) throws IOException {
		if (debug) {
			System.out.println("Writing event: " + EventType.getEvent(header));
		}
		stats[header]++;
		/*
		 * counts[header]++; total++; if ((total & 0xfffff) == 0) {
		 * System.err.println("Total events = "+total); for(int i=0;
		 * i<counts.length; i++) { int count = counts[i]; if (count == 0) {
		 * continue; }
		 * System.err.println("\t"+EventType.getEvent(i)+" = "+count); }
		 * System.err.println("Trace  bytes = "+traceBytes);
		 * System.err.println("Common bytes = "+commonBytes);
		 * System.err.println("Traced bytes = "+tracedBytes);
		 * System.err.println("Field bytes  = "+fieldBytes); }
		 */
		if (IdConstants.writeOutput) {
			f_out.writeByte(header);
		}
	}

	private void writeLong_unsafe(final byte header, final long l,
			final boolean compress) throws IOException {
		writeHeader(header);
		if (compress) {
			writeCompressedMaybeNegativeLong(l);
		} else {
			if (debug) {
				System.out.println("\tLong: " + l);
			}
			if (IdConstants.writeOutput) {
				f_out.writeLong(l);
			}
		}
	}

	private void writeLong(final byte header, final long l,
			final boolean compress) {
		try {
			writeLong_unsafe(header, l, compress);
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	private void writeTwoLongs(final byte header, final long l1, final long l2) {
		try {
			writeHeader(header);
			writeCompressedMaybeNegativeLong(l1);
			writeCompressedMaybeNegativeLong(l2);
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	private void writeThread(final long tid) throws IOException {
		if (tid != lastThread) {
			lastThread = tid;
			writeLong_unsafe(Thread.getByte(), tid, true);
		}
	}

	@SuppressWarnings( { "unused", "deprecation" })
	private void writeTrace(final long tid) throws IOException {
		/*
		 * if (lastWasTraceNode) { total++; if (tid != lastTrace) { same++; } if
		 * ((total & 0xfff) == 0) {
		 * System.err.println(same+" same out of "+total+" TraceNode->Trace"); }
		 * }
		 */
		if (tid != lastTrace) {
			lastTrace = tid;
			writeLong_unsafe(Trace.getByte(), tid, true);
		}
	}

	@SuppressWarnings("unused")
	private void writeReceiver(final long id) throws IOException {
		if (id != lastReceiver) {
			lastReceiver = id;
			writeLong_unsafe(Receiver.getByte(), id, true);
		}
	}

	private boolean isSameReceiver(final long id) {
		if (id != lastReceiver) {
			lastReceiver = id;
			return false;
		}
		return true;
	}

	private void writeLock(final long id) throws IOException {
		if (id != lastLock) {
			lastLock = id;
			writeLong_unsafe(Lock.getByte(), id, true);
		}
	}

	private void writeUnderConstruction(final Boolean constructing)
			throws IOException {
		if (constructing != lastUnderConstruction) {
			lastUnderConstruction = constructing;
			writeHeader((constructing ? Under_Construction
					: Not_Under_Construction).getByte());
		}
	}

	private void writeCommon(final byte header, final WithinThreadEvent e)
			throws IOException {
		writeThread(e.getWithinThread().getId());

		int bytes = 9; // header, time
		writeHeader(header);
		if (debug) {
			System.out.println("\tTime: " + e.getNanoTime());
		}
		// f_out.writeLong(e.getNanoTime());
		writeCompressedLong(e.getNanoTime() - start);
		if (!IdConstants.factorOutThread) {
			bytes += writeCompressedLong(e.getWithinThread().getId());
		}
		// commonBytes += bytes;
	}

	/*
	 * private long lastTrace = 0; private int totalTraces = 0, sameTraces = 0;
	 */
	private void writeTracedEvent(final byte header, final TracedEvent e)
			throws IOException {
		writeCommon(header, e);
		/* tracedBytes += */writeCompressedLong(e.getTraceId());
		/*
		 * totalTraces++; if (lastTrace == e.getTraceId()) { sameTraces++; }
		 * else { lastTrace = e.getTraceId(); } if ((totalTraces & 0xffff) == 0)
		 * { System.err.println(sameTraces+" same as last out of "+totalTraces);
		 * }
		 */
	}

	private void writeFieldAccess_unsafe(final byte header, final FieldAccess e)
			throws IOException {
		writeTracedEvent(header, e);
		/* fieldBytes += */writeCompressedMaybeNegativeLong(e.getFieldId());
	}

	private void writeFieldAccessStatic(final byte header,
			final FieldAccessStatic e) {
		try {
			writeUnderConstruction(e.classUnderConstruction());
			writeFieldAccess_unsafe(header, e);
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	private void writeFieldAccessInstance(byte header,
			final FieldAccessInstance e) {
		try {
			writeUnderConstruction(e.receiverUnderConstruction());
			final long id = e.getReceiver().getId();
			final boolean same = isSameReceiver(id);
			if (!same) {
				header = (e.isWrite() ? FieldWrite_Instance_WithReceiver
						: FieldRead_Instance_WithReceiver).getByte();
			}
			writeFieldAccess_unsafe(header, e);

			/* fieldBytes += */// writeCompressedInt(e.receiverUnderConstruction()
			// ? UNDER_CONSTRUCTION.mask() : 0);
			/*
			 * total++; if (id == lastReceiver) { same++; } else { lastReceiver
			 * = id; } if ((total & 0xffff) == 0) {
			 * System.err.println(same+" same as last out of "+total); }
			 */
			if (!same) {
				/* fieldBytes += */writeCompressedLong(id);
			}
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	private void writeLockEvent(final byte header, final Lock e) {
		try {
			if (IdConstants.factorOutLock) {
				writeLock(e.getLockObject().getId());
			}
			writeTracedEvent(header, e);
			if (!IdConstants.factorOutLock) {
				writeCompressedLong(e.getLockObject().getId());
			}
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	private void writeLockEvent(final byte header, final Lock e, final int flags) {
		try {
			writeLockEvent(header, e);
			writeCompressedInt(flags);
		} catch (final IOException ioe) {
			handleIOException(ioe);
		}
	}

	/*
	 * private void writeTraceEvent(byte header, Trace e) { try {
	 * writeCommon(header, e); writeCompressedMaybeNegativeLong(e.getSiteId());
	 * } catch (IOException ioe) { handleIOException(ioe); } }
	 */
	private void handleIOException(final IOException e) {
		Store.logAProblem("Problem while outputting", e);
	}

	private int writeCompressedMaybeNegativeInt(final int i) throws IOException {
		if (i == -1) {
			if (debug) {
				System.out.println("\tInt: " + i);
			}
			if (IdConstants.writeOutput) {
				buf[0] = EventType.MINUS_ONE;
				if (debug) {
					System.out.println("buf[0] = " + buf[0]);
				}
				f_out.write(buf, 0, 1);
			}
			return 1;
		}
		// FIX handle other cases (not needed yet)
		return writeCompressedInt(i);
	}

	private int writeCompressedInt(final int i) throws IOException {
		if (debug) {
			System.out.println("\tInt: " + i);
		}
		final int len;
		buf[1] = (byte) (i & 0xff);
		if (i == (i & 0xffff)) {
			if (i == (i & 0xff)) {
				len = i == 0 ? 1 : 2;
			} else {
				// Need only bottom 2 bytes
				len = 3;
				buf[2] = (byte) (i >>> 8);
			}
		} else {
			// Need at least bottom 3 bytes
			buf[2] = (byte) (i >>> 8);
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
		buf[0] = (byte) (len - 1);
		if (debug) {
			for (int j = 0; j < len; j++) {
				System.out.println("buf[" + j + "] = " + buf[j]);
			}
		}
		if (IdConstants.writeOutput) {
			f_out.write(buf, 0, len);
		}
		/*
		 * totalInts += len; compressedInts += (4-len); if (totalInts >
		 * 10000000) { System.err.println("Total ints = "+totalInts);
		 * System.err.println("Compressed = "+compressedInts); }
		 */
		return len;
	}

	private int writeCompressedMaybeNegativeLong(final long l)
			throws IOException {
		if (l >= 0) {
			return writeCompressedLong(l);
		}
		if (l == -1L) {
			if (IdConstants.writeOutput) {
				buf[0] = EventType.MINUS_ONE;
				f_out.write(buf, 0, 1);
			}
			return 1;
		}
		final int len;
		long mask = ~0xffffffffL;
		long masked = l & mask;
		if (masked == mask) {
			if (debug) {
				System.out.println("\t-? Long: " + l);
			}
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
			if (IdConstants.writeOutput) {
				f_out.write(buf, 0, len);
			}
		} else {
			return writeCompressedLong(l);
		}
		// totalInts += len;
		// compressedInts += (8-len);
		return len;
	}

	private int writeCompressedLong(final long l) throws IOException {
		final byte len;
		final long mask = 0xffffffffL;
		final long masked = l & mask;
		if (l == masked) {
			// top 4 bytes are all zeroes
			return writeCompressedInt((int) l);
		} else {
			if (debug) {
				System.out.println("\tLong: " + l);
			}
			buf[1] = (byte) l;
			buf[2] = (byte) (l >>> 8);
			buf[3] = (byte) (l >>> 16);
			buf[4] = (byte) (l >>> 24);
			buf[5] = (byte) (l >>> 32);
			buf[6] = (byte) (l >>> 40);

			if (l == (l & 0xffffffffffffL)) {
				// Need at most bottom 6 bytes
				if (l == (l & 0xffffffffffL)) {
					// Need only bottom 5 bytes
					len = 6;
				} else {
					// Need only bottom 6 bytes
					len = 7;
				}
			} else {
				// Need at least bottom 7 bytes
				buf[7] = (byte) (l >>> 48);
				if (l == (l & 0xffffffffffffffL)) {
					// Need only bottom 7 bytes
					len = 8;
				} else {
					len = 9;
					buf[8] = (byte) (l >>> 56);
				}
			}
			buf[0] = (byte) (len - 1);
		}
		if (IdConstants.writeOutput) {
			f_out.write(buf, 0, len);
		}
		// totalInts += len;
		// compressedInts += (8-len);
		return len;
	}
}
