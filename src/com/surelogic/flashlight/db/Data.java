package com.surelogic.flashlight.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.surelogic.flashlight.Activator;
import com.surelogic.flashlight.FLog;

public final class Data {

	private Data() {
		// no instances
	}

	private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	private static final String SCHEMA_VERSION = "1.0";

	private static final String JDBC_PRE = "jdbc:derby:";

	private static final String JDBC_POST = System
			.getProperty("file.separator")
			+ "prep;user=FL";

	public static void bootAndCheckSchema(final URL schemaURL)
			throws CoreException {
		assert schemaURL != null;

		try {
			/*
			 * Load the Derby driver. When the embedded Driver is used this
			 * action start the Derby engine.
			 */
			Class.forName(DRIVER);
		} catch (java.lang.ClassNotFoundException e) {
			throw new CoreException(FLog.createErrorStatus(
					"Unable to startup the embedded Flashlight database using "
							+ DRIVER + ".", e));
		}

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

		List<StringBuilder> stmts = getSQLStatements(schemaURL);
		final Statement st = c.createStatement();
		try {
			for (StringBuilder b : stmts) {
				st.executeUpdate(b.toString());
			}
		} finally {
			st.close();
		}
	}

	private static List<StringBuilder> getSQLStatements(final URL schemaURL)
			throws CoreException {
		assert schemaURL != null;

		List<StringBuilder> result = new ArrayList<StringBuilder>();

		try {
			final InputStream is = schemaURL.openStream();
			final InputStreamReader isr = new InputStreamReader(is);
			final BufferedReader br = new BufferedReader(isr);

			try {
				StringBuilder b = new StringBuilder();
				String buffer;
				while ((buffer = br.readLine()) != null) {
					buffer = buffer.trim();
					if (buffer.startsWith("--") || buffer.equals("")) {
						// comment or blank line -- ignore this line
					} else if (buffer.endsWith(";")) {
						// end of an SQL statement -- add to our resulting list
						b.append(buffer);
						b.deleteCharAt(b.length() - 1); // remove the ";"
						result.add(b);
						b = new StringBuilder();
					} else {
						// add this line (with a newline after the first line)
						if (b.length() > 0)
							b.append("\n");
						b.append(buffer);
					}
				}
				br.readLine();
			} finally {
				br.close();
			}
		} catch (IOException e) {
			throw new CoreException(FLog.createErrorStatus(
					"Unable to open/read the Flashlight schema file: "
							+ schemaURL, e));
		}
		return result;
	}

	private static String getConnectionURL() {
		IPath pluginState = Activator.getDefault().getStateLocation();
		return JDBC_PRE + pluginState.toOSString() + JDBC_POST;
	}
}
