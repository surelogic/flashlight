package com.surelogic.flashlight.client.eclipse.views.run;

import java.io.File;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.ILifecycle;
import com.surelogic.flashlight.client.eclipse.dialogs.DeleteRunDialog;
import com.surelogic.flashlight.client.eclipse.dialogs.LogDialog;
import com.surelogic.flashlight.client.eclipse.jobs.DeleteRawFilesJob;
import com.surelogic.flashlight.client.eclipse.jobs.PrepJob;
import com.surelogic.flashlight.client.eclipse.jobs.RefreshRunManagerJob;
import com.surelogic.flashlight.client.eclipse.jobs.UnPrepJob;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.model.IRunManagerObserver;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * Mediator for the {@link RunView}.
 */
public final class RunViewMediator implements IRunManagerObserver, ILifecycle {

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
		RunManager.getInstance().addObserver(this);
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
			final Job job = new RefreshRunManagerJob();
			job.schedule();
		}
	};

	public Action getRefreshAction() {
		return f_refresh;
	}

	void refresh() {
		f_tableViewer.refresh();
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
				if (prep != null) {
					if (!MessageDialog.openConfirm(f_table.getShell(),
							"Confirm Flashlight Re-Prep",
							"Do you wish to delete and re-prepare the data for "
									+ description.getName() + "?")) {
						return; // bail
					}
					Job job = new UnPrepJob(prep);
					job.setUser(true);
					job.schedule();
				}
				Job job = new PrepJob(description);
				job.setUser(true);
				job.schedule();
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
					Job job = new UnPrepJob(prep);
					job.setUser(true);
					job.schedule();
				}
				if (hasRawFiles && d.deleteRawDataFiles()) {
					Job job = new DeleteRawFilesJob(description);
					job.setUser(true);
					job.schedule();
				}
			}
		}
	};

	public Action getDeleteRun() {
		return f_deleteRun;
	}

	private final void setToolbarState() {
		final RunDescription o = getTableSelectionData();
		final boolean somethingIsSelected = o != null;
		f_deleteRun.setEnabled(somethingIsSelected);
		boolean rawActionsEnabled = somethingIsSelected;
		if (somethingIsSelected) {
			rawActionsEnabled = o.getRawFileHandles() != null;
		}
		f_prep.setEnabled(rawActionsEnabled);
		f_showLog.setEnabled(rawActionsEnabled);
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
