package com.surelogic.flashlight.client.eclipse.views;

import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.images.CommonImages;
import com.surelogic.flashlight.common.Utility;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * View to display all raw and prepared Flashlight runs to the user.
 */
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
		final Map<Integer, RunViewColumnWrapper> f_indexToColumn = f_mediator
				.getIndexToColumnMap();
		f_indexToColumn.put(next++, new RunViewColumnWrapper(table, "Run",
				SWT.LEFT, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final RunDescription description) {
				return description.getName();
			}
		});

		f_indexToColumn.put(next++, new RunViewColumnWrapper(table, "Time",
				SWT.LEFT, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final RunDescription description) {
				return Utility.toStringMS(description.getStartTimeOfRun());
			}
		});

		f_indexToColumn.put(next++, new RunViewColumnWrapper(table, "By",
				SWT.CENTER, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final RunDescription description) {
				return description.getUserName();
			}
		});

		f_indexToColumn.put(next++, new RunViewColumnWrapper(table, "Java",
				SWT.CENTER, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final RunDescription description) {
				return description.getJavaVersion();
			}
		});

		f_indexToColumn.put(next++, new RunViewColumnWrapper(table, "Vendor",
				SWT.CENTER, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final RunDescription description) {
				return description.getJavaVendor();
			}
		});

		f_indexToColumn.put(next++, new RunViewColumnWrapper(table, "OS",
				SWT.CENTER, TableUtility.SORT_COLUMN_ALPHABETICALLY) {
			@Override
			String getText(final RunDescription description) {
				return description.getOSName() + " (" + description.getOSVersion() + ") on "
						+ description.getOSArch();
			}
		});

		f_indexToColumn.put(next++, new RunViewColumnWrapper(table,
				"Max Memory (MB)", SWT.RIGHT,
				TableUtility.SORT_COLUMN_NUMERICALLY) {
			@Override
			String getText(final RunDescription description) {
				return description.getMaxMemoryMb() + "";
			}
		});

		f_indexToColumn.put(next++,
				new RunViewColumnWrapper(table, "Processors", SWT.CENTER,
						TableUtility.SORT_COLUMN_NUMERICALLY) {
					@Override
					String getText(final RunDescription description) {
						return description.getProcessors() + "";
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

		f_mediator.init();

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

	/**
	 * Must be invoked within the SWT event thread.
	 */
	void refresh() {
		f_mediator.refresh();
	}
}
