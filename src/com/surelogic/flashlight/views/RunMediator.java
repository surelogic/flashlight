package com.surelogic.flashlight.views;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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

import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.images.CommonImages;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.common.entities.Run;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.Raw;
import com.surelogic.flashlight.common.model.IRunDescription;
import com.surelogic.flashlight.jobs.DeleteRawFilesJob;
import com.surelogic.flashlight.jobs.PrepJob;
import com.surelogic.flashlight.jobs.UnPrepJob;

public final class RunMediator {

	private boolean firstRefresh = true;

	private final Table f_table;

	RunMediator(final Table table) {
		f_table = table;
		f_table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				setToolbarState();
			}
		});
	}

	private TableItem getTableSelection() {
		TableItem[] items = f_table.getSelection();
		if (items.length == 1) {
			return items[0];
		}
		return null;
	}

	private Object getTableSelectionData() {
		TableItem selection = getTableSelection();
		if (selection != null) {
			return selection.getData();
		}
		return null;
	}

	private final Action f_refresh = new Action() {
		@Override
		public void run() {
			refresh();
		}
	};

	public Action getRefreshAction() {
		return f_refresh;
	}

	void refresh() {
		/*
		 * Make a best effort to remember the row the user had selected before
		 * as we refresh.
		 */
		final int oldSelection = f_table.getSelectionIndex();

		Raw[] raws = Raw.findRawFiles(FileUtility.getFlashlightDataDirectory());

		// remove the contents
		TableItem[] items = f_table.getItems();
		for (TableItem ti : items) {
			ti.dispose();
		}

		// add in the raw data
		for (Raw raw : raws) {
			TableItem ti = new TableItem(f_table, SWT.NONE);
			ti.setImage(0, SLImages
					.getWorkbenchImage(ISharedImages.IMG_OBJ_FILE));

			final long sizeKb = raw.getDataFile().length() / 1024;
			String textSize;
			if (sizeKb < 1024) {
				textSize = sizeKb + " KB";
			} else {
				textSize = (sizeKb / 1024) + " MB";
			}
			ti.setText(0, textSize);
			for (int i = 2; i < f_table.getColumnCount(); i++) {
				ti.setText(i, f_indexToColumn.get(i).getText(raw));
			}
			ti.setData(raw);
			// add a warning image if the log indicates problems
			if (!raw.isLogClean()) {
				ti.setImage(2, PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
				ti.setText(2, ti.getText(2) + " (warnings in log)");
			}
		}

		// see what is in the database
		items = f_table.getItems();
		try {
			final Connection c = Data.getInstance().getConnection();
			try {
				Run[] runs = RunDAO.getAll(c);
				for (Run run : runs) {
					boolean found = false;
					for (TableItem ti : items) {
						if (run.isSameRun((Raw) ti.getData())) {
							found = true;
							ti.setImage(1, SLImages
									.getImage(CommonImages.IMG_FL_PREP));
						}

					}
					if (!found) {
						TableItem nti = new TableItem(f_table, SWT.NONE);
						nti.setImage(1, SLImages
								.getImage(CommonImages.IMG_FL_PREP));
						for (int i = 2; i < f_table.getColumnCount(); i++) {
							nti.setText(i, f_indexToColumn.get(i).getText(run));
						}
						nti.setData(run);
					}
				}
			} finally {
				c.close();
			}
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Lookup of all runs failed",
					e);
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

		if (oldSelection != -1 && oldSelection < f_table.getItemCount()) {
			f_table.setSelection(oldSelection);
		}
		setToolbarState();
	}

	private final Action f_prep = new Action() {
		@Override
		public void run() {
			final Object o = getTableSelectionData();
			if (o instanceof Raw) {
				final Raw raw = (Raw) o;
				/*
				 * Is it already prepared?
				 */
				int unPrepRunId = getRunId(raw);

				if (unPrepRunId != -1) {
					if (!MessageDialog.openConfirm(f_table.getShell(),
							"Confirm Flashlight Re-Prep",
							"Do you wish to delete and re-prepare the data for "
									+ raw.getName() + "?")) {
						return; // bail
					}
					Job job = new UnPrepJob(unPrepRunId, raw.getName());
					job.setUser(true);
					job.schedule();
				}
				Job job = new PrepJob(raw);
				job.setUser(true);
				job.schedule();
			}
		}
	};

	/**
	 * Looks up the id for a run in the database.
	 * 
	 * @param raw
	 *            the raw run representation.
	 * @return the id of the prepared data, or -1 if the data is not found in
	 *         the database.
	 */
	private int getRunId(Raw raw) {
		int unPrepId = -1;
		try {
			final Connection em = Data.getInstance().getConnection();
			try {
				Run r = RunDAO.find(em, raw.getName(), raw.getStartTimeOfRun());
				if (r != null) {
					unPrepId = r.getRun();
				}
			} finally {
				em.close();
			}
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Lookup of a run failed", e);
		}
		return unPrepId;
	}

	public Action getPrepAction() {
		return f_prep;
	}

	private final Action f_showLog = new Action() {
		@Override
		public void run() {
			final Object o = getTableSelectionData();
			if (o instanceof Raw) {
				final Raw raw = (Raw) o;
				LogDialog d = new LogDialog(f_table.getShell(), raw
						.getLogFile(), "Instrumentation Log for "
						+ raw.getName());
				d.open();
			}
		}
	};

	public Action getShowLogAction() {
		return f_showLog;
	}

	private final Action f_deleteRun = new Action() {
		@Override
		public void run() {
			int unPrepRunId = -1;
			Raw raw = null;
			String runName = "Unknown";
			final Object o = getTableSelectionData();
			if (o instanceof IRunDescription) {
				runName = ((IRunDescription) o).getName();
			} else {
				SLLogger.getLogger().log(Level.SEVERE,
						"Selected run is not an IRunDescription",
						new Exception("at this location"));
				return;
			}
			if (o instanceof Raw) {
				raw = (Raw) o;
				/*
				 * Is it already prepared?
				 */
				unPrepRunId = getRunId(raw);
			} else if (o instanceof Run) {
				unPrepRunId = ((Run) o).getRun();
			}
			final boolean onlyPrep = raw == null;
			final boolean onlyRaw = unPrepRunId == -1;
			if (onlyPrep && onlyRaw) {
				SLLogger.getLogger().log(
						Level.SEVERE,
						"Selected run " + runName
								+ " lacks both raw and prepared data",
						new Exception("at this location"));
				return;
			}
			DeleteRunDialog d = new DeleteRunDialog(f_table.getShell(),
					runName, onlyRaw, onlyPrep);
			d.open();
			if (Window.CANCEL == d.getReturnCode())
				return;

			if (unPrepRunId != -1) {
				Job job = new UnPrepJob(unPrepRunId, runName);
				job.setUser(true);
				job.schedule();
			}
			if (raw != null && d.deleteRawDataFiles()) {
				Job job = new DeleteRawFilesJob(raw);
				job.setUser(true);
				job.schedule();
			}
		}
	};

	public Action getDeleteRun() {
		return f_deleteRun;
	}

	private final void setToolbarState() {
		boolean rawActionsEnabled = false;
		final Object o = getTableSelectionData();
		if (o instanceof Raw) {
			rawActionsEnabled = true;
		}
		f_prep.setEnabled(rawActionsEnabled);
		f_showLog.setEnabled(rawActionsEnabled);
		f_deleteRun.setEnabled(o != null);
	}

	static abstract class ColumnWrapper {

		private final TableColumn f_column;

		TableColumn getColumn() {
			return f_column;
		}

		ColumnWrapper(Table table, String columnTitle, int style,
				Listener columnSortListener) {
			f_column = new TableColumn(table, style);
			f_column.setText(columnTitle);
			f_column.addListener(SWT.Selection, columnSortListener);
			f_column.setMoveable(true);
		}

		abstract String getText(final IRunDescription run);
	}

	private final Map<Integer, ColumnWrapper> f_indexToColumn = new HashMap<Integer, ColumnWrapper>();

	public Map<Integer, ColumnWrapper> getIndexToColumnMap() {
		return f_indexToColumn;
	}

	public void dispose() {
		// nothing
	}

	public void setFocus() {
		f_table.setFocus();
		refresh();
	}
}
