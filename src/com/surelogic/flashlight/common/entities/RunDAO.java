package com.surelogic.flashlight.common.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

/**
 * A data access object for {@link Run}.
 * <p>
 * This roughly follows the pattern from page 462 of <i>Core J2EE Patterns</i>
 * (2nd edition) by Alur, Crupi, and Malks (Prentice Hall 2003). I've made it a
 * utility.
 */
public final class RunDAO {

	private RunDAO() {
		// no instances
	}

	private static final String f_createSQL = "insert into RUN (Name, "
			+ "RawDataVersion, UserName, JavaVersion, JavaVendor, OsName, "
			+ "OsArch, OsVersion, MaxMemoryMB, Processors, Started) "
			+ "values (?,?,?,?,?,?,?,?,?,?,?)";

	/**
	 * Creates a new run in the database.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @param name
	 *            the name of the run.
	 * @param rawDataVersion
	 *            the raw data version.
	 * @param userName
	 *            the user who did the run.
	 * @param javaVersion
	 *            the Java version the run was made on.
	 * @param javaVendor
	 *            the vendor who produced the JVM the run was made on.
	 * @param osName
	 *            the operating system the run was made on.
	 * @param osArch
	 *            the processor architecture the run was made on.
	 * @param osVersion
	 *            the version of the operating system the run was made on.
	 * @param maxMemoryMb
	 *            the maximum memory the JVM used when the run was made.
	 * @param processors
	 *            the number of processors used during the run.
	 * @param started
	 *            the time the run was started.
	 * @return the new run.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static Run create(Connection c, String name, String rawDataVersion,
			String userName, String javaVersion, String javaVendor,
			String osName, String osArch, String osVersion, int maxMemoryMb,
			int processors, Timestamp started) throws SQLException {
		PreparedStatement s = c.prepareStatement(f_createSQL);
		try {
			int i = 1;
			s.setString(i++, name);
			s.setString(i++, rawDataVersion);
			s.setString(i++, userName);
			s.setString(i++, javaVersion);
			s.setString(i++, javaVendor);
			s.setString(i++, osName);
			s.setString(i++, osArch);
			s.setString(i++, osVersion);
			s.setInt(i++, maxMemoryMb);
			s.setInt(i++, processors);
			s.setTimestamp(i++, started);
			s.executeUpdate();
		} finally {
			s.close();
		}
		return find(c, name, started);
	}

	private static final String f_findByNameSQL = "select * from RUN where Name=? and Started=?";

	/**
	 * Looks up a run in the database by its name and start time.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @param name
	 *            the name of the run.
	 * @param started
	 *            the time the run started.
	 * @return the run, or <code>null</code> if no such run exists.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static Run find(Connection c, String name, Timestamp started)
			throws SQLException {
		PreparedStatement s = c.prepareStatement(f_findByNameSQL);
		try {
			s.setString(1, name);
			s.setTimestamp(2, started);
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				return convertRowToObject(rs);
			}
		} finally {
			s.close();
		}
		return null;
	}

	private static final String f_findByRunSQL = "select * from RUN where Run=?";

	/**
	 * Looks up a run in the database by its identifier.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @param run
	 *            the run identifier.
	 * @return the run, or <code>null</code> if no such run exists.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static Run find(Connection c, int run) throws SQLException {
		PreparedStatement s = c.prepareStatement(f_findByRunSQL);
		try {
			s.setInt(1, run);
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				return convertRowToObject(rs);
			}
		} finally {
			s.close();
		}
		SLLogger.getLogger()
				.log(
						Level.WARNING,
						"find on the run identified by " + run
								+ " produced a null Run");
		return null;
	}

	private static final String f_getAllSQL = "select * from RUN";

	/**
	 * Looks up all the runs in the database.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @return all the runs in the database, or the empty array if none exist.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static Run[] getAll(Connection c) throws SQLException {
		List<Run> result = new ArrayList<Run>();
		PreparedStatement s = c.prepareStatement(f_getAllSQL);
		try {
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				Run r = convertRowToObject(rs);
				if (r != null) {
					result.add(r);
				} else {
					SLLogger.getLogger().log(Level.WARNING,
							"RunDAO getAll() produced a null Run");
				}
			}
		} finally {
			s.close();
		}
		return result.toArray(new Run[result.size()]);
	}

	private static Run convertRowToObject(ResultSet rs) throws SQLException {
		int i = 1;
		int run = rs.getInt(i++);
		String name = rs.getString(i++);
		String rawDataVersion = rs.getString(i++);
		String userName = rs.getString(i++);
		String javaVersion = rs.getString(i++);
		String javaVendor = rs.getString(i++);
		String osName = rs.getString(i++);
		String osArch = rs.getString(i++);
		String osVersion = rs.getString(i++);
		int maxMemoryMb = rs.getInt(i++);
		int processors = rs.getInt(i++);
		Timestamp started = rs.getTimestamp(i++);
		return new Run(run, name, rawDataVersion, userName, javaVersion,
				javaVendor, osName, osArch, osVersion, maxMemoryMb, processors,
				started);
	}
}
