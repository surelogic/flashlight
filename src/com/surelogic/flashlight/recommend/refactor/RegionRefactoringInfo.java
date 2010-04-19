package com.surelogic.flashlight.recommend.refactor;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.flashlight.common.entities.PrepRunDescription;

public class RegionRefactoringInfo {

	private IJavaProject selectedProject;
	private final List<IJavaProject> projects;
	private final List<PrepRunDescription> runs;
	private List<PrepRunDescription> selectedRuns;

	public RegionRefactoringInfo(final List<IJavaProject> projects,
			final List<PrepRunDescription> runs) {
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

	public List<PrepRunDescription> getRuns() {
		return runs;
	}

	public List<PrepRunDescription> getSelectedRuns() {
		return selectedRuns;
	}

	public void setSelectedRuns(final List<PrepRunDescription> selectedRuns) {
		this.selectedRuns = selectedRuns;
	}

}
