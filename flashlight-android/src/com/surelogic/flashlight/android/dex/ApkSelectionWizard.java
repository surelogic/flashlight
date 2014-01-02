package com.surelogic.flashlight.android.dex;

import org.eclipse.jface.wizard.Wizard;

public class ApkSelectionWizard extends Wizard {

    private final ProjectSelectionPage psPage;
    private final ApkSelectionPage apkPage;

    public ApkSelectionWizard(ApkSelectionInfo info) {
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
        return true;
    }

}
