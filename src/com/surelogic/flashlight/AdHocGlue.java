package com.surelogic.flashlight;

import com.surelogic.flashlight.common.AbstractFlashlightAdhocGlue;
import com.surelogic.flashlight.preferences.PreferenceConstants;

public final class AdHocGlue extends AbstractFlashlightAdhocGlue {

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}
}
