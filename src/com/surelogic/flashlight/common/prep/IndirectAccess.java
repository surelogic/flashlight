package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.*;
import static com.surelogic._flashlight.common.IdConstants.*;

import java.sql.*;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public final class IndirectAccess extends Event {
	//private static final String f_psQ = "INSERT INTO ACCESS (TS,InThread,Trace,Field,RW,Receiver,UnderConstruction) VALUES (?, ?, ?, ?, ?, ?, ?)";

	//private PreparedStatement f_ps;
	
	public IndirectAccess(IntrinsicLockDurationRowInserter i) {
		super(i); // NEEDED?
	}

	public String getXMLElementName() {
		return "indirect-access";
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		//f_ps = c.prepareStatement(f_psQ);
		//f_scanResults = scanResults;
	}
	
	public void parse(PreppedAttributes attributes) throws SQLException {
		final long nanoTime = attributes.getEventTime();
		final long inThread = attributes.getThreadId();
		final long trace = attributes.getTraceId();
		final long receiver = attributes.getLong(RECEIVER);
		
		if ((nanoTime == ILLEGAL_ID) || (inThread == ILLEGAL_ID) || 
			(trace == ILLEGAL_ID) || (receiver == ILLEGAL_RECEIVER_ID)) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, site, or field in " + getXMLElementName());
			return;
		}
		insert(nanoTime, inThread, trace, receiver);
	}

	private int count;

	private void insert(final long nanoTime, final long inThread,
			final long trace, final long receiver) throws SQLException {
//		int idx = 1;
//		f_ps.setTimestamp(idx++, getTimestamp(nanoTime), now);
//		f_ps.setLong(idx++, inThread);
//		f_ps.setLong(idx++, trace);
//		f_ps.setLong(idx++, receiver);
	
		if (doInsert) {
			//f_ps.addBatch();
			if (++count == 10000) {
				//f_ps.executeBatch();
				count = 0;
			}
		}
	}

	@Override
	public void flush(final long endTime) throws SQLException {
		if (count > 0) {
			//f_ps.executeBatch();
			count = 0;
		}
		//f_ps.close();
		super.flush(endTime);
	}
}
