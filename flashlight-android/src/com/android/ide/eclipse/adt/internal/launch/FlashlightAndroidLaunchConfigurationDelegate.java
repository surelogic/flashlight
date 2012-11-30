package com.android.ide.eclipse.adt.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.android.SdkConstants;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;
import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidPrintStream;
import com.android.ide.eclipse.adt.internal.build.AaptExecException;
import com.android.ide.eclipse.adt.internal.build.AaptParser;
import com.android.ide.eclipse.adt.internal.build.AaptResultException;
import com.android.ide.eclipse.adt.internal.build.BuildHelper;
import com.android.ide.eclipse.adt.internal.build.BuildHelper.ResourceMarker;
import com.android.ide.eclipse.adt.internal.build.DexException;
import com.android.ide.eclipse.adt.internal.build.Messages;
import com.android.ide.eclipse.adt.internal.build.NativeLibInJarException;
import com.android.ide.eclipse.adt.internal.build.builders.PostCompilerBuilder;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs.BuildVerbosity;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.project.ApkInstallManager;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.sdk.ProjectState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.internal.build.DebugKeyProvider.KeytoolException;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.InstrumentationFileTranslator;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteManager.AlreadyInstrumentedException;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.dialogs.ShowTextDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper.RuntimeConfig;
import com.surelogic.flashlight.client.eclipse.launch.LaunchUtils;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;
import com.surelogic.flashlight.eclipse.client.jobs.ReadFlashlightStreamJob;

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

    Logger log = SLLogger
            .getLoggerFor(FlashlightAndroidLaunchConfigurationDelegate.class);

    @SuppressWarnings("restriction")
    @Override
    public void launch(final ILaunchConfiguration configuration,
            final String mode, final ILaunch launch,
            final IProgressMonitor monitor) throws CoreException {
        IProject project = EclipseUtility
                .getProject(configuration
                        .getAttribute(
                                IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                                ""));
        AndroidLaunch androidLaunch = (AndroidLaunch) launch;
        FLData data = doFullIncrementalDebugBuild(configuration, project,
                monitor);

        // if we have a valid debug port, this means we're debugging an app
        // that's already launched.
        // int debugPort =
        // AndroidLaunchController.getPortForConfig(configuration);
        // if (debugPort != INVALID_DEBUG_PORT) {
        // AndroidLaunchController.launchRemoteDebugger(debugPort,
        // androidLaunch, monitor);
        // return;
        // }

        if (project == null) {
            AdtPlugin.printErrorToConsole("Couldn't get project object!");
            androidLaunch.stopLaunch();
            return;
        }

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

        final Job job = new ConnectToProjectJob(data, manifestData.getPackage());
        job.schedule();
    }

    @SuppressWarnings("restriction")
    private FLData doFullIncrementalDebugBuild(
            final ILaunchConfiguration launchConfig, final IProject project,
            final IProgressMonitor monitor) throws CoreException {
        // First have android do their full build
        ProjectHelper.doFullIncrementalDebugBuild(project, monitor);

        // Get list of projects that we depend on
        List<IJavaProject> androidProjectList = new ArrayList<IJavaProject>();
        try {
            androidProjectList = ProjectHelper
                    .getAndroidProjectDependencies(BaseProjectHelper
                            .getJavaProject(project));
        } catch (JavaModelException e) {
            AdtPlugin.printErrorToConsole(project, e);
        }
        // Recursively build dependencies
        for (IJavaProject dependency : androidProjectList) {
            ProjectHelper.doFullIncrementalDebugBuild(dependency.getProject(),
                    monitor);
        }

        // Do an incremental build to pick up all the deltas
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

        // If the preferences indicate not to use post compiler optimization
        // then the incremental build will have done everything necessary,
        // otherwise,
        // we have to run the final builder manually (if requested).
        if (AdtPrefs.getPrefs().getBuildSkipPostCompileOnFileSave()) {
            // Create the map to pass to the PostC builder
            Map<String, String> args = new TreeMap<String, String>();
            args.put(PostCompilerBuilder.POST_C_REQUESTED, ""); //$NON-NLS-1$

            // call the post compiler manually, forcing FULL_BUILD otherwise
            // Eclipse won't
            // call the builder since the delta is empty.
            project.build(IncrementalProjectBuilder.FULL_BUILD,
                    PostCompilerBuilder.ID, args, monitor);
        }

        // because the post compiler builder does a delayed refresh due to
        // library not picking the refresh up if it's done during the build,
        // we want to force a refresh here as this call is generally asking for
        // a build to use the apk right after the call.
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

        // list of referenced projects. This is a mix of java projects and
        // library projects
        // and is computed below.
        IProject[] allRefProjects = null;

        ProjectState projectState = Sdk.getProjectState(project);

        if (projectState == null) {
            return null;
        }
        boolean isLibrary = projectState.isLibrary();

        List<IProject> libProjects = projectState.getFullLibraryProjects();

        IJavaProject javaProject = JavaCore.create(project);

        // get the list of referenced projects.
        // get the list of referenced projects.
        List<IProject> javaProjects = ProjectHelper
                .getReferencedProjects(project);
        List<IJavaProject> referencedJavaProjects = BuildHelper
                .getJavaProjects(javaProjects);

        // mix the java project and the library projecst
        final int size = libProjects.size() + javaProjects.size();
        ArrayList<IProject> refList = new ArrayList<IProject>(size);
        refList.addAll(libProjects);
        refList.addAll(javaProjects);
        allRefProjects = refList.toArray(new IProject[size]);

        // get the android output folder
        IFolder androidOutputFolder = BaseProjectHelper
                .getAndroidOutputFolder(project);
        IFolder resOutputFolder = androidOutputFolder
                .getFolder(SdkConstants.FD_RES);

        // now we need to get the classpath list
        List<IPath> sourceList = BaseProjectHelper
                .getSourceClasspaths(javaProject);

        AndroidPrintStream mOutStream = new AndroidPrintStream(project,
                null /* prefix */, AdtPlugin.getOutStream());
        AndroidPrintStream mErrStream = new AndroidPrintStream(project,
                null /* prefix */, AdtPlugin.getOutStream());

        ResourceMarker mResourceMarker = new ResourceMarker() {
            @Override
            public void setWarning(final IResource resource,
                    final String message) {
                BaseProjectHelper.markResource(resource,
                        AdtConstants.MARKER_PACKAGING, message,
                        IMarker.SEVERITY_WARNING);
            }
        };

        BuildHelper helper = new BuildHelper(
                project,
                mOutStream,
                mErrStream,
                false /* jumbo mode doesn't matter here */,
                true /* debugMode */,
                AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE,
                mResourceMarker);

        IPath androidBinLocation = androidOutputFolder.getLocation();
        String osAndroidBinPath = androidBinLocation.toOSString();

        String classesDexPath = osAndroidBinPath + File.separator
                + SdkConstants.FN_APK_CLASSES_DEX;

        String finalPackageName = ProjectHelper
                .getApkFilename(project, null /* config */);
        String osFinalPackagePath = osAndroidBinPath + File.separator
                + finalPackageName;
        // Delete old APK
        new File(osFinalPackagePath).delete();

        Collection<String> dxInputPaths = helper.getCompiledCodePaths();
        // Now we instrument all of the code in dxInputPaths and replace it with
        // ours.
        IFile manifestFile = project
                .getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
        FLData data = null;
        try {
            data = instrumentClasses(launchConfig, project, dxInputPaths);
            LaunchUtils.createSourceZips(null, data.allProjects,
                    data.sourceDir, null);
            try {
                dxInputPaths = data.getClasspathEntries();
                helper.packageResources(manifestFile, libProjects, null, 0,
                        osAndroidBinPath, AdtConstants.FN_RESOURCES_AP_);

                helper.executeDx(javaProject, dxInputPaths, classesDexPath);

                helper.finalDebugPackage(osAndroidBinPath + File.separator
                        + AdtConstants.FN_RESOURCES_AP_, classesDexPath,
                        osFinalPackagePath, libProjects, mResourceMarker);
            } finally {
                data.deleteTempFiles();
            }
        } catch (DexException e) {
            String message = e.getMessage();

            AdtPlugin.printErrorToConsole(project, message);
            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, message,
                    IMarker.SEVERITY_ERROR);

            Throwable cause = e.getCause();

            if (cause instanceof NoClassDefFoundError
                    || cause instanceof NoSuchMethodError) {
                AdtPlugin.printErrorToConsole(project,
                        Messages.Incompatible_VM_Warning,
                        Messages.Requires_1_5_Error);
            }
            return null;

        } catch (AaptResultException e) {
            // attempt to parse the error output
            String[] aaptOutput = e.getOutput();
            boolean parsingError = AaptParser.parseOutput(aaptOutput, project);

            // if we couldn't parse the output we display it in the console.
            if (parsingError) {
                AdtPlugin.printErrorToConsole(project, (Object[]) aaptOutput);

                // if the exec failed, and we couldn't parse the error output
                // (and
                // therefore not all files that should have been marked, were
                // marked),
                // we put a generic marker on the project and abort.
                BaseProjectHelper.markResource(project,
                        AdtConstants.MARKER_PACKAGING,
                        Messages.Unparsed_AAPT_Errors, IMarker.SEVERITY_ERROR);
            }
            return null;
        } catch (AaptExecException e) {
            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, e.getMessage(),
                    IMarker.SEVERITY_ERROR);
            return null;
        } catch (KeytoolException e) {
            String eMessage = e.getMessage();

            // mark the project with the standard message
            String msg = String
                    .format(Messages.Final_Archive_Error_s, eMessage);
            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);

            // output more info in the console
            AdtPlugin.printErrorToConsole(
                    project,
                    msg,
                    String.format(Messages.ApkBuilder_JAVA_HOME_is_s,
                            e.getJavaHome()),
                    Messages.ApkBuilder_Update_or_Execute_manually_s,
                    e.getCommandLine());

            return null;
        } catch (ApkCreationException e) {
            String eMessage = e.getMessage();

            // mark the project with the standard message
            String msg = String
                    .format(Messages.Final_Archive_Error_s, eMessage);
            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
        } catch (AndroidLocationException e) {
            String eMessage = e.getMessage();

            // mark the project with the standard message
            String msg = String
                    .format(Messages.Final_Archive_Error_s, eMessage);
            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
        } catch (NativeLibInJarException e) {
            String msg = e.getMessage();

            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);

            AdtPlugin.printErrorToConsole(project,
                    (Object[]) e.getAdditionalInfo());
        } catch (CoreException e) {
            // mark project and return
            String msg = String.format(Messages.Final_Archive_Error_s,
                    e.getMessage());
            AdtPlugin.printErrorToConsole(project, msg);
            BaseProjectHelper.markResource(project,
                    AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
        } catch (DuplicateFileException e) {
            String msg1 = String
                    .format("Found duplicate file for APK: %1$s\nOrigin 1: %2$s\nOrigin 2: %3$s",
                            e.getArchivePath(), e.getFile1(), e.getFile2());
            String msg2 = String.format(Messages.Final_Archive_Error_s, msg1);
            AdtPlugin.printErrorToConsole(project, msg2);
            BaseProjectHelper
                    .markResource(project, AdtConstants.MARKER_PACKAGING, msg2,
                            IMarker.SEVERITY_ERROR);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // we are done.

        // refresh the bin folder content with no recursion.
        androidOutputFolder.refreshLocal(IResource.DEPTH_ONE, monitor);

        // reset the installation manager to force new installs of this project
        ApkInstallManager.getInstance().resetInstallationFor(project);

        return data;
    }

    private static class FLData {

        final IProject project;
        final Set<IProject> allProjects;
        final File log;
        final File fieldsFile;
        final File sitesFile;
        final File runDir;
        final File infoDir;
        final File completeFile;
        final File portFile;
        final File sourceDir;
        final Date time;
        final String projectName;
        final String runName;
        final List<String> originalClasspaths;
        final List<String> classpaths;
        final int outputPort;
        final RuntimeConfig conf;

        FLData(final ILaunchConfiguration launch, final IProject project,
                final Collection<String> dxInputPaths) throws IOException,
                CoreException {
            conf = LaunchHelper.getRuntimeConfig(launch);
            this.project = project;
            outputPort = InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT;
            time = new Date();
            originalClasspaths = new ArrayList<String>(dxInputPaths);
            classpaths = new ArrayList<String>(originalClasspaths.size());
            for (int i = 0; i < originalClasspaths.size(); i++) {
                File tmpFile;
                if (new File(originalClasspaths.get(i)).isDirectory()) {
                    tmpFile = File.createTempFile("fl_", "class");
                    tmpFile.delete();
                    tmpFile.mkdir();
                } else {
                    tmpFile = File.createTempFile("fl_classes_", ".jar");
                }

                classpaths.add(tmpFile.getAbsolutePath());
            }
            allProjects = new HashSet<IProject>();
            allProjects.add(project);
            for (final IProject p : ResourcesPlugin.getWorkspace().getRoot()
                    .getProjects()) {
                if (originalClasspaths.contains(p.getLocation().toOSString())) {
                    allProjects.add(p);
                }
            }
            SimpleDateFormat df = new SimpleDateFormat(
                    InstrumentationConstants.DATE_FORMAT);
            projectName = project.getName();
            runName = projectName + df.format(time);
            runDir = new File(
                    EclipseUtility.getFlashlightDataDirectory(),
                    runName);
            runDir.mkdir();
            sourceDir = new File(runDir, "source");
            sourceDir.mkdir();
            fieldsFile = new File(runDir,
                    InstrumentationConstants.FL_FIELDS_FILE_NAME);
            log = new File(runDir, InstrumentationConstants.FL_LOG_FILE_NAME);
            sitesFile = new File(runDir,
                    InstrumentationConstants.FL_SITES_FILE_NAME);
            completeFile = new File(runDir,
                    InstrumentationConstants.FL_COMPLETE_RUN);
            portFile = new File(runDir,
                    InstrumentationConstants.FL_PORT_FILE_NAME);
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
            list.add(getRuntimeJarPath());
            return list;
        }

        public void createInfoClasses() throws IOException {
            File infoClassDest = new File(infoDir,
                    InstrumentationConstants.FL_PROPERTIES_CLASS);
            infoClassDest.getParentFile().mkdirs();
            Properties props = new Properties();
            props.setProperty(InstrumentationConstants.FL_RUN, runName);

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
            for (String p : classpaths) {
                FileUtility.recursiveDelete(new File(p));
            }
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

    @SuppressWarnings("unchecked")
    private FLData instrumentClasses(final ILaunchConfiguration launchConfig,
            final IProject project, final Collection<String> dxInputPaths)
            throws IOException, CoreException {
        ConfigurationBuilder configBuilder = LaunchHelper
                .buildConfigurationFromPreferences(launchConfig);

        /* Get the entries that the user does not want instrumented */
        final List<String> noInstrumentUser = launchConfig
                .getAttribute(
                        FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
                        Collections.emptyList());
        final List<String> noInstrumentBoot = launchConfig
                .getAttribute(
                        FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
                        Collections.emptyList());

        FLData data = new FLData(launchConfig, project, dxInputPaths);

        RewriteManager rm = new AndroidRewriteManager(
                configBuilder.getConfiguration(), new PrintWriterMessenger(
                        new PrintWriter(data.log)), data.fieldsFile,
                data.sitesFile);
        String runtimePath = getRuntimeJarPath();
        List<String> instrumentLast = LaunchHelper
                .sanitizeInstrumentationList(data.originalClasspaths);
        List<Integer> toInstrument = new ArrayList<Integer>();
        for (int i = 0; i < data.originalClasspaths.size(); i++) {
            String fromPath = data.originalClasspaths.get(i);
            File from = new File(fromPath);
            File to = new File(data.classpaths.get(i));
            boolean ignore = noInstrumentBoot.contains(fromPath)
                    || noInstrumentUser.contains(fromPath);
            if (!ignore && instrumentLast.contains(fromPath)) {
                toInstrument.add(i);
            }
            if (from.isDirectory()) {
                if (ignore) {
                    rm.addClasspathDir(from);
                } else {
                    rm.addDirToDir(from, to);
                }
            } else if (from.exists()) {
                if (ignore) {
                    rm.addClasspathJar(from);
                } else {
                    rm.addJarToJar(from, to, runtimePath);
                }
            } else {
                log.warning(from.getAbsolutePath().toString()
                        + " could not be found on the classpath could not be found when trying to instrument this project with Flashlight.");
            }
        }
        for (int i : toInstrument) {
            String fromPath = data.originalClasspaths.get(i);
            File from = new File(fromPath);
            File to = new File(data.classpaths.get(i));
            if (from.isDirectory()) {
                rm.addDirToDir(from, to);
            } else if (from.exists()) {
                rm.addJarToJar(from, to, runtimePath);
            } else {
                log.warning(from.getAbsolutePath().toString()
                        + " could not be found on the classpath could not be found when trying to instrument this project with Flashlight.");
            }
        }
        // We check the classpath to see if there are any entries that aren't
        // being exported, but that we will need in order to fully instrument
        // the project.
        IJavaProject javaProject = JavaCore.create(project);
        for (IClasspathEntry cpe : javaProject.getResolvedClasspath(true)) {
            if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                String path = cpe.getPath().toOSString();
                if (!data.originalClasspaths.contains(path)) {
                    File pathFile = new File(path);
                    if (pathFile.isDirectory()) {
                        rm.addClasspathDir(pathFile);
                    } else if (pathFile.exists()) {
                        rm.addClasspathJar(pathFile);
                    }
                }
            }
        }
        try {
            rm.execute();
        } catch (AlreadyInstrumentedException e) {
            final StringWriter s = new StringWriter();
            PrintWriter w = new PrintWriter(s);
            w.println("Instrumentation and execution were aborted because classes were found that have already been instrumented:");
            for (final String cname : e.getClasses()) {
                w.print("  ");
                w.println(cname);
            }
            w.println();
            w.println("Flashlight cannot collect meaningful data under these circumstances.");
            w.flush();

            final SLUIJob job = new SLUIJob() {
                final String message = s.toString();

                @Override
                public IStatus runInUIThread(final IProgressMonitor monitor) {
                    ShowTextDialog.showText(getDisplay().getActiveShell(),
                            "Instrumentation aborted.", message);
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
            return null;
        }
        data.createInfoClasses();
        return data;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws CoreException
     */
    @SuppressWarnings("restriction")
    @Override
    public ILaunch getLaunch(final ILaunchConfiguration configuration,
            final String mode) throws CoreException {
        return new AndroidLaunch(configuration, mode, null);
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
                        EclipseJob.getInstance().schedule(
                                new WatchFlashlightMonitorJob(
                                        new MonitorStatus(data.runName,
                                                data.time.toString(),
                                                data.fieldsFile, data.portFile,
                                                data.completeFile)));
                        EclipseJob.getInstance().schedule(
                                new ReadFlashlightStreamJob(data.runName,
                                        data.runDir, data.outputPort, id));
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
