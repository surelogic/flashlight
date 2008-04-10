package com.surelogic.flashlight.views;

import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.images.CommonImages;
import com.surelogic.flashlight.common.Utility;
import com.surelogic.flashlight.common.entities.IRunDescription;
import com.surelogic.flashlight.views.RunMediator.ColumnWrapper;

public final class RunView extends ViewPart {

	public static final String ID = "com.surelogic.flashlight.RunView";

	private RunMediator f_mediator;

	@Override
	public void createPartControl(Composite parent) {

		final Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		f_mediator = new RunMediator(table);

		final TableColumn raw = new TableColumn(table, SWT.LEFT);
		raw.setText("Raw");
		raw.setMoveable(true);

		final TableColumn prep = new TableColumn(table, SWT.RIGHT);
		prep.setText("Prep");
		prep.setMoveable(true);

		int next = 2;
		final Map<Integer, ColumnWrapper> f_indexToColumn = f_mediator
				.getIndexToColumnMap();
		f_indexToColumn.put(next++, new ColumnWrapper(table, "Run", SWT.LEFT,
				TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getName();
			}
		});

		f_indexToColumn.put(next++, new ColumnWrapper(table, "Time", SWT.LEFT,
				TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return Utility.toStringMS(run.getStartTimeOfRun());
			}
		});

		f_indexToColumn.put(next++, new ColumnWrapper(table, "By", SWT.CENTER,
				TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getUserName();
			}
		});

		f_indexToColumn.put(next++, new ColumnWrapper(table, "Java",
				SWT.CENTER, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getJavaVersion();
			}
		});

		f_indexToColumn.put(next++, new ColumnWrapper(table, "Vendor",
				SWT.CENTER, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getJavaVendor();
			}
		});

		f_indexToColumn.put(next++, new ColumnWrapper(table, "OS", SWT.CENTER,
				TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getOSName() + " (" + run.getOSVersion() + ") on "
						+ run.getOSArch();
			}
		});
		f_indexToColumn.put(next++, new ColumnWrapper(table, "Max Memory (MB)",
				SWT.RIGHT, TableUtility.SORT_COLUMN_NUMERICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getMaxMemoryMB() + "";
			}
		});

		f_indexToColumn.put(next++, new ColumnWrapper(table, "Processors",
				SWT.CENTER, TableUtility.SORT_COLUMN_NUMERICALLY) {
			@Override
			String getText(final IRunDescription run) {
				return run.getProcessors() + "";
			}
		});

		final Action refreshAction = f_mediator.getRefreshAction();
		refreshAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_REFRESH));
		refreshAction.setToolTipText("Refresh the contents of this view");
		getViewSite().getActionBars().getToolBarManager().add(refreshAction);

		final Action prepAction = f_mediator.getPrepAction();
		prepAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_FL_PREP_DATA));
		prepAction.setToolTipText("Prepare the raw data from this run");
		prepAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(prepAction);

		final Action showLogAction = f_mediator.getShowLogAction();
		showLogAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_FL_READ_LOG));
		showLogAction
				.setToolTipText("Show the instrumentation log from this run");
		showLogAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(showLogAction);

		final Action deleteRunAction = f_mediator.getDeleteRun();
		deleteRunAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_DELETE));
		deleteRunAction.setToolTipText("Delete this run");
		deleteRunAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(deleteRunAction);

		refresh();
	}

	@Override
	public void dispose() {
		/*
		 * We need to check for null because Eclipse gives us no guarantee that
		 * createPartControl() was called.
		 */
		if (f_mediator != null) {
			f_mediator.dispose();
			f_mediator = null;
		}
		super.dispose();
	}

	@Override
	public void setFocus() {
		/*
		 * This is a good point to refresh the contents of the view.
		 */
		if (f_mediator != null) {
			f_mediator.setFocus();
		}
	}

	public static void refreshViewContents() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				final IViewPart view = ViewUtility.showView(RunView.ID);
				if (view instanceof RunView) {
					((RunView) view).refresh();

				}
			}
		});
	}

	void refresh() {
		f_mediator.refresh();
	}
}
