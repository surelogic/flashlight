package com.surelogic.flashlight.recommend.refactor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RegionRefactoringWizard extends RefactoringWizard {

	private final RegionRefactoringInfo info;

	public RegionRefactoringWizard(final Refactoring refactoring,
			final RegionRefactoringInfo info) {
		super(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
		this.info = info;
	}

	@Override
	protected void addUserInputPages() {
		final RunSelectionPage run = new RunSelectionPage(info);
		final ProjectSelectionPage project = new ProjectSelectionPage(info);
		addPage(project);
		addPage(run);
		setDefaultPageTitle("Infer JSure annotations...");
	}

}
