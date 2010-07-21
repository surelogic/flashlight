package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.logging.Level;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import com.surelogic.adhoc.eclipse.EclipseQueryUtility;
import com.surelogic.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQueryFullyBound;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.jobs.PopulateBrowserWithRunInformationJob;
import com.surelogic.flashlight.common.model.RunDescription;

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

	@Override
	protected void setupNoResultsPane(final Composite parent) {
		final RunDescription run = AdHocDataSource.getInstance()
				.getSelectedRun();
		if (run == null) {
			super.setupNoResultsPane(parent);
		} else {
			final Browser browser = new Browser(parent, SWT.NONE);
			browser.setForeground(parent.getDisplay().getSystemColor(
					SWT.COLOR_INFO_FOREGROUND));
			browser.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_INFO_BACKGROUND));

			/*
			 * Schedule a job to populate the browser with information about the
			 * selected run.
			 */
			final Job job = new PopulateBrowserWithRunInformationJob(run,
					browser);
			job.schedule();

			browser.addLocationListener(new LocationListener() {

				public void changing(LocationEvent event) {
					int index = event.location.indexOf(QUERY_PAT);
					if (index != -1) {
						/*
						 * We need to run a query
						 */
						event.doit = false; // don't really open the link
						parseAndRunQuery(event.location);
					}
				}

				public void changed(LocationEvent event) {
					// nothing
				}
			});

			browser.addOpenWindowListener(new OpenWindowListener() {
				public void open(final WindowEvent event) {
					event.required = true; // Cancel opening of new windows
				}
			});

			// Replace browser's built-in context menu with none
			browser.setMenu(new Menu(parent.getShell(), SWT.NONE));
		}
	}

	public static final String QUERY_PAT = "?query=";

	private void parseAndRunQuery(final String queryUrl) {
		try {
			final AdHocQueryFullyBound query = getManager().parseQueryUrl(
					queryUrl);
			EclipseQueryUtility.scheduleQuery(query, getManager()
					.getDataSource().getCurrentAccessKeys());
		} catch (IllegalStateException problem) {
			SLLogger.getLogger().log(Level.WARNING, I18N.err(213, queryUrl),
					problem);
		}
	}
}
