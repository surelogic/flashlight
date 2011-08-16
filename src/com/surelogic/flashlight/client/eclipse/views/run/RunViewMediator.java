package com.surelogic.flashlight.client.eclipse.views.run;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AggregateSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.TableUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.dialogs.DeleteRunDialog;
import com.surelogic.flashlight.client.eclipse.dialogs.LogDialog;
import com.surelogic.flashlight.client.eclipse.jobs.PromptToPrepAllRawData;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.refactoring.RegionModelRefactoring;
import com.surelogic.flashlight.client.eclipse.refactoring.RegionRefactoringInfo;
import com.surelogic.flashlight.client.eclipse.refactoring.RegionRefactoringWizard;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.jobs.ConvertBinaryToXMLJob;
import com.surelogic.flashlight.common.jobs.DeleteRawFilesSLJob;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.jobs.UnPrepSLJob;
import com.surelogic.flashlight.common.model.IRunManagerObserver;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * Mediator for the {@link RunView}.
 */
public final class RunViewMediator extends AdHocManagerAdapter implements
        IRunManagerObserver, ILifecycle {

    private final TableViewer f_tableViewer;
    private final Table f_table;

    RunViewMediator(final TableViewer tableViewer) {
        f_tableViewer = tableViewer;
        f_table = tableViewer.getTable();
    }

    @Override
    public void init() {
        f_table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                updateRunManager();
                setToolbarState();
            }
        });
        f_table.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                final RunDescription[] selected = getSelectedRunDescriptions();
                if (selected.length == 1) {
                    final RunDescription description = selected[0];
                    if (description != null) {
                        /*
                         * Is it already prepared?
                         */
                        if (description.isPrepared()) {
                            /*
                             * Change the focus to the query menu view.
                             */
                            EclipseUIUtility.showView(QueryMenuView.class
                                    .getName());
                        } else {
                            /*
                             * Prepare this run.
                             */
                            f_prepAction.run();
                        }
                    }
                }
            }
        });
        f_table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if ((e.character == SWT.DEL || e.character == SWT.BS)
                        && e.stateMask == 0) {
                    if (f_deleteAction.isEnabled()) {
                        f_deleteAction.run();
                    }
                }
            }
        });

        RunManager.getInstance().addObserver(this);
        AdHocManager.getInstance(AdHocDataSource.getInstance()).addObserver(
                this);
        TableUtility.packColumns(f_tableViewer);
        setToolbarState();
    }

    /**
     * Gets the run description attached to the passed table item or returns
     * {@code null} if one does not exist.
     * 
     * @param item
     *            a table item.
     * @return the run description attached to the passed table item, or
     *         {@code null} if one does not exist.
     */
    private RunDescription getData(final TableItem item) {
        if (item != null) {
            final Object o = item.getData();
            if (o instanceof RunDescription) {
                return (RunDescription) o;
            }
        }
        return null;
    }

    /**
     * Gets the set of currently selected run descriptions.
     * 
     * @return the set of currently selected run descriptions, or an empty array
     *         if no row is selected.
     */
    private RunDescription[] getSelectedRunDescriptions() {
        final TableItem[] items = f_table.getSelection();
        final RunDescription[] results = new RunDescription[items.length];
        for (int i = 0; i < items.length; i++) {
            results[i] = getData(items[i]);

        }
        return results;
    }

    private void setSelectedRunDescription(final RunDescription run) {
        if (run == null) {
            return;
        }
        final TableItem[] items = f_table.getItems();
        for (int i = 0; i < items.length; i++) {
            final RunDescription itemData = getData(items[i]);
            if (run.equals(itemData)) {
                f_table.setSelection(i);
                break;
            }
        }
    }

    private final Action f_refreshAction = new Action() {
        @Override
        public void run() {
            EclipseJob.getInstance().scheduleDb(
                    new RefreshRunManagerSLJob(true), false, false,
                    RunManager.getInstance().getRunIdentities());
        }
    };

    public Action getRefreshAction() {
        return f_refreshAction;
    }

    void refresh() {
        f_tableViewer.refresh();
        TableUtility.packColumns(f_tableViewer);
        setToolbarState();
    }

    private final Action f_prepAction = new Action() {
        @Override
        public void run() {
            final List<RunDescription> notPrepped = new ArrayList<RunDescription>();
            RunDescription one = null;
            boolean hasPrep = false;
            for (final RunDescription d : getSelectedRunDescriptions()) {
                notPrepped.add(d);
                one = d;
                if (d.isPrepared()) {
                    hasPrep = true;
                }
            }

            if (hasPrep) {
                final String title;
                final String msg;
                if (notPrepped.size() == 1) {
                    title = I18N.msg("flashlight.dialog.reprep.title");
                    msg = I18N.msg("flashlight.dialog.reprep.msg",
                            one.getName(),
                            SLUtility.toStringHMS(one.getStartTimeOfRun()));
                } else {
                    title = I18N.msg("flashlight.dialog.reprep.multi.title");
                    msg = I18N.msg("flashlight.dialog.reprep.multi.msg");
                }
                if (!MessageDialog.openConfirm(f_table.getShell(), title, msg)) {
                    return; // bail out on cancel
                }
            }
            PromptToPrepAllRawData.runPrepJob(notPrepped);
        }
    };

    public Action getPrepAction() {
        return f_prepAction;
    }

    private final Action f_showLogAction = new Action() {
        @Override
        public void run() {
            final RunDescription[] selected = getSelectedRunDescriptions();
            for (final RunDescription description : selected) {
                if (description != null) {
                    final RawFileHandles handles = description
                            .getRawFileHandles();
                    if (handles != null) {
                        final File logFile = handles.getLogFile();
                        if (logFile != null) {
                            /*
                             * This dialog is modeless so that we can open more
                             * than one.
                             */
                            final LogDialog d = new LogDialog(
                                    f_table.getShell(), handles.getLogFile(),
                                    description);
                            d.open();
                        }
                    }
                }
            }
        }
    };

    public Action getShowLogAction() {
        return f_showLogAction;
    }

    private final Action f_convertToXmlAction = new Action() {
        @Override
        public void run() {
            final RunDescription[] selected = getSelectedRunDescriptions();
            for (final RunDescription description : selected) {
                if (description != null) {
                    final RawFileHandles handles = description
                            .getRawFileHandles();
                    if (handles != null) {
                        for (final File dataFile : handles.getDataFiles()) {
                            EclipseJob.getInstance().schedule(
                                    new ConvertBinaryToXMLJob(dataFile));
                        }
                    }
                }
            }
        }
    };

    public Action getConvertToXmlAction() {
        return f_convertToXmlAction;
    }

    private final Action f_deleteAction = new Action() {
        @Override
        public void run() {
            final ArrayList<SLJob> jobs = new ArrayList<SLJob>();
            final ArrayList<String> keys = new ArrayList<String>();
            final RunDescription[] selected = getSelectedRunDescriptions();
            if (selected.length > 0) {
                final DeleteRunDialog d = new DeleteRunDialog(
                        f_table.getShell(), selected[0], selected.length > 1);
                d.open();
                if (Window.CANCEL == d.getReturnCode()) {
                    return;
                }
                for (final RunDescription description : selected) {
                    final RawFileHandles handles = description
                            .getRawFileHandles();
                    if (description.isPrepared()) {
                        jobs.add(new UnPrepSLJob(description, AdHocDataSource
                                .getManager()));
                    }
                    if (handles != null) {
                        final File dataDir = FlashlightPreferencesUtility
                                .getFlashlightDataDirectory();
                        jobs.add(new DeleteRawFilesSLJob(dataDir, description));
                    }
                    keys.add(description.toIdentityString());
                }
            }
            if (!jobs.isEmpty()) {
                final RunDescription one = selected.length == 1 ? selected[0]
                        : null;
                final String jobName;
                if (one != null) {
                    jobName = I18N.msg("flashlight.jobs.delete.one",
                            one.getName(),
                            SLUtility.toStringHMS(one.getStartTimeOfRun()));
                } else {
                    jobName = I18N.msg("flashlight.jobs.delete.many");
                }

                final SLJob job = new AggregateSLJob(jobName, jobs);
                EclipseJob.getInstance().scheduleDb(job, true, false,
                        keys.toArray(new String[keys.size()]));
                EclipseJob.getInstance().scheduleDb(
                        new RefreshRunManagerSLJob(false), false, false,
                        RunManager.getInstance().getRunIdentities());
            }
        }
    };

    public Action getDeleteAction() {
        return f_deleteAction;
    }

    private final Action f_inferJSureAnnoAction = new Action() {
        @Override
        public void run() {
            final RunDescription[] selectedRunDescriptions = getSelectedRunDescriptions();
            final List<RunDescription> preparedRuns = new ArrayList<RunDescription>();
            boolean hasRun = false;
            for (final RunDescription description : selectedRunDescriptions) {
                if (description.isPrepared()) {
                    hasRun = true;
                    preparedRuns.add(description);
                }
            }
            if (hasRun) {
                inferJSureAnnoHelper(preparedRuns);
            }
        }
    };

    private void inferJSureAnnoHelper(final List<RunDescription> runs) {
        for (final IJavaProject p : JDTUtility.getJavaProjects()) {
            for (final RunDescription run : runs) {
                final String runName = run.getName();
                final int idx = runName.lastIndexOf('.');
                final String runPackage = runName.substring(0, idx);
                final String runClass = runName.substring(idx + 1);
                if (JDTUtility.findIType(p.getElementName(), runPackage,
                        runClass) != null) {
                    final RegionRefactoringInfo info = new RegionRefactoringInfo(
                            Collections.singletonList(p), runs);
                    info.setSelectedProject(p);
                    info.setSelectedRuns(runs);
                    final RegionModelRefactoring refactoring = new RegionModelRefactoring(
                            info);
                    final RegionRefactoringWizard wizard = new RegionRefactoringWizard(
                            refactoring, info, false);
                    final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(
                            wizard);
                    try {
                        final String title = I18N
                                .msg("flashlight.recommend.refactor.regionIsThis");
                        final int answer = op.run(EclipseUIUtility.getShell(),
                                title);
                        if (answer == IDialogConstants.OK_ID) {
                            try {
                                // TODO add jar and prompt to analyze
                            } catch (final NoClassDefFoundError ignore) {
                                /*
                                 * Expected if JSure is not installed
                                 */
                            }
                        }
                    } catch (final InterruptedException ignore) {
                        /*
                         * The operation was cancelled by the user.
                         */
                    }
                    return;
                }
            }
        }
    }

    public Action getInferJSureAnnoAction() {
        return f_inferJSureAnnoAction;
    }

    private final void updateRunManager() {
        final RunDescription[] selected = getSelectedRunDescriptions();
        AdHocDataSource.getInstance().setSelectedRun(
                selected.length == 0 ? null : selected[0]);
    }

    private final void setToolbarState() {
        /*
         * Consider auto-selecting the run if one and only one run exists.
         */
        final boolean autoSelectRun = f_table.getSelectionCount() == 0
                && f_table.getItemCount() == 1;
        if (autoSelectRun) {
            f_table.setSelection(0);
        }

        final RunDescription[] selected = getSelectedRunDescriptions();
        final boolean somethingIsSelected = selected.length > 0;
        f_deleteAction.setEnabled(somethingIsSelected);
        f_inferJSureAnnoAction.setEnabled(somethingIsSelected);
        boolean rawActionsEnabled = somethingIsSelected;
        boolean binaryActionsEnabled = somethingIsSelected;
        /*
         * Only enable raw actions if all the selected runs have raw data. Only
         * enable binary actions if all the selected runs have raw binary data.
         */
        for (final RunDescription rd : selected) {
            final RawFileHandles rfh = rd.getRawFileHandles();
            if (rfh == null) {
                rawActionsEnabled = false;
                binaryActionsEnabled = false;
            } else {
                if (binaryActionsEnabled) {
                    if (!rfh.isDataFileBinary()) {
                        binaryActionsEnabled = false;
                    }
                }
            }
        }
        /*
         * If only one item is selected change the focus of the query menu and
         * if the selection changed inform the SourceView so it shows code from
         * that run.
         */
        if (selected.length == 0 || !selected[0].isPrepared()) {
            AdHocDataSource.getInstance().setSelectedRun(null);
            AdHocDataSource.getManager().setGlobalVariableValue(
                    AdHocManager.DATABASE, null);
            if (AdHocDataSource.getManager().getSelectedResult() == null) {
                AdHocDataSource.getManager().notifySelectedResultChange();
            }
        } else {
            final RunDescription o = selected[0];
            AdHocDataSource.getInstance().setSelectedRun(o);
            AdHocDataSource.getManager().setGlobalVariableValue(
                    AdHocManager.DATABASE, o.toIdentityString());
            AdHocDataSource.getManager().setSelectedResult(null);
        }
        f_prepAction.setEnabled(rawActionsEnabled);
        f_showLogAction.setEnabled(rawActionsEnabled);
        f_convertToXmlAction.setEnabled(binaryActionsEnabled);
    }

    /**
     * Probably we have not been called from the SWT event dispatch thread.
     */
    @Override
    public void notify(final RunManager manager) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }

    @Override
    public void dispose() {
        RunManager.getInstance().removeObserver(this);
        AdHocManager.getInstance(AdHocDataSource.getInstance()).removeObserver(
                this);
    }

    public void setFocus() {
        f_table.setFocus();
    }

    @Override
    public void notifySelectedResultChange(final AdHocQueryResult result) {
        if (result != null) {
            /*
             * This method is trying to change the selected run in the run view
             * when the user selects a query result that is not using the data
             * from the run currently selected.
             */
            final String db = result.getQueryFullyBound().getVariableValues()
                    .get(AdHocManager.DATABASE);
            final RunDescription desired = RunManager.getInstance()
                    .getRunByIdentityString(db);

            final RunDescription selected = AdHocDataSource.getInstance()
                    .getSelectedRun();

            if (desired != null) {
                if (!desired.equals(selected)) {
                    AdHocDataSource.getInstance().setSelectedRun(desired);
                    final UIJob job = new SLUIJob() {
                        @Override
                        public IStatus runInUIThread(
                                final IProgressMonitor monitor) {
                            setSelectedRunDescription(desired);
                            return Status.OK_STATUS;
                        }
                    };
                    job.schedule();
                }
            }
        }
    }
}
