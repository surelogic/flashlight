package com.surelogic.flashlight.recommend.refactor;

import java.util.ArrayList;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.entities.PrepRunDescription;

/**
 * Choose one or more runs from those currently available in Flashlight.
 * 
 * @author nathan
 * 
 */
public class RunSelectionPage extends UserInputWizardPage {

	private Tree f_runTree;
	private TreeColumn f_nameColumn;
	private final RegionRefactoringInfo f_info;
	private String selected;

	public RunSelectionPage(final RegionRefactoringInfo info) {
		super(I18N.msg("flashlight.recommend.dialog.run.title"));
		setTitle(I18N.msg("flashlight.recommend.dialog.run.title"));
		setDescription(I18N.msg("flashlight.recommend.dialog.run.info"));
		f_info = info;
	}

	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FormLayout());
		setControl(container);
		f_runTree = new Tree(container, SWT.MULTI);
		final FormData formData = new FormData();
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		f_runTree.setLayoutData(formData);
		f_runTree.setHeaderVisible(true);
		f_runTree.setLinesVisible(true);
		f_runTree.addSelectionListener(new SelectionListener() {

			public void widgetSelected(final SelectionEvent e) {
				validate();
			}

			public void widgetDefaultSelected(final SelectionEvent e) {
				validate();
			}
		});
		f_nameColumn = new TreeColumn(f_runTree, SWT.NONE);
		f_nameColumn.setText(I18N.msg("flashlight.recommend.dialog.runCol"));
		f_nameColumn.pack();
		Dialog.applyDialogFont(container);
		validate();
	}

	void validate() {
		final List<PrepRunDescription> selected = new ArrayList<PrepRunDescription>();
		for (final TreeItem item : f_runTree.getSelection()) {
			selected.add((PrepRunDescription) item.getData());
		}
		f_info.setSelectedRuns(selected);
		setPageComplete(!selected.isEmpty());
	}

	@Override
	public void setVisible(final boolean visible) {
		f_runTree.removeAll();
		final String project = f_info.getSelectedProject().getElementName();
		if (!project.equals(selected)) {
			selected = project;
			for (final PrepRunDescription run : f_info.getRuns()) {
				boolean noneSelected = true;
				final String runName = run.getDescription().getName();
				final int idx = runName.lastIndexOf('.');
				final String runPackage = runName.substring(0, idx);
				final String runClass = runName.substring(idx + 1);
				if (JDTUtility.findIType(project, runPackage, runClass) != null) {
					final TreeItem item = new TreeItem(f_runTree, SWT.NONE);
					item.setText(run.getDescription().getName());
					item.setData(run);
					if (noneSelected) {
						noneSelected = false;
						f_runTree.setSelection(item);
					}
				}
			}
			validate();
		}
		super.setVisible(visible);
	}

}
