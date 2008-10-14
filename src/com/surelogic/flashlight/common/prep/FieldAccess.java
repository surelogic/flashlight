package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FIELD;
import static com.surelogic._flashlight.common.AttributeType.IN_CLASS;
import static com.surelogic._flashlight.common.AttributeType.LINE;
import static com.surelogic._flashlight.common.AttributeType.RECEIVER;
import static com.surelogic._flashlight.common.AttributeType.THREAD;
import static com.surelogic._flashlight.common.AttributeType.TIME;
import static com.surelogic._flashlight.common.FlagType.UNDER_CONSTRUCTION;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_FIELD_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_RECEIVER_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import org.xml.sax.Attributes;

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

	public void parse(final int runId, final Attributes attributes)
			throws SQLException {
		long nanoTime = -1;
		long inThread = -1;
		long inClass = -1;
		int lineNumber = -1;
		long field = ILLEGAL_FIELD_ID;
		long receiver = ILLEGAL_RECEIVER_ID;
		boolean underConstruction = false;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if (TIME.matches(aName)) {
					nanoTime = Long.parseLong(aValue);
				} else if (THREAD.matches(aName)) {
					inThread = Long.parseLong(aValue);
				} else if (IN_CLASS.matches(aName)) {
					inClass = Long.parseLong(aValue);
				} else if (LINE.matches(aName)) {
					lineNumber = Integer.parseInt(aValue);
				} else if (FIELD.matches(aName)) {
					field = Long.parseLong(aValue);
				} else if (RECEIVER.matches(aName)) {
					receiver = Long.parseLong(aValue);
				} else if (UNDER_CONSTRUCTION.matches(aName)) {
					underConstruction = "yes".equals(aValue);
				}
			}
		}
		if ((nanoTime == -1) || (inThread == -1) || (inClass == -1)
				|| (lineNumber == -1) || (field == ILLEGAL_FIELD_ID)) {
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
