package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public abstract class FieldAccess extends Event {

	private static final String f_psQ = "INSERT INTO ACCESS (Run,TS,InThread,InClass,AtLine,Field,RW,Receiver,UnderConstruction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static PreparedStatement f_ps;

	private static ScanRawFilePreScan f_scanResults;

	private long skipped, inserted;

	private final BeforeTrace before;

	public FieldAccess(BeforeTrace before) {
		this.before = before;
	}

	public void parse(int runId, Attributes attributes) throws SQLException {
		long nanoTime = -1;
		long inThread = -1;
		long inClass = -1;
		int lineNumber = -1;
		long field = -1;
		long receiver = -1;
		boolean underConstruction = false;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if ("nano-time".equals(aName)) {
					nanoTime = Long.parseLong(aValue);
				} else if ("thread".equals(aName)) {
					inThread = Long.parseLong(aValue);
				} else if ("in-class".equals(aName)) {
					inClass = Long.parseLong(aValue);
				} else if ("line".equals(aName)) {
					lineNumber = Integer.parseInt(aValue);
				} else if ("field".equals(aName)) {
					field = Long.parseLong(aValue);
				} else if ("receiver".equals(aName)) {
					receiver = Long.parseLong(aValue);
				} else if ("under-construction".equals(aName)) {
					underConstruction = "yes".equals(aValue);
				}
			}
		}
		if (nanoTime == -1 || inThread == -1 || inClass == -1
				|| lineNumber == -1 || field == -1) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, file, line or field in "
							+ getXMLElementName());
			return;
		}
		if (receiver == -1) {
			if (f_scanResults.isSingleThreadedStaticField(field)) {
				skipped++;
				return;
			}
		} else {
			useObject(receiver);
			if (f_scanResults.isThreadedField(field, receiver)) {
				skipped++;
				return;
			}
		}
		before.threadEvent(inThread);
		insert(runId, nanoTime, inThread, inClass, lineNumber, field, receiver,
				underConstruction);
		useObject(inThread);
		useObject(inClass);
		useField(field);
		inserted++;
	}

	private void insert(int runId, long nanoTime, long inThread, long inClass,
			int lineNumber, long field, long receiver, boolean underConstruction)
			throws SQLException {
		f_ps.setInt(1, runId);
		f_ps.setTimestamp(2, getTimestamp(nanoTime));
		f_ps.setLong(3, inThread);
		f_ps.setLong(4, inClass);
		f_ps.setInt(5, lineNumber);
		f_ps.setLong(6, field);
		f_ps.setString(7, getRW());
		if (receiver == -1) {
			f_ps.setNull(8, Types.BIGINT);
		} else {
			f_ps.setLong(8, receiver);
		}
		f_ps.setString(9, underConstruction ? "Y" : "N");
		f_ps.executeUpdate();
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults,
			Set<Long> unreferencedObjects, Set<Long> unreferencedFields)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
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
	public void flush(int runId, long endTime) throws SQLException {
		if (f_ps != null) {
			f_ps.close();
		}
		super.flush(runId, endTime);
	}

	/**
	 * Indicates the type of field access.
	 * 
	 * @return <tt>R</tt> for a field read, or <tt>W</tt> for a field write.
	 */
	abstract protected String getRW();
}
