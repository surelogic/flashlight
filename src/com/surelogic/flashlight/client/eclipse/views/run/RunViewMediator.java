package com.surelogic.flashlight.client.eclipse.views.run;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.client.eclipse.Data;
import com.surelogic.flashlight.client.eclipse.dialogs.DeleteRunDialog;
import com.surelogic.flashlight.client.eclipse.dialogs.LogDialog;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.jobs.ConvertBinaryToXMLJob;
import com.surelogic.flashlight.common.jobs.DeleteRawFilesSLJob;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.jobs.UnPrepSLJob;
import com.surelogic.flashlight.common.model.IRunManagerObserver;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * Mediator for the {@link RunView}.
 */
public final class RunViewMediator implements IRunManagerObserver, ILifecycle {

	private static final String RUN_VARIABLE = "RUN";

	private final TableViewer f_tableViewer;
	private final Table f_table;

	RunViewMediator(final TableViewer tableViewer) {
		f_tableViewer = tableViewer;
		f_table = tableViewer.getTable();
	}

	public void init() {
		f_table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				setToolbarState();
			}
		});
		f_table.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				final RunDescription description = getTableSelectionData();
				if (description != null) {
					/*
					 * Is it already prepared?
					 */
					PrepRunDescription prep = description
							.getPrepRunDescription();
					final boolean hasPrep = prep != null;
					if (hasPrep) {
						/*
						 * Change the focus to the query menu view.
						 */
						ViewUtility.showView(QueryMenuView.class.getName());
					} else {
						/*
						 * Prepare this run.
						 */
						f_prep.run();
					}
				}
			}
		});

		RunManager.getInstance().addObserver(this);
		packColumns();
		setToolbarState();
	}

	private RunDescription getData(final TableItem item) {
		if (item != null) {
			final Object o = item.getData();
			if (o instanceof RunDescription) {
				return (RunDescription) o;
			}
		}
		return null;
	}

	private TableItem getTableSelection() {
		TableItem[] items = f_table.getSelection();
		if (items.length == 1) {
			return items[0];
		}
		return null;
	}

	private RunDescription getTableSelectionData() {
		return getData(getTableSelection());
	}

	private final Action f_refresh = new Action() {
		@Override
		public void run() {
			EclipseJob.getInstance().scheduleDb(
					new RefreshRunManagerSLJob(Data.getInstance()));
		}
	};

	public Action getRefreshAction() {
		return f_refresh;
	}

	void refresh() {
		f_tableViewer.refresh();
		packColumns();
		setToolbarState();
	}

	private final Action f_prep = new Action() {
		@Override
		public void run() {
			final RunDescription description = getTableSelectionData();
			if (description != null) {
				/*
				 * Is it already prepared?
				 */
				PrepRunDescription prep = description.getPrepRunDescription();
				final boolean hasPrep = prep != null;
				if (hasPrep) {
					if (!MessageDialog.openConfirm(f_table.getShell(), I18N
							.msg("flashlight.dialog.reprep.title"), I18N.msg(
							"flashlight.dialog.reprep.msg", description
									.getName()))) {
						return; // bail
					}
					EclipseJob.getInstance().scheduleDb(
							new UnPrepSLJob(prep, Data.getInstance()), true,
							false);
				}
				final File dataFile = description.getRawFileHandles()
						.getDataFile();
				EclipseJob.getInstance().scheduleDb(
						new PrepSLJob(dataFile, Data.getInstance()), true,
						false);
			}
		}
	};

	public Action getPrepAction() {
		return f_prep;
	}

	private final Action f_showLog = new Action() {
		@Override
		public void run() {
			final RunDescription description = getTableSelectionData();
			if (description != null) {
				final RawFileHandles handles = description.getRawFileHandles();
				if (handles != null) {
					final File logFile = handles.getLogFile();
					if (logFile != null) {
						LogDialog d = new LogDialog(f_table.getShell(), handles
								.getLogFile(), description.getName());
						d.open();
					}
				}
			}
		}
	};

	public Action getShowLogAction() {
		return f_showLog;
	}

	private final Action f_convertToXML = new Action() {
		@Override
		public void run() {
			final RunDescription description = getTableSelectionData();
			if (description != null) {
				final RawFileHandles handles = description.getRawFileHandles();
				if (handles != null) {
					final File dataFile = handles.getDataFile();
					EclipseJob.getInstance().schedule(
							new ConvertBinaryToXMLJob(dataFile));
				}
			}
		}
	};

	public Action getConvertToXMLAction() {
		return f_convertToXML;
	}

	private final Action f_deleteRun = new Action() {
		@Override
		public void run() {
			final RunDescription description = getTableSelectionData();
			if (description != null) {
				final RawFileHandles handles = description.getRawFileHandles();
				PrepRunDescription prep = description.getPrepRunDescription();

				boolean hasRawFiles = handles != null;
				boolean hasPrep = prep != null;

				DeleteRunDialog d = new DeleteRunDialog(f_table.getShell(),
						description.getName(), hasRawFiles, hasPrep);
				d.open();
				if (Window.CANCEL == d.getReturnCode())
					return;

				if (hasPrep) {
					EclipseJob.getInstance().scheduleDb(
							new UnPrepSLJob(prep, Data.getInstance()), true,
							false);
				}
				if (hasRawFiles && d.deleteRawDataFiles()) {
					EclipseJob.getInstance().schedule(
							new DeleteRawFilesSLJob(description, Data
									.getInstance()));
				}
			}
		}
	};

	public Action getDeleteRun() {
		return f_deleteRun;
	}

	/**
	 * Pack the columns of our table to the ideal width.
	 */
	private final void packColumns() {
		for (TableColumn col : f_tableViewer.getTable().getColumns()) {
			col.pack();
		}
	}

	private final void setToolbarState() {
		final RunDescription o = getTableSelectionData();
		final boolean somethingIsSelected = o != null;
		f_deleteRun.setEnabled(somethingIsSelected);
		boolean rawActionsEnabled = somethingIsSelected;
		boolean binaryActionsEnabled = false;
		String runVariableValue = null;
		if (somethingIsSelected) {
			rawActionsEnabled = o.getRawFileHandles() != null;
			final PrepRunDescription prep = o.getPrepRunDescription();
			if (prep != null) {
				runVariableValue = Integer.toString(prep.getRun());
			}
			binaryActionsEnabled = rawActionsEnabled
					&& o.getRawFileHandles().isDataFileBinary();
		}
		AdHocDataSource.getManager().setGlobalVariableValue(RUN_VARIABLE,
				runVariableValue);
		f_prep.setEnabled(rawActionsEnabled);
		f_showLog.setEnabled(rawActionsEnabled);
		f_convertToXML.setEnabled(binaryActionsEnabled);
	}

	/**
	 * Probably we have not been called from the SWT event dispatch thread.
	 */
	public void notify(RunManager manager) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}

	public void dispose() {
		RunManager.getInstance().removeObserver(this);
	}

	public void setFocus() {
		f_table.setFocus();
	}
}
