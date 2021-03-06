package com.surelogic.flashlight.client.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.core.animation.timing.TimingSource.TickListener;
import org.jdesktop.swt.animation.timing.sources.SWTTimingSource;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLJobTracker;
import com.surelogic.common.jobs.SLProgressMonitorObserver;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.model.IRunManagerObserver;
import com.surelogic.flashlight.client.eclipse.model.LaunchedRun;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.model.RunState;

public final class RunControlDialog extends Dialog implements IRunManagerObserver, TickListener {

  /**
   * This field tracks the dialog. The field is thread-confined to the SWT
   * thread.
   */
  static RunControlDialog f_dialogInstance = null;

  static final Runnable f_openDialogJob = new Runnable() {
    @Override
    public void run() {
      if (f_dialogInstance == null) {
        final Shell shell = EclipseUIUtility.getShell();
        final TimingSource ts = new SWTTimingSource(1, TimeUnit.SECONDS, shell.getDisplay());
        f_dialogInstance = new RunControlDialog(shell, ts);
        RunManager.getInstance().addObserver(f_dialogInstance);
        ts.addTickListener(f_dialogInstance);
        ts.init();
        f_dialogInstance.open();
      }
    }
  };

  /**
   * Opens and displays the run control dialog. If the dialog is already open
   * this call has no effect.
   * <p>
   * This call may be invoked from any thread context.
   */
  public static void show() {
    EclipseUIUtility.nowOrAsyncExec(f_openDialogJob);
  }

  RunControlDialog(Shell parentShell, @NonNull final TimingSource disposeOnClose) {
    super(parentShell);
    /*
     * Ensure that this dialog is modeless.
     */
    setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.MAX | SWT.MODELESS);
    setBlockOnOpen(false);
    f_disposeOnClose = disposeOnClose;
  }

  final TimingSource f_disposeOnClose;

  @Override
  public boolean close() {
    f_disposeOnClose.dispose();
    RunManager.getInstance().removeObserver(this);
    f_dialogInstance = null;
    return super.close();
  }

  ScrolledComposite f_sc = null;
  Composite f_dialogArea = null;

  @Override
  protected Control createDialogArea(Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    final ScrolledComposite sc = new ScrolledComposite(composite, SWT.V_SCROLL);
    sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    applyDialogFont(sc);
    sc.setExpandHorizontal(true);
    sc.setExpandVertical(true);
    f_sc = sc;

    // Create a child composite to hold the controls
    final Composite c = new Composite(sc, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
    c.setLayout(layout);
    c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    applyDialogFont(c);
    c.setBackground(EclipseColorUtility.getSlightlyDarkerBackgroundColor());

    sc.setContent(c);
    sc.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        Rectangle r = sc.getClientArea();
        sc.setMinSize(c.computeSize(r.width, SWT.DEFAULT));
      }
    });

    f_dialogArea = c;
    updateGUIModel();
    return composite;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(I18N.msg("flashlight.dialog.run.control.title"));
    newShell.setImage(SLImages.getImage(CommonImages.IMG_FL_RUN_CONTROL));
  }

  /**
   * A full replacement of the default button-only behavior.
   * <p>
   * Note that out parent implementation is very particular about what gets
   * returned. Be sure to look at the superclass implementation when making any
   * changes.
   * 
   * @param parent
   *          the parent composite to contain the button bar.
   * @return the button bar control. The returned control's layout data must be
   *         an instance of <code>GridData</code>.
   */
  @Override
  protected Control createButtonBar(Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    final GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    composite.setFont(parent.getFont());

    /*
     * This whole mess below is to implement a search box that works on the
     * various operating systems.
     */
    final Text search = new Text(composite, SWT.SEARCH | SWT.ICON_CANCEL);
    search.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    if ((search.getStyle() & SWT.ICON_CANCEL) == 0) {
      ToolBar toolBar = new ToolBar(composite, SWT.FLAT);
      ToolItem item = new ToolItem(toolBar, SWT.PUSH);
      item.setImage(SLImages.getImage(CommonImages.IMG_GRAY_X));
      item.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          search.setText("");
          searchCleared();
        }
      });
      layout.numColumns++;
      toolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    }
    search.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        if (e.detail == SWT.CANCEL) {
          searchCleared();
        } else {
          searchFor(search.getText());
        }
      }
    });
    showSearchPrompt(search); // at start
    search.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent e) {
        final String text = search.getText();
        if (text == null || "".equals(text)) {
          showSearchPrompt(search);
        }
      }

      @Override
      public void focusGained(FocusEvent e) {
        clearSearchPromptIfNecessary(search);
      }
    });
    search.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        if (search.getData() != SEARCH_PROMPT) {
          searchFor(search.getText());
        }
      }
    });

    final Button clearList = new Button(composite, SWT.PUSH);
    clearList.setText(I18N.msg("flashlight.dialog.run.control.clear_list"));
    clearList.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    clearList.setFont(parent.getFont());
    clearList.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        RunManager.getInstance().setDisplayToUserIfReadyOrCancelled(false);
      }
    });

    final Button dismiss = new Button(composite, SWT.PUSH);
    dismiss.setText(I18N.msg("flashlight.dialog.run.control.dismiss"));
    dismiss.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    dismiss.setFont(parent.getFont());
    dismiss.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        RunControlDialog.this.close();
      }
    });

    return composite;
  }

  static final String SEARCH_PROMPT = "Search...";

  void showSearchPrompt(final Text search) {
    search.setData(SEARCH_PROMPT);
    search.setText(SEARCH_PROMPT);
    search.setForeground(search.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
  }

  void clearSearchPromptIfNecessary(final Text search) {
    if (search.getData() == SEARCH_PROMPT) {
      search.setText("");
      search.setForeground(null);
      search.setData(null);
    }
  }

  @Nullable
  String f_searchFilterText = null;

  void searchCleared() {
    if (f_searchFilterText != null) {
      f_searchFilterText = null;
      updateGUIModel();
    }
  }

  void searchFor(@Nullable String value) {
    if (value == null || "".equals(value)) {
      searchCleared();
    } else {
      if (!value.equalsIgnoreCase(f_searchFilterText)) {
        f_searchFilterText = value.toLowerCase();
        updateGUIModel();
      }
    }
  }

  boolean filterOutDueToSearch(final LaunchedRun lrun) {
    if (f_searchFilterText == null) {
      return false;
    }

    final String runLabel = lrun.getRunLabel().toLowerCase();
    return !runLabel.startsWith(f_searchFilterText);
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    return Activator.getDefault().getDialogSettings();
  }

  /**
   * A wrapper for links to the run and its state plus all GUI objects for the
   * displayed panel in the dialog.
   */
  final class RunControlItem {

    RunControlItem(@NonNull final Composite parent, @NonNull final LaunchedRun lrun) {
      if (parent == null) {
        throw new IllegalArgumentException(I18N.err(44, "parent"));
      }
      updateLogicalModel(lrun);

      f_bk = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.numColumns = 3;
      layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
      f_bk.setLayout(layout);
      f_bk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      f_image = new Label(f_bk, SWT.NONE);
      f_image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

      f_labels = new Composite(f_bk, SWT.NONE);
      layout = new GridLayout();
      layout.numColumns = 1;
      layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
      f_labels.setLayout(layout);
      f_labels.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      f_runLabel = new Label(f_labels, SWT.NONE);
      f_runLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_runLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
      f_stateLabel = new Label(f_labels, SWT.NONE);
      f_stateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

      final ToolBar toolBar = new ToolBar(f_bk, SWT.FLAT);
      f_button = new ToolItem(toolBar, SWT.PUSH);
      f_button.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          buttonPressed();
        }
      });
      toolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

      updateGUIInformation();
    }

    private void updateLogicalModel(@NonNull final LaunchedRun lrun) {
      if (lrun == null) {
        throw new IllegalArgumentException(I18N.err(44, "lrun"));
      }
      f_lrun = lrun;
    }

    /*
     * The logical model: a run and its state
     */

    @NonNull
    LaunchedRun f_lrun;

    @NonNull
    String getRunLabel() {
      return f_lrun.getRunLabel();
    }

    @NonNull
    String getRunStateLabel() {
      return f_lrun.getState().getLabel();
    }

    @NonNull
    String getTimeInformation(boolean finished) {
      if (!finished) {
        final Date launchDate = f_lrun.getStartTime();
        final long launched = launchDate.getTime();
        final long now = new Date().getTime();
        final long durationMS = now - launched;
        return "Started at " + SLUtility.toStringNoDayHMS(launchDate) + " running for "
            + SLUtility.toStringDurationS(durationMS, TimeUnit.MILLISECONDS);
      } else {
        return "";
      }
    }

    /*
     * The GUI portion of this, never changes once created.
     */
    @NonNull
    final Composite f_bk;
    @NonNull
    final Label f_image;
    @NonNull
    final Composite f_labels;
    @NonNull
    final Label f_runLabel;
    @NonNull
    final Label f_stateLabel;
    @Nullable
    Control f_bottom;
    @NonNull
    final ToolItem f_button;

    final int f_dimension = 64;
    final Point f_size = new Point(f_dimension, f_dimension);
    final Image f_javaRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_JAVA_APP, false), f_size);
    final Image f_javaFinished = SLImages.getGrayscaleImage(f_javaRunning);
    final Image f_androidRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_ANDROID_APP, false), f_size);
    final Image f_androidFinished = SLImages.getGrayscaleImage(f_androidRunning);

    Image getImage() {
      if (f_lrun.isAndroid()) {
        return f_lrun.isReady() ? f_androidFinished : f_androidRunning;
      } else {
        return f_lrun.isReady() ? f_javaFinished : f_javaRunning;
      }
    }

    void updateDisplayWith(@NonNull final LaunchedRun lrun) {
      if (f_bk.isDisposed()) {
        return;
      }

      updateLogicalModel(lrun);
      updateGUIInformation();
    }

    void updateGUIInformation() {
      if (f_bk.isDisposed()) {
        return;
      }

      final boolean finished = f_lrun.isFinishedCollectingData() || f_lrun.isCancelled();

      final Image image = getImage();
      if (f_image.getImage() != image) {
        f_image.setImage(image);
      }

      f_runLabel.setText(getRunLabel());
      f_runLabel.setForeground(finished ? f_runLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY) : null);
      f_stateLabel.setText(getRunStateLabel());

      /*
       * Change and setup or update the bottom informational control
       */
      if (f_lrun.isFinishedCollectingData()) {
        SLJobTracker tracker = f_lrun.getPrepareJobTracker();
        if (tracker != null) {
          // show prep progress
          final ProgressBar prepProgressBar;
          if (f_bottom instanceof ProgressBar) {
            prepProgressBar = (ProgressBar) f_bottom;
          } else {
            if (f_bottom != null) {
              f_bottom.dispose();
            }
            prepProgressBar = new ProgressBar(f_labels, SWT.NONE);
            prepProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            prepProgressBar.setMaximum(100);
            prepProgressBar.setMinimum(0);
            f_bottom = prepProgressBar;
          }
          // fix text display to indicate prepare job is running
          f_stateLabel.setText(getRunStateLabel() + "...preparing data for querying...");
          /*
           * If the progress bar is already tracking this prepare job we do not
           * want to resetup the callbacks. We set the tracker as the progress
           * bar's data and check if they are the same.
           */
          Object data = prepProgressBar.getData();
          if (tracker != data) {
            tracker.clearObservers();
            prepProgressBar.setData(tracker);
            final int percentage = fixUpPercentage(tracker.getPercentageOrState());
            prepProgressBar.setSelection(percentage);
            tracker.addObserver(new SLProgressMonitorObserver() {
              @Override
              public void notifyPercentComplete(int percentage) {
                final int uiPercentage = fixUpPercentage(percentage);
                EclipseUIUtility.nowOrAsyncExec(new Runnable() {
                  @Override
                  public void run() {
                    if (!prepProgressBar.isDisposed()) {
                      prepProgressBar.setSelection(uiPercentage);
                    }
                  }
                });
              }
            });
          }
        } else {
          // display no control
          if (f_bottom != null) {
            f_bottom.dispose();
            f_bottom = null;
          }
        }
      } else if (!f_lrun.isCancelled()) {
        // show duration
        final Label durationLabel;
        if (f_bottom instanceof Label) {
          durationLabel = (Label) f_bottom;
        } else {
          if (f_bottom != null) {
            f_bottom.dispose();
          }
          durationLabel = new Label(f_labels, SWT.NONE);
          durationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
          durationLabel.setForeground(EclipseColorUtility.getSubtleTextColor());
          f_bottom = durationLabel;
        }
        durationLabel.setText(getTimeInformation(finished));
      }

      final Image buttonImage = f_button.getImage();
      final Image newImage;
      final String tipText;
      if (f_lrun.isFinishedCollectingData() || f_lrun.isCancelled()) {
        newImage = SLImages.getImage(CommonImages.IMG_GRAY_X);
        tipText = "flashlight.dialog.run.control.button.tip-dismiss";
      } else {
        newImage = SLImages.getImage(CommonImages.IMG_RED_X);
        tipText = "flashlight.dialog.run.control.button.tip-stop-collection";
      }
      if (buttonImage != newImage) {
        f_button.setImage(newImage);
      }
      f_button.setToolTipText(I18N.msg(tipText));
      f_button.setEnabled(f_lrun.getState() != RunState.STOP_COLLECTION_REQUESTED
          && f_lrun.getState() != RunState.INSTRUMENTATION_AND_LAUNCH);
    }

    final void buttonPressed() {
      if (f_lrun.isFinishedCollectingData() || f_lrun.isCancelled()) {
        RunManager.getInstance().setDisplayToUser(f_lrun.getRunIdString(), false);
      } else {
        RunManager.getInstance().requestDataCollectionToStop(f_lrun.getRunIdString());
      }
    }

    void dispose() {
      f_bk.dispose();
    }
  }

  final LinkedList<RunControlItem> f_guiModel = new LinkedList<>();

  /**
   * May be invoked within any thread context.
   */
  void updateGUIModel() {
    EclipseUIUtility.nowOrAsyncExec(new Runnable() {
      @Override
      public void run() {
        if (f_dialogArea == null || f_dialogArea.isDisposed()) {
          return;
        }

        final ArrayList<LaunchedRun> launchedRuns = RunManager.getInstance().getLaunchedRuns();

        for (Iterator<LaunchedRun> iterator = launchedRuns.iterator(); iterator.hasNext();) {
          LaunchedRun launchedRun = iterator.next();
          if (!launchedRun.getDisplayToUser()) {
            // clear out dismissed launched runs the user doesn't
            // want to see
            iterator.remove();
          } else {
            // check if filtered out by the user's search
            if (filterOutDueToSearch(launchedRun)) {
              iterator.remove();
            }
          }
        }

        /*
         * Update the GUI with updated information
         */
        for (int i = 0; i < launchedRuns.size(); i++) {
          final LaunchedRun lrun = launchedRuns.get(i);

          if (i < f_guiModel.size()) {
            final RunControlItem gui = f_guiModel.get(i);
            gui.updateDisplayWith(lrun);
          } else {
            final RunControlItem gui = new RunControlItem(f_dialogArea, lrun);
            f_guiModel.add(gui);
          }
        }
        // dispose of items no longer needed.
        final int modelSize = f_guiModel.size();
        for (int d = launchedRuns.size(); d < modelSize; d++) {
          final RunControlItem gui = f_guiModel.get(d);
          gui.dispose();
        }
        for (int d = launchedRuns.size(); d < modelSize; d++) {
          f_guiModel.removeLast();
        }

        f_dialogArea.layout(true, true);
        Rectangle r = f_sc.getClientArea();
        f_sc.setMinSize(f_dialogArea.computeSize(r.width, SWT.DEFAULT));
        f_sc.layout(true, true);
      }
    });
  }

  static int fixUpPercentage(final int percentage) {
    if (percentage == -1) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(298));
    }
    if (percentage < 0) {
      return 0;
    }
    if (percentage > 100) {
      return 100;
    }
    return percentage;
  }

  @Override
  public void timingSourceTick(TimingSource source, long nanoTime) {
    // CALLED IN SWT THREAD
    updateGUIModel();
  }

  @Override
  public void notifyLaunchedRunChange() {
    // NOT CALLED IN THE UI THREAD
    updateGUIModel();
  }

  @Override
  public void notifyPrepareDataJobScheduled() {
    // NOT CALLED IN THE UI THREAD
    updateGUIModel();
  }

  @Override
  public void notifyCollectionCompletedRunDirectoryChange() {
    // Ignore
  }
}
