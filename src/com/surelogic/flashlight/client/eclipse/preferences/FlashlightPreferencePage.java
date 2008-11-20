package com.surelogic.flashlight.client.eclipse.preferences;

import java.util.*;

import org.eclipse.jface.preference.*;
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

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.dialogs.ChangeDataDirectoryDialog;
import com.surelogic.common.eclipse.preferences.AbstractLicensePreferencePage;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.images.CommonImages;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.common.FlashlightUtility;
import com.surelogic.flashlight.common.jobs.DisconnectAllDatabases;

public class FlashlightPreferencePage extends AbstractLicensePreferencePage {
	private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	private BooleanFieldEditor f_autoIncreaseHeap;
	private BooleanFieldEditor f_autoPerspectiveSwitch;
	private IntegerFieldEditor f_maxRowsPerQuery;
	private BooleanFieldEditor f_promptPerspectiveSwitch;
	private Label f_dataDirectory;

	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(I18N.msg("flashlight.preference.page.title.msg"));

		UsageMeter.getInstance().tickUse(
				"Flashlight FlashlightPreferencePage opened");
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		GridLayout grid = new GridLayout();
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
			public void handleEvent(Event event) {
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

		f_promptPerspectiveSwitch = new BooleanFieldEditor(
				PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH,
				I18N.msg("flashlight.preference.page.promptPerspectiveSwitch"),
				onGroup);
		finishSetup(f_promptPerspectiveSwitch);

		f_autoPerspectiveSwitch = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH,
				I18N.msg("flashlight.preference.page.autoPerspectiveSwitch"),
				onGroup);
		finishSetup(f_autoPerspectiveSwitch);

		final Group iGroup = new Group(panel, SWT.NONE);
		iGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		iGroup.setText(I18N.msg("flashlight.preference.page.group.inst"));

		FlashlightInstrumentationWidgets instr = 
			new FlashlightInstrumentationWidgets(this, getPreferenceStore(), 
					                             iGroup, iGroup);
		for(FieldEditor e : instr.getEditors()) {
			e.load();
		}
		f_editors.addAll(instr.getEditors());
		iGroup.setLayout(new GridLayout(2, false));

		final Group qGroup = new Group(panel, SWT.NONE);
		qGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		qGroup.setText(I18N.msg("flashlight.preference.page.group.query"));

		f_maxRowsPerQuery = new IntegerFieldEditor(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY, I18N
						.msg("flashlight.preference.page.maxRowsPerQuery"),
				qGroup);
		f_maxRowsPerQuery.setValidRange(1024, 65535);
		finishSetup(f_maxRowsPerQuery);
		
		return panel;
	}

	private void finishSetup(FieldEditor editor) {
		editor.setPage(this);
		editor.setPreferenceStore(getPreferenceStore());
		editor.load();
		
		f_editors.add(editor);
	}

	private void updateDataDirectory() {
		f_dataDirectory.setText(FlashlightUtility.getFlashlightDataDirectory()
				.getAbsolutePath());
	}

	@Override
	protected void performDefaults() {
		for(FieldEditor editor : f_editors) {
			editor.loadDefault();
		}
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		for(FieldEditor editor : f_editors) {
			editor.store();
		}
		return super.performOk();
	}
}