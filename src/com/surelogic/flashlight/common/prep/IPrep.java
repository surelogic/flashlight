package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;

import org.xml.sax.Attributes;

/**
 * Ensure that any new subclasses are added to the <tt>f_elements</tt> array
 * in what used to be PrepJob.
 */
public interface IPrep {

	String getXMLElementName();

	void parse(final int runId, final Attributes attributes);

	void setup(final Connection c, final Timestamp start, final long startNS,
			final DataPreScan st, Set<Long> unreferencedObjects,
			Set<Long> unreferencedFields) throws SQLException;

	void flush(final int runId) throws SQLException;
	
	void printStats();
	
	void close() throws SQLException;
}
