package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;

public interface IRangePrep extends IPrep {

	/**
	 * Gets the XML element name that this handler deals with.
	 * 
	 * @return the XML element name that this handler deals with.
	 */
	@Override
  String getXMLElementName();

	/**
	 * Allows the event handler to perform any required setup. Always called
	 * before the first call to {@link #parse(int, Attributes)}.
	 * 
	 * @param c
	 *            a connection to the Flashlight database.
	 * @param start
	 *            the start wall clock time.
	 * @param startNS
	 *            the start time from {@link System#nanoTime()}.
	 * @param st
	 *            results of the raw file pre-scan.
	 * @param begin
	 * @param end
	 * @throws SQLException
	 *             if something goes wrong, this will cause the prep to fail.
	 */
	void setup(final Connection c, final Timestamp start, final long startNS,
			final ScanRawFileFieldsPreScan st, long begin, long end)
			throws SQLException;

	/**
	 * Called for each instance of the XML element name found in the raw file.
	 * 
	 * @param runId
	 *            the database run identifier.
	 * @param attributes
	 *            the XML attributes of the XML element.
	 * @throws SQLException
	 *             if something goes wrong, this will cause the prep to fail.
	 */
	@Override
  void parse(final PreppedAttributes attributes) throws SQLException;

	/**
	 * Called after the last call to {@link #parse(int, Attributes)} to allow a
	 * handler to flush results into the database.
	 * 
	 * @param runId
	 *            the database run identifier.
	 * @param endTime
	 *            the {@link System#nanoTime()} at the end of the raw file (from
	 *            the last <tt>time</tt> attribute).
	 * @throws SQLException
	 *             if something goes wrong, this will cause the prep to fail.
	 */
	@Override
  void flush(final long endTime) throws SQLException;

	/**
	 * Logs some status if the logging level {@link Level#FINE} is loggable.
	 * This method will be called after {@link #flush(int, long)}.
	 */
	@Override
  void printStats();
}
