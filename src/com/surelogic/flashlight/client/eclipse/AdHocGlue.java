package com.surelogic.flashlight.client.eclipse;

import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.AbstractFlashlightAdhocGlue;

public final class AdHocGlue extends AbstractFlashlightAdhocGlue {

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}
}
