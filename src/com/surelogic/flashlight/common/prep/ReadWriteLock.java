package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.READ_LOCK_ID;
import static com.surelogic._flashlight.common.AttributeType.WRITE_LOCK_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

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

	private static final String f_psQ = "INSERT INTO RWLOCK (Id,ReadLock,WriteLock) VALUES (?, ?, ?)";

	private PreparedStatement f_ps;

	private Timestamp startTime;

	private int count;

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

	public void parse(final PreppedAttributes attributes) throws SQLException {
		final long id = attributes.getLong(ID);
		final long readLock = attributes.getLong(READ_LOCK_ID);
		final long writeLock = attributes.getLong(WRITE_LOCK_ID);
		if ((id == ILLEGAL_ID) || (readLock == ILLEGAL_ID)
				|| (writeLock == ILLEGAL_ID)) {
			SLLogger
					.getLogger()
					.log(Level.SEVERE,
							"Missing id, read-lock-id, or write-lock-id in read-write-lock-definition");
			return;
		}
		insert(id, readLock, writeLock);
		f_rowInserter.defineRWLock(id, readLock, writeLock, startTime);
	}

	private void insert(final long id, final long readLock, final long writeLock)
			throws SQLException {
		int idx = 1;
		f_ps.setLong(idx++, id);
		f_ps.setLong(idx++, readLock);
		f_ps.setLong(idx++, writeLock);
		if (doInsert) {
			f_ps.addBatch();
			if (++count == 10000) {
				f_ps.executeBatch();
				count = 0;
			}
			f_ps.execute();
		}
	}

	@Override
	public void flush(final long endTime) throws SQLException {
		if (count > 0) {
			f_ps.executeBatch();
			count = 0;
		}
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
		super.flush(endTime);
	}
}
