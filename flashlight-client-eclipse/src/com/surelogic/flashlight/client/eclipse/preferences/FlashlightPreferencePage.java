package com.surelogic.flashlight.client.eclipse.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.adhoc.views.ExportQueryDialog;
import com.surelogic.common.ui.preferences.AbstractCommonPreferencePage;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;

public class FlashlightPreferencePage extends AbstractCommonPreferencePage {
  private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();
  private BooleanFieldEditor f_autoIncreaseHeap;
  private IntegerFieldEditor f_maxRowsPerQuery;
  private BooleanFieldEditor f_promptAboutLotsOfSavedQueries;
  private IntegerFieldEditor f_objectWindowSize;

  public FlashlightPreferencePage() {
    super("flashlight.", FlashlightPreferencesUtility.getSwitchPreferences());
  }

  @Override
  protected Control createContents(final Composite parent) {
    final Composite panel = new Composite(parent, SWT.NONE);
    final GridLayout grid = new GridLayout();
    panel.setLayout(grid);

    final Group dataGroup = new Group(panel, SWT.NONE);
    dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    dataGroup.setText(I18N.msg("flashlight.preference.page.group.data"));
    dataGroup.setLayout(new GridLayout());

    final Label dataDirectory = new Label(dataGroup, SWT.NONE);
    dataDirectory.setText(EclipseUtility.getFlashlightDataDirectory().getAbsolutePath());
    dataDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    final Group onGroup = new Group(panel, SWT.NONE);
    onGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    onGroup.setText(I18N.msg("flashlight.preference.page.group.onLaunch"));

    f_autoIncreaseHeap = new BooleanFieldEditor(FlashlightPreferencesUtility.AUTO_INCREASE_HEAP_AT_LAUNCH,
        I18N.msg("flashlight.preference.page.autoIncreaseHeap"), onGroup);
    finishSetup(f_autoIncreaseHeap);

    setupForPerspectiveSwitch(onGroup);

    final Group iGroup = new Group(panel, SWT.NONE);
    iGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    iGroup.setText(I18N.msg("flashlight.preference.page.group.inst"));

    final FlashlightInstrumentationWidgets instr = new FlashlightInstrumentationWidgets(this, EclipseUIUtility.getPreferences(),
        iGroup);
    for (final FieldEditor e : instr.getEditors()) {
      e.load();
    }
    f_editors.addAll(instr.getEditors());
    iGroup.setLayout(new GridLayout(3, false));

    final Group pGroup = new Group(panel, SWT.NONE);
    pGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    pGroup.setText(I18N.msg("flashlight.preference.page.group.prep"));

    f_objectWindowSize = new IntegerFieldEditor(FlashlightPreferencesUtility.PREP_OBJECT_WINDOW_SIZE,
        I18N.msg("flashlight.preference.page.objectWindowSize"), pGroup);
    f_objectWindowSize.setValidRange(10000, 1000000);
    f_objectWindowSize.fillIntoGrid(pGroup, 2);
    finishSetup(f_objectWindowSize);

    pGroup.setLayout(new GridLayout(2, false));

    final Group qGroup = new Group(panel, SWT.NONE);
    qGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    qGroup.setText(I18N.msg("flashlight.preference.page.group.query"));

    f_maxRowsPerQuery = new IntegerFieldEditor(FlashlightPreferencesUtility.MAX_ROWS_PER_QUERY,
        I18N.msg("flashlight.preference.page.maxRowsPerQuery"), qGroup);
    f_maxRowsPerQuery.setValidRange(1024, 65535);
    f_maxRowsPerQuery.fillIntoGrid(qGroup, 2);
    finishSetup(f_maxRowsPerQuery);

    f_promptAboutLotsOfSavedQueries = new BooleanFieldEditor(FlashlightPreferencesUtility.PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES,
        I18N.msg("flashlight.preference.page.promptAboutLotsOfSavedQueries"), qGroup);
    f_promptAboutLotsOfSavedQueries.fillIntoGrid(qGroup, 2);
    finishSetup(f_promptAboutLotsOfSavedQueries);

    qGroup.setLayout(new GridLayout(2, false));

    if (XUtil.useExperimental) {
      final Button exportButton = new Button(parent, SWT.PUSH);
      exportButton.setText("Export New Queries File");
      exportButton.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
      exportButton.addListener(SWT.Selection, new Listener() {
        @Override
        public void handleEvent(final Event event) {
          new ExportQueryDialog(EclipseUIUtility.getShell(), AdHocDataSource.getManager()).open();
        }
      });
    }
    return panel;
  }

  private void finishSetup(final FieldEditor editor) {
    editor.setPage(this);
    editor.setPreferenceStore(EclipseUIUtility.getPreferences());
    editor.load();

    f_editors.add(editor);
  }

  @Override
  protected void performDefaults() {
    for (final FieldEditor editor : f_editors) {
      editor.loadDefault();
    }
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    for (final FieldEditor editor : f_editors) {
      editor.store();
    }
    return super.performOk();
  }
}