package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FIELD;
import static com.surelogic._flashlight.common.AttributeType.RECEIVER;
import static com.surelogic._flashlight.common.FlagType.UNDER_CONSTRUCTION;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_FIELD_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_RECEIVER_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public abstract class StaticFieldAccess extends Event {

	private static final String f_psQ = "INSERT INTO ACCESS (TS,InThread,Trace,Field,RW,Receiver,UnderConstruction) VALUES (?, ?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;

	private ScanRawFilePreScan f_scanResults;

	private long skipped, inserted;

	public StaticFieldAccess() {
		super();
	}

	public void parse(final PreppedAttributes attributes) throws SQLException {
		final long receiver = attributes.getLong(RECEIVER);
		if (receiver == ILLEGAL_RECEIVER_ID) {
			final long nanoTime = attributes.getEventTime();
			final long inThread = attributes.getThreadId();
			final long trace = attributes.getTraceId();
			final long field = attributes.getLong(FIELD);
			final boolean underConstruction = attributes
					.getBoolean(UNDER_CONSTRUCTION);
			if (nanoTime == ILLEGAL_ID || inThread == ILLEGAL_ID
					|| trace == ILLEGAL_ID || field == ILLEGAL_FIELD_ID) {
				SLLogger.getLogger().log(
						Level.SEVERE,
						"Missing nano-time, thread, site, or field in "
								+ getXMLElementName());
				return;
			}
			if (!f_scanResults.isSynthetic(field)
					&& f_scanResults.isThreadedStaticField(field)) {
				insert(nanoTime, inThread, trace, field, receiver,
						underConstruction);
				inserted++;
			} else {
				skipped++;
			}
		}
	}

	private int count;

	private void insert(final long nanoTime, final long inThread,
			final long trace, final long field, final long receiver,
			final boolean underConstruction) throws SQLException {
		int idx = 1;
		f_ps.setTimestamp(idx++, getTimestamp(nanoTime), now);
		f_ps.setLong(idx++, inThread);
		f_ps.setLong(idx++, trace);
		f_ps.setLong(idx++, field);
		f_ps.setString(idx++, getRW());
		if (receiver == ILLEGAL_FIELD_ID) {
			f_ps.setNull(idx++, Types.BIGINT);
		} else {
			f_ps.setLong(idx++, receiver);
		}
		f_ps.setString(idx++, underConstruction ? "Y" : "N");
		if (doInsert) {
			f_ps.addBatch();
			if (++count == 10000) {
				f_ps.executeBatch();
				count = 0;
			}
		}
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		f_ps = c.prepareStatement(f_psQ);
		f_scanResults = scanResults;
	}

	@Override
	public void printStats() {
		System.out.println(getClass().getName() + " Skipped   = " + skipped);
		System.out.println(getClass().getName() + " Inserted  = " + inserted);
		System.out.println(getClass().getName() + " %Inserted = " + inserted
				* 100.0 / (skipped + inserted));
	}

	@Override
	public void flush(final long endTime) throws SQLException {
		if (count > 0) {
			f_ps.executeBatch();
			count = 0;
		}
		f_ps.close();
		super.flush(endTime);
	}

	/**
	 * Indicates the type of field access.
	 * 
	 * @return <tt>R</tt> for a field read, or <tt>W</tt> for a field write.
	 */
	abstract protected String getRW();
}
