package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public class ReadWriteLock extends TrackUnreferenced {

	private static final String f_psQ = "INSERT INTO RWLOCK (Run,Id,ReadLock,WriteLock) VALUES (?, ?, ?, ?)";

	private static PreparedStatement f_ps;

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final DataPreScan scanResults,
			Set<Long> unreferencedObjects, Set<Long> unreferencedFields)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
		if (f_ps == null) {
			f_ps = c.prepareStatement(f_psQ);
		}
	}

	public String getXMLElementName() {
		return "read-write-lock-definition";
	}

	public void parse(int runId, Attributes attributes) {

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
		useObject(id);
		useObject(readLock);
		useObject(writeLock);
	}

	private void insert(int runId, long id, long readLock, long writeLock) {
		try {
			f_ps.setLong(1, runId);
			f_ps.setLong(2, readLock);
			f_ps.setLong(3, writeLock);
			f_ps.execute();
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert of read-write-lock-definition failed", e);
		}
	}

	public void close() throws SQLException {
		f_ps.close();
	}

}
