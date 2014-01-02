package com.surelogic.flashlight.android.dex;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;

public class ApkSelectionInfo {
    private IProject selectedProject;
    private final List<IProject> projects;
    File apk;

    public ApkSelectionInfo(List<IProject> projects) {
        this.projects = projects;
    }

    public IProject getSelectedProject() {
        return selectedProject;
    }

    public void setSelectedProject(IProject selectedProject) {
        this.selectedProject = selectedProject;
    }

    public List<IProject> getProjects() {
        return projects;
    }

    public File getApk() {
        return apk;
    }

    public void setApk(File apk) {
        this.apk = apk;
    }

    public boolean isSelectionValid() {
        return selectedProject != null && apk != null && apk.exists();
    }

}
