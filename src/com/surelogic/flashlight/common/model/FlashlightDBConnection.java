package com.surelogic.flashlight.common.model;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.schema.FlashlightSchemaData;

public final class FlashlightDBConnection extends DerbyConnection {

	private static final ConcurrentHashMap<String, FlashlightDBConnection> INSTANCES = new ConcurrentHashMap<String, FlashlightDBConnection>();
	/**
	 * Path name of where the flashlight database is located.
	 */
	private final String dbLocation;

	/**
	 * Create a new connection.
	 */
	private FlashlightDBConnection(final String dbLoc) {
		dbLocation = dbLoc;
	}

	/**
	 * Create a new connection.
	 */
	public static DBConnection getInstance(final String dbLoc) {
		final FlashlightDBConnection conn = new FlashlightDBConnection(dbLoc);
		final FlashlightDBConnection old = INSTANCES.putIfAbsent(dbLoc, conn);
		if (old != null) {
			return old;
		} else {
			conn.loggedBootAndCheckSchema();
			return conn;
		}
	}

	@Override
	public synchronized void destroy() {
		INSTANCES.remove(dbLocation);
		super.destroy();
	}

	/**
	 * Shuts down all open database connections.
	 * 
	 * @throws IllegalStateException
	 *             if the databases are not shut down properly
	 */
	public static void shutdownConnections() {
		if (!INSTANCES.isEmpty()) {
			try {
				INSTANCES.clear();
				DriverManager.getConnection("jdbc:derby:;shutdown=true");
			} catch (final SQLException e) {
				if (e.getErrorCode() == 50000) {
					SLLogger.getLogger().log(Level.FINE, "Derby shut down", e);
				} else {
					throw new IllegalStateException(e);
				}
			}
		}
	}

	/**
	 * Create a new connection.
	 */
	public static DBConnection getInstance(final File dbLoc) {
		return getInstance(dbLoc.getAbsolutePath());
	}

	@Override
	protected String getDatabaseLocation() {
		return dbLocation;
	}

	@Override
	protected String getSchemaName() {
		return "FLASHLIGHT";
	}

	public SchemaData getSchemaLoader() {
		return new FlashlightSchemaData();
	}
}
