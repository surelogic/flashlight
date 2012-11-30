package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.surelogic.common.SLUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.files.RunDirectory;

/**
 * Choose one or more runs from those currently available in Flashlight.
 */
public class RunSelectionPage extends UserInputWizardPage {

    private Table f_runTable;
    private TableColumn[] f_cols;

    private final RegionRefactoringInfo f_info;
    private String selected;

    public RunSelectionPage(final RegionRefactoringInfo info) {
        super(I18N.msg("flashlight.recommend.dialog.run.title"));
        setTitle(I18N.msg("flashlight.recommend.dialog.run.title"));
        setDescription(I18N.msg("flashlight.recommend.dialog.run.info"));
        f_info = info;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FormLayout());
        setControl(container);
        f_runTable = new Table(container, SWT.FULL_SELECTION | SWT.CHECK);
        final FormData formData = new FormData();
        formData.bottom = new FormAttachment(100, 0);
        formData.right = new FormAttachment(100, 0);
        formData.top = new FormAttachment(0, 0);
        formData.left = new FormAttachment(0, 0);
        f_runTable.setLayoutData(formData);
        f_runTable.setHeaderVisible(true);
        f_runTable.setLinesVisible(true);
        f_runTable.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                validate();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                validate();
            }
        });
        f_cols = new TableColumn[2];
        f_cols[0] = new TableColumn(f_runTable, SWT.NONE);
        f_cols[0].setText(I18N.msg("flashlight.recommend.dialog.runCol"));
        f_cols[1] = new TableColumn(f_runTable, SWT.NONE);
        f_cols[1].setText(I18N.msg("flashlight.recommend.dialog.timeCol"));
        for (TableColumn c : f_cols) {
            c.pack();
        }
        Dialog.applyDialogFont(container);
        validate();
    }

    void validate() {
        final List<RunDirectory> selected = new ArrayList<RunDirectory>();
        for (final TableItem item : f_runTable.getItems()) {
            if (item.getChecked()) {
                selected.add((RunDirectory) item.getData());
            }
        }
        f_info.setSelectedRuns(selected);
        setPageComplete(!selected.isEmpty());
    }

    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            final String project = f_info.getSelectedProject().getElementName();
            if (!project.equals(selected)) {
                f_runTable.removeAll();
                selected = project;
                final List<RunDirectory> runList = new ArrayList<RunDirectory>(
                        f_info.getRuns());
                Collections.sort(runList, new Comparator<RunDirectory>() {
                    @Override
                    public int compare(final RunDirectory r1,
                            final RunDirectory r2) {
                        int cmp = r1.getRunDescription().getName().compareTo(r2.getRunDescription().getName());
                        if (cmp == 0) {
                            cmp = r1.getRunDescription().getStartTimeOfRun().compareTo(
                                    r2.getRunDescription().getStartTimeOfRun());
                        }
                        return cmp;
                    }
                });
                for (final RunDirectory run : runList) {
                    boolean noneSelected = true;
                    final String runName = run.getRunDescription().getName();
                    final int idx = runName.lastIndexOf('.');
                    if (idx > 0) {
                        final String runPackage = runName.substring(0, idx);
                        final String runClass = runName.substring(idx + 1);
                        if (JDTUtility.findIType(project, runPackage, runClass) != null) {
                            final TableItem item = new TableItem(f_runTable,
                                    SWT.NONE);
                            item.setText(0, run.getRunDescription().getName());
                            item.setText(1, SLUtility.toStringHMS(run.getRunDescription()
                                    .getStartTimeOfRun()));
                            item.setText(run.getRunDescription().getName());
                            item.setData(run);
                            if (noneSelected) {
                                noneSelected = false;
                                f_runTable.setSelection(item);
                            }
                        }
                    }
                }
                for (TableColumn c : f_cols) {
                    c.pack();
                }
                validate();
            }
        }
        super.setVisible(visible);
    }

}
