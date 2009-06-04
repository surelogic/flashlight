package com.surelogic.flashlight.common.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic.common.jdbc.QB;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * A data access object for {@link PrepRunDescription} and
 * {@link RunDescription}.
 * <p>
 * This roughly follows the pattern from page 462 of <i>Core J2EE Patterns</i>
 * (2nd edition) by Alur, Crupi, and Malks (Prentice Hall 2003). I've made it a
 * utility.
 */
public final class RunDAO {

	/**
	 * Creates a new run in the database.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @param run
	 *            a description of the run to create.
	 * @return the new run.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static PrepRunDescription create(final Connection c,
			final RunDescription run) throws SQLException {
		final PreparedStatement s = c.prepareStatement(QB.get("RunDAO.insert"));
		try {
			int i = 1;
			s.setString(i++, run.getName());
			s.setString(i++, run.getRawDataVersion());
			s.setString(i++, run.getUserName());
			s.setString(i++, run.getJavaVersion());
			s.setString(i++, run.getJavaVendor());
			s.setString(i++, run.getOSName());
			s.setString(i++, run.getOSArch());
			s.setString(i++, run.getOSVersion());
			s.setInt(i++, run.getMaxMemoryMb());
			s.setInt(i++, run.getProcessors());
			s.setTimestamp(i++, run.getStartTimeOfRun());
			s.executeUpdate();
		} finally {
			s.close();
		}
		return new PrepRunDescription(run);
	}

	/**
	 * Looks up the run in the database by its identifier.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @return the run, or <code>null</code> if no such run exists.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static PrepRunDescription find(final Connection c)
			throws SQLException {
		final PreparedStatement s = c.prepareStatement(QB.get("RunDAO.select"));
		try {
			final ResultSet rs = s.executeQuery();
			try {
				if (rs.next()) {
					return convertRowToObject(rs);
				}
			} finally {
				rs.close();
			}
		} finally {
			s.close();
		}
		return null;
	}

	private static PrepRunDescription convertRowToObject(final ResultSet rs)
			throws SQLException {
		int i = 1;
		final String name = rs.getString(i++);
		final String rawDataVersion = rs.getString(i++);
		final String userName = rs.getString(i++);
		final String javaVersion = rs.getString(i++);
		final String javaVendor = rs.getString(i++);
		final String osName = rs.getString(i++);
		final String osArch = rs.getString(i++);
		final String osVersion = rs.getString(i++);
		final int maxMemoryMb = rs.getInt(i++);
		final int processors = rs.getInt(i++);
		final Timestamp started = rs.getTimestamp(i++);
		final RunDescription description = new RunDescription(name,
				rawDataVersion, userName, javaVersion, javaVendor, osName,
				osArch, osVersion, maxMemoryMb, processors, started);
		return new PrepRunDescription(description);
	}

	private RunDAO() {
		// no instances
	}
}
