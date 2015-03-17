package com.android.ide.eclipse.adt.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.progress.UIJob;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;
import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.project.ApkInstallManager;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.InstrumentationFileTranslator;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.android.jobs.ReadFlashlightStreamJob;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper.RuntimeConfig;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;

/**
 * This Launch Configuration is mostly cribbed from
 * {@link AndroidLaunchConfiguration}. The primary difference is that, after
 * ensuring that the project is fully compiled and packaged, we generate our own
 * package.
 *
 * @author nathan
 *
 */
public class FlashlightAndroidLaunchConfigurationDelegate extends
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

    static Logger getLog() {
        return SLLogger
                .getLoggerFor(FlashlightAndroidLaunchConfigurationDelegate.class);
    }

    @SuppressWarnings("restriction")
    @Override
    public void launch(final ILaunchConfiguration configuration,
            final String mode, final ILaunch launch,
            final IProgressMonitor monitor) throws CoreException {
        InstrumentedAndroidLaunch androidLaunch = (InstrumentedAndroidLaunch) launch;
        IProject project = EclipseUtility
                .getProject(configuration
                        .getAttribute(
                                IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                                ""));
        if (project == null) {
            AdtPlugin.printErrorToConsole("Couldn't get project object!");
            androidLaunch.stopLaunch();
            return;
        }

        if (!checkManifest(project)) {
            androidLaunch.stopLaunch();
            new UIJob("Display Flashlight Android Configuration Problem") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    MessageDialog
                    .openInformation(
                            EclipseUIUtility.getShell(),
                            I18N.msg("flashlight.eclipse.android.launchError.title"),
                            I18N.msg("flashlight.eclipse.android.launchError.message"));
                    return Status.OK_STATUS;
                }
            }.schedule();
            return;
        }

        RunId runId = new RunId(project.getName(), new Date());
        RunManager.getInstance().notifyPerformingInstrumentationAndLaunch(
                runId.getId());
        androidLaunch.setRunId(runId.getId());
        FLData data = AndroidBuildUtil.doFullIncrementalDebugBuild(runId,
                configuration, project, monitor);

        // if we have a valid debug port, this means we're debugging an app
        // that's already launched.
        // int debugPort =
        // AndroidLaunchController.getPortForConfig(configuration);
        // if (debugPort != INVALID_DEBUG_PORT) {
        // AndroidLaunchController.launchRemoteDebugger(debugPort,
        // androidLaunch, monitor);
        // return;
        // }

        if (ProjectHelper.hasError(project, true)) {
            // TODO
            throw new IllegalStateException(
                    "Your project contains error(s), please fix them before running your application.");
        }

        // FIXME Clear out the AdtPlugin calls

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
        } else {
            // We are going to store away the apk for debugging purposes
            File apkFile = applicationPackage.getRawLocation().toFile();
            FileUtility.copy(apkFile,
                    new File(data.apkFolder, apkFile.getName()));
        }

        // we need some information from the manifest
        ManifestData manifestData = AndroidManifestHelper.parseForData(project);

        if (manifestData == null) {
            AdtPlugin.printErrorToConsole(project,
                    "Failed to parse AndroidManifest: aborting!");
            androidLaunch.stopLaunch();
            return;
        }

        doLaunch(configuration, mode, monitor, project, androidLaunch, config,
                controller, applicationPackage, manifestData);
        if (!androidLaunch.isStopped()) {
            final Job job = new ConnectToProjectJob(data,
                    manifestData.getPackage());
            job.schedule();
        }
    }

    @SuppressWarnings("restriction")
    class InstrumentedAndroidLaunch extends AndroidLaunch {
        String runId;
        boolean done;

        public InstrumentedAndroidLaunch(
                ILaunchConfiguration launchConfiguration, String mode,
                ISourceLocator locator) {
            super(launchConfiguration, mode, locator);
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public boolean isStopped() {
            return done;
        }

        @Override
        public void stopLaunch() {
            if (runId != null) {
                RunManager.getInstance()
                .notifyLaunchCancelledPriorToCollectingData(runId);
            }
            done = true;
            super.stopLaunch();
        }

    }

    private static final class PermissionChecker extends DefaultHandler {
        boolean found;

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if (qName.equals("uses-permission")) {
                if ("android.permission.INTERNET".equals(attributes
                        .getValue("android:name"))) {
                    found = true;
                }
            }

        }

    }

    private boolean checkManifest(IProject project) {
        PermissionChecker checker = new PermissionChecker();
        IFile manifestFile = ProjectHelper.getManifest(project);
        if (manifestFile != null) {
            try {
                SAXParser parser = SAXParserFactory.newInstance()
                        .newSAXParser();
                parser.parse(new InputSource(manifestFile.getContents()),
                        checker);
            } catch (ParserConfigurationException e) {
                SLLogger.getLoggerFor(
                        FlashlightAndroidLaunchConfigurationDelegate.class)
                        .log(Level.WARNING, "Problem configuring sax parser", e);
            } catch (IOException e) {
                // Do nothing, this could be caused by a malformed file.
            } catch (SAXException e) {
                // Do nothing, this could be caused by a malformed file.
            } catch (CoreException e) {
                // Do nothing, this could be caused by a malformed file.
            }
        }
        return checker.found;
    }

    static class FLData {

        final IProject project;
        final Set<IProject> allProjects;
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
        final RunId runId;
        final List<String> originalClasspaths;
        final List<String> classpaths;
        final int outputPort;
        final RuntimeConfig conf;

        FLData(RunId runId, final ILaunchConfiguration launch,
                final IProject project, final Collection<String> dxInputPaths)
                        throws IOException, CoreException {
            this.runId = runId;
            runDir = new File(EclipseUtility.getFlashlightDataDirectory(),
                    runId.getId());
            runDir.mkdir();
            conf = LaunchHelper.getRuntimeConfig(launch);
            this.project = project;
            outputPort = InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT;
            originalClasspaths = new ArrayList<String>(dxInputPaths);
            classpaths = new ArrayList<String>(originalClasspaths.size());
            File projectsDir = new File(runDir,
                    InstrumentationConstants.FL_PROJECTS_FOLDER_LOC);
            for (int i = 0; i < originalClasspaths.size(); i++) {
                File cp = new File(originalClasspaths.get(i));
                File newCpFile = new File(projectsDir, String.format("%d - %s",
                        i, cp.getName()));
                if (cp.isDirectory()) {
                    newCpFile.mkdir();
                }
                classpaths.add(newCpFile.getAbsolutePath());
            }
            allProjects = new HashSet<IProject>();
            allProjects.add(project);
            for (final IProject p : ResourcesPlugin.getWorkspace().getRoot()
                    .getProjects()) {
                if (originalClasspaths.contains(p.getLocation().toOSString())) {
                    allProjects.add(p);
                }
            }

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

        public Collection<String> getClasspathEntries() {
            List<String> list = new ArrayList<String>(classpaths.size() + 2);
            list.addAll(classpaths);
            list.add(infoDir.getAbsolutePath());
            list.add(AndroidBuildUtil.getRuntimeJarPath());
            return list;
        }

        public void createInfoClasses() throws IOException {
            File infoClassDest = new File(infoDir,
                    InstrumentationConstants.FL_PROPERTIES_CLASS);
            infoClassDest.getParentFile().mkdirs();
            Properties props = new Properties();
            props.setProperty(InstrumentationConstants.FL_RUN, runId.getName());
            props.setProperty(InstrumentationConstants.FL_DATE_OVERRIDE,
                    runId.getDateSuffix());
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

    /**
     * {@inheritDoc}
     *
     * @throws CoreException
     */
    @Override
    public ILaunch getLaunch(final ILaunchConfiguration configuration,
            final String mode) throws CoreException {
        return new InstrumentedAndroidLaunch(configuration, mode, null);
    }

    @Override
    public boolean buildForLaunch(final ILaunchConfiguration configuration,
            final String mode, final IProgressMonitor monitor)
                    throws CoreException {
        // if this returns true, this forces a full workspace rebuild which is
        // not
        // what we want.
        // Instead in the #launch method, we'll rebuild only the launching
        // project.
        return false;
    }

    @SuppressWarnings("restriction")
    protected void doLaunch(final ILaunchConfiguration configuration,
            final String mode, final IProgressMonitor monitor,
            final IProject project, final AndroidLaunch androidLaunch,
            final AndroidLaunchConfiguration config,
            final AndroidLaunchController controller,
            final IFile applicationPackage, final ManifestData manifestData) {

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
                    AdtPlugin
                    .printErrorToConsole(project,
                            "No Launcher activity found!",
                            "The launch will only sync the application package on the device!");
                    config.mLaunchAction = ACTION_DO_NOTHING;
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
                        AdtPlugin
                        .printErrorToConsole(project,
                                "No Launcher activity found!",
                                "The launch will only sync the application package on the device!");
                        config.mLaunchAction = ACTION_DO_NOTHING;
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
                AdtPlugin
                .printErrorToConsole(project,
                        "No Launcher activity found!",
                        "The launch will only sync the application package on the device!");
                config.mLaunchAction = ACTION_DO_NOTHING;
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

    private static final class ConnectToProjectJob extends Job {

        private final int timeout;
        private final FLData data;
        private final String pakkage;

        ConnectToProjectJob(final FLData data, final String packageName) {
            this(data, packageName, 100);
        }

        ConnectToProjectJob(final FLData data, final String packageName,
                final int timeout) {
            super("Setting up forward for Flashlight Monitor");
            this.timeout = timeout;
            this.data = data;
            pakkage = packageName;
        }

        @SuppressWarnings("restriction")
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            for (IDevice id : AndroidDebugBridge.getBridge().getDevices()) {
                try {
                    if (ApkInstallManager.getInstance().isApplicationInstalled(
                            data.project, pakkage, id)) {
                        id.createForward(data.conf.getConsolePort(),
                                data.conf.getConsolePort());
                        id.createForward(data.outputPort, data.outputPort);
                        EclipseUtility.toEclipseJob(
                                new WatchFlashlightMonitorJob(
                                        new MonitorStatus(data.runId.getId())))
                                        .schedule();
                        EclipseUtility.toEclipseJob(
                                new ReadFlashlightStreamJob(data.runId.getId(),
                                        data.runDir, data.outputPort, id))
                                        .schedule();
                        // FIXME ReadLogcatJob doesn't work right now
                        // EclipseUtility.toEclipseJob(
                        // new ReadLogcatJob(data.runId, id)).schedule();
                        return Status.OK_STATUS;
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            if (timeout > 1) {
                new ConnectToProjectJob(data, pakkage, timeout - 1)
                .schedule(1000);
                return Status.OK_STATUS;
            } else {
                return SLEclipseStatusUtility.createInfoStatus(String.format(
                        "Could not locate the device hosting %s",
                        data.project.getName()));
            }
        }
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
    private String getActivityName(final ILaunchConfiguration configuration) {
        String empty = "";
        String activityName;
        try {
            activityName = configuration.getAttribute(ATTR_ACTIVITY, empty);
        } catch (CoreException e) {
            return null;
        }

        return activityName != empty ? activityName : null;
    }

}
