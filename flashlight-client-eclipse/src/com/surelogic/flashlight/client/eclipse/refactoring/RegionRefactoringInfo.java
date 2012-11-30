package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.flashlight.common.files.RunDirectory;

public class RegionRefactoringInfo {

  private IJavaProject selectedProject;
  private final List<IJavaProject> projects;
  private final List<RunDirectory> runs;
  private List<RunDirectory> selectedRuns;

  public RegionRefactoringInfo(final List<IJavaProject> projects, final List<RunDirectory> runs) {
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

  public List<RunDirectory> getRuns() {
    return runs;
  }

  public List<RunDirectory> getSelectedRuns() {
    return selectedRuns;
  }

  public void setSelectedRuns(final List<RunDirectory> selectedRuns) {
    this.selectedRuns = selectedRuns;
  }
}
