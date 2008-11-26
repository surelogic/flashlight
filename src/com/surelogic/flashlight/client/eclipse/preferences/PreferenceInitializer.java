package com.surelogic.flashlight.client.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.flashlight.client.eclipse.Activator;

import static com.surelogic._flashlight.common.InstrumentationConstants.*;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		store.setDefault(PreferenceConstants.P_RAWQ_SIZE, FL_RAWQ_SIZE_DEFAULT);
		store.setDefault(PreferenceConstants.P_OUTQ_SIZE, FL_OUTQ_SIZE_DEFAULT);
		store.setDefault(PreferenceConstants.P_REFINERY_SIZE, FL_REFINERY_SIZE_DEFAULT);
		store.setDefault(PreferenceConstants.P_USE_SPY, true);
		store.setDefault(PreferenceConstants.P_CONSOLE_PORT, FL_CONSOLE_PORT_DEFAULT);
		store.setDefault(PreferenceConstants.P_MAX_ROWS_PER_QUERY, 5000);
		store.setDefault(PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH, true);
		store.setDefault(PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH, true);
		store.setDefault(PreferenceConstants.P_AUTO_INCREASE_HEAP_AT_LAUNCH,
				true);
		store.setDefault(PreferenceConstants.P_USE_REFINERY, true);
		store.setDefault(PreferenceConstants.P_OUTPUT_TYPE, FL_OUTPUT_TYPE_DEFAULT.isBinary());
		store.setDefault(PreferenceConstants.P_COMPRESS_OUTPUT, FL_OUTPUT_TYPE_DEFAULT.isCompressed());
		store.setDefault(PreferenceConstants.P_USE_FILTERING, false);
	}
}
