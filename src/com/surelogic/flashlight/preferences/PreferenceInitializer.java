package com.surelogic.flashlight.preferences;

import java.io.File;
import java.util.logging.Level;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.Activator;
import com.surelogic.flashlight.common.files.Raw;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		create(Raw.DEFAULT_FLASHLIGHT_DATA_LOCATION);
		store.setDefault(PreferenceConstants.P_RAW_PATH,
				Raw.DEFAULT_FLASHLIGHT_DATA_LOCATION);
		store.setDefault(PreferenceConstants.P_RAWQ_SIZE, 5000);
		store.setDefault(PreferenceConstants.P_OUTQ_SIZE, 5000);
		store.setDefault(PreferenceConstants.P_REFINERY_SIZE, 5000);
		store.setDefault(PreferenceConstants.P_USE_SPY, true);
		store.setDefault(PreferenceConstants.P_CONSOLE_PORT, 43524);
		store.setDefault(PreferenceConstants.P_MAX_ROWS_PER_QUERY, 5000);
	}

	/**
	 * Creates the specified path in the filesystem unless the path already
	 * exists.
	 * 
	 * @param path
	 *            the filesystem path.
	 */
	private void create(final String path) {
		File p = new File(path);
		if (!p.exists()) {
			if (!p.mkdirs()) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Couldn't create " + path);
			}
		}
	}
}
