package com.surelogic.flashlight.common.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.QB;
import com.surelogic.common.logging.SLLogger;
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
	public static PrepRunDescription create(Connection c, RunDescription run)
			throws SQLException {
		PreparedStatement s = c.prepareStatement(QB.get(20));
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
		return find(c, run.getName(), run.getStartTimeOfRun());
	}

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
	public static PrepRunDescription find(Connection c, String name,
			Timestamp started) throws SQLException {
		PreparedStatement s = c.prepareStatement(QB.get(21));
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
	public static PrepRunDescription find(Connection c, int run)
			throws SQLException {
		PreparedStatement s = c.prepareStatement(QB.get(22));
		try {
			s.setInt(1, run);
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				return convertRowToObject(rs);
			}
		} finally {
			s.close();
		}
		SLLogger.getLogger().log(Level.WARNING, I18N.err(110, run),
				new Exception());
		return null;
	}

	/**
	 * Looks up all the runs in the database.
	 * 
	 * @param c
	 *            the database connection to use.
	 * @return all the runs in the database, or an empty list if none exist.
	 * @throws SQLException
	 *             if something goes wrong interacting with the database.
	 */
	public static Set<PrepRunDescription> getAll(Connection c)
			throws SQLException {
		Set<PrepRunDescription> result = new HashSet<PrepRunDescription>();
		PreparedStatement s = c.prepareStatement(QB.get(23));
		try {
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				PrepRunDescription r = convertRowToObject(rs);
				if (r != null) {
					result.add(r);
				} else {
					SLLogger.getLogger().log(Level.WARNING, I18N.err(111),
							new Exception());
				}
			}
		} finally {
			s.close();
		}
		return result;
	}

	private static PrepRunDescription convertRowToObject(ResultSet rs)
			throws SQLException {
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
		final RunDescription description = new RunDescription(name,
				rawDataVersion, userName, javaVersion, javaVendor, osName,
				osArch, osVersion, maxMemoryMb, processors, started);
		return new PrepRunDescription(run, description);
	}

	private RunDAO() {
		// no instances
	}
}
