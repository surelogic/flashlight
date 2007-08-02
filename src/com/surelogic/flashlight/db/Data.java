package com.surelogic.flashlight.db;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.surelogic.common.eclipse.Derby;
import com.surelogic.flashlight.Activator;
import com.surelogic.flashlight.FLog;

public final class Data {

	private Data() {
		// no instances
	}

	private static final String SCHEMA_VERSION = "1.0";

	private static final String JDBC_PRE = "jdbc:derby:";

	private static final String JDBC_POST = System
			.getProperty("file.separator")
			+ "prep;user=FL";

	public static void bootAndCheckSchema(final URL schemaURL)
			throws CoreException {
		assert schemaURL != null;

		Derby.bootEmbedded();

		final String connectionURL = getConnectionURL() + ";create = true";
		try {
			boolean schemaExists = false; // assume the worst
			final Connection c = DriverManager.getConnection(connectionURL);
			try {
				final Statement st = c.createStatement();
				try {
					final ResultSet ver = st
							.executeQuery("select FLASHLIGHT from VERSION");
					String schemaVer = "NONE";
					while (ver.next()) {
						schemaVer = ver.getString(1);
					}
					if (schemaVer.equals(SCHEMA_VERSION)) {
						schemaExists = true;
						FLog
								.logInfo("Schema (version "
										+ SCHEMA_VERSION
										+ ") exists in the embedded Flashlight database "
										+ getConnectionURL() + ".");
					}
				} catch (SQLException e) {
					/*
					 * Ignore, this exception occurred because the schema was
					 * not found within the embedded database.
					 */
				} finally {
					st.close();
				}
				if (!schemaExists) {
					loadSchema(c, schemaURL);
					FLog.logInfo("Schema (version " + SCHEMA_VERSION
							+ ") created in the embedded Flashlight database "
							+ getConnectionURL() + ".");
				}
			} finally {
				c.close();
			}
		} catch (SQLException e) {
			throw new CoreException(FLog.createErrorStatus(e));
		}
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(getConnectionURL());
	}

	private static void loadSchema(final Connection c, final URL schemaURL)
			throws SQLException, CoreException {
		assert c != null;
		assert schemaURL != null;

		List<StringBuilder> stmts;
		try {
			stmts = Derby.getSQLStatements(schemaURL);
		} catch (RuntimeException e) {
			throw new CoreException(FLog.createErrorStatus(
					"Unable to open/read the Flashlight schema file: "
							+ schemaURL, e));
		}
		final Statement st = c.createStatement();
		try {
			for (StringBuilder b : stmts) {
				st.executeUpdate(b.toString());
			}
		} finally {
			st.close();
		}
	}

	private static String getConnectionURL() {
		IPath pluginState = Activator.getDefault().getStateLocation();
		return JDBC_PRE + pluginState.toOSString() + JDBC_POST;
	}
}
