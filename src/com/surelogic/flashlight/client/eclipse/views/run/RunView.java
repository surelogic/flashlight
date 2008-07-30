package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.eclipse.ColumnViewerSorter;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.images.CommonImages;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;
import com.surelogic.flashlight.common.model.RunViewModel;

/**
 * View to display all raw and prepared Flashlight runs to the user.
 */
public final class RunView extends ViewPart {

	public static final String ID = RunView.class.getName();

	private RunViewMediator f_mediator;

	@Override
	public void createPartControl(Composite parent) {
		final TableViewer tableViewer = new TableViewer(parent, SWT.BORDER
				| SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new RunViewContentProvider());
		final RunViewModel model = new RunViewModel();

		for (int index = 0; index < model.getColumnCount(); index++) {
			final int columnIndex = index;
			final TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWTUtility.adaptJustification(model
							.getColumnJustification(columnIndex)));
			column.getColumn().setText(model.getColumnTitle(index));
			new ColumnViewerSorter<RunDescription>(tableViewer, column
					.getColumn()) {
				@Override
				protected int doCompare(Viewer viewer, RunDescription e1,
						RunDescription e2) {
					return model.getColumnComparator(columnIndex).compare(e1,
							e2);
				}
			};
		}
		tableViewer.setLabelProvider(new RunViewLabelProvider(model));
		// Set the header to visible
		tableViewer.getTable().setHeaderVisible(true);
		// Set the line of the table visible
		tableViewer.getTable().setLinesVisible(true);
		// Set the input so we see data
		tableViewer.setInput(RunManager.getInstance());
		// Pack the columns to the ideal with
		for (TableColumn col : tableViewer.getTable().getColumns()) {
			col.pack();
		}

		f_mediator = new RunViewMediator(tableViewer);

		final Action refreshAction = f_mediator.getRefreshAction();
		refreshAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_REFRESH));
		refreshAction.setToolTipText(I18N
				.msg("flashlight.run.view.tooltip.refresh"));
		getViewSite().getActionBars().getToolBarManager().add(refreshAction);

		final Action prepAction = f_mediator.getPrepAction();
		prepAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_FL_PREP_DATA));
		prepAction.setToolTipText(I18N.msg("flashlight.run.view.tooltip.prep"));
		prepAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(prepAction);

		final Action showLogAction = f_mediator.getShowLogAction();
		showLogAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_FL_READ_LOG));
		showLogAction.setToolTipText(I18N
				.msg("flashlight.run.view.tooltip.log"));
		showLogAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(showLogAction);

		final Action deleteRunAction = f_mediator.getDeleteRun();
		deleteRunAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_DELETE));
		deleteRunAction.setToolTipText(I18N
				.msg("flashlight.run.view.tooltip.delete"));
		deleteRunAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(deleteRunAction);

		f_mediator.init();
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
		if (f_mediator != null)
			f_mediator.setFocus();
	}

	/**
	 * Must be invoked within the SWT event thread.
	 */
	void refresh() {
		if (f_mediator != null)
			f_mediator.refresh();
	}
}
