package com.surelogic.flashlight.client.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

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
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.model.IRunManagerObserver;
import com.surelogic.flashlight.client.eclipse.model.RunState;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.model.DataCollectingRunState;
import com.surelogic.flashlight.common.model.IDataCollectingRun;

public final class RunControlDialog extends Dialog implements IRunManagerObserver, TickListener {

  /**
   * This field tracks the dialog. The field is thread-confined to the SWT
   * thread.
   */
  private static RunControlDialog f_dialogInstance = null;

  private static final Runnable f_openDialogJob = new Runnable() {
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

  private RunControlDialog(Shell parentShell, @NonNull final TimingSource disposeOnClose) {
    super(parentShell);
    /*
     * Ensure that this dialog is modeless.
     */
    setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.MAX | SWT.MODELESS);
    setBlockOnOpen(false);
    f_disposeOnClose = disposeOnClose;
  }

  private final TimingSource f_disposeOnClose;

  @Override
  public boolean close() {
    f_disposeOnClose.dispose();
    RunManager.getInstance().removeObserver(this);
    f_dialogInstance = null;
    return super.close();
  }

  private Composite f_dialogArea = null;

  @Override
  protected Control createDialogArea(Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    final ScrolledComposite sc = new ScrolledComposite(composite, SWT.BORDER | SWT.V_SCROLL);
    sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    applyDialogFont(sc);
    sc.setExpandHorizontal(true);
    sc.setExpandVertical(true);

    // Create a child composite to hold the controls
    final Composite c = new Composite(sc, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
    c.setLayout(layout);
    c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    applyDialogFont(c);
    c.setBackground(EclipseColorUtility.getRunControlBackgroundColor());

    sc.setContent(c);
    sc.addControlListener(new ControlAdapter() {
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
    newShell.setText(I18N.msg("flashligh.dialog.run.control.title"));
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
    layout.numColumns = 2;
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    composite.setFont(parent.getFont());

    final Button clearList = new Button(composite, SWT.PUSH);
    clearList.setText(I18N.msg("flashligh.dialog.run.control.clear_list"));
    clearList.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    clearList.setFont(parent.getFont());
    clearList.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        // TODO RunControlManager.getInstance().clearAllFinishedRuns();
      }
    });

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
        public void widgetSelected(SelectionEvent e) {
          search.setText("");
          searchCleared();
        }
      });
      layout.numColumns++;
      toolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    }
    search.addSelectionListener(new SelectionAdapter() {
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
      public void focusLost(FocusEvent e) {
        final String text = search.getText();
        if (text == null || "".equals(text)) {
          showSearchPrompt(search);
        }
      }

      public void focusGained(FocusEvent e) {
        clearSearchPromptIfNecessary(search);
      }
    });

    return composite;
  }

  private static final String SEARCH_PROMPT = "Search...";

  private void showSearchPrompt(final Text search) {
    search.setText(SEARCH_PROMPT);
    search.setForeground(search.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    search.setData(SEARCH_PROMPT);
  }

  private void clearSearchPromptIfNecessary(final Text search) {
    if (search.getData() == SEARCH_PROMPT) {
      search.setText("");
      search.setForeground(null);
      search.setData(null);
    }
  }

  private void searchCleared() {
    System.out.println("searchCleared()");
  }

  private void searchFor(@Nullable String value) {
    System.out.println("searchFor(" + value + ")");

    if (value == null || "".equals(value))
      searchCleared();
  }

  protected IDialogSettings getDialogBoundsSettings() {
    return Activator.getDefault().getDialogSettings();
  }

  @Override
  public void notifyLaunchedRunChange() {
    // Nothing to do
  }

  @Override
  public void notifyPrepareDataJobScheduled() {
    // NOT CALLED IN THE UI THREAD
    updateGUIModel();
  }

  @Override
  public void notifyCollectionCompletedRunDirectoryChange() {
    // NOT CALLED IN THE UI THREAD
    updateGUIModel();
  }

  /**
   * A wrapper for links to the run and its state plus all GUI objects for the
   * displayed panel in the dialog.
   */
  private final class RunControlItem {

    RunControlItem(@NonNull final Composite parent, @NonNull String runIdString, @NonNull RunState state) {
      if (parent == null)
        throw new IllegalArgumentException(I18N.err(44, "parent"));
      updateLogicalModel(runIdString, state);

      f_bk = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.numColumns = 3;
      layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
      f_bk.setLayout(layout);
      f_bk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      f_image = new Label(f_bk, SWT.NONE);
      f_image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

      final Composite labels = new Composite(f_bk, SWT.NONE);
      layout = new GridLayout();
      layout.numColumns = 1;
      layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
      labels.setLayout(layout);
      labels.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      f_runLabel = new Label(labels, SWT.NONE);
      f_runLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_runLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
      f_stateLabel = new Label(labels, SWT.NONE);
      f_stateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_stateLabel.setForeground(EclipseColorUtility.getSubtleTextColor());
      f_durationLabel = new Label(labels, SWT.NONE);
      f_durationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_durationLabel.setForeground(f_durationLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

      final ToolBar toolBar = new ToolBar(f_bk, SWT.FLAT);
      f_clearFinishedRun = new ToolItem(toolBar, SWT.PUSH);
      f_clearFinishedRun.setImage(SLImages.getImage(CommonImages.IMG_GRAY_X));
      f_clearFinishedRun.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
//          if (f_runIdString != null && f_state != null && f_state.equals(InstrumentedApplicationState.DONE_COLLECTING_DATA))
            //RunControlManager.getInstance().clearFinishedRun(f_run);
        }
      });
      toolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

      updateGUIInformation();
    }

    private void updateLogicalModel(@NonNull String runIdString, @NonNull RunState state) {
      if (runIdString == null)
        throw new IllegalArgumentException(I18N.err(44, "runIdString"));
      f_runIdString = runIdString;
      if (state == null)
        throw new IllegalArgumentException(I18N.err(44, "state"));
      f_state = state;
    }

    /*
     * The logical model: a run and its state
     */

    @NonNull
    String f_runIdString;
    @NonNull
    RunState f_state;

    @NonNull
    String getRunLabel() {
      return "TODO"; // f_run.getRunSimpleNameforUI();
    }

    @NonNull
    String getRunStateLabel() {
      return f_state.getLabel();
    }

    @NonNull
    String getTimeInformation(boolean finished) {
      if (!finished) {
        final Date launchDate = null; // TODO f_run.getLaunchTime();
        final long launched = launchDate.getTime();
        final long now = new Date().getTime();
        final long durationMS = now - launched;
        return "Started at " + SLUtility.toStringHMS(launchDate) + " running for "
            + SLUtility.toStringDurationS(durationMS, TimeUnit.MILLISECONDS);
      } else
        return "";
    }

    /*
     * The GUI portion of this, never changes once created.
     */
    @NonNull
    final Composite f_bk;
    @NonNull
    final Label f_image;
    @NonNull
    final Label f_runLabel;
    @NonNull
    final Label f_stateLabel;
    @NonNull
    final Label f_durationLabel;
    @NonNull
    final ToolItem f_clearFinishedRun;

    final int f_dimension = 64;
    final Point f_size = new Point(f_dimension, f_dimension);
    final Image f_javaRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_JAVA_APP, false), f_size);
    final Image f_javaFinished = SLImages.getGrayscaleImage(f_javaRunning);
    final Image f_androidRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_ANDROID_APP, false), f_size);
    final Image f_androidFinished = SLImages.getGrayscaleImage(f_androidRunning);

    Image getImage() {
      if (true /* TODOf_run.isAndroid() */) {
        return f_state == RunState.DONE_COLLECTING_DATA ? f_androidFinished : f_androidRunning;
      } else {
        return f_state == RunState.DONE_COLLECTING_DATA ? f_javaFinished : f_javaRunning;
      }
    }

    void updateDisplayWith(@NonNull String runIdString, @NonNull RunState state) {
      if (f_bk.isDisposed())
        return;

      updateLogicalModel(runIdString, state);
      updateGUIInformation();
    }

    void updateGUIInformation() {
      if (f_bk.isDisposed())
        return;

      final boolean finished = f_state.equals(DataCollectingRunState.FINISHED);

      final Image image = getImage();
      if (f_image.getImage() != image)
        f_image.setImage(image);

      f_runLabel.setText(getRunLabel());
      f_runLabel.setForeground(finished ? f_runLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY) : null);
      f_stateLabel.setText(getRunStateLabel());
      f_durationLabel.setText(getTimeInformation(finished));
      f_clearFinishedRun.setEnabled(finished);
    }

    void dispose() {
      if (!f_bk.isDisposed())
        f_bk.dispose();
    }
  }

  private final Comparator<Pair<IDataCollectingRun, DataCollectingRunState>> f_newestToOldest = new Comparator<Pair<IDataCollectingRun, DataCollectingRunState>>() {
    public int compare(Pair<IDataCollectingRun, DataCollectingRunState> o1, Pair<IDataCollectingRun, DataCollectingRunState> o2) {
      /*
       * Compares its two arguments for order. Returns a negative integer, zero,
       * or a positive integer as the first argument is less than, equal to, or
       * greater than the second.
       */
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null || o1.first() == null)
        return 1;
      if (o2 == null || o2.first() == null)
        return -1;
      return -1;
      // TODO return
      // o2.first().getLaunchTime().compareTo(o1.first().getLaunchTime());
    }
  };

  private final LinkedList<RunControlItem> f_guiModel = new LinkedList<RunControlItem>();

  private void updateGUIModel() {
    EclipseUIUtility.nowOrAsyncExec(new Runnable() {
      public void run() {
        if (f_dialogArea == null || f_dialogArea.isDisposed())
          return;

        final ArrayList<Pair<String, DataCollectingRunState>> runInfo = null; // TODORunControlManager.getInstance().getManagedRuns();
       // Collections.sort(runInfo, f_newestToOldest);
        /*
         * Update the GUI with updated information
         */
        for (int i = 0; i < runInfo.size(); i++) {
          final Pair<IDataCollectingRun, DataCollectingRunState> pair = null; // TODO runInfo.get(i);
          if (i < f_guiModel.size()) {
            final RunControlItem gui = f_guiModel.get(i);
           // gui.updateDisplayWith(pair.first(), pair.second());
          } else {
            final RunControlItem gui = null; //TODOnew RunControlItem(f_dialogArea, pair.first(), pair.second());
            f_guiModel.add(gui);
          }
        }
        // dispose of items no longer needed.
        final int modelSize = f_guiModel.size();
        for (int d = runInfo.size(); d < modelSize; d++) {
          final RunControlItem gui = f_guiModel.get(d);
          gui.dispose();
        }
        for (int d = runInfo.size(); d < modelSize; d++)
          f_guiModel.removeLast();

        EclipseUIUtility.asyncExec(new Runnable() {
          public void run() {
            if (f_dialogArea == null || f_dialogArea.isDisposed())
              return;
            f_dialogArea.layout();
          }
        });
      }
    });
  }

  public void timingSourceTick(TimingSource source, long nanoTime) {
    // CALLED IN SWT THREAD
    updateGUIModel();
  }
}
