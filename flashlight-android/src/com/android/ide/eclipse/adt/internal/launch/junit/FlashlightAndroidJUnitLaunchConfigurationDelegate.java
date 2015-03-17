package com.android.ide.eclipse.adt.internal.launch.junit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import com.android.SdkConstants;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Instrumentation;
import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.launch.LaunchMessages;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;

public class FlashlightAndroidJUnitLaunchConfigurationDelegate extends
        LaunchConfigurationDelegate {

    /** Launch config attribute that stores instrumentation runner. */
    static final String ATTR_INSTR_NAME = AdtPlugin.PLUGIN_ID
            + ".instrumentation"; //$NON-NLS-1$

    @Override
    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {

    }

    /**
     * Get the target Android application's package for the given
     * instrumentation runner, or <code>null</code> if it could not be found.
     *
     * @param manifestParser
     *            the {@link ManifestData} for the test project
     * @param runner
     *            the instrumentation runner class name
     * @return the target package or <code>null</code>
     */
    private String getTargetPackage(ManifestData manifestParser, String runner) {
        for (Instrumentation instr : manifestParser.getInstrumentations()) {
            if (instr.getName().equals(runner)) {
                return instr.getTargetPackage();
            }
        }
        return null;
    }

    /**
     * Gets a instrumentation runner for the launch.
     * <p/>
     * If a runner is stored in the given <code>configuration</code>, will
     * return that. Otherwise, will try to find the first valid runner for the
     * project. If a runner can still not be found, will return
     * <code>null</code>, and will log an error to the console.
     *
     * @param project
     *            the {@link IProject} for the app
     * @param configuration
     *            the {@link ILaunchConfiguration} for the launch
     * @param manifestData
     *            the {@link ManifestData} for the project
     *
     * @return <code>null</code> if no instrumentation runner can be found,
     *         otherwise return the fully qualified runner name.
     */
    private String getRunner(IProject project,
            ILaunchConfiguration configuration, ManifestData manifestData) {
        try {
            String runner = getRunnerFromConfig(configuration);
            if (runner != null) {
                return runner;
            }
            final InstrumentationRunnerValidator instrFinder = new InstrumentationRunnerValidator(
                    BaseProjectHelper.getJavaProject(project), manifestData);
            runner = instrFinder.getValidInstrumentationTestRunner();
            if (runner != null) {
                AdtPlugin
                .printErrorToConsole(
                        project,
                        String.format(
                                LaunchMessages.AndroidJUnitDelegate_NoRunnerConfigMsg_s,
                                runner));
                return runner;
            }
            AdtPlugin.printErrorToConsole(project, String.format(
                    LaunchMessages.AndroidJUnitDelegate_NoRunnerConsoleMsg_4s,
                    project.getName(),
                    SdkConstants.CLASS_INSTRUMENTATION_RUNNER,
                    AdtConstants.LIBRARY_TEST_RUNNER,
                    SdkConstants.FN_ANDROID_MANIFEST_XML));
            return null;
        } catch (CoreException e) {
            AdtPlugin.log(e, "Error when retrieving instrumentation info"); //$NON-NLS-1$
        }

        return null;
    }

    private String getRunnerFromConfig(ILaunchConfiguration configuration) {
        return getStringLaunchAttribute(ATTR_INSTR_NAME, configuration);
    }

    /**
     * Helper method to retrieve a string attribute from the launch
     * configuration
     *
     * @param attributeName
     *            name of the launch attribute
     * @param configuration
     *            the {@link ILaunchConfiguration} to retrieve the attribute
     *            from
     * @return the attribute's value. <code>null</code> if not found.
     */
    private String getStringLaunchAttribute(String attributeName,
            ILaunchConfiguration configuration) {
        try {
            String attrValue = configuration.getAttribute(attributeName, "");
            if (attrValue.length() < 1) {
                return null;
            }
            return attrValue;
        } catch (CoreException e) {
            AdtPlugin.log(e,
                    String.format("Error when retrieving launch info %1$s", //$NON-NLS-1$
                            attributeName));
        }
        return null;
    }
}
