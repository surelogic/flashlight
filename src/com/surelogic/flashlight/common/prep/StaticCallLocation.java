package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FILE;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.IN_CLASS;
import static com.surelogic._flashlight.common.AttributeType.LINE;
import static com.surelogic._flashlight.common.AttributeType.LOCATION;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic._flashlight.common.PreppedAttributes;

public final class StaticCallLocation extends AbstractPrep {

	private PreparedStatement f_ps;

	public String getXMLElementName() {
		return "static-call-location";
	}

	public void parse(final int runId, final PreppedAttributes attributes)
			throws SQLException {
		int idx = 1;
		f_ps.setLong(idx++, runId);
		f_ps.setLong(idx++, attributes.getLong(ID));
		f_ps.setInt(idx++, attributes.getInt(LINE));
		f_ps.setLong(idx++, attributes.getLong(IN_CLASS));
		f_ps.setString(idx++, attributes.getString(FILE));
		f_ps.setString(idx++, attributes.getString(LOCATION));
		f_ps.execute();
	}

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		f_ps = c
				.prepareStatement("INSERT INTO SITE (Run,Id,AtLine,InClass,InFile,Location) VALUES (?,?,?,?,?,?)");
	}

	@Override
	public void flush(final int runId, final long endTime) throws SQLException {
		super.flush(runId, endTime);
		f_ps.close();
	}

}
