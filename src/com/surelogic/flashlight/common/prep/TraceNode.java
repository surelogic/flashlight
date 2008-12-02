package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.PARENT_ID;
import static com.surelogic._flashlight.common.AttributeType.SITE_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic._flashlight.common.EventType;
import com.surelogic._flashlight.common.PreppedAttributes;

public final class TraceNode extends AbstractPrep {

	private PreparedStatement f_ps;

	public String getXMLElementName() {
		return EventType.Trace_Node.getLabel();
	}

	public void parse(final PreppedAttributes attributes) throws SQLException {
		int idx = 1;
		final long id = attributes.getTraceId();
		long parent = attributes.getLong(PARENT_ID);
		if (parent == 0) {
			parent = id;
		}
		f_ps.setLong(idx++, id);
		f_ps.setLong(idx++, attributes.getLong(SITE_ID));
		f_ps.setLong(idx++, parent);
		f_ps.execute();
	}

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		f_ps = c
				.prepareStatement("INSERT INTO TRACE (Id,Site,Parent) VALUES (?,?,?)");
	}

	@Override
	public void flush(final long endTime) throws SQLException {
		super.flush(endTime);
		f_ps.close();
	}

}
