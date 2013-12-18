package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.android.ide.common.xml.ManifestData;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.internal.launch.EmptyLaunchAction;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
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
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.android.dex.ApkSelectionInfo;
import com.surelogic.flashlight.android.dex.ApkSelectionWizard;
import com.surelogic.flashlight.android.dex2jar.DexHelper;

public class RunApkAction implements IWorkbenchWindowActionDelegate {

    @Override
    public void run(IAction action) {
        Shell shell = EclipseUIUtility.getShell();
        ApkSelectionInfo info = new ApkSelectionInfo(JDTUtility.getProjects());
        ApkSelectionWizard wizard = new ApkSelectionWizard(info);
        WizardDialog wd = new WizardDialog(shell, wizard);
        if (wd.open() == SWT.OK) {
            File f = info.getApk();
            if (f != null && f.exists()) {
                File tmpDir = null;
                try {
                    tmpDir = File.createTempFile("flashligh", "dir");
                    tmpDir.delete();
                    tmpDir.mkdir();
                    File outDir = new File(tmpDir, "out");
                    File outJar = new File(tmpDir, "out.jar");

                    File fieldsFile = new File(tmpDir, "fields.txt");
                    File sitesFile = new File(tmpDir, "sites.txt.gz");

                    File origJar = new File(tmpDir, "orig.jar");
                    File origDir = new File(tmpDir, "orig");
                    // Extract bytecode from package
                    DexHelper.extractJarFromApk(f, origJar);
                    // Unzip the jar so we can add our instrumentation
                    FileUtility.unzipFile(origJar, origDir);
                    // Instrument the jar
                    DexRewriter dex = new DexRewriter(buildConfiguration(),
                            new PrintWriterMessenger(
                                    new PrintWriter(System.out)), fieldsFile,
                            sitesFile, new File(tmpDir,
                                    "class_hierarchy.txt.gz"), new File(tmpDir,
                                    "happens-before-config.xml"));
                    dex.addDirToDir(origDir, outDir);
                    dex.addClasspathJar(new File(
                            "/home/nathan/java/android-sdk-linux/platforms/android-15/android.jar"));
                    Map<String, Map<String, Boolean>> execute = dex.execute();
                    String runId = f.getName()
                            + new SimpleDateFormat(
                                    InstrumentationConstants.DATE_FORMAT)
                                    .format(new Date())
                            + InstrumentationConstants.ANDROID_LAUNCH_SUFFIX;
                    createInfoClasses(runId, outDir, fieldsFile, sitesFile);
                    FileUtility.zipDir(outDir, outJar);
                    AndroidLaunchController controller = AndroidLaunchController
                            .getInstance();
                    IProject project = info.getSelectedProject();

                    ManifestData manifestData = AndroidManifestHelper
                            .parseForData(project);
                    controller.launch(project, ILaunchManager.RUN_MODE
                            .toString(), EclipseUtility.resolveIFile(info
                            .getApk().toString()), manifestData.getPackage(),
                            manifestData.getPackage(), manifestData
                                    .getDebuggable(), manifestData
                                    .getMinSdkVersionString(),
                            new EmptyLaunchAction(), null, null, null);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } catch (AlreadyInstrumentedException e) {
                    throw new IllegalStateException(e);
                } finally {
                    if (tmpDir != null) {
                        FileUtility.recursiveDelete(tmpDir);
                    }
                }
            }
        }
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

    public static void createInfoClasses(String runId, File infoDir,
            File fieldsFile, File sitesFile) throws IOException {
        File infoClassDest = new File(infoDir,
                InstrumentationConstants.FL_PROPERTIES_CLASS);
        infoClassDest.getParentFile().mkdirs();
        Properties props = new Properties();
        props.setProperty(InstrumentationConstants.FL_RUN, runId);
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

        File fieldsClass = new File(infoDir,
                InstrumentationConstants.FL_FIELDS_CLASS);
        InstrumentationFileTranslator.writeFields(fieldsFile, fieldsClass);

        File sitesClass = new File(infoDir,
                InstrumentationConstants.FL_SITES_CLASS);
        InstrumentationFileTranslator.writeSites(sitesFile, sitesClass);
    }
}
