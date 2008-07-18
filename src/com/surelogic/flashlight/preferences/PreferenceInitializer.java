package com.surelogic.flashlight.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.flashlight.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		store.setDefault(PreferenceConstants.P_RAWQ_SIZE, 5000);
		store.setDefault(PreferenceConstants.P_OUTQ_SIZE, 5000);
		store.setDefault(PreferenceConstants.P_REFINERY_SIZE, 5000);
		store.setDefault(PreferenceConstants.P_USE_SPY, true);
		store.setDefault(PreferenceConstants.P_CONSOLE_PORT, 43524);
		store.setDefault(PreferenceConstants.P_MAX_ROWS_PER_QUERY, 5000);
	}
}
