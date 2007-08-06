package com.surelogic.flashlight;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.core.runtime.IPath;

import com.surelogic.common.eclipse.Derby;
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

	public static void bootAndCheckSchema() throws Exception {

		Derby.bootEmbedded();

		final String connectionURL = getConnectionURL() + ";create=true";
		final Connection c = DriverManager.getConnection(connectionURL);
		try {
			checkAndUpdate(c);
		} finally {
			c.close();
		}
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(getConnectionURL());
	}

	private static String getConnectionURL() {
		IPath pluginState = Activator.getDefault().getStateLocation();
		return JDBC_PRE + pluginState.toOSString() + JDBC_POST;
	}

	/**
	 * Up this number when you add a new schema version SQL script to this
	 * package.
	 */
	public static final int schemaVersion = 0;

	public static final String SQL_SCRIPT_PREFIX = "/lib/database/schema_";

	public static void checkAndUpdate(final Connection c) throws SQLException,
			IOException {
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
		while (result.length() < 4)
			result = "0" + result;
		return result;
	}
}
