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

import com.surelogic.adhoc.eclipse.dialogs.LotsOfSavedQueriesDialog;
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
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.jobs.JobConstants;
import com.surelogic.flashlight.common.model.RunDescription;

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

	private boolean isValid() {
		return Activator.getDefault() != null;
	}

	/**
	 * The currently selected run, may be {@code null} which indicates that no
	 * run is selected.
	 */
	private volatile RunDescription f_selectedRun;

	/**
	 * Gets the currently selected run.
	 * 
	 * @return the currently selected run, or {@code null} if no run is
	 *         selected.
	 */
	public RunDescription getSelectedRun() {
		return f_selectedRun;
	}

	/**
	 * Sets the currently selected run.
	 * 
	 * @param runDescription
	 *            the run that is now selected, or {@code null} if no run is now
	 *            selected.
	 */
	public void setSelectedRun(final RunDescription runDescription) {
		f_selectedRun = runDescription;
	}

	public File getQuerySaveFile() {
		return new File(PreferenceConstants.getFlashlightDataDirectory(),
				"flashlight-queries.xml");
	}

	public URL getDefaultQueryUrl() {
		return Thread
				.currentThread()
				.getContextClassLoader()
				.getResource(
						"/com/surelogic/flashlight/common/default-flashlight-queries.xml");
	}

	public void badQuerySaveFileNotification(final Exception e) {
		try {
			SLLogger.getLogger().log(Level.SEVERE,
					I18N.err(4, getQuerySaveFile().getAbsolutePath()), e);
		} catch (final Exception e2) {
			SLLogger.getLogger().log(Level.SEVERE,
					I18N.err(4, "(unavailable)"), e);
		}
	}

	public final DBConnection getDB() {
		final RunDescription desc = getSelectedRun();
		return desc == null ? null : desc.getDB();
	}

	public int getMaxRowsPerQuery() {
		return Activator.getDefault().getPluginPreferences().getInt(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY);
	}

	public void init() {
		if (isValid()) {
			getManager().addObserver(this);
			getManager().addObserver(JumpToCode.getInstance());
		}
	}

	public void dispose() {
		if (isValid()) {
			getManager().removeObserver(JumpToCode.getInstance());
			getManager().removeObserver(this);
			AdHocManager.shutdown();
		}
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

	@Override
	public void notifyResultModelChange(final AdHocManager manager) {
		if (manager.getHasALotOfSqlDataResults()) {
			if (PreferenceConstants.getPromptAboutLotsOfSavedQueries()) {
				final UIJob job = new SLUIJob() {
					@Override
					public IStatus runInUIThread(final IProgressMonitor monitor) {
						final boolean doNotPromptAgain = LotsOfSavedQueriesDialog
								.show();
						if (doNotPromptAgain) {
							PreferenceConstants
									.setPromptAboutLotsOfSavedQueries(false);
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		}
	}

	public String getEditorViewId() {
		return QueryEditorView.class.getName();
	}

	public String[] getCurrentAccessKeys() {
		return new String[] { getSelectedRun().toIdentityString(),
				JobConstants.QUERY_KEY };
	}
}
