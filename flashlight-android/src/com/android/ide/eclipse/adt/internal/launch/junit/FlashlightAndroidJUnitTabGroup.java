package com.android.ide.eclipse.adt.internal.launch.junit;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.android.ide.eclipse.adt.internal.launch.EmulatorConfigTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightFieldsTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightInstrumentationTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightMethodsTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightTab;

public class FlashlightAndroidJUnitTabGroup extends
        AbstractLaunchConfigurationTabGroup {

    /**
     * Creates the UI tabs for the Android JUnit configuration
     */
    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new AndroidJUnitLaunchConfigurationTab(),
                new EmulatorConfigTab(ILaunchManager.RUN_MODE.equals(mode)),
                new FlashlightInstrumentationTab(), new FlashlightTab(),
                new FlashlightMethodsTab(), new FlashlightFieldsTab(),
                new CommonTab() };
        setTabs(tabs);
    }
}