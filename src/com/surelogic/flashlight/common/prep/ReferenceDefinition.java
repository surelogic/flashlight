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

	private static final String f_psQ = "INSERT INTO OBJECT (Run,Id,Type,Threadname,PackageName,ClassName,Flag) VALUES (?, ?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;
	private ScanRawFilePreScan preScan;

	public final void parse(final int runId, final PreppedAttributes attributes)
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
		insert(runId, id, type, threadName, packageName, className);
	}

	private void insert(final int runId, final long id, final long type,
			final String threadName, final String packageName,
			final String className) throws SQLException {
		if (!filterUnused() || preScan.couldBeReferencedObject(id)) {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, id);
			f_ps.setLong(3, type);
			if (threadName != null) {
				f_ps.setString(4, threadName);
			} else {
				f_ps.setNull(4, Types.VARCHAR);
			}
			if (className != null) {
				f_ps.setString(5, packageName);
				f_ps.setString(6, className);
			} else {
				f_ps.setNull(5, Types.VARCHAR);
				f_ps.setNull(6, Types.VARCHAR);
			}
			f_ps.setString(7, getFlag());
			f_ps.executeUpdate();
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
	public void flush(final int runId, final long endTime) throws SQLException {
		f_ps.close();
		super.flush(runId, endTime);
	}
}
