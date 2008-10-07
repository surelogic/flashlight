package com.surelogic.flashlight.client.eclipse;

import java.io.File;

import com.surelogic.common.FileUtility;
import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.flashlight.schema.FlashlightSchemaData;

public final class Data extends DerbyConnection {

	private static final Data INSTANCE = new Data();

	public static DBConnection getInstance() {
		INSTANCE.loggedBootAndCheckSchema();
		return INSTANCE;
	}

	private Data() {
		// singleton
	}

	@Override
	protected String getDatabaseLocation() {
		return FileUtility.getFlashlightDataDirectory() + File.separator
				+ DATABASE_PATH_FRAGMENT;
	}

	@Override
	protected String getSchemaName() {
		return "FLASHLIGHT";
	}

	public SchemaData getSchemaLoader() {
		return new FlashlightSchemaData();
	}

}
