package com.surelogic.flashlight;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.AbstractFlashlightAdhocGlue;
import com.surelogic.flashlight.preferences.PreferenceConstants;

public final class AdHocGlue extends AbstractFlashlightAdhocGlue {
	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}

	public File getQuerySaveFile() {
		File qsf = new File(PreferenceConstants.getFlashlightRawDataPath()
				+ System.getProperty("file.separator") + "queries.xml");
		if (!qsf.exists()) {
			try {
				copyDefaultQueryFile(qsf);
			} catch (IOException e) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Problem creating default query file", e);
			}
		}
		return qsf;
	}

	public Executor getExecutor() {
		// TODO
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
