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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQueryFullyBound;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.core.adhoc.EclipseQueryUtility;
import com.surelogic.common.html.SimpleHTMLPrinter;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.common.ui.tooltip.ToolTip;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;
import com.surelogic.flashlight.client.eclipse.jobs.PopulateBrowserWithRunInformationJob;
import com.surelogic.flashlight.common.files.RunDirectory;

public final class QueryResultsView extends AbstractQueryResultsView {

    @Override
    public AdHocManager getManager() {
        return AdHocDataSource.getManager();
    }

    @Override
    public void displayResult(final AdHocQueryResult result) {
        showQueryTitle();
        super.displayResult(result);
    }

    /*
     * XXX We override this to handle the case where we have a result selected
     * before we create the actual control. We can't just fix this in the
     * superclass because the superclass doesn't have access to our manager.
     * (non-Javadoc)
     * 
     * @see
     * com.surelogic.common.ui.adhoc.views.results.AbstractQueryResultsView#
     * createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);
        AdHocQueryResult result = AdHocDataSource.getManager()
                .getSelectedResult();
        if (result != null) {
            displayResult(result);
        }
    }

    private static final String BROWSER_FLAG = "com.surelogic.browserFlag";

    private static Browser getBrowser(final Composite parent) {
        String flag = System.getProperty(BROWSER_FLAG);
        if (flag != null && flag.equalsIgnoreCase("DEFAULT")) {
            return new Browser(parent, SWT.NONE);
        }
        try {
            return new Browser(parent, SWT.MOZILLA);
        } catch (Error e) {
            // Do nothing
        }
        /*
         * Sadly the browser doen't fail too well on Mac, so clean out junky
         * control.
         */
        for (Control c : parent.getChildren()) {
            c.dispose();
        }
        return new Browser(parent, SWT.NONE);
    }

    @Override
    protected void setupNoResultsPane(final Composite parent) {
        final RunDirectory run = AdHocDataSource.getInstance()
                .getSelectedRun();
        if (run == null || !run.isPreparedOrIsBeingPrepared()) {
            showQueryTitle();
            super.setupNoResultsPane(parent);
        } else {
            showOverviewTitle();

            final Browser browser = getBrowser(parent);
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

                @Override
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

                @Override
                public void changed(final LocationEvent event) {
                    // nothing
                }
            });

            browser.addOpenWindowListener(new OpenWindowListener() {
                @Override
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

    private void parseAndJumpToLine(final RunDirectory run, final String url) {
        try {
            Map<String, String> params = SimpleHTMLPrinter
                    .extractParametersFromURL(url);
            params.put(AdHocManager.DATABASE, run.getDescription().toIdentityString());
            JumpToCode.getInstance().jumpToCode(params);
        } catch (IllegalStateException problem) {
            SLLogger.getLogger()
                    .log(Level.WARNING, I18N.err(213, url), problem);
        }
    }

    private void showQueryTitle() {
        setPartName(I18N.msg("flashlight.query.view.queryResultsTitle"));
    }

    private void showOverviewTitle() {
        setPartName(I18N.msg("flashlight.query.view.overviewTitle"));
    }

    @Override
    public ToolTip getToolTip(final Shell shell) {
        return new ToolTip(shell, FlashlightImageLoader.getInstance());
    }

}
