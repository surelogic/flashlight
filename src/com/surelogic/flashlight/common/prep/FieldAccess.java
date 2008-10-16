package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.*;
import static com.surelogic._flashlight.common.FlagType.UNDER_CONSTRUCTION;
import static com.surelogic._flashlight.common.IdConstants.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public abstract class FieldAccess extends Event {

	private static final String f_psQ = "INSERT INTO ACCESS (Run,TS,InThread,InClass,AtLine,Field,RW,Receiver,UnderConstruction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;

	private ScanRawFilePreScan f_scanResults;

	private long skipped, inserted;

	private final BeforeTrace before;

	public FieldAccess(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(i);
		this.before = before;
	}

	public void parse(final int runId, final PreppedAttributes attributes)
			throws SQLException {
		long nanoTime = attributes.getLong(TIME);
		long inThread = attributes.getLong(THREAD);
		long inClass = attributes.getLong(IN_CLASS);
		int lineNumber = attributes.getInt(LINE);
		long field = attributes.getLong(FIELD);
		long receiver = attributes.getLong(RECEIVER);
		boolean underConstruction = attributes.getBoolean(UNDER_CONSTRUCTION);
		if ((nanoTime == ILLEGAL_ID) || (inThread == ILLEGAL_ID) || (inClass == ILLEGAL_ID)
				|| (lineNumber == ILLEGAL_LINE) || (field == ILLEGAL_FIELD_ID)) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, file, line or field in "
							+ getXMLElementName());
			return;
		}
		if (receiver == ILLEGAL_RECEIVER_ID) {
			if (!f_scanResults.isThreadedStaticField(field)) {
				skipped++;
				return;
			}
		} else {
			if (!f_scanResults.isThreadedField(field, receiver)) {
				skipped++;
				return;
			}
		}
		before.threadEvent(inThread);
		insert(runId, nanoTime, inThread, inClass, lineNumber, field, receiver,
				underConstruction);
		inserted++;
	}

	private void insert(final int runId, final long nanoTime,
			final long inThread, final long inClass, final int lineNumber,
			final long field, final long receiver,
			final boolean underConstruction) throws SQLException {
		f_ps.setInt(1, runId);
		f_ps.setTimestamp(2, getTimestamp(nanoTime));
		f_ps.setLong(3, inThread);
		f_ps.setLong(4, inClass);
		f_ps.setInt(5, lineNumber);
		f_ps.setLong(6, field);
		f_ps.setString(7, getRW());
		if (receiver == ILLEGAL_FIELD_ID) {
			f_ps.setNull(8, Types.BIGINT);
		} else {
			f_ps.setLong(8, receiver);
		}
		f_ps.setString(9, underConstruction ? "Y" : "N");
		f_ps.executeUpdate();
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
		System.out.println(getClass().getName() + " %Inserted = "
				+ (inserted * 100.0 / (skipped + inserted)));
	}

	@Override
	public void flush(final int runId, final long endTime) throws SQLException {
		f_ps.close();
		super.flush(runId, endTime);
	}

	/**
	 * Indicates the type of field access.
	 * 
	 * @return <tt>R</tt> for a field read, or <tt>W</tt> for a field write.
	 */
	abstract protected String getRW();
}
