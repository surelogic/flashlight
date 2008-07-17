package com.surelogic.flashlight.common;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.flashlight.schema.FlashlightSchemaData;

public final class Data extends DerbyConnection {

	private Data() {
		// no instances
	}

	private static final String SCHEMA_NAME = "FLASHLIGHT";

	private String dbLocation = null;

	private ExecutorService exec = Executors.newSingleThreadExecutor();

	public ExecutorService getExecutor() {
		return exec;
	}

	public void restartExecutor() {
		exec.shutdownNow();
		exec = Executors.newSingleThreadExecutor();
	}

	public static URL getDefaultQueryFileURL() {
		return Data.class.getResource("/lib/queries/queries.xml");
	}

	@Override
	protected boolean deleteDatabaseOnStartup() {
		return false;
	}

	@Override
	protected synchronized String getDatabaseLocation() {
		return dbLocation;
	}

	public synchronized void setDatabaseLocation(final String location) {
		dbLocation = location;
	}

	@Override
	protected SchemaData getSchemaLoader() {
		return new FlashlightSchemaData();
	}

	@Override
	protected String getSchemaName() {
		return SCHEMA_NAME;
	}

	@Override
	protected void setDeleteDatabaseOnStartup(final boolean bool) {
		// Do nothing
	}

	private static final Data INSTANCE = new Data();

	public static Data getInstance() {
		return INSTANCE;
	}
}
