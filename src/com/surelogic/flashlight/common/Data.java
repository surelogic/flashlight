package com.surelogic.flashlight.common;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.surelogic.common.derby.Derby;
import com.surelogic.common.jdbc.FutureDatabaseException;
import com.surelogic.common.jdbc.SchemaUtility;

public final class Data {

	private Data() {
		// no instances
	}

	private static final String SCHEMA_NAME = "FLASHLIGHT";
	private static final String JDBC_PRE = "jdbc:derby:";
	private static final String JDBC_POST = System
			.getProperty("file.separator")
			+ "db;user=" + SCHEMA_NAME;

	private static String dbLocation = null;

	public static synchronized boolean isBooted() {
		return (dbLocation != null);
	}

	public static synchronized boolean bootAndCheckSchema(String location)
			throws Exception {
		if (location == null) {
			throw new IllegalArgumentException("Null db location");
		}
		if (isBooted()) {
			// already initialized
			return false;
		}
		/*
		 * if (!new File(location).exists()) { throw new
		 * IllegalArgumentException("Non-existent db location: "+location); }
		 */
		dbLocation = location;

		Derby.bootEmbedded();

		final String connectionURL = getConnectionURL() + ";create=true";
		final Connection c = DriverManager.getConnection(connectionURL);
		c.setAutoCommit(false);
		Exception e = null;
		try {
			checkAndUpdate(c);
			c.commit();
		} catch (final SQLException e1) {
			c.rollback();
			e = e1;
		} finally {
			try {
				c.close();
			} catch (final SQLException e1) {
				if (e == null) {
					e = e1;
				}
			}
		}
		if (e != null) {
			throw e;
		}
		return true;
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(getConnectionURL());
	}

	private static String getConnectionURL() {
		return JDBC_PRE + dbLocation + JDBC_POST;
	}

	/**
	 * Up this number when you add a new schema version SQL script to this
	 * package.
	 */
	public static final int schemaVersion = 2;

	public static final String SQL_SCRIPT_PREFIX = "/lib/database/schema_";

	public static void checkAndUpdate(final Connection c) throws SQLException,
			IOException, FutureDatabaseException {
		final int arrayLength = schemaVersion + 1;

		final URL[] scripts = new URL[arrayLength];
		for (int i = 0; i < scripts.length; i++) {
			scripts[i] = Data.class.getResource(SQL_SCRIPT_PREFIX
					+ getZeroPadded(i) + ".sql");
		}
		SchemaUtility.checkAndUpdate(c, scripts, null);
	}

	/**
	 * Pads the given positive integer with 0s and returns a string of at least
	 * 4 characters. For example: <code>getZeroPadded(0)</code> results in the
	 * string <code>"0000"</code>; <code>getZeroPadded(436)</code> results
	 * in the string <code>"0456"</code>; <code>getZeroPadded(56900)</code>
	 * results in the string <code>"56900"</code>.
	 * 
	 * @param n
	 *            a non-negative integer (i.e., n >=0).
	 * @return a
	 */
	private static String getZeroPadded(final int n) {
		assert n >= 0;

		String result = "" + n;
		while (result.length() < 4) {
			result = "0" + result;
		}
		return result;
	}
}
