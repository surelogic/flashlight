package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.Map;
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
import org.eclipse.swt.widgets.Shell;

import com.surelogic.adhoc.eclipse.EclipseQueryUtility;
import com.surelogic.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQueryFullyBound;
import com.surelogic.common.eclipse.tooltip.ToolTip;
import com.surelogic.common.html.SimpleHTMLPrinter;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;
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
		if (run == null || !run.isPrepared()) {
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

				public void changing(final LocationEvent event) {
					int index = event.location.indexOf(QUERY_PAT);
					if (index != -1) {
						/*
						 * We need to run a query
						 */
						event.doit = false; // don't really open the link
						parseAndRunQuery(event.location);
						// It's worth a shot, and we can't actually make two
						// separate calls very easily
						parseAndJumpToLine(run, event.location);
					} else {
						index = event.location.indexOf(LOC_PAT);
						if (index != -1) {
							event.doit = false;
							parseAndJumpToLine(run, event.location);
						}
					}
				}

				public void changed(final LocationEvent event) {
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

	public static final String LOC_PAT = "?loc=";

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

	private void parseAndJumpToLine(final RunDescription run, final String url) {
		try {
			Map<String, String> params = SimpleHTMLPrinter
					.extractParametersFromURL(url);
			params.put(AdHocManager.DATABASE, run.toIdentityString());
			JumpToCode.getInstance().jumpToCode(params);
		} catch (IllegalStateException problem) {
			SLLogger.getLogger()
					.log(Level.WARNING, I18N.err(213, url), problem);
		}
	}

	@Override
	public ToolTip getToolTip(final Shell shell) {
		return new ToolTip(shell, FlashlightImageLoader.getInstance());
	}

}
