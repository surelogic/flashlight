package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.AbstractFlashlightAdhocDataSource;

public final class FlashlightAdHocDataSource extends AbstractFlashlightAdhocDataSource {

	public static final FlashlightAdHocDataSource INSTANCE = new FlashlightAdHocDataSource();

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}
}
