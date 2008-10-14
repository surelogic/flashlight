package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.adhoc.IAdHocDataSource;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.Data;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public final class AdHocDataSource extends AdHocManagerAdapter implements
		IAdHocDataSource, ILifecycle {

	private static final AdHocDataSource INSTANCE = new AdHocDataSource();

	static {
		INSTANCE.init();
	}

	public static AdHocDataSource getInstance() {
		return INSTANCE;
	}

	private AdHocDataSource() {
		// singleton
	}

	public static AdHocManager getManager() {
		return AdHocManager.getInstance(INSTANCE);
	}

	public File getQuerySaveFile() {
		return new File(FileUtility.getFlashlightDataDirectory()
				+ File.separator + "flashlight-queries.xml");
	}

	public URL getDefaultQueryUrl() {
		return AdHocDataSource.class
				.getResource("/com/surelogic/flashlight/common/default-flashlight-queries.xml");
	}

	public void badQuerySaveFileNotification(final Exception e) {
		SLLogger.getLogger().log(Level.SEVERE,
				I18N.err(4, getQuerySaveFile().getAbsolutePath()), e);
	}

	public final DBConnection getDB() {
		return Data.getInstance();
	}

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}

	public void init() {
		getManager().addObserver(this);
		getManager().addObserver(JumpToCode.getInstance());
	}

	public void dispose() {
		getManager().removeObserver(JumpToCode.getInstance());
		getManager().removeObserver(this);
		AdHocManager.shutdown();
	}

	@Override
	public void notifySelectedResultChange(final AdHocQueryResult result) {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				final IViewPart view = ViewUtility.showView(
						QueryResultsView.class.getName(), null,
						IWorkbenchPage.VIEW_VISIBLE);
				if (view instanceof QueryResultsView) {
					final QueryResultsView queryResultsView = (QueryResultsView) view;
					queryResultsView.displayResult(result);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
