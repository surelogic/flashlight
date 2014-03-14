package com.surelogic.flashlight.android.dex;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.android.sdklib.IAndroidTarget;

public class ApkSelectionInfo {
    private IProject selectedProject;
    private final List<IProject> projects;
    private IAndroidTarget selectedTarget;
    private final List<IAndroidTarget> targets;
    File apk;

    public ApkSelectionInfo(List<IProject> projects,
            List<IAndroidTarget> targets) {
        this.projects = projects;
        this.targets = targets;
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

    public IAndroidTarget getSelectedTarget() {
        return selectedTarget;
    }

    public void setSelectedTarget(IAndroidTarget selectedTarget) {
        this.selectedTarget = selectedTarget;
    }

    public List<IAndroidTarget> getTargets() {
        return targets;
    }

    public boolean isSelectionValid() {
        return apk != null && apk.exists() && selectedTarget != null;
    }

}
