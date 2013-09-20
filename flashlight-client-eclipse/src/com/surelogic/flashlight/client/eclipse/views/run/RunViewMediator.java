package com.surelogic.flashlight.client.eclipse.views.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.progress.UIJob;

import com.surelogic.NonNull;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.TableUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.dialogs.DeleteRunDialog;
import com.surelogic.flashlight.client.eclipse.dialogs.LogDialog;
import com.surelogic.flashlight.client.eclipse.dialogs.RunControlDialog;
import com.surelogic.flashlight.client.eclipse.jobs.DeleteRunDirectoryJob;
import com.surelogic.flashlight.client.eclipse.model.IRunManagerObserver;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.model.RunManagerObserverAdapter;
import com.surelogic.flashlight.client.eclipse.refactoring.RegionModelRefactoring;
import com.surelogic.flashlight.client.eclipse.refactoring.RegionRefactoringInfo;
import com.surelogic.flashlight.client.eclipse.refactoring.RegionRefactoringWizard;
import com.surelogic.flashlight.client.eclipse.views.adhoc.FlashlightDataSource;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * Mediator for the {@link RunView}.
 */
public final class RunViewMediator extends AdHocManagerAdapter implements ILifecycle {

  private final TableViewer f_tableViewer;
  private final Table f_table;

  private final IRunManagerObserver f_rmo = new RunManagerObserverAdapter() {
    @Override
    public void notifyCollectionCompletedRunDirectoryChange() {
      EclipseUIUtility.asyncExec(new Runnable() {
        @Override
        public void run() {
          refresh();
        }
      });
    }
  };

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
        final RunDirectory[] selected = getSelectedRunDirectories();
        if (selected.length == 1) {
          final RunDirectory description = selected[0];
          if (description != null) {
            /*
             * Is it already prepared?
             */
            if (description.isPrepared()) {
              /*
               * Change the focus to the query menu view.
               */
              EclipseUIUtility.showView(QueryMenuView.class.getName());
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
        if ((e.character == SWT.DEL || e.character == SWT.BS) && e.stateMask == 0) {
          if (f_deleteAction.isEnabled()) {
            f_deleteAction.run();
          }
        }
      }
    });

    RunManager.getInstance().addObserver(f_rmo);
    AdHocManager.getInstance(FlashlightDataSource.getInstance()).addObserver(this);
    TableUtility.packColumns(f_tableViewer);
    setToolbarState();
  }

  /**
   * Gets the run description attached to the passed table item or returns
   * {@code null} if one does not exist.
   * 
   * @param item
   *          a table item.
   * @return the run description attached to the passed table item, or
   *         {@code null} if one does not exist.
   */
  private RunDirectory getData(final TableItem item) {
    if (item != null) {
      final Object o = item.getData();
      if (o instanceof RunDirectory) {
        return (RunDirectory) o;
      }
    }
    return null;
  }

  @NonNull
  private RunDirectory[] getSelectedRunDirectoriesNotBeingPrepared() {
    final List<RunDirectory> result = new ArrayList<RunDirectory>();
    for (RunDirectory r : getSelectedRunDirectories()) {
      if (!RunManager.getInstance().isBeingPrepared(r))
        result.add(r);
    }
    return result.toArray(new RunDirectory[result.size()]);
  }

  @NonNull
  private List<RunDirectory> getSelectedRunDirectoriesThatArePrepared() {
    final List<RunDirectory> result = new ArrayList<RunDirectory>();
    for (RunDirectory r : getSelectedRunDirectories()) {
      if (r.isPrepared())
        result.add(r);
    }
    return result;
  }

  @NonNull
  private RunDirectory[] getSelectedRunDirectories() {
    final TableItem[] items = f_table.getSelection();
    final RunDirectory[] results = new RunDirectory[items.length];
    for (int i = 0; i < items.length; i++) {
      results[i] = getData(items[i]);

    }
    return results;
  }

  private void setSelectedRunDirectory(final RunDirectory run) {
    if (run == null) {
      return;
    }
    final TableItem[] items = f_table.getItems();
    for (int i = 0; i < items.length; i++) {
      final RunDirectory itemData = getData(items[i]);
      if (run.getDescription().equals(itemData.getDescription())) {
        f_table.setSelection(i);
        break;
      }
    }
  }

  private final Action f_refreshAction = new Action() {
    @Override
    public void run() {
      RunManager.getInstance().refresh();
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
      final List<RunDirectory> notPrepped = new ArrayList<RunDirectory>();
      RunDirectory one = null;
      boolean hasPrep = false;
      for (final RunDirectory d : getSelectedRunDirectoriesNotBeingPrepared()) {
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
          msg = I18N.msg("flashlight.dialog.reprep.msg", one.getDescription().getName(),
              SLUtility.toStringDayHMS(one.getDescription().getStartTimeOfRun()));
        } else {
          title = I18N.msg("flashlight.dialog.reprep.multi.title");
          msg = I18N.msg("flashlight.dialog.reprep.multi.msg");
        }
        if (!MessageDialog.openConfirm(f_table.getShell(), title, msg)) {
          return; // bail out on cancel
        }
      }
      RunManager.getInstance().prepareAll(notPrepped);
    }
  };

  public Action getPrepAction() {
    return f_prepAction;
  }

  private final Action f_showLogAction = new Action() {
    @Override
    public void run() {
      final RunDirectory[] selected = getSelectedRunDirectories();
      for (final RunDirectory runDirectory : selected) {
        if (runDirectory != null) {
          /*
           * This dialog is modeless so that we can open more than one.
           */
          final LogDialog d = new LogDialog(f_table.getShell(), runDirectory);
          d.open();
        }
      }
    }
  };

  public Action getShowLogAction() {
    return f_showLogAction;
  }

  private final Action f_showRunControlAction = new Action() {
    @Override
    public void run() {
      RunControlDialog.show();
    }
  };

  public Action getShowRunControlAction() {
    return f_showRunControlAction;
  }

  private final Action f_deleteAction = new Action() {
    @Override
    public void run() {
      final RunDirectory[] selected = getSelectedRunDirectories();
      if (selected.length > 0) {
        final DeleteRunDialog d = new DeleteRunDialog(f_table.getShell(), selected[0].getDescription(), selected.length > 1);
        d.open();
        if (Window.CANCEL == d.getReturnCode()) {
          return;
        }
        for (final RunDirectory runDir : selected) {
          final SLJob job = new DeleteRunDirectoryJob(runDir);
          final Job eJob = EclipseUtility
              .toEclipseJob(job, runDir.getRunIdString(), RunManager.getInstance().getRefreshAccessKey());
          eJob.setUser(true);
          eJob.schedule();
        }
        RunManager.getInstance().refresh(); // after all deletes
      }
    }
  };

  public Action getDeleteAction() {
    return f_deleteAction;
  }

  private final Action f_inferJSureAnnoAction = new Action() {
    @Override
    public void run() {
      final List<RunDirectory> preparedRuns = getSelectedRunDirectoriesThatArePrepared();
      if (!preparedRuns.isEmpty()) {
        inferJSureAnnoHelper(preparedRuns);
      }
    }
  };

  private void inferJSureAnnoHelper(final List<RunDirectory> runs) {
    for (final IJavaProject p : JDTUtility.getJavaProjects()) {
      for (final RunDirectory run : runs) {
        final String runName = run.getDescription().getName();
        final int idx = runName.lastIndexOf('.');
        final String runPackage = runName.substring(0, idx);
        final String runClass = runName.substring(idx + 1);
        if (JDTUtility.findIType(p.getElementName(), runPackage, runClass) != null) {
          final RegionRefactoringInfo info = new RegionRefactoringInfo(Collections.singletonList(p), runs);
          info.setSelectedProject(p);
          info.setSelectedRuns(runs);
          final RegionModelRefactoring refactoring = new RegionModelRefactoring(info);
          final RegionRefactoringWizard wizard = new RegionRefactoringWizard(refactoring, info, false);
          final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
          try {
            final String title = I18N.msg("flashlight.recommend.refactor.regionIsThis");
            final int answer = op.run(EclipseUIUtility.getShell(), title);
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
    final RunDirectory[] selected = getSelectedRunDirectories();
    FlashlightDataSource.getInstance().setSelectedRun(selected.length == 0 ? null : selected[0]);
  }

  private final void setToolbarState() {
    /*
     * Consider auto-selecting the run if one and only one run exists.
     */
    final boolean autoSelectRun = f_table.getSelectionCount() == 0 && f_table.getItemCount() == 1;
    if (autoSelectRun) {
      f_table.setSelection(0);
    }

    final boolean somethingSelected = getSelectedRunDirectories().length > 0;
    f_deleteAction.setEnabled(somethingSelected); // will block if being prepped

    final RunDirectory[] notBeingPreparedSelected = getSelectedRunDirectoriesNotBeingPrepared();
    final boolean somethingIsSelectedNotBeingPrepared = notBeingPreparedSelected.length > 0;
    f_inferJSureAnnoAction.setEnabled(somethingIsSelectedNotBeingPrepared);
    boolean rawActionsEnabled = somethingIsSelectedNotBeingPrepared;

    /*
     * If only one item is selected change the focus of the query menu and if
     * the selection changed inform the SourceView so it shows code from that
     * run.
     */
    if (notBeingPreparedSelected.length == 0 || !notBeingPreparedSelected[0].isPrepared()) {
      FlashlightDataSource.getInstance().setSelectedRun(null);
      FlashlightDataSource.getManager().setGlobalVariableValue(AdHocManager.DATABASE, null);
      if (FlashlightDataSource.getManager().getSelectedResult() == null) {
        FlashlightDataSource.getManager().notifySelectedResultChange();
      }
    } else {
      final RunDirectory o = notBeingPreparedSelected[0];
      FlashlightDataSource.getInstance().setSelectedRun(o);
      FlashlightDataSource.getManager().setGlobalVariableValue(AdHocManager.DATABASE, o.getRunIdString());
      FlashlightDataSource.getManager().setSelectedResult(null);
      FlashlightDataSource.getManager().notifySelectedResultChange(); // may already be null (change overview)
    }
    f_prepAction.setEnabled(rawActionsEnabled);
    f_showLogAction.setEnabled(somethingSelected);
  }

  @Override
  public void dispose() {
    RunManager.getInstance().removeObserver(f_rmo);
    AdHocManager.getInstance(FlashlightDataSource.getInstance()).removeObserver(this);
  }

  public void setFocus() {
    f_table.setFocus();
  }

  @Override
  public void notifySelectedResultChange(final AdHocQueryResult result) {
    if (result != null) {
      /*
       * This method is trying to change the selected run in the run view when
       * the user selects a query result that is not using the data from the run
       * currently selected.
       */
      final String db = result.getQueryFullyBound().getVariableValues().get(AdHocManager.DATABASE);
      final RunDirectory desired = RunManager.getInstance().getCollectionCompletedRunDirectoryByIdString(db);

      final RunDirectory selected = FlashlightDataSource.getInstance().getSelectedRun();

      if (desired != null) {
        if (!desired.equals(selected)) {
          FlashlightDataSource.getInstance().setSelectedRun(desired);
          final UIJob job = new SLUIJob() {
            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
              setSelectedRunDirectory(desired);
              return Status.OK_STATUS;
            }
          };
          job.schedule();
        }
      }
    }
  }
}
