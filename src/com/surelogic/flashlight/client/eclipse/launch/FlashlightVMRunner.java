package com.surelogic.flashlight.client.eclipse.launch;

import static com.surelogic._flashlight.common.InstrumentationConstants.FL_COLLECTION_TYPE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_CONSOLE_PORT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DATE_OVERRIDE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DIR;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_FIELDS_FILE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_NO_SPY;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTPUT_TYPE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTQ_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_POSTMORTEM;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RAWQ_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_OFF;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SITES_FILE;
import static com.surelogic.flashlight.common.files.InstrumentationFileHandles.FIELDS_FILE_NAME;
import static com.surelogic.flashlight.common.files.InstrumentationFileHandles.INSTRUMENTATION_LOG_FILE_NAME;
import static com.surelogic.flashlight.common.files.InstrumentationFileHandles.SITES_FILE_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.progress.UIJob;

import com.surelogic._flashlight.common.CollectionType;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.rewriter.ClassNameUtil;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.common.core.SourceZip;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.jobs.LaunchTerminationDetectionJob;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class FlashlightVMRunner implements IVMRunner {
    private static final String MAX_HEAP_PREFIX = "-Xmx";
    private static final long DEFAULT_MAX_HEAP_SIZE = 64 * 1024 * 1024;

    private final IVMRunner delegateRunner;
    private final File runOutputDir;
    private final String mainTypeName;

    private final File projectOutputDir;
    private final File externalOutputDir;
    private final File sourceDir;
    private final File fieldsFile;
    private final File sitesFile;
    private final File logFile;

    private final String datePostfix;
    private final String pathToFlashlightLib;

    private final List<String> classpath;

    private final List<String> instrumentUser;
    private final List<String> instrumentBoot;

    private final boolean ALWAYS_APPEND_TO_BOOT = true;

    private static final class Entry {
        public final String outputName;
        public final boolean asJar;

        public Entry(final String name, final boolean jar) {
            outputName = name;
            asJar = jar;
        }
    }

    public FlashlightVMRunner(final IVMRunner other, final String mainType,
            final List<String> classpath, final List<String> iUser,
            final List<String> iBoot, final boolean java14)
            throws CoreException {
        delegateRunner = other;
        this.classpath = classpath;
        instrumentUser = iUser;
        instrumentBoot = iBoot;

        // Get the path to the flashlight-runtime.jar
        final IPath bundleBase = Activator.getDefault().getBundleLocation();
        if (bundleBase != null) {
            final String name;
            if (java14) {
                name = "lib/flashlight-runtime.java1.4.jar";
            } else {
                name = "lib/flashlight-runtime.jar";
            }
            final IPath jarLocation = bundleBase.append(name);
            pathToFlashlightLib = jarLocation.toOSString();
        } else {
            throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
                    "No bundle location found for the Flashlight plug-in."));
        }

        mainTypeName = mainType;
        final SimpleDateFormat dateFormat = new SimpleDateFormat(
                "-yyyy.MM.dd-'at'-HH.mm.ss.SSS");
        datePostfix = dateFormat.format(new Date());
        final String runName = mainTypeName + datePostfix;
        final File dataDir = FlashlightPreferencesUtility
                .getFlashlightDataDirectory();
        runOutputDir = new File(dataDir, runName);
        if (!runOutputDir.exists()) {
            runOutputDir.mkdirs();
        }

        /* Init references to the different components of the output directory */
        projectOutputDir = new File(runOutputDir, "projects");
        externalOutputDir = new File(runOutputDir, "external");
        sourceDir = new File(runOutputDir, "source");
        fieldsFile = new File(runOutputDir, FIELDS_FILE_NAME);
        sitesFile = new File(runOutputDir, SITES_FILE_NAME
                + FileUtility.GZIP_SUFFIX);
        logFile = new File(runOutputDir, INSTRUMENTATION_LOG_FILE_NAME);
        if (!projectOutputDir.exists()) {
            projectOutputDir.mkdir();
        }
        if (!externalOutputDir.exists()) {
            externalOutputDir.mkdir();
        }
        if (!sourceDir.exists()) {
            sourceDir.mkdir();
        }

    }

    @Override
    public void run(final VMRunnerConfiguration configuration,
            final ILaunch launch, final IProgressMonitor monitor)
            throws CoreException {
        if (!SLLicenseUtility.validate(SLLicenseProduct.FLASHLIGHT)) {
            return;
        }

        /*
         * Build the set of projects used by the application being run, and
         * build the map of original to instrumented names.
         */
        final Set<IProject> interestingProjects = new HashSet<IProject>();
        final Map<String, Entry> classpathEntryMap = new HashMap<String, Entry>();
        getInterestingProjectsAndBuildEntryMap(interestingProjects,
                classpathEntryMap);

        /*
         * Amount of work is 1 for each project we need to zip, 2 for each
         * directory we need to process, plus 1 remaining unit for the delegate.
         */
        final int totalWork = interestingProjects.size() + // source zips
                classpath.size() + // scanning
                instrumentUser.size() + instrumentBoot.size() + // instrumenting
                1; // running
        final SubMonitor progress = SubMonitor.convert(monitor, totalWork);

        // Check if projects changed since last Flashlight run?
        final RunDirectory lastRun = findLastRunDirectory();

        if (createSourceZips(lastRun, interestingProjects, progress)) {
            // Canceled, abort early
            return;
        }

        /*
         * Build the instrumented class files. First we scan each directory to
         * the build the field database, and then we instrument each directory.
         */
        if (instrumentClassfiles(launch.getLaunchConfiguration(),
                classpathEntryMap, progress)) {
            // Canceled, abort early
            return;
        }

        /*
         * Fix the classpath.
         */
        final String[] newClassPath = updateClassPath(configuration,
                classpathEntryMap);
        final String[] newBootClassPath = updateBootClassPath(configuration,
                classpathEntryMap);

        /* Create an updated runner configuration. */
        final VMRunnerConfiguration newConfig = updateRunnerConfiguration(
                configuration, launch.getLaunchConfiguration(), newClassPath,
                newBootClassPath, classpathEntryMap);

        /* Done with our set up, call the real runner */
        delegateRunner.run(newConfig, launch, monitor);

        final boolean postmortem = launch.getLaunchConfiguration()
                .getAttribute(
                        FlashlightPreferencesUtility.POSTMORTEM_MODE,
                        EclipseUIUtility.getPreferences().getBoolean(
                                FlashlightPreferencesUtility.POSTMORTEM_MODE));
        /*
         * Create and launch a job that detects when the instrumented run
         * terminates, and switches to the flashlight perspective on
         * termination.
         */
        final LaunchTerminationDetectionJob terminationDetector = new LaunchTerminationDetectionJob(
                launch, LaunchTerminationDetectionJob.DEFAULT_PERIOD) {
            @Override
            protected IStatus terminationAction() {
                if (postmortem) {
                    final UIJob job = new SwitchToFlashlightPerspectiveJob();
                    job.schedule();
                }
                return Status.OK_STATUS;
            }
        };
        terminationDetector.reschedule();
    }

    private RunDirectory findLastRunDirectory() throws CoreException {
        RunDescription latest = null;
        for (final RunDescription run : RunManager.getInstance()
                .getRunDescriptions()) {
            if (mainTypeName.equals(run.getName())) {
                if (latest == null
                        || run.getStartTimeOfRun().after(
                                latest.getStartTimeOfRun())) {
                    latest = run;
                }
            }
        }
        return latest == null ? null : latest.getRunDirectory();
    }

    /**
     * Create zips of the sources from the projects Also checks to see if it can
     * reuse an old one from the last run
     * 
     * @return Whether instrumentation was canceled.
     */
    private boolean createSourceZips(final RunDirectory lastRun,
            final Set<IProject> projects, final SubMonitor progress) {
        final List<File> oldZips = lastRun == null ? Collections
                .<File> emptyList() : lastRun.getSourceHandles()
                .getSourceZips();
        for (final IProject project : projects) {
            final String projectName = project.getName();
            progress.subTask("Creating source zip for " + projectName);
            final SourceZip srcZip = new SourceZip(project);
            final File zipFile = new File(sourceDir, projectName + ".src.zip");
            try {
                // Check if old zip is up-to-date
                File orig = null;

                for (final File old : oldZips) {
                    if (old.getName().equals(zipFile.getName())) {
                        final IJavaProject jp = JDTUtility
                                .getJavaProject(project.getName());
                        if (JDTUtility.projectUpdatedSince(jp,
                                old.lastModified())) {
                            orig = null;
                        } else {
                            orig = old;
                        }
                        break;
                    }
                }
                if (orig != null) {
                    FileUtility.copy(orig, zipFile);
                } else {
                    srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
                }
            } catch (final IOException e) {
                SLLogger.getLogger().log(
                        Level.SEVERE,
                        "Unable to create source zip for project "
                                + projectName, e);
            }

            if (progress.isCanceled()) {
                return true;
            }
            progress.worked(1);
        }
        return false;
    }

    /**
     * Instrument the classfiles.
     * 
     * @param progress
     * @return Whether instrumentation was canceled.
     * @throws CoreException
     */
    @SuppressWarnings("cast")
    private boolean instrumentClassfiles(final ILaunchConfiguration launch,
            final Map<String, Entry> entryMap, final SubMonitor progress)
            throws CoreException {
        /*
         * Bug 1615: Sanity check the instrumented classpath entries first:
         * Check that no entry marked for instrumentation is a file system
         * parent of any other entry that is marked for instrumentation.
         * 
         * Could be expensive: O(n^2)
         * 
         * 2011-03-22: Jar files that are nested inside of directories are added
         * to a special list so that they can be forced to instructed last. This
         * way we make sure the handling of the directories doesn't overwrite
         * the instrumented jar file.
         */
        final List<String> allInstrument = new LinkedList<String>();
        allInstrument.addAll(instrumentUser);
        allInstrument.addAll(instrumentBoot);
        final List<String> instrumentLast = new ArrayList<String>();
        final StringBuilder sb = new StringBuilder();
        for (final String potentialParent : allInstrument) {
            final String test = potentialParent + File.separator;
            for (final String potentialChild : allInstrument) {
                if (potentialChild.startsWith(test)) {
                    if (new File(potentialChild).isDirectory()) {
                        sb.append("Classpath entry ");
                        sb.append(potentialParent);
                        sb.append(" is instrumented and nests the instrumented classpath directory entry ");
                        sb.append(potentialChild);
                        sb.append("  ");
                    } else { // nested jar file
                        instrumentLast.add(potentialChild);
                    }
                }
            }
        }
        if (sb.length() > 0) {
            throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
                    sb.toString()));
            // FIXME throw new FlashlightLaunchException(sb.toString());
        }

        runOutputDir.mkdirs();
        PrintWriter logOut = null;
        try {
            logOut = new PrintWriter(logFile);
            final RewriteMessenger messenger = new PrintWriterMessenger(logOut);

            // Read the property file
            final Properties flashlightProps = new Properties();
            final File flashlightPropFile = new File(
                    System.getProperty("user.home"),
                    "flashlight-rewriter.properties");
            boolean failed = false;
            try {
                flashlightProps.load(new FileInputStream(flashlightPropFile));
            } catch (final IOException e) {
                failed = true;
            } catch (final IllegalArgumentException e) {
                failed = true;
            }

            final ConfigurationBuilder configBuilder;
            if (failed) {
                SLLogger.getLogger().log(Level.INFO,
                        I18N.err(162, flashlightPropFile));
                configBuilder = new ConfigurationBuilder();
            } else {
                configBuilder = new ConfigurationBuilder(flashlightProps);
            }

            try {
                configBuilder
                        .setIndirectUseDefault(launch
                                .getAttribute(
                                        FlashlightPreferencesUtility.USE_DEFAULT_INDIRECT_ACCESS_METHODS,
                                        true));
            } catch (final CoreException e) {
                // eat it
            }
            try {
                final List<String> xtraMethods = (List<String>) launch
                        .getAttribute(
                                FlashlightPreferencesUtility.ADDITIONAL_INDIRECT_ACCESS_METHODS,
                                Collections.emptyList());
                for (final String s : xtraMethods) {
                    configBuilder.addAdditionalMethods(new File(s));
                }
            } catch (final CoreException e) {
                // eat it
            }

            try {
                final List<String> blacklist = (List<String>) launch
                        .getAttribute(
                                FlashlightPreferencesUtility.CLASS_BLACKLIST,
                                Collections.emptyList());
                for (final String internalTypeName : blacklist) {
                    configBuilder.addToBlacklist(internalTypeName);
                }
            } catch (final CoreException e) {
                // eat it
            }

            try {
                final String filterName = launch.getAttribute(
                        FlashlightPreferencesUtility.FIELD_FILTER,
                        FieldFilter.NONE.name());
                configBuilder.setFieldFilter(Enum.valueOf(FieldFilter.class,
                        filterName));

                final List<String> filterPkgs = launch.getAttribute(
                        FlashlightPreferencesUtility.FIELD_FILTER_PACKAGES,
                        Collections.emptyList());
                configBuilder.getFilterPackages().clear();
                for (final String pkg : filterPkgs) {
                    configBuilder.addToFilterPackages(pkg.replace('.', '/'));
                }

            } catch (final CoreException e) {
                // eat it
            }

            final RewriteManager manager = new VMRewriteManager(
                    configBuilder.getConfiguration(), messenger, fieldsFile,
                    sitesFile, progress);
            // Init the RewriteManager
            initializeRewriteManager(manager, entryMap, instrumentLast);

            try {
                final Map<String, Map<String, Boolean>> badDups = manager
                        .execute();
                if (badDups != null) { // uh oh
                    final StringBuilder sb2 = new StringBuilder();
                    for (final Map.Entry<String, Map<String, Boolean>> entry : badDups
                            .entrySet()) {
                        /*
                         * Scan the classpath: if the first classpath entry that
                         * is in the set is NOT instrumented, then we have a
                         * problem.
                         */
                        for (final String x : classpath) {
                            final Boolean isInstrumented = entry.getValue()
                                    .get(x);
                            if (isInstrumented != null) {
                                // Found the first entry
                                if (!isInstrumented) {
                                    // It's not instrumented, record a problem
                                    sb2.append("Class ");
                                    sb2.append(ClassNameUtil
                                            .internal2FullyQualified(entry
                                                    .getKey()));
                                    sb2.append(" appears on the classpath more than once, only some entries are instrumented, and the first entry is NOT instrumented: ");
                                    sb2.append(entry.getValue().toString());
                                    sb2.append("  ");
                                }
                                break; // stop searching after finding the first
                                // match
                            }
                        }
                    }
                    if (sb2.length() > 0) {
                        throw new CoreException(
                                SLEclipseStatusUtility.createErrorStatus(0,
                                        sb2.toString()));
                    }
                }
            } catch (final CanceledException e) {
                /*
                 * Do nothing, the user canceled the launch early. Caught to
                 * prevent propagation of the local exception being used as a
                 * control flow hack.
                 */
            }
        } catch (final FileNotFoundException e) {
            SLLogger.getLogger().log(
                    Level.SEVERE,
                    "Unable to create instrumentation log file "
                            + logFile.getAbsolutePath(), e);
        } finally {
            if (logOut != null) {
                logOut.close();
            }
        }

        return false;
    }

    private void initializeRewriteManager(final RewriteManager manager,
            final Map<String, Entry> entryMap, final List<String> instrumentLast) {
        /*
         * 2011-03-22: Jar files that are nested inside of directories are added
         * to instrumentLast so that they can be forced to instructed last. This
         * way we make sure the handling of the directories doesn't overwrite
         * the instrumented jar file.
         */
        for (final String cpEntry : classpath) {
            final File asFile = new File(cpEntry);
            if (!instrumentLast.contains(cpEntry)) {
                if (instrumentUser.contains(cpEntry)
                        || instrumentBoot.contains(cpEntry)) {
                    // Instrument the entry
                    addToRewriteManager(manager, entryMap, cpEntry, asFile);
                } else {
                    // Only scan it
                    if (asFile.isDirectory()) {
                        manager.addClasspathDir(asFile);
                    } else {
                        manager.addClasspathJar(asFile);
                    }
                }
            }
        }

        // Add the instrumentLast entries to the manager
        for (final String cpEntry : instrumentLast) {
            final File asFile = new File(cpEntry);
            addToRewriteManager(manager, entryMap, cpEntry, asFile);
        }
    }

    public void addToRewriteManager(final RewriteManager manager,
            final Map<String, Entry> entryMap, final String cpEntry,
            final File asFile) {
        final Entry mapped = entryMap.get(cpEntry);
        final File destFile = new File(mapped.outputName);
        if (asFile.isDirectory()) {
            if (mapped.asJar) {
                manager.addDirToJar(asFile, destFile, null);
            } else {
                manager.addDirToDir(asFile, destFile);
            }
        } else {
            if (mapped.asJar) {
                manager.addJarToJar(asFile, destFile, null);
            } else {
                manager.addJarToDir(asFile, destFile, null);
            }
        }
    }

    @SuppressWarnings("unused")
    private String[] updateClassPath(final VMRunnerConfiguration configuration,
            final Map<String, Entry> entryMap) {
        /*
         * (1) Replace each project "binary output directory" with its
         * corresponding instrumented directory.
         */
        final String[] classPath = configuration.getClassPath();
        final List<String> newClassPathList = new ArrayList<String>(
                classPath.length + 1);
        replaceClasspathEntries(classPath, entryMap, newClassPathList,
                instrumentUser);

        /*
         * (2) Also add the flashlight jar file to the classpath, unless the
         * bootclasspath is non-empty. If it's not empty we add the flashlight
         * lib to the bootclasspath so it is accessible to the instrumented
         * bootclasspath items.
         */
        if (!ALWAYS_APPEND_TO_BOOT && instrumentBoot.isEmpty()) {
            newClassPathList.add(pathToFlashlightLib);
        }

        final String[] newClassPath = new String[newClassPathList.size()];
        return newClassPathList.toArray(newClassPath);
    }

    private String[] updateBootClassPath(
            final VMRunnerConfiguration configuration,
            final Map<String, Entry> entryMap) {
        /*
         * (1) Replace each project "binary output directory" with its
         * corresponding instrumented directory.
         */
        final String[] classPath = configuration.getBootClassPath();
        if (classPath != null && classPath.length != 0) {
            final List<String> newClassPathList = new ArrayList<String>(
                    classPath.length + 1);
            replaceClasspathEntries(classPath, entryMap, newClassPathList,
                    instrumentBoot);

            /*
             * (2) Also add the flashlight jar file to the classpath, if the
             * bootclasspath is non-empty
             */
            if (!instrumentBoot.isEmpty()) {
                newClassPathList.add(pathToFlashlightLib);
            }

            final String[] newClassPath = new String[newClassPathList.size()];
            return newClassPathList.toArray(newClassPath);
        } else {
            return classPath;
        }
    }

    private static void replaceClasspathEntries(final String[] classPath,
            final Map<String, Entry> entryMap,
            final List<String> newClassPathList, final List<String> toInstrument) {
        for (int i = 0; i < classPath.length; i++) {
            final String oldEntry = classPath[i];
            if (toInstrument.contains(oldEntry)) {
                final Entry newEntry = entryMap.get(oldEntry);
                newClassPathList.add(newEntry.outputName);
            } else {
                newClassPathList.add(oldEntry);
            }
        }
    }

    private VMRunnerConfiguration updateRunnerConfiguration(
            final VMRunnerConfiguration original,
            final ILaunchConfiguration launch, final String[] newClassPath,
            final String[] newBootClassPath, final Map<String, Entry> entryMap) {
        // Create a new configuration and update the class path
        final VMRunnerConfiguration newConfig = new VMRunnerConfiguration(
                original.getClassToLaunch(), newClassPath);

        // Update the VM arguments: We need to add parameters for the Flashlight
        // Store
        final String[] vmArgs = original.getVMArguments();

        // Check for the use or absence of "-XmX"
        int heapSettingPos = -1;
        for (int i = 0; i < vmArgs.length; i++) {
            if (vmArgs[i].startsWith(MAX_HEAP_PREFIX)) {
                heapSettingPos = i;
                break;
            }
        }
        final long maxHeapSize;
        if (heapSettingPos != -1) {
            final String maxHeapSetting = vmArgs[heapSettingPos];
            // We assume the -Xmx option is well-formed
            final int multiplier;
            final int lastChar = maxHeapSetting
                    .charAt(maxHeapSetting.length() - 1);
            final int endPos;
            if (lastChar == 'k' || lastChar == 'K') {
                multiplier = 10;
                endPos = maxHeapSetting.length() - 1;
            } else if (lastChar == 'm' || lastChar == 'M') {
                multiplier = 20;
                endPos = maxHeapSetting.length() - 1;
            } else {
                multiplier = 0;
                endPos = maxHeapSetting.length();
            }
            final long rawHeapSize = Long.parseLong(maxHeapSetting.substring(4,
                    endPos));
            maxHeapSize = rawHeapSize << multiplier;
        } else {
            maxHeapSize = DEFAULT_MAX_HEAP_SIZE;
        }

        // We will add at most eleven arguments, but maybe less
        final IPreferenceStore prefs = EclipseUIUtility.getPreferences();
        final List<String> newVmArgsList = new ArrayList<String>(
                vmArgs.length + 11);
        try {
            final int rawQSize = launch.getAttribute(
                    FlashlightPreferencesUtility.RAWQ_SIZE,
                    prefs.getInt(FlashlightPreferencesUtility.RAWQ_SIZE));
            final int refSize = launch.getAttribute(
                    FlashlightPreferencesUtility.REFINERY_SIZE,
                    prefs.getInt(FlashlightPreferencesUtility.REFINERY_SIZE));
            final int outQSize = launch.getAttribute(
                    FlashlightPreferencesUtility.OUTQ_SIZE,
                    prefs.getInt(FlashlightPreferencesUtility.OUTQ_SIZE));
            final int cPort = launch.getAttribute(
                    FlashlightPreferencesUtility.CONSOLE_PORT,
                    prefs.getInt(FlashlightPreferencesUtility.CONSOLE_PORT));
            final String useBinary = launch.getAttribute(
                    FlashlightPreferencesUtility.OUTPUT_TYPE,
                    prefs.getString(FlashlightPreferencesUtility.OUTPUT_TYPE));
            final boolean postmortem = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.POSTMORTEM_MODE,
                            prefs.getBoolean(FlashlightPreferencesUtility.POSTMORTEM_MODE));
            final boolean compress = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.COMPRESS_OUTPUT,
                            prefs.getBoolean(FlashlightPreferencesUtility.COMPRESS_OUTPUT));
            final String collectionType = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.COLLECTION_TYPE,
                            prefs.getString(FlashlightPreferencesUtility.COLLECTION_TYPE));
            final boolean useSpy = launch.getAttribute(
                    FlashlightPreferencesUtility.USE_SPY,
                    prefs.getBoolean(FlashlightPreferencesUtility.USE_SPY));
            final boolean useRefinery = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.USE_REFINERY,
                            prefs.getBoolean(FlashlightPreferencesUtility.USE_REFINERY));

            newVmArgsList.add("-DFL_RUN=" + mainTypeName);
            newVmArgsList.add("-D" + FL_DIR + "="
                    + runOutputDir.getAbsolutePath());
            newVmArgsList.add("-D" + FL_FIELDS_FILE + "="
                    + fieldsFile.getAbsolutePath());
            newVmArgsList.add("-D" + FL_SITES_FILE + "="
                    + sitesFile.getAbsolutePath());
            newVmArgsList.add("-D" + FL_RAWQ_SIZE + "=" + rawQSize);
            newVmArgsList.add("-D" + FL_REFINERY_SIZE + "=" + refSize);
            newVmArgsList.add("-D" + FL_OUTQ_SIZE + "=" + outQSize);
            newVmArgsList.add("-D" + FL_CONSOLE_PORT + "=" + cPort);
            newVmArgsList.add("-D" + FL_DATE_OVERRIDE + "=" + datePostfix);
            newVmArgsList.add("-D" + FL_POSTMORTEM + "="
                    + Boolean.toString(postmortem));
            newVmArgsList.add("-D" + FL_OUTPUT_TYPE + "="
                    + OutputType.get(useBinary, compress));
            newVmArgsList.add("-D"
                    + FL_COLLECTION_TYPE
                    + "="
                    + CollectionType
                            .valueOf(collectionType, CollectionType.ALL));
            if (!useRefinery) {
                newVmArgsList.add("-D" + FL_REFINERY_OFF + "=true");
            }
            if (!useSpy) {
                newVmArgsList.add("-D" + FL_NO_SPY + "=true");
            }
        } catch (final CoreException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Couldn't setup launch for " + launch.getName(), e);
            return null;
        }

        if (prefs
                .getBoolean(FlashlightPreferencesUtility.AUTO_INCREASE_HEAP_AT_LAUNCH)) {
            final long maxSystemHeapSize = (long) MemoryUtility
                    .computeMaxMemorySizeInMb() << 20;
            final long newHeapSizeRaw = Math.min(3 * maxHeapSize,
                    maxSystemHeapSize);
            newVmArgsList.add(MAX_HEAP_PREFIX + Long.toString(newHeapSizeRaw));
            SLLogger.getLogger().log(
                    Level.INFO,
                    "Increasing maximum heap size for launched application to "
                            + newHeapSizeRaw + " bytes from " + maxHeapSize
                            + " bytes");

            // Add the original arguments afterwards, skipping the original -Xmx
            // setting
            for (int i = 0; i < vmArgs.length; i++) {
                if (i != heapSettingPos) {
                    newVmArgsList.add(vmArgs[i]);
                }
            }
        } else {
            // Add the original arguments unchanged
            for (int i = 0; i < vmArgs.length; i++) {
                newVmArgsList.add(vmArgs[i]);
            }
        }
        // Get the new array of vm arguments
        final String[] newVmArgs = new String[newVmArgsList.size()];
        newConfig.setVMArguments(newVmArgsList.toArray(newVmArgs));

        // Copy the rest of the arguments unchanged
        newConfig.setBootClassPath(newBootClassPath);
        newConfig.setEnvironment(original.getEnvironment());
        newConfig.setProgramArguments(original.getProgramArguments());
        newConfig.setResumeOnStartup(original.isResumeOnStartup());
        newConfig.setVMSpecificAttributesMap(updateVMSpecificAttributesMap(
                original.getVMSpecificAttributesMap(), entryMap));
        newConfig.setWorkingDirectory(original.getWorkingDirectory());
        return newConfig;
    }

    @SuppressWarnings("unused")
    private Map updateVMSpecificAttributesMap(final Map originalMap,
            final Map<String, Entry> entryMap) {
        Map original = originalMap;
        if (original == null) {
            if (!ALWAYS_APPEND_TO_BOOT) {
                return null;
            }
            original = Collections.EMPTY_MAP;
        }

        final Map updated = new HashMap();
        final String[] originalPrepend = (String[]) original
                .get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND);
        final String[] originalBootpath = (String[]) original
                .get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH);
        final String[] originalAppend = (String[]) original
                .get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_APPEND);

        boolean needsFlashlightLib = false;
        if (originalPrepend != null) {
            final List<String> newPrependList = new ArrayList(
                    originalPrepend.length);
            needsFlashlightLib |= updateBootpathArray(entryMap,
                    originalPrepend, newPrependList);
            final String[] newPrepend = new String[originalPrepend.length];
            updated.put(
                    IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND,
                    newPrependList.toArray(newPrepend));
        }

        if (originalBootpath != null) {
            final List<String> newBootpathList = new ArrayList(
                    originalBootpath.length);
            needsFlashlightLib |= updateBootpathArray(entryMap,
                    originalBootpath, newBootpathList);
            final String[] newBootpath = new String[originalBootpath.length];
            updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH,
                    newBootpathList.toArray(newBootpath));
        }

        List<String> newAppendList = null;
        if (originalAppend != null) {
            newAppendList = new ArrayList(originalAppend.length + 1);
            needsFlashlightLib |= updateBootpathArray(entryMap, originalAppend,
                    newAppendList);
        }
        if (ALWAYS_APPEND_TO_BOOT || needsFlashlightLib) {
            if (newAppendList == null) {
                newAppendList = new ArrayList(1);
            }
            newAppendList.add(pathToFlashlightLib);
        }
        if (newAppendList != null) {
            final String[] newAppend = new String[newAppendList.size()];
            updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_APPEND,
                    newAppendList.toArray(newAppend));
        }

        return updated;
    }

    private boolean updateBootpathArray(final Map<String, Entry> entryMap,
            final String[] originalBoothpath, final List<String> newBootpath) {
        boolean needsFlashlightLib = false;
        for (final String entry : originalBoothpath) {
            if (instrumentBoot.contains(entry)) {
                final String newEntry = entryMap.get(entry).outputName;
                newBootpath.add(newEntry);
                needsFlashlightLib = true;
            } else {
                newBootpath.add(entry);
            }
        }
        return needsFlashlightLib;
    }

    private static final class CanceledException extends RuntimeException {
        public CanceledException() {
            super();
        }
    }

    private static final class VMRewriteManager extends RewriteManager {
        private final SubMonitor progress;

        public VMRewriteManager(final Configuration c,
                final RewriteMessenger m, final File ff, final File sf,
                final SubMonitor sub) {
            super(c, m, ff, sf);
            progress = sub;
        }

        @Override
        protected void preScan(final String srcPath) {
            progress.subTask("Scanning " + srcPath);
        }

        @Override
        protected void postScan(final String srcPath) {
            // check for cancellation
            if (progress.isCanceled()) {
                throw new CanceledException();
            }
            progress.worked(1);
        }

        @Override
        protected void preInstrument(final String srcPath, final String destPath) {
            progress.subTask("Instrumenting " + srcPath);
        }

        @Override
        protected void postInstrument(final String srcPath,
                final String destPath) {
            // check for cancellation
            if (progress.isCanceled()) {
                throw new CanceledException();
            }
            progress.worked(1);
        }

        @Override
        protected void exceptionScan(final String srcPath, final IOException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Error scanning classfiles in " + srcPath, e);
        }

        @Override
        protected void exceptionInstrument(final String srcPath,
                final String destPath, final IOException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Error instrumenting classfiles in " + srcPath, e);
        }

        @Override
        protected void exceptionLoadingMethodsFile(final JAXBException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Problem loading indirect access methods", e);
        }

        @Override
        protected void exceptionCreatingFieldsFile(final File fieldsFile,
                final FileNotFoundException e) {
            SLLogger.getLogger().log(
                    Level.SEVERE,
                    "Unable to create fields file "
                            + fieldsFile.getAbsolutePath(), e);
        }

        @Override
        protected void exceptionCreatingSitesFile(final File sitesFile,
                final IOException e) {
            SLLogger.getLogger().log(
                    Level.SEVERE,
                    "Unable to create sites file "
                            + sitesFile.getAbsolutePath(), e);
        }
    }

    private void getInterestingProjectsAndBuildEntryMap(
            final Set<IProject> interestingProjects,
            final Map<String, Entry> classpathEntryMap) {
        // Get the list of open projects in the workpace
        final List<IProject> openProjects = getOpenProjects();

        /*
         * For each classpath entry we see if it is from a workspace project. If
         * so, we add the project to the list of interesting projects. Also, we
         * compute the location of the instrumentation for the classpath entry.
         */
        scanProjects(classpath, openProjects, interestingProjects,
                classpathEntryMap);
    }

    private static List<IProject> getOpenProjects() {
        final List<IProject> openProjects = new ArrayList<IProject>();
        for (final IProject p : ResourcesPlugin.getWorkspace().getRoot()
                .getProjects()) {
            if (p.isOpen()) {
                openProjects.add(p);
            }
        }
        return openProjects;
    }

    private void scanProjects(final List<String> classpathEntries,
            final List<IProject> projects,
            final Set<IProject> interestingProjects,
            final Map<String, Entry> classpathEntryMap) {
        for (final String entry : classpathEntries) {
            final boolean isJar = !new File(entry).isDirectory();
            boolean foundProject = false;
            for (final IProject project : projects) {
                final String projectLoc = project.getLocation().toOSString();
                if (isFromProject(projectLoc, entry)) {
                    final File newEntry = buildInstrumentedName(entry,
                            projectLoc, isJar);
                    classpathEntryMap.put(entry,
                            new Entry(newEntry.getAbsolutePath(), isJar));
                    interestingProjects.add(project);
                    foundProject = true;
                    break;
                }
            }

            /*
             * If we didn't find the project, then the entry exists outside of
             * the workspace.
             */
            if (!foundProject) {
                final String correctedEntry = fixLeadingDriveLetter(entry);
                final File newLocation =
                // new File(externalOutputDir, isJar ? correctedEntry :
                // (correctedEntry + ".jar"));
                new File(externalOutputDir, correctedEntry);
                classpathEntryMap.put(entry,
                        new Entry(newLocation.getAbsolutePath(), isJar));
            }
        }
    }

    private static boolean isFromProject(final String projectLoc,
            final String entry) {
        if (entry.startsWith(projectLoc)) {
            return entry.length() == projectLoc.length()
                    || entry.charAt(projectLoc.length()) == File.separatorChar;
        }
        return false;
    }

    private File buildInstrumentedName(final String entry,
            final String projectLoc, final boolean isJar) {
        final String projectDirName = projectLoc.substring(projectLoc
                .lastIndexOf(File.separatorChar) + 1);
        final String binaryName;
        if (projectLoc.length() == entry.length()) {
            // e.g. no bin directory
            binaryName = projectDirName;
        } else {
            binaryName = entry.substring(projectLoc.length() + 1);
        }
        // final String jarName = !isJar ? binaryName + ".jar" : binaryName;
        final String jarName = binaryName;
        final File newEntry = new File(new File(projectOutputDir,
                projectDirName), jarName);
        return newEntry;
    }

    private static String fixLeadingDriveLetter(final String entry) {
        String correctedEntry = entry;
        if (entry.length() > 0) {
            final char driveLetter = entry.charAt(0);
            if (driveLetter != '\\' && driveLetter != '/') {
                correctedEntry = driveLetter + "-drive" + entry.substring(2);
            }
        }
        return correctedEntry;
    }
}
