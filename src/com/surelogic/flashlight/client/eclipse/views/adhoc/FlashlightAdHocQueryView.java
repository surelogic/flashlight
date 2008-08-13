package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.adhoc.views.AdHocQueryView;
import com.surelogic.common.adhoc.AdHocManager;

public final class FlashlightAdHocQueryView extends AdHocQueryView {

	@Override
	public AdHocManager getManager() {
		return AdHocManager.getInstance(AdHocGlue.INSTANCE);
	}
}
