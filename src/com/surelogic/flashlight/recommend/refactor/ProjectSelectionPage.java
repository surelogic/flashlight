package com.surelogic.flashlight.recommend.refactor;

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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.surelogic.common.i18n.I18N;

public class ProjectSelectionPage extends UserInputWizardPage {

	private Tree f_projectTree;
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

		f_projectTree = new Tree(container, SWT.SINGLE);
		f_projectTree.setHeaderVisible(true);
		f_projectTree.setLinesVisible(true);
		final FormData formData = new FormData();
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		f_projectTree.setLayoutData(formData);
		f_projectTree.addSelectionListener(new SelectionListener() {

			public void widgetSelected(final SelectionEvent e) {
				validate();
			}

			public void widgetDefaultSelected(final SelectionEvent e) {
				validate();
			}
		});
		final TreeColumn nameColumn = new TreeColumn(f_projectTree, SWT.NONE);
		nameColumn.setText(I18N.msg("flashlight.recommend.dialog.projectCol"));
		for (final IJavaProject project : f_info.getProjects()) {
			final TreeItem item = new TreeItem(f_projectTree, SWT.NONE);
			item.setText(project.getProject().getName());
			item.setData(project);
			if (project.equals(f_info.getSelectedProject())) {
				f_projectTree.setSelection(item);
			}
		}
		nameColumn.pack();
		Dialog.applyDialogFont(container);
		validate();
	}

	void validate() {
		final TreeItem[] items = f_projectTree.getSelection();
		setPageComplete(items.length > 0);
		for (final TreeItem item : items) {
			f_info.setSelectedProject((IJavaProject) item.getData());
		}
	}

}