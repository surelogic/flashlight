package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.AbstractFlashlightAdhocDataSource;

public final class AdHocDataSource extends AbstractFlashlightAdhocDataSource {

	public static final AdHocDataSource INSTANCE = new AdHocDataSource();

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}
}
