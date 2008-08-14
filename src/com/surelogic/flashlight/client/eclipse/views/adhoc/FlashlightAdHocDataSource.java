package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.adhoc.IAdHocManagerObserver;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.AbstractFlashlightAdhocDataSource;

public final class FlashlightAdHocDataSource extends
		AbstractFlashlightAdhocDataSource implements IAdHocManagerObserver,
		ILifecycle {

	private static final FlashlightAdHocDataSource INSTANCE = new FlashlightAdHocDataSource();

	static {
		INSTANCE.init();
	}

	public static FlashlightAdHocDataSource getInstance() {
		return INSTANCE;
	}

	private FlashlightAdHocDataSource() {
		// singleton
	}

	public static AdHocManager getManager() {
		return AdHocManager.getInstance(INSTANCE);
	}

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}

	public void init() {
		getManager().addObserver(this);
	}

	public void dispose() {
		getManager().removeObserver(this);
		AdHocManager.shutdown();
	}

	public void notifyNewResult(final AdHocQueryResult result) {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IViewPart view = ViewUtility.showView(QueryResultsView.class
						.getName());
				if (view instanceof QueryResultsView) {
					QueryResultsView queryResultsView = (QueryResultsView) view;
					queryResultsView.displayResult(result);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public void notifyQueryModelChange(AdHocManager manager) {
		// nothing to do
	}

	public void notifyResultModelChange(AdHocManager manager) {
		// nothing to do
	}
}
