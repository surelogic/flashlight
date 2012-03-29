package com.android.ide.eclipse.adt.internal.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.surelogic.flashlight.client.eclipse.launch.FlashlightFieldsTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightInstrumentationTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightMethodsTab;
import com.surelogic.flashlight.client.eclipse.launch.FlashlightTab;

public class FlashlightAppTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(final ILaunchConfigurationDialog dialog,
            final String mode) {
        final ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new MainLaunchConfigTab(), new EmulatorConfigTab(),
                new FlashlightInstrumentationTab(), new FlashlightTab(),
                new FlashlightMethodsTab(), new FlashlightFieldsTab(),
                new CommonTab() };
        setTabs(tabs);
    }

}
