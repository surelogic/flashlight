package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.jobs.JobConstants;

public final class QueryResultsView extends AbstractQueryResultsView {
	public QueryResultsView() {
		super(JobConstants.ACCESS_KEY);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight QueryResultsView opened");
		super.createPartControl(parent);
	}
	
	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}
}
