package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.jobs.PrepSLJob;

/**
 * Defines the interface for a handler for an XML element name encountered when
 * parsing the raw file.
 * <p>
 * Ensure that any new subclasses are added to the array returned by the {@code
 * getParseHandlers} method in {@link PrepSLJob}.
 */
public interface IPrep {

  /**
   * Gets the XML element name that this handler deals with.
   * 
   * @return the XML element name that this handler deals with.
   */
  String getXMLElementName();

  /**
   * Called for each instance of the XML element name found in the raw file.
   * 
   * @param runId
   *          the database run identifier.
   * @param attributes
   *          the XML attributes of the XML element.
   * @throws SQLException
   *           if something goes wrong, this will cause the prep to fail.
   */
  void parse(final PreppedAttributes attributes) throws SQLException;

  /**
   * Called after the last call to {@link #parse(int, Attributes)} to allow a
   * handler to flush results into the database.
   * 
   * @param runId
   *          the database run identifier.
   * @param endTime
   *          the {@link System#nanoTime()} at the end of the raw file (from the
   *          last <tt>time</tt> attribute).
   * @throws SQLException
   *           if something goes wrong, this will cause the prep to fail.
   */
  void flush(final long endTime) throws SQLException;

  /**
   * Logs some status if the logging level {@link Level#FINE} is loggable. This
   * method will be called after {@link #flush(int, long)}.
   */
  void printStats();
}
