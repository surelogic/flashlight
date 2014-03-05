package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.progress.UIJob;

import brut.androlib.ApkDecoder;
import brut.common.BrutException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.TimeoutException;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.build.DexWrapper;
import com.android.ide.eclipse.adt.internal.launch.ActivityLaunchAction;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchConfiguration;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchConfiguration.TargetMode;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.internal.launch.DelayedLaunchInfo;
import com.android.ide.eclipse.adt.internal.launch.DeviceChooserDialog;
import com.android.ide.eclipse.adt.internal.launch.DeviceChooserDialog.DeviceChooserResponse;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.InstrumentationFileTranslator;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteManager.AlreadyInstrumentedException;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.android.dex.ApkSelectionInfo;
import com.surelogic.flashlight.android.dex.ApkSelectionWizard;
import com.surelogic.flashlight.android.dex2jar.DexHelper;
import com.surelogic.flashlight.android.dex2jar.DexHelper.DexTool;
import com.surelogic.flashlight.android.jobs.ReadFlashlightStreamJob;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.launch.LaunchUtils;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;

public class RunApkAction implements IWorkbenchWindowActionDelegate {

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
    private static final short ADECODE_SOURCES_NONE = 0x0000;

    static File getManifest(File apk, File tmpDir) throws IOException,
            InterruptedException, BrutException {
        File output = new File(tmpDir, "decompressed-apk");
        /* apktool d --no-src apk output */
        ApkDecoder decoder = new ApkDecoder();
        decoder.setDecodeSources(ADECODE_SOURCES_NONE);
        decoder.setApkFile(apk);
        decoder.setOutDir(output);
        decoder.decode();
        return new File(output, "AndroidManifest.xml");
    }

    @Override
    public void run(IAction action) {
        Shell shell = EclipseUIUtility.getShell();
        ApkSelectionInfo info = new ApkSelectionInfo(JDTUtility.getProjects());
        ApkSelectionWizard wizard = new ApkSelectionWizard(info);
        WizardDialog wd = new WizardDialog(shell, wizard);
        int flag = wd.open();
        if (info.isSelectionValid() && flag != SWT.CANCEL) {
            File apkFile = info.getApk();
            if (apkFile != null && apkFile.exists()) {
                new LaunchApkJob(info).schedule();
            }
        }
    }

    private static IDevice launch(IProject project,
            DeviceChooserResponse response, File apk, ManifestData manifestData) {
        // FIXME make this configurable
        AndroidLaunchConfiguration config = new AndroidLaunchConfiguration();
        config.mAvdName = null;
        config.mEmulatorCommandLine = "";
        config.mLaunchAction = 0;
        config.mNetworkDelay = "none";
        config.mNetworkSpeed = "full";
        config.mNoBootAnim = false;
        config.mTargetMode = TargetMode.AUTO;
        config.mWipeData = false;

        // Do launch
        AvdInfo avd = response.getAvdToLaunch();
        IDevice device = response.getDeviceToUse();
        if (avd != null) {
            ArrayList<String> list = new ArrayList<String>();
            String path = AdtPlugin.getOsAbsoluteEmulator();
            list.add(path);
            // TODO finish setting up emulator
        } else if (device != null) {
            try {
                device.installPackage(apk.getAbsolutePath(), true);
                Activity launcherActivity = manifestData.getLauncherActivity();
                if (launcherActivity != null) {
                    String activityName = launcherActivity.getName();
                    ActivityLaunchAction action = new ActivityLaunchAction(
                            activityName, AndroidLaunchController.getInstance());
                    DelayedLaunchInfo info = new DelayedLaunchInfo(project,
                            manifestData.getPackage(),
                            manifestData.getPackage(), action,
                            EclipseUtility.resolveIFile(apk.getAbsolutePath()),
                            manifestData.getDebuggable(),
                            manifestData.getMinSdkVersionString(), null,
                            new NullProgressMonitor());
                    action.doLaunchAction(info, device);
                }
                return device;
                // FIXME more clear exception handling
            } catch (InstallException e) {
                throw new RuntimeException(e);
            }
        }

        return device;

    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // Do Nothing
    }

    @Override
    public void dispose() {
        // Do Nothing
    }

    @Override
    public void init(IWorkbenchWindow window) {
        // Do Nothing
    }

    static class DexToolWrapper implements DexTool {
        private final DexWrapper dexWrapper;

        DexToolWrapper() {
            Sdk sdk = Sdk.getCurrent();
            dexWrapper = sdk.getDexWrapper(sdk.getLatestBuildTool());
        }

        @Override
        public int run(String osOutFilePath, Collection<String> osFilenames,
                boolean forceJumbo, boolean verbose, PrintStream outStream,
                PrintStream errStream) throws CoreException {
            return dexWrapper.run(osOutFilePath, osFilenames, forceJumbo,
                    verbose, outStream, errStream);
        }

    }

    static class DexRewriter extends RewriteManager {
        DexRewriter(Configuration c, RewriteMessenger m, File ff, File sf,
                File chf, File hbf) {
            super(c, m, ff, sf, chf, hbf);
        }

        @Override
        protected void exceptionScan(String srcPath, IOException e) {
            throw new RuntimeException(e);
        }

        @Override
        protected void exceptionInstrument(String srcPath, String destPath,
                IOException e) {
            throw new RuntimeException(e);
        }

        @Override
        protected void exceptionLoadingMethodsFile(JAXBException e) {
            throw new RuntimeException(e);
        }

        @Override
        protected void exceptionCreatingFieldsFile(File fieldsFile,
                FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        @Override
        protected void exceptionCreatingClassHierarchyFile(File fieldsFile,
                IOException e) {
            throw new RuntimeException(e);
        }

        @Override
        protected void exceptionCreatingSitesFile(File sitesFile, IOException e) {
            throw new RuntimeException(e);
        }

    }

    static Configuration buildConfiguration() {
        ConfigurationBuilder c = new ConfigurationBuilder();

        return c.getConfiguration();
    }

    public static RunData createInfoClasses(RunData data) throws IOException {
        File infoClassDest = new File(data.classesDir,
                InstrumentationConstants.FL_PROPERTIES_CLASS);
        infoClassDest.getParentFile().mkdirs();
        Properties props = new Properties();
        props.setProperty(InstrumentationConstants.FL_RUN, data.runName);
        props.setProperty(InstrumentationConstants.FL_ANDROID, "true");

        props.setProperty(InstrumentationConstants.FL_COLLECTION_TYPE,
                InstrumentationConstants.FL_COLLECTION_TYPE_DEFAULT.toString());
        props.setProperty(InstrumentationConstants.FL_CONSOLE_PORT, Integer
                .toString(InstrumentationConstants.FL_CONSOLE_PORT_DEFAULT));

        props.setProperty(InstrumentationConstants.FL_OUTPUT_PORT, Integer
                .toString(InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT));

        props.setProperty(InstrumentationConstants.FL_OUTQ_SIZE,
                Integer.toString(InstrumentationConstants.FL_OUTQ_SIZE_DEFAULT));
        props.setProperty(InstrumentationConstants.FL_RAWQ_SIZE,
                Integer.toString(InstrumentationConstants.FL_RAWQ_SIZE_DEFAULT));
        props.setProperty(InstrumentationConstants.FL_REFINERY_SIZE, Integer
                .toString(InstrumentationConstants.FL_REFINERY_SIZE_DEFAULT));
        props.setProperty(InstrumentationConstants.FL_POSTMORTEM,
                Boolean.toString(true));
        InstrumentationFileTranslator.writeProperties(props, infoClassDest);

        File fieldsClass = new File(data.classesDir,
                InstrumentationConstants.FL_FIELDS_CLASS);
        InstrumentationFileTranslator.writeFields(data.fieldsFile, fieldsClass);

        File sitesClass = new File(data.classesDir,
                InstrumentationConstants.FL_SITES_CLASS);
        InstrumentationFileTranslator.writeSites(data.sitesFile, sitesClass);
        return data;
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

    private static class RunData {
        IDevice device;
        private final int consolePort;
        private final int outputPort;
        public final String runId;
        private final File classesDir;
        private final File runDir;
        private final File sourceDir;
        private final File apkFolder;
        private final File fieldsFile;
        private final File log;
        private final File sitesFile;
        private final File classesFile;
        private final File hbFile;
        private final File portFile;
        private final File tmpDir;
        private final File decompiledJarFile;
        private final String runName;

        RunData(String runName, String runId) throws IOException {

            consolePort = InstrumentationConstants.FL_CONSOLE_PORT_DEFAULT;
            outputPort = InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT;
            this.runName = runName;
            this.runId = runId;
            runDir = new File(EclipseUtility.getFlashlightDataDirectory(),
                    runId);
            runDir.mkdir();
            sourceDir = new File(runDir,
                    InstrumentationConstants.FL_SOURCE_FOLDER_LOC);
            sourceDir.mkdirs();
            apkFolder = new File(runDir,
                    InstrumentationConstants.FL_APK_FOLDER_LOC);
            apkFolder.mkdirs();
            decompiledJarFile = new File(runDir,
                    InstrumentationConstants.FL_DECOMPILED_JAR_LOC);
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
                writer.println(consolePort);
            } finally {
                writer.close();
            }
            tmpDir = File.createTempFile("fl_tmp", "dir");
            tmpDir.delete();
            tmpDir.mkdir();
            classesDir = new File(runDir,
                    InstrumentationConstants.FL_PROJECTS_FOLDER_LOC);
            classesDir.mkdirs();
        }

        public void deleteTempFiles() {
            FileUtility.recursiveDelete(tmpDir);
        }

    }

    private static class LaunchApkJob extends Job {
        ApkSelectionInfo info;

        public LaunchApkJob(ApkSelectionInfo info) {
            super("Launching apk");
            this.info = info;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            File apkFile = info.getApk();
            if (apkFile != null && apkFile.exists()) {
                IFile apk = EclipseUtility.resolveIFile(apkFile
                        .getAbsolutePath());

                String apkName = apk.getName();
                int idx = apkName.lastIndexOf('.');
                if (idx > 0) {
                    apkName = apkName.substring(0, idx);
                }
                String runId = apkName
                        + new SimpleDateFormat(
                                InstrumentationConstants.DATE_FORMAT)
                                .format(new Date())
                        + InstrumentationConstants.ANDROID_LAUNCH_SUFFIX;
                RunManager.getInstance()
                        .notifyPerformingInstrumentationAndLaunch(runId);
                final RunData data;
                try {
                    data = new RunData(apkName, runId);
                    try {
                        Sdk sdk = Sdk.getCurrent();
                        // Determine goal project target platform
                        IAndroidTarget projectTarget = sdk.getTarget(info
                                .getSelectedProject());
                        final ManifestData manifestData = AndroidManifestHelper
                                .parseForData(getManifest(apkFile, data.tmpDir)
                                        .getAbsolutePath());
                        if (projectTarget == null) {
                            int targetVersion = manifestData
                                    .getTargetSdkVersion();
                            for (IAndroidTarget t : sdk.getTargets()) {
                                if (t.getVersion().getApiLevel() == targetVersion) {
                                    projectTarget = t;
                                }
                            }
                        }
                        if (projectTarget == null) {
                            int minVersion = manifestData.getMinSdkVersion();
                            for (IAndroidTarget t : sdk.getTargets()) {
                                if (t.getVersion().getApiLevel() == minVersion) {
                                    projectTarget = t;
                                }
                            }
                        }
                        if (projectTarget == null
                                && sdk.getTargets().length > 0) {
                            projectTarget = sdk.getTargets()[0];
                        }
                        final IAndroidTarget targetPlatform = projectTarget;

                        File outJar = new File(data.tmpDir, "out.jar");

                        File origDir = new File(data.tmpDir, "orig");
                        // Extract bytecode from package
                        DexHelper.extractJarFromApk(apkFile,
                                data.decompiledJarFile);
                        // Unzip the jar so we can add our instrumentation
                        FileUtility.unzipFile(data.decompiledJarFile, origDir);
                        // Instrument the jar
                        DexRewriter dex = new DexRewriter(buildConfiguration(),
                                new PrintWriterMessenger(new PrintWriter(
                                        data.log)), data.fieldsFile,
                                data.sitesFile, data.classesFile, data.hbFile);
                        dex.addDirToDir(origDir, data.classesDir);
                        for (String path : targetPlatform.getBootClasspath()) {
                            dex.addClasspathJar(new File(path));
                        }

                        Map<String, Map<String, Boolean>> execute = dex
                                .execute();
                        // Set up source information if we have any
                        if (info.getSelectedProject() != null) {
                            LaunchUtils.createSourceZips(null, Collections
                                    .singleton(info.getSelectedProject()),
                                    data.sourceDir, null);
                        }
                        // Create instrumentation data
                        createInfoClasses(data);
                        FileUtility.zipDir(data.classesDir, outJar);
                        DexWrapper dexWrapper = sdk.getDexWrapper(sdk
                                .getLatestBuildTool());
                        // Build final Apk
                        final File newApk = DexHelper.rewriteApkWithJar(
                                new DexToolWrapper(), apkFile,
                                getRuntimeJarPath(), outJar, data.runDir);

                        // Launch Apk
                        final AndroidVersion minApiVersion = new AndroidVersion(
                                manifestData.getMinSdkVersion(),
                                manifestData.getMinSdkVersionString());
                        final DeviceChooserResponse response = new DeviceChooserResponse();
                        UIJob job = new UIJob("Choose a device") {

                            @Override
                            public IStatus runInUIThread(
                                    IProgressMonitor monitor) {
                                Shell shell = EclipseUIUtility.getShell();

                                DeviceChooserDialog dialog = new DeviceChooserDialog(
                                        shell, response, "", targetPlatform,
                                        minApiVersion, false);
                                if (dialog.open() == Window.OK) {
                                    IDevice device = launch(
                                            info.getSelectedProject(),
                                            response, newApk, manifestData);
                                    data.device = device;
                                    new ConnectToProjectJob(data).schedule();
                                }
                                return Status.OK_STATUS;
                            }
                        };
                        job.schedule();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    } catch (AlreadyInstrumentedException e) {
                        throw new IllegalStateException(e);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    } catch (BrutException e) {
                        throw new IllegalStateException(e);
                    } catch (CoreException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        data.deleteTempFiles();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            return Status.OK_STATUS;
        }

    }

    private static final class ConnectToProjectJob extends Job {

        private final RunData data;

        ConnectToProjectJob(RunData data) {
            super("Setting up forward for Flashlight Monitor");
            this.data = data;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                data.device.createForward(data.consolePort, data.consolePort);
                data.device.createForward(data.outputPort, data.outputPort);
            } catch (TimeoutException e) {
                return SLEclipseStatusUtility.createErrorStatus(318, e);
            } catch (AdbCommandRejectedException e) {
                return SLEclipseStatusUtility.createErrorStatus(318, e);
            } catch (IOException e) {
                return SLEclipseStatusUtility.createErrorStatus(318, e);
            }
            EclipseUtility
                    .toEclipseJob(
                            new WatchFlashlightMonitorJob(new MonitorStatus(
                                    data.runId))).schedule();
            EclipseUtility.toEclipseJob(
                    new ReadFlashlightStreamJob(data.runId, data.runDir,
                            data.outputPort, data.device)).schedule();
            return Status.OK_STATUS;
        }
    }

}
