package com.surelogic.flashlight.client.eclipse.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.window.Window;
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
import org.eclipse.ui.PlatformUI;

import com.surelogic.adhoc.views.ExportQueryDialog;
import com.surelogic.common.CommonImages;
import com.surelogic.common.FileUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.dialogs.ChangeDataDirectoryDialog;
import com.surelogic.common.eclipse.dialogs.ErrorDialogUtility;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.eclipse.preferences.AbstractCommonPreferencePage;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLSeverity;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.jobs.DisconnectAllDatabases;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.model.RunManager;

public class FlashlightPreferencePage extends AbstractCommonPreferencePage {
	private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	private BooleanFieldEditor f_autoIncreaseHeap;
	private IntegerFieldEditor f_maxRowsPerQuery;
	private BooleanFieldEditor f_promptAboutLotsOfSavedQueries;
	private BooleanFieldEditor f_promptToPrepAllRawData;
	private BooleanFieldEditor f_autoPrepAllRawData;
	private Label f_dataDirectory;
	private IntegerFieldEditor f_objectWindowSize;

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
				final File existing = PreferenceConstants
						.getFlashlightDataDirectory();
				final ChangeDataDirectoryDialog dialog = new ChangeDataDirectoryDialog(
						change.getShell(),
						existing,
						I18N.msg("flashlight.change.data.directory.dialog.title"),
						SLImages.getImage(CommonImages.IMG_FL_LOGO),
						I18N.msg("flashlight.change.data.directory.dialog.information"));

				if (dialog.open() != Window.OK) {
					return;
				}

				if (!dialog.isValidChangeToDataDirectory()) {
					return;
				}

				final File destination = dialog.getNewDataDirectory();
				final boolean moveOldToNew = dialog.moveOldToNew();
				if (EclipseJob.getInstance().isActiveOfType(PrepSLJob.class)) {
					// We can't do a move while we are prepping a job
					PlatformUI.getWorkbench().getDisplay()
							.asyncExec(new Runnable() {
								public void run() {
									ErrorDialogUtility
											.open(null,
													"Cannot move Flashlight data directory",
													SLEclipseStatusUtility
															.createWarningStatus(
																	173,
																	I18N.err(173)));
								}
							});
					// FIXME we can't continue
				}
				SLJob moveJob = FileUtility.moveDataDirectory(existing,
						destination, moveOldToNew,
						new DisconnectAllDatabases(), null);
				SLStatus result = moveJob.run(new NullSLProgressMonitor());
				if (result.getSeverity() == SLSeverity.OK) {
					PreferenceConstants.setFlashlightDataDirectory(destination);
					updateDataDirectory();
					RunManager.getInstance().setDataDirectory(destination);
					AdHocDataSource.getManager().deleteAllResults();
				} else {
					IStatus status = SLEclipseStatusUtility.convert(result,
							Activator.getDefault());
					ErrorDialogUtility.open(
							change.getShell(),
							I18N.msg("flashlight.change.data.directory.dialog.failed"),
							status);
				}
			}
		});

		final Group onGroup = new Group(panel, SWT.NONE);
		onGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		onGroup.setText(I18N.msg("flashlight.preference.page.group.onLaunch"));

		f_autoIncreaseHeap = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_INCREASE_HEAP_AT_LAUNCH,
				I18N.msg("flashlight.preference.page.autoIncreaseHeap"),
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

		final Group pGroup = new Group(panel, SWT.NONE);
		pGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		pGroup.setText(I18N.msg("flashlight.preference.page.group.prep"));

		f_objectWindowSize = new IntegerFieldEditor(
				PreferenceConstants.P_PREP_OBJECT_WINDOW_SIZE,
				I18N.msg("flashlight.preference.page.objectWindowSize"), pGroup);
		f_objectWindowSize.setValidRange(10000, 1000000);
		f_objectWindowSize.fillIntoGrid(pGroup, 2);
		finishSetup(f_objectWindowSize);

		f_promptToPrepAllRawData = new BooleanFieldEditor(
				PreferenceConstants.P_PROMPT_TO_PREP_ALL_RAW_DATA,
				I18N.msg("flashlight.preference.page.promptToPrepAllRawData"),
				pGroup);
		f_promptToPrepAllRawData.fillIntoGrid(pGroup, 2);
		finishSetup(f_promptToPrepAllRawData);

		f_autoPrepAllRawData = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_PREP_ALL_RAW_DATA,
				I18N.msg("flashlight.preference.page.autoPrepAllRawData"),
				pGroup);
		f_autoPrepAllRawData.fillIntoGrid(pGroup, 2);
		finishSetup(f_autoPrepAllRawData);

		pGroup.setLayout(new GridLayout(2, false));

		final Group qGroup = new Group(panel, SWT.NONE);
		qGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		qGroup.setText(I18N.msg("flashlight.preference.page.group.query"));

		f_maxRowsPerQuery = new IntegerFieldEditor(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY,
				I18N.msg("flashlight.preference.page.maxRowsPerQuery"), qGroup);
		f_maxRowsPerQuery.setValidRange(1024, 65535);
		f_maxRowsPerQuery.fillIntoGrid(qGroup, 2);
		finishSetup(f_maxRowsPerQuery);

		f_promptAboutLotsOfSavedQueries = new BooleanFieldEditor(
				PreferenceConstants.P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES,
				I18N.msg("flashlight.preference.page.promptAboutLotsOfSavedQueries"),
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
		f_dataDirectory.setText(PreferenceConstants
				.getFlashlightDataDirectory().getAbsolutePath());
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