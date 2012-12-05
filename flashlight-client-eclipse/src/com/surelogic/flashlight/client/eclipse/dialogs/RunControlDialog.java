package com.surelogic.flashlight.client.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.common.model.DataCollectingRunState;
import com.surelogic.flashlight.common.model.IDataCollectingRun;
import com.surelogic.flashlight.common.model.IRunControlObserver;
import com.surelogic.flashlight.common.model.RunControlManager;

public final class RunControlDialog extends Dialog implements IRunControlObserver {

  public static void show() {
    RunControlDialog dialog = new RunControlDialog(EclipseUIUtility.getShell());
    RunControlManager.getInstance().register(dialog);
    dialog.open();
    RunControlManager.getInstance().remove(dialog);
  }

  private RunControlDialog(Shell parentShell) {
    super(parentShell);
    /*
     * Ensure that this dialog is modeless.
     */
    setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.MAX | SWT.MODELESS);
    setBlockOnOpen(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite c = (Composite) super.createDialogArea(parent);
    c.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));

    return c;
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

  private static class RunModel {

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
      final long launched = f_run.getLaunchTime().getTime();
      final long now = new Date().getTime();
      final long durationMS = now - launched;
      return SLUtility.toStringDurationS(durationMS, TimeUnit.MILLISECONDS);
    }

  }

  private final ArrayList<RunModel> f_model = new ArrayList<RunModel>();

  @Nullable
  private RunModel getModelFor(IDataCollectingRun run) {
    for (RunModel element : f_model) {
      if (element.f_run.equals(run))
        return element;
    }
    return null;
  }

  private void updateModelSafe() {
    Display.getCurrent().asyncExec(new Runnable() {
      public void run() {
        updateModelInUIThreadContext();
      }
    });
  }

  private void updateModelInUIThreadContext() {
    ArrayList<Pair<IDataCollectingRun, DataCollectingRunState>> runInfo = RunControlManager.getInstance().getManagedRuns();
    List<RunModel> stillExists = new ArrayList<RunModel>();
    for (Pair<IDataCollectingRun, DataCollectingRunState> pair : runInfo) {
      RunModel runModel = getModelFor(pair.first());
      if (runModel == null) {
        // new run
        runModel = new RunModel(pair.first(), pair.second());
        // TODO
      }
      stillExists.add(runModel);
      // update run
      // TODO
    }
    for (Iterator<RunModel> iterator = f_model.iterator(); iterator.hasNext();) {
      final RunModel runModel = iterator.next();
      if (!stillExists.contains(runModel)) {
        iterator.remove();
        // TODO UI
      }
    }
  }

}
