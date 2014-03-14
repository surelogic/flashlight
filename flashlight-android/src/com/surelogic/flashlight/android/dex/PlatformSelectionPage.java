package com.surelogic.flashlight.android.dex;

import java.util.List;

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
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.IAndroidTarget;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.TableUtility;

public class PlatformSelectionPage extends WizardPage {

    private final ApkSelectionInfo info;

    private Table platformTable;
    boolean noSelectionMade = true;

    PlatformSelectionPage(ApkSelectionInfo info) {
        super(I18N.msg("flashlight.dex.platform.title"));
        this.info = info;
        setTitle(I18N.msg("flashlight.dex.platform.title"));
        setDescription(I18N.msg("flashlight.dex.platform.title"));
    }

    @Override
    public void createControl(Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FormLayout());
        setControl(container);

        platformTable = new Table(container, SWT.NONE);
        platformTable.setHeaderVisible(false);
        platformTable.setLinesVisible(true);
        final FormData formData = new FormData();
        formData.bottom = new FormAttachment(100, 0);
        formData.right = new FormAttachment(100, 0);
        formData.top = new FormAttachment(0, 0);
        formData.left = new FormAttachment(0, 0);
        TableColumn col = new TableColumn(platformTable, SWT.NONE);
        col.setText("Version");
        col = new TableColumn(platformTable, SWT.NONE);
        col.setText("Name");
        col = new TableColumn(platformTable, SWT.NONE);
        col.setText("Desc");
        platformTable.setLayoutData(formData);
        platformTable.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                validate();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                validate();

            }

        });
        List<IAndroidTarget> targets = info.getTargets();
        for (IAndroidTarget target : info.getTargets()) {
            TableItem item = new TableItem(platformTable, NONE);
            item.setText(new String[] { target.getVersionName(),
                    target.getName(), target.getDescription() });
            item.setData(target);
        }
        if (info.getSelectedProject() != null) {
            IAndroidTarget target = Sdk.getCurrent().getTarget(
                    info.getSelectedProject());
            for (TableItem item : platformTable.getItems()) {
                if (((IAndroidTarget) item.getData()).equals(target)) {
                    platformTable.setSelection(item);
                }
            }
        } else {
            platformTable.setSelection(targets.size() - 1);
        }
        TableUtility.packColumns(platformTable);
        Dialog.applyDialogFont(container);
        validate();
    }

    void validate() {
        final TableItem[] items = platformTable.getSelection();
        setPageComplete(items.length == 1);
        for (final TableItem item : items) {
            info.setSelectedTarget((IAndroidTarget) item.getData());
        }
    }

}
