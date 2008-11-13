package com.surelogic.flashlight.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;

import com.surelogic.common.eclipse.preferences.AbstractLicensePreferencePage;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.Activator;

public class FlashlightPreferencePage extends AbstractLicensePreferencePage {

	private BooleanFieldEditor f_autoIncreaseHeap;
	private BooleanFieldEditor f_autoPerspectiveSwitch;
	private IntegerFieldEditor f_consolePort;
	private IntegerFieldEditor f_maxRowsPerQuery;
	private BooleanFieldEditor f_promptPerspectiveSwitch;
	private BooleanFieldEditor f_useSpyThread;

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

		final Group onGroup = new Group(panel, SWT.NONE);
		onGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		onGroup.setText(I18N.msg("flashlight.preference.page.group.onLaunch"));

		f_autoIncreaseHeap = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_INCREASE_HEAP_AT_LAUNCH, I18N
						.msg("flashlight.preference.page.autoIncreaseHeap"),
				onGroup);
		f_autoIncreaseHeap.setPage(this);
		f_autoIncreaseHeap.setPreferenceStore(getPreferenceStore());
		f_autoIncreaseHeap.load();

		f_promptPerspectiveSwitch = new BooleanFieldEditor(
				PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH,
				I18N.msg("flashlight.preference.page.promptPerspectiveSwitch"),
				onGroup);
		f_promptPerspectiveSwitch.setPage(this);
		f_promptPerspectiveSwitch.setPreferenceStore(getPreferenceStore());
		f_promptPerspectiveSwitch.load();

		f_autoPerspectiveSwitch = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH,
				I18N.msg("flashlight.preference.page.autoPerspectiveSwitch"),
				onGroup);
		f_autoPerspectiveSwitch.setPage(this);
		f_autoPerspectiveSwitch.setPreferenceStore(getPreferenceStore());
		f_autoPerspectiveSwitch.load();

		final Group iGroup = new Group(panel, SWT.NONE);
		iGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		iGroup.setText(I18N.msg("flashlight.preference.page.group.inst"));

		f_consolePort = new IntegerFieldEditor(
				PreferenceConstants.P_CONSOLE_PORT, I18N
						.msg("flashlight.preference.page.consolePort"), iGroup);
		f_consolePort.setValidRange(1024, 65535);
		f_consolePort.setPage(this);
		f_consolePort.setPreferenceStore(getPreferenceStore());
		f_consolePort.load();

		f_useSpyThread = new BooleanFieldEditor(PreferenceConstants.P_USE_SPY,
				I18N.msg("flashlight.preference.page.useSpyThread"), iGroup);
		f_useSpyThread.setPage(this);
		f_useSpyThread.setPreferenceStore(getPreferenceStore());
		f_useSpyThread.load();

		final Group qGroup = new Group(panel, SWT.NONE);
		qGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		qGroup.setText(I18N.msg("flashlight.preference.page.group.query"));

		f_maxRowsPerQuery = new IntegerFieldEditor(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY, I18N
						.msg("flashlight.preference.page.maxRowsPerQuery"),
				qGroup);
		f_maxRowsPerQuery.setValidRange(1024, 65535);
		f_maxRowsPerQuery.setPage(this);
		f_maxRowsPerQuery.setPreferenceStore(getPreferenceStore());
		f_maxRowsPerQuery.load();

		return panel;
	}

	// /**
	// * Creates the field editors. Field editors are abstractions of the common
	// * GUI blocks needed to manipulate various types of preferences. Each
	// field
	// * editor knows how to save and restore itself.
	// */
	// @Override
	// public void createFieldEditors() {
	// addField(new ScaleFieldEditor(PreferenceConstants.P_RAWQ_SIZE,
	// "Raw queue size:", getFieldEditorParent(), 1000, 50000, 1000,
	// 1000));
	// addField(new ScaleFieldEditor(PreferenceConstants.P_REFINERY_SIZE,
	// "Refinery cache size:", getFieldEditorParent(), 1000, 50000,
	// 1000, 1000));
	// addField(new ScaleFieldEditor(PreferenceConstants.P_OUTQ_SIZE,
	// "Out queue size:", getFieldEditorParent(), 1000, 50000, 1000,
	// 1000));
	//
	// }

	@Override
	protected void performDefaults() {
		f_autoIncreaseHeap.loadDefault();
		f_autoPerspectiveSwitch.loadDefault();
		f_consolePort.loadDefault();
		f_maxRowsPerQuery.loadDefault();
		f_promptPerspectiveSwitch.loadDefault();
		f_useSpyThread.loadDefault();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		f_autoIncreaseHeap.store();
		f_autoPerspectiveSwitch.store();
		f_consolePort.store();
		f_maxRowsPerQuery.store();
		f_promptPerspectiveSwitch.store();
		f_useSpyThread.store();
		return super.performOk();
	}
}