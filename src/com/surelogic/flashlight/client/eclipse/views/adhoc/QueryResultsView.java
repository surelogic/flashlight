package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.serviceability.UsageMeter;

public final class QueryResultsView extends AbstractQueryResultsView {

	@Override
	public void createPartControl(final Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight QueryResultsView opened");
		super.createPartControl(parent);
	}

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}
}
