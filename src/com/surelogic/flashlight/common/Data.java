package com.surelogic.flashlight.common;

import java.io.File;
import java.net.URL;

import com.surelogic.common.FileUtility;
import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.flashlight.schema.FlashlightSchemaData;

public final class Data extends DerbyConnection {

	public static final String DEFAULT_FLASHLIGHT_QUERIES_URL = "/com/surelogic/flashlight/common/default-flashlight-queries.xml";

	private static final Data INSTANCE = new Data();

	public static Data getInstance() {
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

	@Override
	protected SchemaData getSchemaLoader() {
		return new FlashlightSchemaData();
	}

	public URL getDefaultQueryUrl() {
		return Data.class.getResource(DEFAULT_FLASHLIGHT_QUERIES_URL);
	}
}
