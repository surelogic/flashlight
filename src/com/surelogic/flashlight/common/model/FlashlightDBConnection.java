package com.surelogic.flashlight.common.model;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.SchemaData;
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
