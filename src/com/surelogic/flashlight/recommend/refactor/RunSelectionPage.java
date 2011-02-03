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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Choose one or more runs from those currently available in Flashlight.
 */
public class RunSelectionPage extends UserInputWizardPage {

	private Table f_runTable;
	// private TableColumn f_nameColumn;
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
		f_runTable = new Table(container, SWT.FULL_SELECTION | SWT.CHECK);
		final FormData formData = new FormData();
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		f_runTable.setLayoutData(formData);
		f_runTable.setHeaderVisible(false);
		f_runTable.setLinesVisible(true);
		f_runTable.addSelectionListener(new SelectionListener() {

			public void widgetSelected(final SelectionEvent e) {
				validate();
			}

			public void widgetDefaultSelected(final SelectionEvent e) {
				validate();
			}
		});

		Dialog.applyDialogFont(container);
		validate();
	}

	void validate() {
		final List<RunDescription> selected = new ArrayList<RunDescription>();
		for (final TableItem item : f_runTable.getItems()) {
			if (item.getChecked()) {
				selected.add((RunDescription) item.getData());
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
				for (final RunDescription run : f_info.getRuns()) {
					boolean noneSelected = true;
					final String runName = run.getName();
					final int idx = runName.lastIndexOf('.');
					if (idx > 0) {
						final String runPackage = runName.substring(0, idx);
						final String runClass = runName.substring(idx + 1);
						if (JDTUtility.findIType(project, runPackage, runClass) != null) {
							final TableItem item = new TableItem(f_runTable,
									SWT.NONE);
							item.setText(run.getName());
							item.setData(run);
							if (noneSelected) {
								noneSelected = false;
								f_runTable.setSelection(item);
							}
						}
					}
				}
				validate();
			}
		}
		super.setVisible(visible);
	}

}
