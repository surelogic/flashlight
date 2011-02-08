package com.surelogic.flashlight.client.eclipse.refactoring;

import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.swt.widgets.TableItem;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.i18n.I18N;

public class ProjectSelectionPage extends UserInputWizardPage {

	private Table f_projectTable;
	private final RegionRefactoringInfo f_info;

	public ProjectSelectionPage(final RegionRefactoringInfo info) {
		super(I18N.msg("flashlight.recommend.dialog.project.title"));
		setTitle(I18N.msg("flashlight.recommend.dialog.project.title"));
		setDescription(I18N.msg("flashlight.recommend.dialog.project.info"));
		f_info = info;
	}

	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FormLayout());
		setControl(container);

		f_projectTable = new Table(container, SWT.NONE);
		f_projectTable.setHeaderVisible(false);
		f_projectTable.setLinesVisible(true);
		final FormData formData = new FormData();
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		f_projectTable.setLayoutData(formData);
		f_projectTable.addSelectionListener(new SelectionListener() {
			public void widgetSelected(final SelectionEvent e) {
				validate();
			}

			public void widgetDefaultSelected(final SelectionEvent e) {
				validate();
			}
		});
		for (final IJavaProject project : f_info.getProjects()) {
			final TableItem item = new TableItem(f_projectTable, SWT.NONE);
			item.setText(project.getProject().getName());
			item.setImage(SLImages.getImage(CommonImages.IMG_PROJECT));
			item.setData(project);
			if (project.equals(f_info.getSelectedProject())) {
				f_projectTable.setSelection(item);
			}
		}
		Dialog.applyDialogFont(container);
		validate();
	}

	void validate() {
		final TableItem[] items = f_projectTable.getSelection();
		setPageComplete(items.length > 0);
		for (final TableItem item : items) {
			f_info.setSelectedProject((IJavaProject) item.getData());
		}
	}

}