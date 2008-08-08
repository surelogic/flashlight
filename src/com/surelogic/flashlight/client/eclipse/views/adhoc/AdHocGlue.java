package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.AbstractFlashlightAdhocGlue;

public final class AdHocGlue extends AbstractFlashlightAdhocGlue {

	public static final AdHocGlue INSTANCE = new AdHocGlue();

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}
}
