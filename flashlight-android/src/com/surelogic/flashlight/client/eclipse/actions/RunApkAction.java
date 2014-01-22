package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipFile;

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

import brut.apktool.Main;
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
import com.surelogic.flashlight.android.dex2jar.DexHelper.DexTool;
import com.surelogic.flashlight.android.jobs.ReadFlashlightStreamJob;
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

    File getManifest(File apk, File tmpDir) throws IOException,
            InterruptedException, BrutException {
        File output = new File(tmpDir, "decompressed-apk");
        Main.main(new String[] { "apktool", "d", "--no-src",
                apk.getAbsolutePath(), output.getAbsolutePath() });
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
                IFile apk = EclipseUtility.resolveIFile(apkFile
                        .getAbsolutePath());

                String apkName = apk.getName();
                int idx = apkName.indexOf('.');
                if (idx > 0) {
                    apkName = apkName.substring(0, idx);
                }
                String runId = apkName
                        + new SimpleDateFormat(
                                InstrumentationConstants.DATE_FORMAT)
                                .format(new Date())
                        + InstrumentationConstants.ANDROID_LAUNCH_SUFFIX;
                RunData data;
                try {
                    data = new RunData(runId);
                    try {
                        File outJar = new File(data.tmpDir, "out.jar");

                        File origJar = new File(data.tmpDir, "orig.jar");
                        File origDir = new File(data.tmpDir, "orig");
                        // Extract bytecode from package
                        DexHelper.extractJarFromApk(apkFile, origJar);
                        // Unzip the jar so we can add our instrumentation
                        FileUtility.unzipFile(origJar, origDir);
                        // Instrument the jar
                        DexRewriter dex = new DexRewriter(buildConfiguration(),
                                new PrintWriterMessenger(new PrintWriter(
                                        System.out)), data.fieldsFile,
                                data.sitesFile, data.classesFile, data.hbFile);
                        dex.addDirToDir(origDir, data.classesDir);
                        dex.addClasspathJar(new File(
                                "/home/nathan/java/android-sdk-linux/platforms/android-15/android.jar"));
                        Map<String, Map<String, Boolean>> execute = dex
                                .execute();
                        createInfoClasses(data);
                        FileUtility.zipDir(data.classesDir, outJar);
                        Sdk sdk = Sdk.getCurrent();
                        DexWrapper dexWrapper = sdk.getDexWrapper(sdk
                                .getLatestBuildTool());
                        File newApk = DexHelper.rewriteApkWithJar(
                                new DexToolWrapper(), apkFile,
                                getRuntimeJarPath(), outJar, data.runDir);

                        IAndroidTarget projectTarget = sdk.getTarget(info
                                .getSelectedProject());
                        ZipFile zf = new ZipFile(apkFile);
                        File manifestFile = new File(data.tmpDir,
                                "AndroidManifest.xml");
                        try {
                            FileUtility.copy("AndroidManifest.xml", zf
                                    .getInputStream(zf
                                            .getEntry("AndroidManifest.xml")),
                                    manifestFile);
                        } finally {
                            zf.close();
                        }

                        ManifestData manifestData = AndroidManifestHelper
                                .parseForData(EclipseUtility
                                        .resolveIFile(getManifest(apkFile,
                                                data.tmpDir).getAbsolutePath()));
                        AndroidVersion minApiVersion;
                        minApiVersion = new AndroidVersion(
                                manifestData.getMinSdkVersionString());
                        DeviceChooserResponse response = new DeviceChooserResponse();
                        DeviceChooserDialog dialog = new DeviceChooserDialog(
                                shell, response, "", projectTarget,
                                minApiVersion);
                        if (dialog.open() == Window.OK) {
                            IDevice device = launch(info.getSelectedProject(),
                                    response, newApk, manifestData);
                            data.device = device;
                            new ConnectToProjectJob(data).schedule();
                        }

                    } catch (AndroidVersionException e) {
                        throw new IllegalStateException(e);
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
        }
    }

    private IDevice launch(IProject project, DeviceChooserResponse response,
            File apk, ManifestData manifestData) {
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
        Activity launcherActivity = manifestData.getLauncherActivity();
        if (launcherActivity != null) {
            String activityName = launcherActivity.getName();
            ActivityLaunchAction action = new ActivityLaunchAction(
                    activityName, AndroidLaunchController.getInstance());
            DelayedLaunchInfo info = new DelayedLaunchInfo(project,
                    manifestData.getPackage(), manifestData.getPackage(),
                    action, EclipseUtility.resolveIFile(apk.getAbsolutePath()),
                    manifestData.getDebuggable(),
                    manifestData.getMinSdkVersionString(), null,
                    new NullProgressMonitor());
            action.doLaunchAction(info, device);
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

    class DexToolWrapper implements DexTool {
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
            PrintWriter writer = new PrintWriter(portFile);
            try {
                writer.println(consolePort);
            } finally {
                writer.close();
            }
            tmpDir = File.createTempFile("fl_tmp", "dir");
            tmpDir.delete();
            tmpDir.mkdir();
            classesDir = new File(tmpDir, "classes");
            classesDir.mkdir();
        }

        public void deleteTempFiles() {
            FileUtility.recursiveDelete(tmpDir);
        }

    }

    public static void main(String[] args) throws IOException,
            InterruptedException, BrutException {
        Main.main(new String[] { "apktool", "d",
                "/home/nathan/tmp/FlashlightTutorial_CounterRace.apk" });
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
            EclipseUtility.toEclipseJob(
                    new ReadFlashlightStreamJob(data.runId, data.runDir,
                            data.outputPort, data.device)).schedule();
            return Status.OK_STATUS;
        }
    }
}
