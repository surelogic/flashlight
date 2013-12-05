package com.surelogic.flashlight.android.dex;

import org.eclipse.jface.wizard.Wizard;

public class ApkSelectionWizard extends Wizard {

    private final ProjectSelectionPage psPage;
    private final ApkSelectionPage apkPage;
    private final ApkSelectionInfo info;

    public ApkSelectionWizard(ApkSelectionInfo info) {
        this.info = info;
        apkPage = new ApkSelectionPage(info);
        psPage = new ProjectSelectionPage(info);

    }

    @Override
    public void addPages() {
        addPage(apkPage);
        addPage(psPage);
    }

    @Override
    public boolean performFinish() {
        return false;
    }

}
