package com.surelogic.flashlight;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IPath;

import com.surelogic.adhoc.IAdHoc;
import com.surelogic.flashlight.preferences.PreferenceConstants;

public final class AdHocGlue implements IAdHoc {

	public Connection getConnection() throws SQLException {
		return Data.getConnection();
	}

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}

	public File getQuerySaveFile() {
		IPath pluginState = Activator.getDefault().getStateLocation();
		return new File(pluginState.toOSString()
				+ System.getProperty("file.separator") + "queries.xml");
	}
}
