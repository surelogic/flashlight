package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.adhoc.views.menu.AbstractQueryMenuView;
import com.surelogic.common.adhoc.AdHocManager;

public final class QueryMenuView extends AbstractQueryMenuView {

	@Override
	public AdHocManager getManager() {
		return FlashlightAdHocDataSource.getManager();
	}

	@Override
	public String getQueryResultsViewId() {
		return QueryResultsView.class.getName();
	}
}
