package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import com.surelogic.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.jobs.ObtainRunHtmlJob;
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

			/*
			 * Schedule a job to populate the browser with information about the
			 * selected run.
			 */
			final Job job = new ObtainRunHtmlJob(run, browser);
			job.schedule();

			browser.addLocationListener(new LocationListener() {

				public void changing(LocationEvent event) {
					System.out.println("changing : " + event);
				}

				public void changed(LocationEvent event) {
					System.out.println("changed : " + event);
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
