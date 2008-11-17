package com.surelogic.flashlight.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
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

	private BooleanFieldEditor f_autoIncreaseHeap;
	private BooleanFieldEditor f_autoPerspectiveSwitch;
	private IntegerFieldEditor f_consolePort;
	private IntegerFieldEditor f_maxRowsPerQuery;
	private IntegerFieldEditor f_outQSize;
	private BooleanFieldEditor f_promptPerspectiveSwitch;
	private IntegerFieldEditor f_rawQSize;
	private IntegerFieldEditor f_refinerySize;
	private BooleanFieldEditor f_useSpyThread;
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
		f_consolePort.fillIntoGrid(iGroup, 2);
		f_consolePort.setValidRange(1024, 65535);
		f_consolePort.setPage(this);
		f_consolePort.setPreferenceStore(getPreferenceStore());
		f_consolePort.load();

		f_rawQSize = new IntegerFieldEditor(PreferenceConstants.P_RAWQ_SIZE,
				I18N.msg("flashlight.preference.page.rawQSize"), iGroup);
		f_rawQSize.fillIntoGrid(iGroup, 2);
		f_rawQSize.setValidRange(1000, 50000);
		f_rawQSize.setPage(this);
		f_rawQSize.setPreferenceStore(getPreferenceStore());
		f_rawQSize.load();

		f_refinerySize = new IntegerFieldEditor(
				PreferenceConstants.P_REFINERY_SIZE, I18N
						.msg("flashlight.preference.page.refinerySize"), iGroup);
		f_refinerySize.fillIntoGrid(iGroup, 2);
		f_refinerySize.setValidRange(1000, 50000);
		f_refinerySize.setPage(this);
		f_refinerySize.setPreferenceStore(getPreferenceStore());
		f_refinerySize.load();

		f_outQSize = new IntegerFieldEditor(PreferenceConstants.P_OUTQ_SIZE,
				I18N.msg("flashlight.preference.page.outQSize"), iGroup);
		f_outQSize.fillIntoGrid(iGroup, 2);
		f_outQSize.setValidRange(1000, 50000);
		f_outQSize.setPage(this);
		f_outQSize.setPreferenceStore(getPreferenceStore());
		f_outQSize.load();

		f_useSpyThread = new BooleanFieldEditor(PreferenceConstants.P_USE_SPY,
				I18N.msg("flashlight.preference.page.useSpyThread"), iGroup);
		f_useSpyThread.fillIntoGrid(iGroup, 2);
		f_useSpyThread.setPage(this);
		f_useSpyThread.setPreferenceStore(getPreferenceStore());
		f_useSpyThread.load();

		iGroup.setLayout(new GridLayout(2, false));

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

	private void updateDataDirectory() {
		f_dataDirectory.setText(FlashlightUtility.getFlashlightDataDirectory()
				.getAbsolutePath());
	}

	@Override
	protected void performDefaults() {
		f_autoIncreaseHeap.loadDefault();
		f_autoPerspectiveSwitch.loadDefault();
		f_consolePort.loadDefault();
		f_maxRowsPerQuery.loadDefault();
		f_outQSize.loadDefault();
		f_promptPerspectiveSwitch.loadDefault();
		f_rawQSize.loadDefault();
		f_refinerySize.loadDefault();
		f_useSpyThread.loadDefault();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		f_autoIncreaseHeap.store();
		f_autoPerspectiveSwitch.store();
		f_consolePort.store();
		f_maxRowsPerQuery.store();
		f_outQSize.store();
		f_promptPerspectiveSwitch.store();
		f_rawQSize.store();
		f_refinerySize.store();
		f_useSpyThread.store();
		return super.performOk();
	}
}