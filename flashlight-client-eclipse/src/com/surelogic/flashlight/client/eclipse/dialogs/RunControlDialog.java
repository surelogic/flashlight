package com.surelogic.flashlight.client.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
import org.eclipse.swt.widgets.Display;
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
import com.surelogic.flashlight.common.model.DataCollectingRunState;
import com.surelogic.flashlight.common.model.IDataCollectingRun;
import com.surelogic.flashlight.common.model.IRunControlObserver;
import com.surelogic.flashlight.common.model.RunControlManager;

public final class RunControlDialog extends Dialog implements IRunControlObserver, TickListener {

  public static void show() {
    final Shell shell = EclipseUIUtility.getShell();
    final TimingSource ts = new SWTTimingSource(1, TimeUnit.SECONDS, shell.getDisplay());
    final RunControlDialog dialog = new RunControlDialog(shell, ts);
    RunControlManager.getInstance().register(dialog);
    ts.addTickListener(dialog);
    ts.init();
    dialog.open();
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
    RunControlManager.getInstance().remove(this);
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
    updateModelSafe();
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
    clearList.addListener(SWT.SELECTED, new Listener() {
      public void handleEvent(Event event) {
        clearListPressed();
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

  private void clearListPressed() {
    System.out.println("clearListPressed()");
  }

  protected IDialogSettings getDialogBoundsSettings() {
    return Activator.getDefault().getDialogSettings();
  }

  @Override
  public void launchedRunStateChanged(final IDataCollectingRun run, final DataCollectingRunState newState) {
    // NOT CALLED IN THE UI THREAD
    updateModelSafe();
  }

  @Override
  public void launchedRunCleared(Set<IDataCollectingRun> cleared) {
    // NOT CALLED IN THE UI THREAD
    updateModelSafe();
  }

  private class RunModel {

    RunModel(@NonNull IDataCollectingRun run, @NonNull DataCollectingRunState state) {
      f_run = run;
      f_state = state;
    }

    @NonNull
    final IDataCollectingRun f_run;
    @NonNull
    DataCollectingRunState f_state;

    @NonNull
    String getRunLabel() {
      return f_run.getRunSimpleNameforUI();
    }

    @NonNull
    String getRunStateLabel() {
      return f_state.getLabel();
    }

    @NonNull
    String getTimeSinceLaunch() {
      final Date launchDate = f_run.getLaunchTime();
      final long launched = launchDate.getTime();
      final long now = new Date().getTime();
      final long durationMS = now - launched;
      return "Started at " + SLUtility.toStringHMS(launchDate) + " running for "
          + SLUtility.toStringDurationS(durationMS, TimeUnit.MILLISECONDS);
    }

    Composite f_bk = null;
    Label f_image = null;
    Label f_runLabel = null;
    Label f_stateLabel = null;
    Label f_durationLabel = null;
    ToolItem f_action = null;

    final int f_dimension = 64;
    final Point f_size = new Point(f_dimension, f_dimension);
    final Image f_javaRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_JAVA_APP, false), f_size);
    final Image f_javaFinished = SLImages.getGrayscaleImage(f_javaRunning);
    final Image f_androidRunning = SLImages.scaleImage(SLImages.getImage(CommonImages.IMG_ANDROID_APP, false), f_size);
    final Image f_androidFinished = SLImages.getGrayscaleImage(f_androidRunning);

    Image getImage() {
      if (f_run.isAndroid()) {
        return f_state == DataCollectingRunState.FINISHED ? f_androidFinished : f_androidRunning;
      } else {
        return f_state == DataCollectingRunState.FINISHED ? f_javaFinished : f_javaRunning;
      }
    }

    void addToUI(@NonNull final Composite parent) {
      f_bk = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.numColumns = 3;
      layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
      f_bk.setLayout(layout);
      f_bk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      f_image = new Label(f_bk, SWT.NONE);
      f_image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
      f_image.setImage(getImage());

      final Composite labels = new Composite(f_bk, SWT.NONE);
      layout = new GridLayout();
      layout.numColumns = 1;
      layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 5;
      labels.setLayout(layout);
      labels.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      f_runLabel = new Label(labels, SWT.NONE);
      f_runLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_runLabel.setText(getRunLabel());
      f_runLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
      f_stateLabel = new Label(labels, SWT.NONE);
      f_stateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_stateLabel.setForeground(EclipseColorUtility.getSubtleTextColor());
      f_durationLabel = new Label(labels, SWT.NONE);
      f_durationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      f_durationLabel.setForeground(f_durationLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

      final ToolBar toolBar = new ToolBar(f_bk, SWT.FLAT);
      f_action = new ToolItem(toolBar, SWT.PUSH);
      f_action.setImage(SLImages.getImage(CommonImages.IMG_GRAY_X));
      f_action.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          modelItemActionPressed(RunModel.this);
        }
      });
      toolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    }

    void updateUI() {
      if (f_bk == null || f_bk.isDisposed())
        return;

      final Image image = getImage();
      if (f_image.getImage() != image)
        f_image.setImage(image);

      f_stateLabel.setText(getRunStateLabel());
      f_durationLabel.setText(getTimeSinceLaunch());
    }

    void removeFromUI() {
      if (f_bk == null || f_bk.isDisposed())
        return;

      f_bk.dispose();
    }
  }

  private final Comparator<RunModel> f_newToOld = new Comparator<RunControlDialog.RunModel>() {
    public int compare(RunModel o1, RunModel o2) {
      /*
       * Compares its two arguments for order. Returns a negative integer, zero,
       * or a positive integer as the first argument is less than, equal to, or
       * greater than the second.
       */
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return 1;
      if (o2 == null)
        return -1;
      return o2.f_run.getLaunchTime().compareTo(o1.f_run.getLaunchTime());
    }
  };

  private void modelItemActionPressed(@NonNull RunModel on) {
    System.out.println("modelItemActionPressed(" + on + ")");
  }

  private final LinkedList<RunModel> f_model = new LinkedList<RunModel>();

  @Nullable
  private RunModel getModelFor(IDataCollectingRun run) {
    for (RunModel element : f_model) {
      if (element.f_run.equals(run))
        return element;
    }
    return null;
  }

  private void updateModelSafe() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        updateModelInUIThreadContext();
      }
    });
  }

  private void updateModelInUIThreadContext() {
    if (f_dialogArea == null || f_dialogArea.isDisposed())
      return;

    final ArrayList<Pair<IDataCollectingRun, DataCollectingRunState>> runInfo = RunControlManager.getInstance().getManagedRuns();
    final List<RunModel> stillExists = new ArrayList<RunModel>();
    final List<RunModel> newModels = new ArrayList<RunModel>();
    for (Pair<IDataCollectingRun, DataCollectingRunState> pair : runInfo) {
      RunModel runModel = getModelFor(pair.first());
      if (runModel == null) {
        runModel = new RunModel(pair.first(), pair.second());
        f_model.addFirst(runModel);
        newModels.add(runModel); // added to UI below
      } else {
        runModel.updateUI();
      }
      stillExists.add(runModel);
    }
    // sort new models and add to UI
    Collections.sort(newModels, f_newToOld);
    for (RunModel runModel : newModels) {
      runModel.addToUI(f_dialogArea);
      runModel.updateUI();
    }
    // remove models that do not still exist
    for (Iterator<RunModel> iterator = f_model.iterator(); iterator.hasNext();) {
      final RunModel runModel = iterator.next();
      if (!stillExists.contains(runModel)) {
        iterator.remove();
        runModel.removeFromUI();
      }
    }
    f_dialogArea.getDisplay().asyncExec(new Runnable() {
      public void run() {
        if (f_dialogArea == null || f_dialogArea.isDisposed())
          return;
        f_dialogArea.layout();
      }
    });
  }

  public void timingSourceTick(TimingSource source, long nanoTime) {
    // CALLED IN SWT THREAD
    updateModelInUIThreadContext();
  }
}
