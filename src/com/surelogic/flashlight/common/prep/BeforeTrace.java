package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

public class BeforeTrace extends Trace {	
	private static final String f_psQ = "INSERT INTO TRACE VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private static long f_id = 0;
	private static PreparedStatement f_ps;
	
	private final Map<Long,Stack<TraceState>> threadToStackTrace = 
		new HashMap<Long,Stack<TraceState>>();
	
	private Stack<TraceState> getTraces(Long inThread) {
		Stack<TraceState> trace = threadToStackTrace.get(inThread);
		if (trace == null) {
			trace = new Stack<TraceState>();
			threadToStackTrace.put(inThread, trace);
		}
		return trace;
	}
	
	static class TraceState {		
		final long id;
		final long time;
		final String file;
		final int line;
		final String location;
		final long prevId;
		
		public TraceState(long id, long time, String file, int lineNumber,
				          String loc, long prev) {
			this.id = id;
			this.time = time;
			this.file = file;
			this.line = lineNumber;
			this.location = loc;
			this.prevId = prev;
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
		String location          = getAttr("at");		
		final long id            = ++f_id;
		final Long thread        = inThread;
		Stack<TraceState> traces = getTraces(thread);
		final long prevId        = traces.isEmpty() ? -1 : traces.peek().id;
		
		// Push onto stack
		TraceState state = new TraceState(id, time, file, lineNumber, location, prevId);
		traces.push(state);
	}
	
	void popTrace(int runId, long inThread, long time,
			      String file, int lineNumber) {
		TraceState state = getTraces(inThread).pop();
		assert state.file.equals(file) && state.line == lineNumber;
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
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: TRACE", e);
		}
	}
	
	public void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
	}
}
