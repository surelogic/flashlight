package com.surelogic.flashlight;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import com.surelogic.adhoc.AbstractAdHocGlue;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.preferences.PreferenceConstants;

public final class AdHocGlue extends AbstractAdHocGlue {

	public Connection getConnection() throws SQLException {
		return Data.getConnection();
	}

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}

	public File getQuerySaveFile() {
		return new File(PreferenceConstants.getFlashlightRawDataPath()
				+ System.getProperty("file.separator") + "queries.xml");
	}

	public Executor getExecutor() {
		// TODO
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
