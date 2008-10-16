package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.READ_LOCK_ID;
import static com.surelogic._flashlight.common.AttributeType.WRITE_LOCK_ID;
import static com.surelogic._flashlight.common.IdConstants.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class ReadWriteLock extends Event {

	public ReadWriteLock(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	private static final String f_psQ = "INSERT INTO RWLOCK (Run,Id,ReadLock,WriteLock) VALUES (?, ?, ?, ?)";

	private PreparedStatement f_ps;

	private Timestamp startTime;

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		f_ps = c.prepareStatement(f_psQ);
		startTime = start;
	}

	public String getXMLElementName() {
		return "read-write-lock-definition";
	}

	public void parse(final int runId, final PreppedAttributes attributes)
			throws SQLException {
		long id = attributes.getLong(ID);
		long readLock = attributes.getLong(READ_LOCK_ID);
		long writeLock = attributes.getLong(WRITE_LOCK_ID);
		if ((id == ILLEGAL_ID) || (readLock == ILLEGAL_ID) || (writeLock == ILLEGAL_ID)) {
			SLLogger
					.getLogger()
					.log(Level.SEVERE,
							"Missing id, read-lock-id, or write-lock-id in read-write-lock-definition");
			return;
		}
		insert(runId, id, readLock, writeLock);
		f_rowInserter.defineRWLock(runId, id, readLock, writeLock, startTime);
	}

	private void insert(final int runId, final long id, final long readLock,
			final long writeLock) throws SQLException {
		f_ps.setLong(1, runId);
		f_ps.setLong(2, id);
		f_ps.setLong(3, readLock);
		f_ps.setLong(4, writeLock);
		f_ps.execute();
	}

	@Override
	public void flush(final int runId, final long endTime) throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
		super.flush(runId, endTime);
	}
}
