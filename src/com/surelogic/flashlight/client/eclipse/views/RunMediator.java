package com.surelogic.flashlight.client.eclipse.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.images.CommonImages;
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
public final class RunMediator implements IRunManagerObserver, ILifecycle {

	private boolean firstRefresh = true;

	private final Table f_table;

	RunMediator(final Table table) {
		f_table = table;
	}

	public void init() {
		f_table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				setToolbarState();
			}
		});
		RunManager.getInstance().addObserver(this);
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
		final RunManager rm = RunManager.getInstance();
		/*
		 * Make a best effort to remember the row the user had selected before
		 * as we refresh.
		 */
		final RunDescription oldSelection = getTableSelectionData();

		// remove the contents
		TableItem[] items = f_table.getItems();
		for (TableItem ti : items) {
			ti.dispose();
		}

		Set<RunDescription> descriptions = rm.getRunDescriptions();

		for (RunDescription description : descriptions) {
			TableItem ti = new TableItem(f_table, SWT.NONE);
			for (int i = 2; i < f_table.getColumnCount(); i++) {
				ti.setText(i, f_indexToColumn.get(i).getText(description));
			}
			ti.setData(description);
			final RawFileHandles handles = rm.getRawFileHandlesFor(description);
			if (handles != null) {
				ti.setImage(0, SLImages
						.getWorkbenchImage(ISharedImages.IMG_OBJ_FILE));

				final long sizeKb = handles.getDataFile().length() / 1024;
				String textSize;
				if (sizeKb < 1024) {
					textSize = sizeKb + " KB";
				} else {
					textSize = (sizeKb / 1024) + " MB";
				}
				ti.setText(0, textSize);
				// add a warning image if the log indicates problems
				if (!handles.isLogClean()) {
					ti.setImage(2, PlatformUI.getWorkbench().getSharedImages()
							.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
					ti.setText(2, ti.getText(2) + " (warnings in log)");
				}
			}
			final PrepRunDescription prep = rm
					.getPrepRunDescriptionFor(description);
			if (prep != null) {
				ti.setImage(1, SLImages.getImage(CommonImages.IMG_FL_PREP));
			}
		}

		// minimize the column widths
		if (firstRefresh) {
			firstRefresh = false;
			for (TableColumn col : f_table.getColumns()) {
				col.pack();
			}
		}

		// no sort column after a refresh
		f_table.setSortColumn(null);

		// restore the old row selection
		if (oldSelection != null) {
			items = f_table.getItems();
			for (TableItem ti : items) {
				if (oldSelection.equals(getData(ti))) {
					f_table.setSelection(ti);
				}
			}
		}

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
				PrepRunDescription prep = RunManager.getInstance()
						.getPrepRunDescriptionFor(description);
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
				final RawFileHandles handles = RunManager.getInstance()
						.getRawFileHandlesFor(description);
				if (handles != null) {
					LogDialog d = new LogDialog(f_table.getShell(), handles
							.getLogFile(), "Instrumentation Log for "
							+ description.getName());
					d.open();
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
				final RawFileHandles handles = RunManager.getInstance()
						.getRawFileHandlesFor(description);
				PrepRunDescription prep = RunManager.getInstance()
						.getPrepRunDescriptionFor(description);

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
		boolean rawActionsEnabled = false;
		final RunDescription o = getTableSelectionData();
		if (o != null) { // TODO Fix logic
			rawActionsEnabled = true;
		}
		f_prep.setEnabled(rawActionsEnabled);
		f_showLog.setEnabled(rawActionsEnabled);
		f_deleteRun.setEnabled(o != null);
	}

	private final Map<Integer, RunViewColumnWrapper> f_indexToColumn = new HashMap<Integer, RunViewColumnWrapper>();

	public Map<Integer, RunViewColumnWrapper> getIndexToColumnMap() {
		return f_indexToColumn;
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
		refresh();
	}
}
