package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

public class BeforeTrace extends Trace {
	private static final String f_psQ = "INSERT INTO TRACE VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private static long f_id = 0;
	private static PreparedStatement f_ps;

	private final Map<Long, TraceStateStack> threadToStackTrace = new HashMap<Long, TraceStateStack>();

	private TraceStateStack getTraces(Long inThread) {
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

		void push(long id, long time, String file, int lineNumber, String loc) {
			this.trace = new TraceState(id, time, file, lineNumber, loc, trace);
		}

		TraceState pop() {
			final TraceState current = trace;
			if (trace != null) {
				trace = trace.parent;
			}
			return current;
		}

	}

	static class TraceState {
		final long id;
		final long time;
		final String file;
		final int line;
		final String location;
		final TraceState parent;
		/*
		 * whether or not the trace has any events recorded against it
		 */
		boolean hasEvents;

		TraceState(long id, long time, String file, int lineNumber, String loc,
				TraceState parent) {
			this.id = id;
			this.time = time;
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
			final long startNS, final DataPreScan scanResults,
			Set<Long> unreferencedObjects, Set<Long> unreferencedFields)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);

		if (f_ps == null) {
			f_ps = c.prepareStatement(f_psQ);
		}
	}

	@Override
	protected void handleTrace(int runId, long inThread, long time,
			String file, int lineNumber) {
		final String location = getAttr("at");
		final long id = ++f_id;
		final Long thread = inThread;
		getTraces(thread).push(id, time, file, lineNumber, location);
	}

	void popTrace(int runId, long inThread, long time, String file,
			int lineNumber) {
		final TraceState state = getTraces(inThread).pop();
		assert state.file.equals(file) && state.line == lineNumber;
		if (state.hasEvents) {
			// insert start and stop time
			try {
				f_ps.setInt(1, runId);
				f_ps.setLong(2, state.id);
				f_ps.setLong(3, inThread);
				f_ps.setString(4, file);
				f_ps.setInt(5, lineNumber);
				f_ps.setString(6, state.location);
				f_ps.setTimestamp(7, getTimestamp(state.time));
				f_ps.setTimestamp(8, getTimestamp(time));
				f_ps.executeUpdate();
			} catch (final SQLException e) {
				SLLogger.getLogger().log(Level.SEVERE, "Insert failed: TRACE",
						e);
			}
		}
	}

	public void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
	}

	/**
	 * This method should be called when any event occurs
	 * 
	 * @param inThread
	 */
	void threadEvent(long inThread) {
		final TraceState trace = getTraces(inThread).peek();
		if (trace != null) {
			trace.threadEvent();
		} else {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"A thread event occured in thread " + inThread
							+ " with no stack available.");
		}
	}
}
