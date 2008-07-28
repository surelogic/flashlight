package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public class ReadWriteLock extends Event {

	private static final String f_psQ = "INSERT INTO RWLOCK (Run,Id,ReadLock,WriteLock) VALUES (?, ?, ?, ?)";

	private static PreparedStatement f_ps;

	private Timestamp startTime;

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults,
			Set<Long> unreferencedObjects, Set<Long> unreferencedFields)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
		if (f_ps == null) {
			f_ps = c.prepareStatement(f_psQ);
		}
		startTime = start;
	}

	public String getXMLElementName() {
		return "read-write-lock-definition";
	}

	public void parse(int runId, Attributes attributes) throws SQLException {
		long id = -1;
		long readLock = -1;
		long writeLock = -1;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if ("id".equals(aName)) {
					id = Long.parseLong(aValue);
				} else if ("read-lock-id".equals(aName)) {
					readLock = Long.parseLong(aValue);
				} else if ("write-lock-id".equals(aName)) {
					writeLock = Long.parseLong(aValue);
				}
			}
		}
		if (id == -1 || readLock == -1 || writeLock == -1) {
			SLLogger
					.getLogger()
					.log(Level.SEVERE,
							"Missing id, read-lock-id, or write-lock-id in read-write-lock-definition");
			return;
		}
		insert(runId, id, readLock, writeLock);
		f_rowInserter.defineRWLock(runId, id, readLock, writeLock, startTime);
		useObject(id);
		useObject(readLock);
		useObject(writeLock);
	}

	private void insert(int runId, long id, long readLock, long writeLock)
			throws SQLException {
		f_ps.setLong(1, runId);
		f_ps.setLong(2, id);
		f_ps.setLong(3, readLock);
		f_ps.setLong(4, writeLock);
		f_ps.execute();
	}

	@Override
	public void flush(int runId, long endTime) throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
		super.flush(runId, endTime);
	}
}
