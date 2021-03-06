package com.surelogic.flashlight.client.eclipse.launch;

import static com.surelogic._flashlight.common.InstrumentationConstants.FL_CLASS_HIERARCHY_FILE_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_COLLECTION_TYPE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_CONSOLE_PORT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DATE_OVERRIDE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DIR;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_EXTERNAL_FOLDER_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_FIELDS_FILE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_FIELDS_FILE_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_HAPPENS_BEFORE_FILE_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_LOG_FILE_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_NO_SPY;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTPUT_TYPE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTQ_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_POSTMORTEM;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_PROJECTS_FOLDER_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RAWQ_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_OFF;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SITES_FILE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SITES_FILE_LOC;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SOURCE_FOLDER_LOC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.Nullable;
import com.surelogic._flashlight.common.CollectionType;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.rewriter.ClassNameUtil;
import com.surelogic._flashlight.rewriter.MissingClassReference;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.ShowTextDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper.RuntimeConfig;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;
import com.surelogic.flashlight.common.model.RunDirectory;
import com.surelogic.flashlight.schema.SchemaResources;

public final class FlashlightVMRunner implements IVMRunner {
  private static final String MAX_HEAP_PREFIX = "-Xmx";
  private static final long DEFAULT_MAX_HEAP_SIZE = 64 * 1024 * 1024;

  private final IVMRunner delegateRunner;
  private final String runId;
  private final File runOutputDir;
  private final String mainTypeName;

  private final File projectOutputDir;
  private final File externalOutputDir;
  private final File sourceDir;
  private final File fieldsFile;
  private final File classHierarchyFile;
  private final File sitesFile;
  private final File hbFile;
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

  public FlashlightVMRunner(final IVMRunner other, final String mainType, final List<String> classpath, final List<String> iUser,
      final List<String> iBoot) throws CoreException {
    delegateRunner = other;
    this.classpath = classpath;
    instrumentUser = iUser;
    instrumentBoot = iBoot;

    // Get the path to the flashlight-runtime.jar
    final File bundleBase = EclipseUtility.getInstallationDirectoryOf(SchemaResources.PLUGIN_ID);
    if (bundleBase != null) {
      final File jarLocation = new File(bundleBase, SchemaResources.RUNTIME_JAR);
      pathToFlashlightLib = jarLocation.getAbsolutePath();
      System.out.println("RUNTIME path: " + pathToFlashlightLib);
    } else {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
          "No bundle location (null returned) found for the Flashlight common plug-in:" + SchemaResources.PLUGIN_ID));
    }

    mainTypeName = mainType;
    final SimpleDateFormat dateFormat = new SimpleDateFormat(InstrumentationConstants.DATE_FORMAT);
    datePostfix = dateFormat.format(new Date());
    runId = mainTypeName + datePostfix + InstrumentationConstants.JAVA_LAUNCH_SUFFIX;
    final File dataDir = EclipseUtility.getFlashlightDataDirectory();
    runOutputDir = new File(dataDir, runId);
    if (!runOutputDir.exists()) {
      runOutputDir.mkdirs();
    }

    /* Init references to the different components of the output directory */
    projectOutputDir = new File(runOutputDir, FL_PROJECTS_FOLDER_LOC);
    externalOutputDir = new File(runOutputDir, FL_EXTERNAL_FOLDER_LOC);
    sourceDir = new File(runOutputDir, FL_SOURCE_FOLDER_LOC);
    fieldsFile = new File(runOutputDir, FL_FIELDS_FILE_LOC);
    classHierarchyFile = new File(runOutputDir, FL_CLASS_HIERARCHY_FILE_LOC);
    sitesFile = new File(runOutputDir, FL_SITES_FILE_LOC);
    logFile = new File(runOutputDir, FL_LOG_FILE_LOC);
    hbFile = new File(runOutputDir, FL_HAPPENS_BEFORE_FILE_LOC);
    if (!projectOutputDir.exists()) {
      projectOutputDir.mkdirs();
    }
    if (!externalOutputDir.exists()) {
      externalOutputDir.mkdirs();
    }
    if (!sourceDir.exists()) {
      sourceDir.mkdirs();
    }
  }

  @Override
  public void run(final VMRunnerConfiguration configuration, final ILaunch launch, final IProgressMonitor monitor)
      throws CoreException {
    RunManager.getInstance().notifyPerformingInstrumentationAndLaunch(runId);
    /*
     * Build the set of projects used by the application being run, and build
     * the map of original to instrumented names.
     */
    final Set<IProject> interestingProjects = new HashSet<>();
    final Map<String, Entry> classpathEntryMap = new HashMap<>();
    getInterestingProjectsAndBuildEntryMap(interestingProjects, classpathEntryMap);

    /*
     * Amount of work is 1 for each project we need to zip, 2 for each directory
     * we need to process, plus 1 remaining unit for the delegate.
     */
    final int totalWork = interestingProjects.size() + // source zips
        classpath.size() + // scanning
        instrumentUser.size() + instrumentBoot.size() + // instrumenting
        1; // running
    final SubMonitor progress = SubMonitor.convert(monitor, totalWork);

    // Check if projects changed since last Flashlight run?
    final RunDirectory lastRun = findLastRunDirectory();

    if (LaunchUtils.createSourceZips(lastRun, interestingProjects, sourceDir, progress)) {
      // Canceled, abort early
      return;
    }

    /*
     * Build the instrumented class files. First we scan each directory to the
     * build the field database, and then we instrument each directory.
     */
    if (instrumentClassfiles(launch.getLaunchConfiguration(), classpathEntryMap, progress)) {
      // Canceled, abort early
      return;
    }

    /*
     * Fix the classpath.
     */
    final String[] newClassPath = updateClassPath(configuration, classpathEntryMap);
    final String[] newBootClassPath = updateBootClassPath(configuration, classpathEntryMap);

    /* Create an updated runner configuration. */
    final VMRunnerConfiguration newConfig = updateRunnerConfiguration(configuration, launch.getLaunchConfiguration(), newClassPath,
        newBootClassPath, classpathEntryMap);

    /* Done with our set up, call the real runner */
    delegateRunner.run(newConfig, launch, monitor);

    /* Let the monitor thread know it should expect a launch */
    final Job job = EclipseUtility.toEclipseJob(new WatchFlashlightMonitorJob(new MonitorStatus(runId)));
    job.setSystem(true);
    job.schedule();

    RunManager.getInstance().notifyCollectingData(runId);
  }

  @Nullable
  private RunDirectory findLastRunDirectory() throws CoreException {
    RunDirectory latest = null;
    for (final RunDirectory run : RunManager.getInstance().getCollectionCompletedRunDirectories()) {
      if (mainTypeName.equals(run.getDescription().getName())) {
        if (latest == null || run.getDescription().getStartTimeOfRun().after(latest.getDescription().getStartTimeOfRun())) {
          latest = run;
        }
      }
    }
    return latest == null ? null : latest;
  }

  /**
   * Instrument the classfiles.
   *
   * @param progress
   * @return Whether instrumentation was canceled.
   * @throws CoreException
   */
  private boolean instrumentClassfiles(final ILaunchConfiguration launch, final Map<String, Entry> entryMap,
      final SubMonitor progress) throws CoreException {
    boolean aborted = false;
    final List<String> allInstrument = new LinkedList<>();
    allInstrument.addAll(instrumentUser);
    allInstrument.addAll(instrumentBoot);
    List<String> instrumentLast = LaunchHelper.sanitizeInstrumentationList(allInstrument);

    runOutputDir.mkdirs();
    PrintWriter logOut = null;
    try {
      logOut = new PrintWriter(logFile);
      final RewriteMessenger messenger = new PrintWriterMessenger(logOut);

      final ConfigurationBuilder configBuilder = LaunchHelper.buildConfigurationFromPreferences(launch);
      final RewriteManager manager = new VMRewriteManager(configBuilder.getConfiguration(), messenger, fieldsFile, sitesFile,
          classHierarchyFile, hbFile, progress);
      // Init the RewriteManager
      initializeRewriteManager(manager, entryMap, instrumentLast);

      try {
        final Map<String, Map<String, Boolean>> badDups = manager.execute();
        if (badDups != null) { // uh oh
          final StringWriter s = new StringWriter();
          PrintWriter w = new PrintWriter(s);
          for (final Map.Entry<String, Map<String, Boolean>> entry : badDups.entrySet()) {
            w.println("Did not instrument class " + ClassNameUtil.internal2FullyQualified(entry.getKey())
                + " because it appears on the classpath more than once, and is inconsistently marked for instrumentation.");
            for (final Map.Entry<String, Boolean> entry2 : entry.getValue().entrySet()) {
              if (entry2.getValue().booleanValue()) {
                w.println("    Instrumented on classpath entry " + entry2.getKey());
              } else {
                w.println("    Not instrumented on classpath entry " + entry2.getKey());
              }
            }

          }

          w.flush();
          final SLUIJob job = new SLUIJob() {
            final String message = s.toString();

            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
              ShowTextDialog.showText(getDisplay().getActiveShell(), "Could not instrument some classes.", message);
              return Status.OK_STATUS;
            }
          };
          job.schedule();
        }

        final Set<MissingClassReference> badRefs = manager.getBadReferences();
        if (!badRefs.isEmpty()) {
          final StringWriter s = new StringWriter();
          PrintWriter w = new PrintWriter(s);
          for (final MissingClassReference r : badRefs) {
            w.print("Method ");
            w.println(r.getReferringMethod());
            w.print("in class ");
            w.println(r.getReferringClassName());
            w.print("was not instrumented because it refers to the type ");
            w.println(r.getMissingClassName());
            w.println("that is not on the classpath.");
            w.println();
          }
          w.flush();
          final String message = s.toString();

          final SLUIJob job = new SLUIJob() {
            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
              ShowTextDialog.showText(getDisplay().getActiveShell(), "Instrumentation aborted.", message);
              return Status.OK_STATUS;
            }
          };
          job.schedule();
        }
      } catch (final RewriteManager.AlreadyInstrumentedException e) {
        /*
         * Found classes on the classpath that are already instrumented. This
         * creates a bad situation, and the whole execution must be prevented.
         */
        aborted = true;
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
            ShowTextDialog.showText(getDisplay().getActiveShell(), "Instrumentation aborted.", message);
            return Status.OK_STATUS;
          }
        };
        job.schedule();

        final StringBuilder x = new StringBuilder("Instrumentation aborted because some classes have already been instrumented: ");
        boolean first = true;
        for (final String cname : e.getClasses()) {
          if (!first) {
            x.append(", ");
          } else {
            first = false;
          }
          x.append(cname);
        }
        SLLogger.getLogger().log(Level.SEVERE, x.toString());

      } catch (final CanceledException e) {
        /*
         * Do nothing, the user canceled the launch early. Caught to prevent
         * propagation of the local exception being used as a control flow hack.
         */
        aborted = true;
      }
    } catch (final FileNotFoundException e) {
      aborted = true;
      SLLogger.getLogger().log(Level.SEVERE, "Unable to create instrumentation log file " + logFile.getAbsolutePath(), e);
    } finally {
      if (logOut != null) {
        logOut.close();
      }
    }

    return aborted;
  }

  private void initializeRewriteManager(final RewriteManager manager, final Map<String, Entry> entryMap,
      final List<String> instrumentLast) {
    /*
     * 2011-03-22: Jar files that are nested inside of directories are added to
     * instrumentLast so that they can be forced to instructed last. This way we
     * make sure the handling of the directories doesn't overwrite the
     * instrumented jar file.
     */
    for (final String cpEntry : classpath) {
      final File asFile = new File(cpEntry);
      if (!instrumentLast.contains(cpEntry)) {
        if (instrumentUser.contains(cpEntry) || instrumentBoot.contains(cpEntry)) {
          // Instrument the entry
          addToRewriteManager(manager, entryMap, cpEntry, asFile);
        } else {
          // Only scan it
          if (asFile.isDirectory()) {
            manager.addClasspathDir(asFile);
          } else if (asFile.exists()) {
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

  public void addToRewriteManager(final RewriteManager manager, final Map<String, Entry> entryMap, final String cpEntry,
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

  private String[] updateClassPath(final VMRunnerConfiguration configuration, final Map<String, Entry> entryMap) {
    /*
     * (1) Replace each project "binary output directory" with its corresponding
     * instrumented directory.
     */
    final String[] classPath = configuration.getClassPath();
    final List<String> newClassPathList = new ArrayList<>(classPath.length + 1);
    replaceClasspathEntries(classPath, entryMap, newClassPathList, instrumentUser);

    /*
     * We always add the runtime to the classpath, but we may add it to the boot
     * classpath as well if we need to. We used to conditionally add this here,
     * but jamaica vm doesn't seem to be respecting the boot path, and it
     * shouldn't hurt to add it to the classpath twice sometimes.
     */
    newClassPathList.add(pathToFlashlightLib);

    final String[] newClassPath = new String[newClassPathList.size()];
    return newClassPathList.toArray(newClassPath);
  }

  private String[] updateBootClassPath(final VMRunnerConfiguration configuration, final Map<String, Entry> entryMap) {
    /*
     * (1) Replace each project "binary output directory" with its corresponding
     * instrumented directory.
     */
    final String[] classPath = configuration.getBootClassPath();
    if (classPath != null && classPath.length != 0) {
      final List<String> newClassPathList = new ArrayList<>(classPath.length + 1);
      replaceClasspathEntries(classPath, entryMap, newClassPathList, instrumentBoot);

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

  private static void replaceClasspathEntries(final String[] classPath, final Map<String, Entry> entryMap,
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

  @SuppressWarnings("unchecked")
  private VMRunnerConfiguration updateRunnerConfiguration(final VMRunnerConfiguration original, final ILaunchConfiguration launch,
      final String[] newClassPath, final String[] newBootClassPath, final Map<String, Entry> entryMap) {
    // Create a new configuration and update the class path
    final VMRunnerConfiguration newConfig = new VMRunnerConfiguration(original.getClassToLaunch(), newClassPath);

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
      final int lastChar = maxHeapSetting.charAt(maxHeapSetting.length() - 1);
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
      final long rawHeapSize = Long.parseLong(maxHeapSetting.substring(4, endPos));
      maxHeapSize = rawHeapSize << multiplier;
    } else {
      maxHeapSize = DEFAULT_MAX_HEAP_SIZE;
    }

    // We will add at most eleven arguments, but maybe less
    final IPreferenceStore prefs = EclipseUIUtility.getPreferences();
    final List<String> newVmArgsList = new ArrayList<>(vmArgs.length + 11);
    try {

      RuntimeConfig conf = LaunchHelper.getRuntimeConfig(launch);
      newVmArgsList.add("-DFL_RUN=" + mainTypeName);
      newVmArgsList.add("-D" + FL_DIR + "=" + runOutputDir.getAbsolutePath());
      newVmArgsList.add("-D" + FL_FIELDS_FILE + "=" + fieldsFile.getAbsolutePath());
      newVmArgsList.add("-D" + FL_SITES_FILE + "=" + sitesFile.getAbsolutePath());
      newVmArgsList.add("-D" + FL_RAWQ_SIZE + "=" + conf.getRawQueueSize());
      newVmArgsList.add("-D" + FL_REFINERY_SIZE + "=" + conf.getRefinerySize());
      newVmArgsList.add("-D" + FL_OUTQ_SIZE + "=" + conf.getOutQueueSize());
      newVmArgsList.add("-D" + FL_CONSOLE_PORT + "=" + conf.getConsolePort());
      newVmArgsList.add("-D" + FL_DATE_OVERRIDE + "=" + datePostfix);
      newVmArgsList.add("-D" + FL_POSTMORTEM + "=" + Boolean.toString(conf.isPostmortem()));
      newVmArgsList.add("-D" + FL_OUTPUT_TYPE + "=" + OutputType.get(true));
      newVmArgsList.add("-D" + FL_COLLECTION_TYPE + "=" + CollectionType.valueOf(conf.getCollectionType(), CollectionType.ALL));
      if (!conf.useRefinery()) {
        newVmArgsList.add("-D" + FL_REFINERY_OFF + "=true");
      }
      if (!conf.useSpy()) {
        newVmArgsList.add("-D" + FL_NO_SPY + "=true");
      }
    } catch (final CoreException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Couldn't setup launch for " + launch.getName(), e);
      return null;
    }

    if (prefs.getBoolean(FlashlightPreferencesUtility.AUTO_INCREASE_HEAP_AT_LAUNCH)) {
      final long maxSystemHeapSize = (long) MemoryUtility.computeMaxMemorySizeInMb() << 20;
      final long newHeapSizeRaw = Math.min(3 * maxHeapSize, maxSystemHeapSize);
      newVmArgsList.add(MAX_HEAP_PREFIX + Long.toString(newHeapSizeRaw));
      SLLogger.getLogger().log(Level.INFO,
          "Increasing maximum heap size for launched application to " + newHeapSizeRaw + " bytes from " + maxHeapSize + " bytes");

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
    newConfig.setVMSpecificAttributesMap(updateVMSpecificAttributesMap(original.getVMSpecificAttributesMap(), entryMap));
    newConfig.setWorkingDirectory(original.getWorkingDirectory());
    return newConfig;
  }

  @SuppressWarnings({ "unused", "rawtypes", "unchecked" })
  private Map updateVMSpecificAttributesMap(final Map originalMap, final Map<String, Entry> entryMap) {
    Map original = originalMap;
    if (original == null) {
      if (!ALWAYS_APPEND_TO_BOOT) {
        return null;
      }
      original = Collections.EMPTY_MAP;
    }

    final Map updated = new HashMap();
    final String[] originalPrepend = (String[]) original.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND);
    final String[] originalBootpath = (String[]) original.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH);
    final String[] originalAppend = (String[]) original.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_APPEND);

    boolean needsFlashlightLib = false;
    if (originalPrepend != null) {
      final List<String> newPrependList = new ArrayList(originalPrepend.length);
      needsFlashlightLib |= updateBootpathArray(entryMap, originalPrepend, newPrependList);
      final String[] newPrepend = new String[originalPrepend.length];
      updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, newPrependList.toArray(newPrepend));
    }

    if (originalBootpath != null) {
      final List<String> newBootpathList = new ArrayList(originalBootpath.length);
      needsFlashlightLib |= updateBootpathArray(entryMap, originalBootpath, newBootpathList);
      final String[] newBootpath = new String[originalBootpath.length];
      updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH, newBootpathList.toArray(newBootpath));
    }

    List<String> newAppendList = null;
    if (originalAppend != null) {
      newAppendList = new ArrayList(originalAppend.length + 1);
      needsFlashlightLib |= updateBootpathArray(entryMap, originalAppend, newAppendList);
    }
    if (ALWAYS_APPEND_TO_BOOT || needsFlashlightLib) {
      if (newAppendList == null) {
        newAppendList = new ArrayList(1);
      }
      newAppendList.add(pathToFlashlightLib);
    }
    if (newAppendList != null) {
      final String[] newAppend = new String[newAppendList.size()];
      updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_APPEND, newAppendList.toArray(newAppend));
    }

    return updated;
  }

  private boolean updateBootpathArray(final Map<String, Entry> entryMap, final String[] originalBoothpath,
      final List<String> newBootpath) {
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
    /**
     *
     */
    private static final long serialVersionUID = -3234551631280412888L;

    public CanceledException() {
      super();
    }
  }

  private static final class VMRewriteManager extends RewriteManager {
    private final SubMonitor progress;

    public VMRewriteManager(final Configuration c, final RewriteMessenger m, final File ff, final File sf, final File chf, File hbf,
        final SubMonitor sub) {
      super(c, m, ff, sf, chf, hbf);
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
    protected void postInstrument(final String srcPath, final String destPath) {
      // check for cancellation
      if (progress.isCanceled()) {
        throw new CanceledException();
      }
      progress.worked(1);
    }

    @Override
    protected void exceptionScan(final String srcPath, final IOException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Error scanning classfiles in " + srcPath, e);
    }

    @Override
    protected void exceptionInstrument(final String srcPath, final String destPath, final IOException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Error instrumenting classfiles in " + srcPath, e);
    }

    @Override
    protected void exceptionLoadingMethodsFile(final JAXBException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Problem loading indirect access methods", e);
    }

    @Override
    protected void exceptionCreatingFieldsFile(final File fieldsFile, final FileNotFoundException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Unable to create fields file " + fieldsFile.getAbsolutePath(), e);
    }

    @Override
    protected void exceptionCreatingClassHierarchyFile(final File chFile, final IOException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Unable to create class hierarchy file " + chFile.getAbsolutePath(), e);
    }

    @Override
    protected void exceptionCreatingSitesFile(final File sitesFile, final IOException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Unable to create sites file " + sitesFile.getAbsolutePath(), e);
    }
  }

  private void getInterestingProjectsAndBuildEntryMap(final Set<IProject> interestingProjects,
      final Map<String, Entry> classpathEntryMap) {
    // Get the list of open projects in the workpace
    final List<IProject> openProjects = getOpenProjects();

    /*
     * For each classpath entry we see if it is from a workspace project. If so,
     * we add the project to the list of interesting projects. Also, we compute
     * the location of the instrumentation for the classpath entry.
     */
    scanProjects(classpath, openProjects, interestingProjects, classpathEntryMap);
  }

  private static List<IProject> getOpenProjects() {
    final List<IProject> openProjects = new ArrayList<>();
    for (final IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (p.isOpen()) {
        openProjects.add(p);
      }
    }
    return openProjects;
  }

  private void scanProjects(final List<String> classpathEntries, final List<IProject> projects,
      final Set<IProject> interestingProjects, final Map<String, Entry> classpathEntryMap) {
    for (final String entry : classpathEntries) {
      final boolean isJar = !new File(entry).isDirectory();
      boolean foundProject = false;
      for (final IProject project : projects) {
        final String projectLoc = project.getLocation().toOSString();
        if (isFromProject(projectLoc, entry)) {
          final File newEntry = buildInstrumentedName(entry, projectLoc, isJar);
          classpathEntryMap.put(entry, new Entry(newEntry.getAbsolutePath(), isJar));
          interestingProjects.add(project);
          foundProject = true;
          break;
        }
      }

      /*
       * If we didn't find the project, then the entry exists outside of the
       * workspace.
       */
      if (!foundProject) {
        final String correctedEntry = fixLeadingDriveLetter(entry);
        final File newLocation =
        // new File(externalOutputDir, isJar ? correctedEntry :
        // (correctedEntry + ".jar"));
        new File(externalOutputDir, correctedEntry);
        classpathEntryMap.put(entry, new Entry(newLocation.getAbsolutePath(), isJar));
      }
    }
  }

  private static boolean isFromProject(final String projectLoc, final String entry) {
    if (entry.startsWith(projectLoc)) {
      return entry.length() == projectLoc.length() || entry.charAt(projectLoc.length()) == File.separatorChar;
    }
    return false;
  }

  private File buildInstrumentedName(final String entry, final String projectLoc, final boolean isJar) {
    final String projectDirName = projectLoc.substring(projectLoc.lastIndexOf(File.separatorChar) + 1);
    final String binaryName;
    if (projectLoc.length() == entry.length()) {
      // e.g. no bin directory
      binaryName = projectDirName;
    } else {
      binaryName = entry.substring(projectLoc.length() + 1);
    }
    // final String jarName = !isJar ? binaryName + ".jar" : binaryName;
    final String jarName = binaryName;
    final File newEntry = new File(new File(projectOutputDir, projectDirName), jarName);
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
