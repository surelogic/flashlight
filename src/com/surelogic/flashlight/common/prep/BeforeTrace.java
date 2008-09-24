package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.surelogic._flashlight.common.AttributeType.*;

public final class BeforeTrace extends Trace {

	private static final String f_psQ = "INSERT INTO TRACE (Run,Id,InThread,InClass,InFile,AtLine,Location,Start,Stop) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private long f_id = 0;
	private PreparedStatement f_ps;

	private long skipped, inserted;

	private final Map<Long, TraceStateStack> threadToStackTrace = new HashMap<Long, TraceStateStack>();

	public BeforeTrace(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	private TraceStateStack getTraces(final Long inThread) {
		TraceStateStack trace = threadToStackTrace.get(inThread);
		if (trace == null) {
			trace = new TraceStateStack();
			threadToStackTrace.put(inThread, trace);
		}
		return trace;
	}

	static class TraceStateStack {
		TraceState trace;

		TraceState peek() {
			return trace;
		}

		void push(final long id, final long time, final long clazz,
				final String file, final int lineNumber, final String loc) {
			this.trace = new TraceState(id, time, clazz, file, lineNumber, loc,
					trace);
		}

		TraceState pop() {
			final TraceState current = trace;
			if (trace != null) {
				trace = trace.parent;
			} else {
				throw new IllegalStateException(
						"No stack available; probably mismatched traces");
			}
			return current;
		}

	}

	static class TraceState {
		final long id;
		final long time;
		final long clazz;
		final String file;
		final int line;
		final String location;
		final TraceState parent;
		/*
		 * whether or not the trace has any events recorded against it
		 */
		boolean hasEvents;

		TraceState(final long id, final long time, final long clazz,
				final String file, final int lineNumber, final String loc,
				final TraceState parent) {
			this.id = id;
			this.time = time;
			this.clazz = clazz;
			this.file = file;
			this.line = lineNumber;
			this.location = loc;
			this.parent = parent;
		}

		public void threadEvent() {
			if (!hasEvents) {
				if (parent != null) {
					parent.threadEvent();
				}
				hasEvents = true;
			}
		}

	}

	public String getXMLElementName() {
		return "before-trace";
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults,
			final Set<Long> unreferencedObjects,
			final Set<Long> unreferencedFields) throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);

		if (f_ps == null) {
			f_ps = c.prepareStatement(f_psQ);
		}
	}

	@Override
	protected void handleTrace(final int runId, final long inThread,
			final long inClass, final long time, final String file,
			final int lineNumber) {
		final String location = getAttr(LOCATION);
		final long id = ++f_id;
		final Long thread = inThread;
		getTraces(thread).push(id, time, inClass, file, lineNumber, location);
	}

	void popTrace(final int runId, final long inThread, final long inClass,
			final long time, final int lineNumber) throws SQLException {
		final TraceState state = getTraces(inThread).pop();
		assert (state.clazz == inClass) && (state.line == lineNumber);
		if (state.hasEvents) {
			useObject(inClass);
			// insert start and stop time
			int idx = 1;
			f_ps.setInt(idx++, runId);
			f_ps.setLong(idx++, state.id);
			f_ps.setLong(idx++, inThread);
			f_ps.setLong(idx++, state.clazz);
			f_ps.setString(idx++, state.file);
			f_ps.setInt(idx++, lineNumber);
			f_ps.setString(idx++, state.location);
			f_ps.setTimestamp(idx++, getTimestamp(state.time));
			f_ps.setTimestamp(idx++, getTimestamp(time));
			f_ps.executeUpdate();
			inserted++;
		} else {
			skipped++;
		}
	}

	@Override
	public void flush(final int runId, final long endTime) throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
		super.flush(runId, endTime);
	}

	/**
	 * This method should be called when any event occurs
	 * 
	 * @param inThread
	 */
	void threadEvent(final long inThread) {
		final TraceState trace = getTraces(inThread).peek();
		if (trace != null) {
			trace.threadEvent();
		}
	}

	@Override
	public void printStats() {
		System.out.println(getClass().getName() + " Skipped   = " + skipped);
		System.out.println(getClass().getName() + " Inserted  = " + inserted);
		System.out.println(getClass().getName() + " %Inserted = "
				+ (inserted * 100.0 / (skipped + inserted)));
	}
}
