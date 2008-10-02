package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.CLASS_NAME;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.THREAD_NAME;
import static com.surelogic._flashlight.common.AttributeType.TYPE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public abstract class ReferenceDefinition extends TrackUnreferenced {

	private static final String f_psQ = "INSERT INTO OBJECT (Run,Id,Type,Threadname,PackageName,ClassName,Flag) VALUES (?, ?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;
	private ScanRawFilePreScan preScan;

	public final void parse(final int runId, final Attributes attributes)
			throws SQLException {
		long id = -1;
		long type = -1;
		String threadName = null;
		String className = null;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if (ID.matches(aName)) {
					id = Long.parseLong(aValue);
				} else if (TYPE.matches(aName)) {
					type = Long.parseLong(aValue);
				} else if (THREAD_NAME.matches(aName)) {
					threadName = aValue;
				} else if (CLASS_NAME.matches(aName)) {
					className = aValue;
				}
			}
		}
		if (id == -1) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id in " + getXMLElementName());
			return;
		}
		if (type == -1) {
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
		if (!preScan.isUnusedObject(id)) {
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
