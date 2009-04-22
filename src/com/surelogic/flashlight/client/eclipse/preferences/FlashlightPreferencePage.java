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
import org.eclipse.ui.IWorkbench;

import com.surelogic.adhoc.views.ExportQueryDialog;
import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.dialogs.ChangeDataDirectoryDialog;
import com.surelogic.common.eclipse.preferences.AbstractCommonPreferencePage;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.FlashlightEclipseUtility;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.FlashlightUtility;
import com.surelogic.flashlight.common.jobs.DisconnectAllDatabases;

public class FlashlightPreferencePage extends AbstractCommonPreferencePage {
	private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	private BooleanFieldEditor f_autoIncreaseHeap;
	private IntegerFieldEditor f_maxRowsPerQuery;
	private BooleanFieldEditor f_promptAboutLotsOfSavedQueries;
	private Label f_dataDirectory;

	public FlashlightPreferencePage() {
		super("flashlight.", PreferenceConstants.prototype);
	}

	@Override
	public void init(final IWorkbench workbench) {
		super.init(workbench);

		UsageMeter.getInstance().tickUse(
				"Flashlight FlashlightPreferencePage opened");
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		final GridLayout grid = new GridLayout();
		panel.setLayout(grid);

		final Group dataGroup = new Group(panel, SWT.NONE);
		dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		dataGroup.setText(I18N.msg("flashlight.preference.page.group.data"));
		dataGroup.setLayout(new GridLayout(2, false));

		f_dataDirectory = new Label(dataGroup, SWT.NONE);
		updateDataDirectory();
		f_dataDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));

		final Button change = new Button(dataGroup, SWT.PUSH);
		change.setText(I18N
				.msg("flashlight.preference.page.changeDataDirectory"));
		change.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false,
				false));
		change.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				ChangeDataDirectoryDialog
						.open(
								change.getShell(),
								FlashlightUtility
										.getFlashlightDataDirectoryAnchor(),
								I18N
										.msg("flashlight.change.data.directory.dialog.title"),
								SLImages.getImage(CommonImages.IMG_FL_LOGO),
								I18N
										.msg("flashlight.change.data.directory.dialog.information"),
								new DisconnectAllDatabases(), null);
				updateDataDirectory();
			}
		});

		final Group onGroup = new Group(panel, SWT.NONE);
		onGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		onGroup.setText(I18N.msg("flashlight.preference.page.group.onLaunch"));

		f_autoIncreaseHeap = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_INCREASE_HEAP_AT_LAUNCH, I18N
						.msg("flashlight.preference.page.autoIncreaseHeap"),
				onGroup);
		finishSetup(f_autoIncreaseHeap);

		setupForPerspectiveSwitch(onGroup);

		final Group iGroup = new Group(panel, SWT.NONE);
		iGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		iGroup.setText(I18N.msg("flashlight.preference.page.group.inst"));

		final FlashlightInstrumentationWidgets instr = new FlashlightInstrumentationWidgets(
				this, getPreferenceStore(), iGroup);
		for (final FieldEditor e : instr.getEditors()) {
			e.load();
		}
		f_editors.addAll(instr.getEditors());
		iGroup.setLayout(new GridLayout(3, false));

		final Group qGroup = new Group(panel, SWT.NONE);
		qGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		qGroup.setText(I18N.msg("flashlight.preference.page.group.query"));

		f_maxRowsPerQuery = new IntegerFieldEditor(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY, I18N
						.msg("flashlight.preference.page.maxRowsPerQuery"),
				qGroup);
		f_maxRowsPerQuery.setValidRange(1024, 65535);
		f_maxRowsPerQuery.fillIntoGrid(qGroup, 2);
		finishSetup(f_maxRowsPerQuery);

		f_promptAboutLotsOfSavedQueries = new BooleanFieldEditor(
				PreferenceConstants.P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES,
				I18N
						.msg("flashlight.preference.page.promptAboutLotsOfSavedQueries"),
				qGroup);
		f_promptAboutLotsOfSavedQueries.fillIntoGrid(qGroup, 2);
		finishSetup(f_promptAboutLotsOfSavedQueries);
		
		qGroup.setLayout(new GridLayout(2, false));

		if (XUtil.useExperimental()) {
			final Button exportButton = new Button(parent, SWT.PUSH);
			exportButton.setText("Export New Queries File");
			exportButton.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT,
					false, false));
			exportButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event event) {
					new ExportQueryDialog(SWTUtility.getShell(),
							AdHocDataSource.getManager()).open();
				}
			});
		}
		return panel;
	}

	private void finishSetup(final FieldEditor editor) {
		editor.setPage(this);
		editor.setPreferenceStore(getPreferenceStore());
		editor.load();

		f_editors.add(editor);
	}

	private void updateDataDirectory() {
		f_dataDirectory.setText(FlashlightEclipseUtility.getFlashlightDataDirectory()
				.getAbsolutePath());
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