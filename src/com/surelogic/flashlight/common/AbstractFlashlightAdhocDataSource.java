package com.surelogic.flashlight.common;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import com.surelogic.common.FileUtility;
import com.surelogic.common.adhoc.IAdHocDataSource;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

public abstract class AbstractFlashlightAdhocDataSource implements
		IAdHocDataSource {

	public File getQuerySaveFile() {
		return new File(FileUtility.getFlashlightDataDirectory()
				+ File.separator + "queries.xml");
	}

	public URL getDefaultQueryUrl() {
		return Data.getDefaultQueryFileURL();
	}

	public void badQuerySaveFileNotification(Exception e) {
		SLLogger.getLogger().log(Level.SEVERE,
				I18N.err(4, getQuerySaveFile().getAbsolutePath()), e);
	}

	public final Connection getConnection() throws SQLException {
		return Data.getInstance().getConnection();
	}
}
