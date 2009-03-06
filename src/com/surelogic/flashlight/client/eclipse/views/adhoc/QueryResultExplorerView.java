package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.adhoc.views.explorer.AbstractQueryResultExplorerView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.serviceability.UsageMeter;

public class QueryResultExplorerView extends AbstractQueryResultExplorerView {

	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse(
				"Flashlight QueryResultExplorerView opened");
		super.createPartControl(parent);
	}

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}
}
