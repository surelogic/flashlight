package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.TimeoutException;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchConfiguration;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchConfiguration.TargetMode;
import com.android.ide.eclipse.adt.internal.launch.DeviceChooserDialog;
import com.android.ide.eclipse.adt.internal.launch.DeviceChooserDialog.DeviceChooserResponse;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.AndroidVersion.AndroidVersionException;
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
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
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

    @Override
    public void run(IAction action) {
        Shell shell = EclipseUIUtility.getShell();
        ApkSelectionInfo info = new ApkSelectionInfo(JDTUtility.getProjects());
        ApkSelectionWizard wizard = new ApkSelectionWizard(info);
        WizardDialog wd = new WizardDialog(shell, wizard);
        int flag = wd.open();
        if (info.isSelectionValid() && flag != SWT.CANCEL) {
            IProject project = info.getSelectedProject();
            File apkFile = info.getApk();
            if (project != null && apkFile != null && apkFile.exists()) {
                IFile apk = EclipseUtility.resolveIFile(apkFile
                        .getAbsolutePath());
                String runId = apk.getName()
                        + new SimpleDateFormat(
                                InstrumentationConstants.DATE_FORMAT)
                                .format(new Date())
                        + InstrumentationConstants.ANDROID_LAUNCH_SUFFIX;
                RunData data;
                try {
                    data = new RunData(runId);
                    try {
                        File outDir = new File(data.infoDir, "out");
                        File outJar = new File(data.infoDir, "out.jar");

                        File origJar = new File(data.infoDir, "orig.jar");
                        File origDir = new File(data.infoDir, "orig");
                        // Extract bytecode from package
                        DexHelper.extractJarFromApk(apkFile, origJar);
                        // Unzip the jar so we can add our instrumentation
                        FileUtility.unzipFile(origJar, origDir);
                        // Instrument the jar
                        DexRewriter dex = new DexRewriter(buildConfiguration(),
                                new PrintWriterMessenger(new PrintWriter(
                                        System.out)), data.fieldsFile,
                                data.sitesFile, data.classesFile, data.hbFile);
                        dex.addDirToDir(origDir, outDir);
                        dex.addClasspathJar(new File(
                                "/home/nathan/java/android-sdk-linux/platforms/android-15/android.jar"));
                        Map<String, Map<String, Boolean>> execute = dex
                                .execute();

                        createInfoClasses(data);
                        FileUtility.zipDir(outDir, outJar);
                        File newApk = DexHelper.rewriteApkWithJar(apkFile,
                                getRuntimeJarPath(), outJar, data.runDir);

                        Sdk sdk = Sdk.getCurrent();
                        IAndroidTarget projectTarget = sdk.getTarget(info
                                .getSelectedProject());
                        ManifestData manifestData = AndroidManifestHelper
                                .parseForData(project);

                        AndroidVersion minApiVersion;
                        minApiVersion = new AndroidVersion(
                                manifestData.getMinSdkVersionString());
                        DeviceChooserResponse response = new DeviceChooserResponse();
                        DeviceChooserDialog dialog = new DeviceChooserDialog(
                                shell, response, "", projectTarget,
                                minApiVersion);
                        if (dialog.open() == Window.OK) {
                            IDevice device = launch(response, newApk);
                            data.device = device;
                            new ConnectToProjectJob(data).schedule();
                        }
                        //
                        //
                        //
                        //
                        //
                        /*
                         * AndroidLaunchController controller =
                         * AndroidLaunchController .getInstance();
                         * AndroidLaunchConfiguration config = new
                         * AndroidLaunchConfiguration();
                         * 
                         * AndroidLaunch launch = new AndroidLaunch(null, "run",
                         * null);
                         * 
                         * manifestData.getLauncherActivity();
                         * IAndroidLaunchAction launchAction = new
                         * EmptyLaunchAction();
                         * 
                         * new DelayedLaunchInfo(project,
                         * manifestData.getPackage(), manifestData.getPackage(),
                         * launchAction, apk, manifestData.getDebuggable(),
                         * manifestData.getMinSdkVersionString(), launch, new
                         * NullProgressMonitor());
                         * 
                         * 
                         * controller.launch(project, ILaunchManager.RUN_MODE
                         * .toString(), EclipseUtility.resolveIFile(info
                         * .getApk().toString()), manifestData.getPackage(),
                         * manifestData.getPackage(), manifestData
                         * .getDebuggable(), manifestData
                         * .getMinSdkVersionString(), new EmptyLaunchAction(),
                         * config, launch, null);
                         */
                    } catch (AndroidVersionException e) {
                        throw new IllegalStateException(e);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    } catch (AlreadyInstrumentedException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        data.deleteTempFiles();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private IDevice launch(DeviceChooserResponse response, File apk) {
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
                return device;
                // FIXME more clear exception handling
            } catch (InstallException e) {
                throw new RuntimeException(e);
            }
        }
        return null;

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
        File infoClassDest = new File(data.infoDir,
                InstrumentationConstants.FL_PROPERTIES_CLASS);
        infoClassDest.getParentFile().mkdirs();
        Properties props = new Properties();
        props.setProperty(InstrumentationConstants.FL_RUN, data.runId);
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

        File fieldsClass = new File(data.infoDir,
                InstrumentationConstants.FL_FIELDS_CLASS);
        InstrumentationFileTranslator.writeFields(data.fieldsFile, fieldsClass);

        File sitesClass = new File(data.infoDir,
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
        private final File runDir;
        private final File sourceDir;
        private final File apkFolder;
        private final File fieldsFile;
        private final File log;
        private final File sitesFile;
        private final File classesFile;
        private final File hbFile;
        private final File portFile;
        private final File infoDir;

        RunData(String runId) throws IOException {
            consolePort = InstrumentationConstants.FL_CONSOLE_PORT_DEFAULT;
            outputPort = InstrumentationConstants.FL_OUTPUT_PORT_DEFAULT;
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
            infoDir = File.createTempFile("fl_info_", "dir");
            infoDir.delete();
        }

        public void deleteTempFiles() {
            FileUtility.recursiveDelete(infoDir);
        }

    }

    private static final class ConnectToProjectJob extends Job {

        private final RunData data;

        ConnectToProjectJob(RunData data) {
            super("Setting up forward for Flashlight Monitor");
            this.data = data;
        }

        @SuppressWarnings("restriction")
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                data.device.createForward(data.consolePort, data.consolePort);
                data.device.createForward(data.outputPort, data.outputPort);
            } catch (TimeoutException e) {
                return SLEclipseStatusUtility.createErrorStatus(310, e);
            } catch (AdbCommandRejectedException e) {
                return SLEclipseStatusUtility.createErrorStatus(310, e);
            } catch (IOException e) {
                return SLEclipseStatusUtility.createErrorStatus(310, e);
            }
            EclipseUtility
                    .toEclipseJob(
                            new WatchFlashlightMonitorJob(new MonitorStatus(
                                    data.runId))).schedule();
            // FIXME ReadLogcatJob doesn't work right now
            // EclipseUtility.toEclipseJob(
            // new ReadLogcatJob(data.runId, id)).schedule();
            return Status.OK_STATUS;
        }
    }
}
