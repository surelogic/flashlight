package com.android.ide.eclipse.adt.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;
import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.InstrumentationFileTranslator;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper.RuntimeConfig;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.model.FlashlightFileUtility;

public class Dex2JarLaunchConfigurationDelegate extends
        LaunchConfigurationDelegate {

    /**
     * Default launch action. This launches the activity that is setup to be
     * found in the HOME screen.
     */
    public final static int ACTION_DEFAULT = 0;
    /** Launch action starting a specific activity. */
    public final static int ACTION_ACTIVITY = 1;
    /** Launch action that does nothing. */
    public final static int ACTION_DO_NOTHING = 2;
    /** Default launch action value. */
    public final static int DEFAULT_LAUNCH_ACTION = ACTION_DEFAULT;
    /**
     * Activity to be launched if {@link #ATTR_LAUNCH_ACTION} is 1
     */
    @SuppressWarnings("restriction")
    public static final String ATTR_ACTIVITY = AdtPlugin.PLUGIN_ID
            + ".activity"; //$NON-NLS-1$

    private final Logger log = SLLogger
            .getLoggerFor(FlashlightAndroidLaunchConfigurationDelegate.class);

    @Override
    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {
        AndroidLaunch androidLaunch = (AndroidLaunch) launch;
        IProject project = EclipseUtility
                .getProject(configuration
                        .getAttribute(
                                IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                                ""));
        ProjectHelper.doFullIncrementalDebugBuild(project, monitor);

        // check if the project has errors, and abort in this case.
        if (ProjectHelper.hasError(project, true)) {
            AdtPlugin
                    .displayError(
                            "Android Launch",
                            "Your project contains error(s), please fix them before running your application.");
            return;
        }

        AdtPlugin.printToConsole(project, "------------------------------"); //$NON-NLS-1$
        AdtPlugin.printToConsole(project, "Android Launch!");

        // check if the project is using the proper sdk.
        // if that throws an exception, we simply let it propagate to the
        // caller.
        if (checkAndroidProject(project) == false) {
            AdtPlugin.printErrorToConsole(project,
                    "Project is not an Android Project. Aborting!");
            androidLaunch.stopLaunch();
            return;
        }
        // Check adb status and abort if needed.
        AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
        if (bridge == null || bridge.isConnected() == false) {
            try {
                int connections = -1;
                int restarts = -1;
                if (bridge != null) {
                    connections = bridge.getConnectionAttemptCount();
                    restarts = bridge.getRestartAttemptCount();
                }

                // if we get -1, the device monitor is not even setup
                // (anymore?).
                // We need to ask the user to restart eclipse.
                // This shouldn't happen, but it's better to let the user know
                // in case it does.
                if (connections == -1 || restarts == -1) {
                    AdtPlugin
                            .printErrorToConsole(
                                    project,
                                    "The connection to adb is down, and a severe error has occured.",
                                    "You must restart adb and Eclipse.",
                                    String.format(
                                            "Please ensure that adb is correctly located at '%1$s' and can be executed.",
                                            AdtPlugin.getOsAbsoluteAdb()));
                    return;
                }

                if (restarts == 0) {
                    AdtPlugin
                            .printErrorToConsole(
                                    project,
                                    "Connection with adb was interrupted.",
                                    String.format(
                                            "%1$s attempts have been made to reconnect.",
                                            connections),
                                    "You may want to manually restart adb from the Devices view.");
                } else {
                    AdtPlugin
                            .printErrorToConsole(
                                    project,
                                    "Connection with adb was interrupted, and attempts to reconnect have failed.",
                                    String.format(
                                            "%1$s attempts have been made to restart adb.",
                                            restarts),
                                    "You may want to manually restart adb from the Devices view.");

                }
                return;
            } finally {
                androidLaunch.stopLaunch();
            }
        }

        // since adb is working, we let the user know
        // TODO have a verbose mode for launch with more info (or some of the
        // less useful info we now have).
        AdtPlugin.printToConsole(project, "adb is running normally.");

        // make a config class
        AndroidLaunchConfiguration config = new AndroidLaunchConfiguration();

        // fill it with the config coming from the ILaunchConfiguration object
        config.set(configuration);

        // get the launch controller singleton
        AndroidLaunchController controller = AndroidLaunchController
                .getInstance();

        // get the application package
        IFile applicationPackage = ProjectHelper.getApplicationPackage(project);
        if (applicationPackage == null) {
            androidLaunch.stopLaunch();
            return;
        }

        // we need some information from the manifest
        ManifestData manifestData = AndroidManifestHelper.parseForData(project);

        if (manifestData == null) {
            AdtPlugin.printErrorToConsole(project,
                    "Failed to parse AndroidManifest: aborting!");
            androidLaunch.stopLaunch();
            return;
        }

        String runId = project.getName()
                + new SimpleDateFormat(InstrumentationConstants.DATE_FORMAT)
                        .format(new Date())
                + InstrumentationConstants.ANDROID_LAUNCH_SUFFIX;
        RunManager.getInstance()
                .notifyPerformingInstrumentationAndLaunch(runId);

        try {
            applicationPackage = instrumentPackage(runId, applicationPackage);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        doLaunch(configuration, mode, monitor, project, androidLaunch, config,
                controller, applicationPackage, manifestData);
    }

    private IFile instrumentPackage(String runId, IFile applicationPackage)
            throws IOException, CoreException {
        FLData data = new FLData(runId, null, null, applicationPackage);

        return null;
    }

    protected void doLaunch(ILaunchConfiguration configuration, String mode,
            IProgressMonitor monitor, IProject project,
            AndroidLaunch androidLaunch, AndroidLaunchConfiguration config,
            AndroidLaunchController controller, IFile applicationPackage,
            ManifestData manifestData) {

        String activityName = null;

        if (config.mLaunchAction == ACTION_ACTIVITY) {
            // Get the activity name defined in the config
            activityName = getActivityName(configuration);

            // Get the full activity list and make sure the one we got matches.
            Activity[] activities = manifestData.getActivities();

            // first we check that there are, in fact, activities.
            if (activities.length == 0) {
                // if the activities list is null, then the manifest is empty
                // and we can't launch the app. We'll revert to a sync-only
                // launch
                AdtPlugin
                        .printErrorToConsole(project,
                                "The Manifest defines no activity!",
                                "The launch will only sync the application package on the device!");
                config.mLaunchAction = ACTION_DO_NOTHING;
            } else if (activityName == null) {
                // if the activity we got is null, we look for the default one.
                AdtPlugin
                        .printErrorToConsole(project,
                                "No activity specified! Getting the launcher activity.");
                Activity launcherActivity = manifestData.getLauncherActivity();
                if (launcherActivity != null) {
                    activityName = launcherActivity.getName();
                }

                // if there's no default activity. We revert to a sync-only
                // launch.
                if (activityName == null) {
                    revertToNoActionLaunch(project, config);
                }
            } else {

                // check the one we got from the config matches any from the
                // list
                boolean match = false;
                for (Activity a : activities) {
                    if (a != null && a.getName().equals(activityName)) {
                        match = true;
                        break;
                    }
                }

                // if we didn't find a match, we revert to the default activity
                // if any.
                if (match == false) {
                    AdtPlugin
                            .printErrorToConsole(project,
                                    "The specified activity does not exist! Getting the launcher activity.");
                    Activity launcherActivity = manifestData
                            .getLauncherActivity();
                    if (launcherActivity != null) {
                        activityName = launcherActivity.getName();
                    } else {
                        // if there's no default activity. We revert to a
                        // sync-only launch.
                        revertToNoActionLaunch(project, config);
                    }
                }
            }
        } else if (config.mLaunchAction == ACTION_DEFAULT) {
            Activity launcherActivity = manifestData.getLauncherActivity();
            if (launcherActivity != null) {
                activityName = launcherActivity.getName();
            }

            // if there's no default activity. We revert to a sync-only launch.
            if (activityName == null) {
                revertToNoActionLaunch(project, config);
            }
        }

        IAndroidLaunchAction launchAction = null;
        if (config.mLaunchAction == ACTION_DO_NOTHING || activityName == null) {
            launchAction = new EmptyLaunchAction();
        } else {
            launchAction = new ActivityLaunchAction(activityName, controller);
        }

        // everything seems fine, we ask the launch controller to handle
        // the rest
        controller.launch(project, mode, applicationPackage,
                manifestData.getPackage(), manifestData.getPackage(),
                manifestData.getDebuggable(),
                manifestData.getMinSdkVersionString(), launchAction, config,
                androidLaunch, monitor);
    }

    /**
     * Checks the project is an android project.
     * 
     * @param project
     *            The project to check
     * @return true if the project is an android SDK.
     * @throws CoreException
     */
    @SuppressWarnings("restriction")
    private boolean checkAndroidProject(final IProject project)
            throws CoreException {
        // check if the project is a java and an android project.
        if (project.hasNature(JavaCore.NATURE_ID) == false) {
            String msg = String.format("%1$s is not a Java project!",
                    project.getName());
            AdtPlugin.displayError("Android Launch", msg);
            return false;
        }

        if (project.hasNature(AdtConstants.NATURE_DEFAULT) == false) {
            String msg = String.format("%1$s is not an Android project!",
                    project.getName());
            AdtPlugin.displayError("Android Launch", msg);
            return false;
        }

        return true;
    }

    /**
     * Returns the name of the activity.
     */
    private String getActivityName(ILaunchConfiguration configuration) {
        String empty = "";
        String activityName;
        try {
            activityName = configuration.getAttribute(ATTR_ACTIVITY, empty);
        } catch (CoreException e) {
            return null;
        }

        return activityName != empty ? activityName : null;
    }

    private final void revertToNoActionLaunch(IProject project,
            AndroidLaunchConfiguration config) {
        AdtPlugin
                .printErrorToConsole(project, "No Launcher activity found!",
                        "The launch will only sync the application package on the device!");
        config.mLaunchAction = ACTION_DO_NOTHING;
    }

    private static class FLData {

        final IProject project;
        final File log;
        final File fieldsFile;
        final File sitesFile;
        final File classesFile;
        final File hbFile;
        final File runDir;
        final File infoDir;
        final File portFile;
        final File sourceDir;
        final File apkFolder;
        final String runId;
        final IFile apk;
        final int outputPort;
        final RuntimeConfig conf;

        FLData(String runId, final ILaunchConfiguration launch,
                final IProject project, final IFile apk) throws IOException,
                CoreException {
            this.runId = runId;
            conf = LaunchHelper.getRuntimeConfig(launch);
            this.project = project;
            this.apk = apk;
            outputPort = InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT;

            runDir = new File(EclipseUtility.getFlashlightDataDirectory(),
                    runId);
            runDir.mkdir();
            sourceDir = new File(runDir,
                    InstrumentationConstants.FL_SOURCE_FOLDER_LOC);
            sourceDir.mkdirs();
            apkFolder = new File(runDir,
                    InstrumentationConstants.FL_APK_FOLDER_LOC);
            apkFolder.mkdirs();
            fieldsFile = new File(runDir,
                    InstrumentationConstants.FL_FIELDS_FILE_LOC);
            log = new File(runDir, InstrumentationConstants.FL_LOG_FILE_LOC);
            sitesFile = new File(runDir,
                    InstrumentationConstants.FL_SITES_FILE_LOC);
            classesFile = new File(runDir,
                    InstrumentationConstants.FL_CLASS_HIERARCHY_FILE_LOC);
            hbFile = new File(runDir,
                    InstrumentationConstants.FL_HAPPENS_BEFORE_FILE_LOC);
            portFile = new File(runDir,
                    InstrumentationConstants.FL_PORT_FILE_LOC);
            PrintWriter writer = new PrintWriter(portFile);
            try {
                writer.println(conf.getConsolePort());
            } finally {
                writer.close();
            }
            infoDir = File.createTempFile("fl_info_", "dir");
            infoDir.delete();
        }

        public void createInfoClasses() throws IOException {
            File infoClassDest = new File(infoDir,
                    InstrumentationConstants.FL_PROPERTIES_CLASS);
            infoClassDest.getParentFile().mkdirs();
            Properties props = new Properties();
            props.setProperty(InstrumentationConstants.FL_RUN,
                    FlashlightFileUtility.getRunName(runId));
            props.setProperty(InstrumentationConstants.FL_ANDROID, "true");

            props.setProperty(InstrumentationConstants.FL_COLLECTION_TYPE,
                    conf.getCollectionType());
            props.setProperty(InstrumentationConstants.FL_CONSOLE_PORT,
                    Integer.toString(conf.getConsolePort()));
            props.setProperty(InstrumentationConstants.FL_OUTPUT_PORT, Integer
                    .toString(InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT));
            props.setProperty(InstrumentationConstants.FL_OUTQ_SIZE,
                    Integer.toString(conf.getOutQueueSize()));
            props.setProperty(InstrumentationConstants.FL_RAWQ_SIZE,
                    Integer.toString(conf.getRawQueueSize()));
            props.setProperty(InstrumentationConstants.FL_REFINERY_SIZE,
                    Integer.toString(conf.getRefinerySize()));
            props.setProperty(InstrumentationConstants.FL_POSTMORTEM,
                    Boolean.toString(conf.isPostmortem()));
            if (!conf.useSpy()) {
                props.setProperty(InstrumentationConstants.FL_NO_SPY, "true");
            }

            InstrumentationFileTranslator.writeProperties(props, infoClassDest);

            File fieldsClass = new File(infoDir,
                    InstrumentationConstants.FL_FIELDS_CLASS);
            InstrumentationFileTranslator.writeFields(fieldsFile, fieldsClass);

            File sitesClass = new File(infoDir,
                    InstrumentationConstants.FL_SITES_CLASS);
            InstrumentationFileTranslator.writeSites(sitesFile, sitesClass);
        }

        public void deleteTempFiles() {
            FileUtility.recursiveDelete(infoDir);
        }

    }

    private static String getRuntimeJarPath() {
        final IPath bundleBase = Activator.getDefault().getBundleLocation();
        if (bundleBase != null) {
            String name = "lib/flashlight-runtime.jar";
            final IPath jarLocation = bundleBase.append(name);
            return jarLocation.toOSString();
        } else {
            throw new IllegalStateException("No bundle location found.");
        }
    }
}
