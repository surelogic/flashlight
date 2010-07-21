package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.Map;

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
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.adhoc.AdHocQueryFullyBound;
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
					// nothing
				}

				public void changed(LocationEvent event) {
					final String q = "?query=";
					int index = event.location.indexOf(q);
					if (index != -1) {
						final String id = event.location.substring(index
								+ q.length());
						AdHocManager manager = getManager();
						if (manager.contains(id)) {
							System.out.println("id is good for query run");
							final AdHocQuery query = manager.get(id);
							final Map<String, String> variables = manager
									.getGlobalVariableValues();
							AdHocQueryFullyBound boundQuery = new AdHocQueryFullyBound(
									query, variables);
							System.out.println("...scheduling...");
							EclipseQueryUtility.scheduleQuery(boundQuery,
									manager.getDataSource()
											.getCurrentAccessKeys());
						}
					}
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
}
