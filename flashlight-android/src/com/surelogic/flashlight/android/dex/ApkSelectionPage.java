package com.surelogic.flashlight.android.dex;

import java.io.File;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.i18n.I18N;

public class ApkSelectionPage extends WizardPage {

    private final ApkSelectionInfo info;

    ApkSelectionPage(ApkSelectionInfo info) {
        super(I18N.msg("flashlight.dex.apk.title"));
        this.info = info;
        setTitle(I18N.msg("flashlight.dex.apk.title"));
        setMessage(I18N.msg("flashlight.dex.apk.title"));
    }

    @Override
    public void createControl(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        FileFieldEditor fe = new FileFieldEditor("bar",
                I18N.msg("flashlight.dex.apk.select"), content);
        String[] filterExtensions = new String[] { "*.apk", "*" };
        fe.setFileExtensions(filterExtensions);
        fe.setPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                info.setApk(new File((String) event.getNewValue()));
            }
        });
        setControl(content);
    }
}
