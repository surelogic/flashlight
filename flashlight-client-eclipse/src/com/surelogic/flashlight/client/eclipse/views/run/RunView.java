package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;
import com.surelogic.flashlight.common.model.RunViewModel;

/**
 * View to display all raw and prepared Flashlight runs to the user.
 */
public final class RunView extends ViewPart {

    private RunViewMediator f_mediator;

    @Override
    public void createPartControl(final Composite parent) {
        final TableViewer tableViewer = new TableViewer(parent, SWT.BORDER
                | SWT.FULL_SELECTION | SWT.MULTI);
        tableViewer.setContentProvider(new RunViewContentProvider());
        final RunViewModel model = new RunViewModel();

        for (int index = 0; index < model.getColumnCount(); index++) {
            final int columnIndex = index;
            final TableViewerColumn column = new TableViewerColumn(tableViewer,
                    EclipseUIUtility.adaptJustification(model
                            .getColumnJustification(columnIndex)));
            column.getColumn().setText(model.getColumnTitle(index));
            new ColumnViewerSorter<RunDescription>(tableViewer,
                    column.getColumn()) {
                @Override
                protected int doCompare(final Viewer viewer,
                        final RunDescription e1, final RunDescription e2) {
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
        // Ensure that the run manager data is fresh
        EclipseJob.getInstance().scheduleDb(new RefreshRunManagerSLJob(false),
                false, true, RunManager.getInstance().getRunIdentities());
        // Set the input so we see data
        tableViewer.setInput(RunManager.getInstance());

        f_mediator = new RunViewMediator(tableViewer);

        final IActionBars actionBars = getViewSite().getActionBars();

        final Action refreshAction = f_mediator.getRefreshAction();
        refreshAction.setImageDescriptor(SLImages
                .getImageDescriptor(CommonImages.IMG_REFRESH));
        refreshAction.setText(I18N.msg("flashlight.run.view.text.refresh"));
        refreshAction.setToolTipText(I18N
                .msg("flashlight.run.view.tooltip.refresh"));
        actionBars.getToolBarManager().add(refreshAction);
        actionBars.getMenuManager().add(refreshAction);

        final Action prepAction = f_mediator.getPrepAction();
        prepAction.setImageDescriptor(SLImages
                .getImageDescriptor(CommonImages.IMG_FL_PREP_DATA));
        prepAction.setText(I18N.msg("flashlight.run.view.text.prep"));
        prepAction.setToolTipText(I18N.msg("flashlight.run.view.tooltip.prep"));
        prepAction.setEnabled(false);
        actionBars.getToolBarManager().add(prepAction);
        actionBars.getMenuManager().add(prepAction);

        final Action showLogAction = f_mediator.getShowLogAction();
        showLogAction.setImageDescriptor(SLImages
                .getImageDescriptor(CommonImages.IMG_FILE));
        showLogAction.setText(I18N.msg("flashlight.run.view.text.log"));
        showLogAction.setToolTipText(I18N
                .msg("flashlight.run.view.tooltip.log"));
        showLogAction.setEnabled(false);
        actionBars.getToolBarManager().add(showLogAction);
        actionBars.getMenuManager().add(showLogAction);

        final Action deleteRunAction = f_mediator.getDeleteAction();
        deleteRunAction.setImageDescriptor(SLImages
                .getImageDescriptor(CommonImages.IMG_RED_X));
        deleteRunAction.setText(I18N.msg("flashlight.run.view.text.delete"));
        deleteRunAction.setToolTipText(I18N
                .msg("flashlight.run.view.tooltip.delete"));
        deleteRunAction.setEnabled(false);
        actionBars.getToolBarManager().add(deleteRunAction);
        actionBars.getMenuManager().add(deleteRunAction);

        final Action inferJSureAnnoAction = f_mediator
                .getInferJSureAnnoAction();
        inferJSureAnnoAction.setText(I18N
                .msg("flashlight.run.view.text.inferJSureAnno"));
        inferJSureAnnoAction.setToolTipText(I18N
                .msg("flashlight.run.view.tooltip.inferJSureAnno"));
        inferJSureAnnoAction.setEnabled(false);
        actionBars.getMenuManager().add(inferJSureAnnoAction);

        /**
         * Add a context menu to the table viewer.
         */
        final MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(prepAction);
                manager.add(showLogAction);
                // manager.add(convertToXmlAction);
                manager.add(new Separator());
                manager.add(inferJSureAnnoAction);
                manager.add(new Separator());
                manager.add(deleteRunAction);
            }
        });
        final Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
        tableViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, tableViewer);

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
        if (f_mediator != null) {
            f_mediator.setFocus();
        }
    }

    /**
     * Must be invoked within the SWT event thread.
     */
    void refresh() {
        if (f_mediator != null) {
            f_mediator.refresh();
        }
    }
}
