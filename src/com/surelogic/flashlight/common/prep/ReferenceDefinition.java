package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.CLASS_NAME;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.THREAD_NAME;
import static com.surelogic._flashlight.common.AttributeType.TYPE;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public abstract class ReferenceDefinition extends AbstractPrep {

	private static final String f_psQ = "INSERT INTO OBJECT (Id,Type,Threadname,PackageName,ClassName,Flag) VALUES (?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;
	private ScanRawFilePreScan preScan;
	private int count;

	public final void parse(final PreppedAttributes attributes)
			throws SQLException {
		final long id = attributes.getLong(ID);
		long type = attributes.getLong(TYPE);
		final String threadName = attributes.getString(THREAD_NAME);
		String className = attributes.getString(CLASS_NAME);
		if (id == ILLEGAL_ID) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id in " + getXMLElementName());
			return;
		}
		if (type == ILLEGAL_ID) {
			type = id;
		}
		String packageName = null;
		if (className != null) {
			final int lastDot = className.lastIndexOf('.');
			if (lastDot == -1) {
				packageName = "(default)";
			} else {
				packageName = className.substring(0, lastDot);
				className = className.substring(lastDot + 1);
			}
		}
		insert(id, type, threadName, packageName, className);
	}

	private void insert(final long id, final long type,
			final String threadName, final String packageName,
			final String className) throws SQLException {
		if (!filterUnused() || preScan.couldBeReferencedObject(id)) {
			int idx = 1;
			f_ps.setLong(idx++, id);
			f_ps.setLong(idx++, type);
			if (threadName != null) {
				f_ps.setString(idx++, threadName);
			} else {
				f_ps.setNull(idx++, Types.VARCHAR);
			}
			if (className != null) {
				f_ps.setString(idx++, packageName);
				f_ps.setString(idx++, className);
			} else {
				f_ps.setNull(idx++, Types.VARCHAR);
				f_ps.setNull(idx++, Types.VARCHAR);
			}
			f_ps.setString(idx++, getFlag());
			if (doInsert) {
				f_ps.addBatch();
				if (++count == 10000) {
					f_ps.executeBatch();
					count = 0;
				}
			}
		}
	}

	protected abstract String getFlag();

	protected abstract boolean filterUnused();

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		f_ps = c.prepareStatement(f_psQ);
		preScan = scanResults;
	}

	@Override
	public void flush(final long endTime) throws SQLException {
		if (count > 0) {
			f_ps.executeBatch();
		}
		count = 0;
		f_ps.close();
		super.flush(endTime);
	}
}
