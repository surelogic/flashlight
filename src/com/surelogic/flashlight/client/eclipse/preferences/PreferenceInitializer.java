package com.surelogic.flashlight.client.eclipse.preferences;

import static com.surelogic._flashlight.common.InstrumentationConstants.*;

import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.preferences.AbstractPrefInitializer;
import com.surelogic.flashlight.client.eclipse.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPrefInitializer {
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		store.setDefault(PreferenceConstants.P_RAWQ_SIZE, FL_RAWQ_SIZE_DEFAULT);
		store.setDefault(PreferenceConstants.P_OUTQ_SIZE, FL_OUTQ_SIZE_DEFAULT);
		store.setDefault(PreferenceConstants.P_REFINERY_SIZE,
				FL_REFINERY_SIZE_DEFAULT);
		store.setDefault(PreferenceConstants.P_USE_SPY, true);
		store.setDefault(PreferenceConstants.P_CONSOLE_PORT,
				FL_CONSOLE_PORT_DEFAULT);
		store.setDefault(PreferenceConstants.P_MAX_ROWS_PER_QUERY, 5000);
		store.setDefault(PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH, true);
		store.setDefault(PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH, true);
		store.setDefault(PreferenceConstants.P_AUTO_INCREASE_HEAP_AT_LAUNCH,
				true);
		store.setDefault(PreferenceConstants.P_USE_REFINERY, true);
		store.setDefault(PreferenceConstants.P_COLLECTION_TYPE,
				FL_COLLECTION_TYPE_DEFAULT.name());
		store.setDefault(PreferenceConstants.P_OUTPUT_TYPE,
				FL_OUTPUT_TYPE_DEFAULT.isBinary());
		store.setDefault(PreferenceConstants.P_COMPRESS_OUTPUT,
				FL_OUTPUT_TYPE_DEFAULT.isCompressed());
		store.setDefault(
				PreferenceConstants.P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, true);
		store.setDefault(PreferenceConstants.P_PROMPT_TO_PREP_ALL_RAW_DATA,
				true);
		store.setDefault(PreferenceConstants.P_AUTO_PREP_ALL_RAW_DATA, false);
		store.setDefault(PreferenceConstants.P_PREP_OBJECT_WINDOW_SIZE, 300000);
		store.setDefault(PreferenceConstants.P_DATA_DIRECTORY,
				getDefaultDataDirectory(FileUtility.FLASHLIGHT_DATA_PATH_FRAGMENT));
		
		// Get the data directory and ensure that it actually exists.
		final String path = store
				.getString(PreferenceConstants.P_DATA_DIRECTORY);
		ensureDataDirectoryExists(path);
	}
}
