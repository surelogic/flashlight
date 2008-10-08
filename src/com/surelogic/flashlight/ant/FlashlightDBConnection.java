package com.surelogic.flashlight.ant;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.flashlight.schema.FlashlightSchemaData;

final class FlashlightDBConnection extends DerbyConnection {
	/**
	 * Path name of where the flashlight database is located.
	 */
	private final String dbLocation;

	/**
	 * Create a new connection. Call {@link #loggedBootAndCheckSchema} after
	 * creating this object.
	 */
	public FlashlightDBConnection(final String dbLoc) {
		dbLocation = dbLoc;
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
