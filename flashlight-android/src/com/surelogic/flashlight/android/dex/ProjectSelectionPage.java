package com.surelogic.flashlight.android.dex;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;

public class ProjectSelectionPage extends WizardPage {

    private Table projectTable;
    private final ApkSelectionInfo info;

    protected ProjectSelectionPage(ApkSelectionInfo info) {
        super(I18N.msg("flashlight.dex.project.title"));
        this.info = info;
        setTitle(I18N.msg("flashlight.dex.project.title"));
        setDescription(I18N.msg("flashlight.dex.project.title"));
    }

    @Override
    public void createControl(Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FormLayout());
        setControl(container);

        projectTable = new Table(container, SWT.NONE);
        projectTable.setHeaderVisible(false);
        projectTable.setLinesVisible(true);
        final FormData formData = new FormData();
        formData.bottom = new FormAttachment(100, 0);
        formData.right = new FormAttachment(100, 0);
        formData.top = new FormAttachment(0, 0);
        formData.left = new FormAttachment(0, 0);
        projectTable.setLayoutData(formData);
        projectTable.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                validate();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                validate();
            }
        });
        final TableItem noItem = new TableItem(projectTable, SWT.NONE);
        noItem.setText("*None*");
        noItem.setData(null);
        if (info.getSelectedProject() == null) {
            projectTable.setSelection(noItem);
        }
        for (final IProject project : info.getProjects()) {
            final TableItem item = new TableItem(projectTable, SWT.NONE);
            item.setText(project.getProject().getName());
            item.setImage(SLImages.getImage(CommonImages.IMG_PROJECT));
            item.setData(project);
            if (project.equals(info.getSelectedProject())) {
                projectTable.setSelection(item);
            }
        }
        Dialog.applyDialogFont(container);
        validate();
    }

    void validate() {
        final TableItem[] items = projectTable.getSelection();
        setPageComplete(items.length == 1);
        for (final TableItem item : items) {
            IProject project = (IProject) item.getData();
            info.setSelectedProject(project);
        }
    }

}
