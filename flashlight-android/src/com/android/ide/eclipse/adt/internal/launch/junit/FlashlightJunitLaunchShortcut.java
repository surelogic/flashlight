package com.android.ide.eclipse.adt.internal.launch.junit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

public class FlashlightJunitLaunchShortcut extends JUnitLaunchShortcut {
    @Override
    protected String getLaunchConfigurationTypeId() {
        return "com.android.ide.eclipse.adt.junit.launchConfigurationType"; //$NON-NLS-1$
    }

    /**
     * Creates a default Android JUnit launch configuration. Sets the
     * instrumentation runner to the first instrumentation found in the
     * AndroidManifest.
     */
    @Override
    protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
            IJavaElement element) throws CoreException {
        ILaunchConfigurationWorkingCopy config = super
                .createLaunchConfiguration(element);
        // just get first valid instrumentation runner
        String instrumentation = new FlashlightInstrumentationRunnerValidator(
                element.getJavaProject()).getValidInstrumentationTestRunner();
        if (instrumentation != null) {
            config.setAttribute(
                    FlashlightAndroidJUnitLaunchConfigurationDelegate.ATTR_INSTR_NAME,
                    instrumentation);
        }
        // if a valid runner is not found, rely on launch delegate to log error.
        // This method is called without explicit user action to launch Android
        // JUnit, so avoid
        // logging an error here.

        FlashlightAndroidJUnitLaunchConfigurationDelegate
                .setJUnitDefaults(config);
        return config;
    }
}
