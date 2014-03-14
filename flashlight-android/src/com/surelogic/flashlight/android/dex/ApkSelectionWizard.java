package com.surelogic.flashlight.android.dex;

import org.eclipse.jface.wizard.Wizard;

public class ApkSelectionWizard extends Wizard {

    private final ProjectSelectionPage psPage;
    private final ApkSelectionPage apkPage;
    private final PlatformSelectionPage pfPage;

    public ApkSelectionWizard(ApkSelectionInfo info) {
        apkPage = new ApkSelectionPage(info);
        psPage = new ProjectSelectionPage(info);
        pfPage = new PlatformSelectionPage(info);
    }

    @Override
    public void addPages() {
        addPage(apkPage);
        addPage(psPage);
        addPage(pfPage);
    }

    @Override
    public boolean performFinish() {
        return true;
    }

}
