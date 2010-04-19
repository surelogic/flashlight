package com.surelogic.flashlight.recommend.refactor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RegionRefactoringWizard extends RefactoringWizard {

	private final RegionRefactoringInfo info;
	private final boolean showWizard;

	public RegionRefactoringWizard(final Refactoring refactoring,
			final RegionRefactoringInfo info, final boolean showWizard) {
		super(refactoring,
				showWizard ? RefactoringWizard.WIZARD_BASED_USER_INTERFACE
						: RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		this.info = info;
		this.showWizard = showWizard;
	}

	public RegionRefactoringWizard(final Refactoring refactoring,
			final RegionRefactoringInfo info) {
		this(refactoring, info, true);
	}

	@Override
	protected void addUserInputPages() {
		if (showWizard) {
			final RunSelectionPage run = new RunSelectionPage(info);
			final ProjectSelectionPage project = new ProjectSelectionPage(info);
			addPage(project);
			addPage(run);
			setDefaultPageTitle("Infer JSure annotations...");
		}
	}

}
