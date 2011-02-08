package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.flashlight.common.model.RunDescription;

public class RegionRefactoringInfo {

	private IJavaProject selectedProject;
	private final List<IJavaProject> projects;
	private final List<RunDescription> runs;
	private List<RunDescription> selectedRuns;

	public RegionRefactoringInfo(final List<IJavaProject> projects,
			final List<RunDescription> runs) {
		this.projects = projects;
		this.runs = runs;
	}

	public IJavaProject getSelectedProject() {
		return selectedProject;
	}

	public void setSelectedProject(final IJavaProject project) {
		this.selectedProject = project;
	}

	public List<IJavaProject> getProjects() {
		return projects;
	}

	public List<RunDescription> getRuns() {
		return runs;
	}

	public List<RunDescription> getSelectedRuns() {
		return selectedRuns;
	}

	public void setSelectedRuns(final List<RunDescription> selectedRuns) {
		this.selectedRuns = selectedRuns;
	}

}
