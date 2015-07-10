package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public interface IOneTimePrep extends IPrep {
  /**
   * Allows the event handler to perform any required setup. Always called
   * before the first call to {@link #parse(int, Attributes)}.
   * 
   * @param c
   *          a connection to the Flashlight database.
   * @param start
   *          the start wall clock time.
   * @param startNS
   *          the start time from {@link System#nanoTime()}.
   * @param st
   *          results of the raw file pre-scan.
   * @param unreferencedObjects
   *          the mutable set of unreferenced objects.
   * @param unreferencedFields
   *          the mutable set of unreferenced fields.
   * @throws SQLException
   *           if something goes wrong, this will cause the prep to fail.
   */
  void setup(final Connection c, final Timestamp start, final long startNS, final ScanRawFilePreScan st) throws SQLException;

}
