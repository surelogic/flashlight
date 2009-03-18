package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
//import java.util.Properties;
import java.util.*;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.ui.progress.UIJob;

import com.surelogic._flashlight.common.*;
import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic.common.eclipse.MemoryUtility;
import com.surelogic.common.eclipse.SourceZip;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.jobs.LaunchTerminationDetectionJob;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import static com.surelogic._flashlight.common.InstrumentationConstants.*;

final class FlashlightVMRunner implements IVMRunner {
  private static final String MAX_HEAP_PREFIX = "-Xmx";
  private static final long DEFAULT_MAX_HEAP_SIZE = 64 * 1024 * 1024;
  
  private static final String LOG_FILE_NAME = "instrumentation.log";
  private static final String FIELDS_FILE_NAME = "fields.txt";
  private static final String SITES_FILE_NAME = "sites.txt";
  private static final String FILTERS_FILE_NAME = "filters.txt";
  
  private final IVMRunner delegateRunner;
  private final File runOutputDir;
  private final String mainTypeName;
  private final File fieldsFile;
  private final File sitesFile;
  private final File logFile;
  private final File filtersFile;
  private final Map<String, String> userEntries;
  private final Map<String, String> userJars;
  private final Map<String, String> bootEntries;
  private final Set<String> everythingElse;
  private final Set<IProject> interestingProjects;
  private final String datePostfix;
  private final String pathToFlashlightLib;
  
  /* We are guaranteed that the outDir exists in the file system already */
  public FlashlightVMRunner(
      final IVMRunner other, final File outDir, final Map<String, String> dirs,
      final Map<String, String> jars, final Map<String, String> bdirs,
      final Set<String> ee,
      final Set<IProject> prjs, final String main, final String date)
  throws CoreException {
    delegateRunner = other;
    runOutputDir = outDir;
    userEntries = dirs;
    userJars = jars;
    bootEntries = bdirs;
    everythingElse = ee;
    interestingProjects = prjs;
    mainTypeName = main;
    datePostfix = date;
    
    fieldsFile = new File(runOutputDir, FIELDS_FILE_NAME);
    sitesFile = new File(runOutputDir, SITES_FILE_NAME);
    logFile = new File(runOutputDir, LOG_FILE_NAME);
    filtersFile = new File(runOutputDir, FILTERS_FILE_NAME);

    // Get the path ot the flashlight-runtime.jar
    final IPath bundleBase = Activator.getDefault().getBundleLocation();
    if (bundleBase != null) {
      final IPath jarLocation = bundleBase.append("lib/flashlight-runtime.jar");
      pathToFlashlightLib = jarLocation.toOSString();
    } else {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
          "No bundle location found for the Flashlight plug-in."));
    }

  }
  
  public void run(final VMRunnerConfiguration configuration, final ILaunch launch,
      final IProgressMonitor monitor) throws CoreException {
    
    /*
     * Amount of work is 1 for each project we need to zip, 2 for each directory
     * we need to process, plus 1 remaining unit for the delegate.
     */
    final int totalWork =
      interestingProjects.size() +
      everythingElse.size() + 
      (2 * (userEntries.size() + userJars.size() + bootEntries.size())) + 
      1;
    final SubMonitor progress = SubMonitor.convert(monitor, totalWork);

    /* Create the source zip */
    if (createSourceZips(progress)) {
      // Canceled, abort early
      return;
    }
    
    /* Build the instrumented class files.  First we scan each directory
     * to the build the field database, and then we instrument each directory.
     */
    if (instrumentClassfiles(progress)) {
      // Canceled, abort early
      return;
    }
    
    /* Fix the classpath.
     */
    final String[] newClassPath = updateClassPath(configuration);
    final String[] newBootClassPath = updateBootClassPath(configuration);
    
    /* Create an updated runner configuration. */
    final VMRunnerConfiguration newConfig =	updateRunnerConfiguration(
    	    configuration, launch.getLaunchConfiguration(), newClassPath,
    	    newBootClassPath);
    
    /* Done with our set up, call the real runner */
    delegateRunner.run(newConfig, launch, monitor);
    
    /* Create and launch a job that detects when the instrumented run terminates,
     * and switches to the flashlight perspective on termination.
     */
    final LaunchTerminationDetectionJob terminationDetector = new LaunchTerminationDetectionJob(
        launch, LaunchTerminationDetectionJob.DEFAULT_PERIOD) {
      @Override
      protected IStatus terminationAction() {
        final UIJob job = new SwitchToFlashlightPerspectiveJob();
        job.schedule();
        return Status.OK_STATUS;
      }
    };
    terminationDetector.reschedule();
  }
  
  private boolean createSourceZips(final SubMonitor progress) {
    final File sourceDir = new File(runOutputDir, "source");
    sourceDir.mkdir();
    
    for (final IProject project : interestingProjects) {
      final String projectName = project.getName();
      progress.subTask("Creating source zip for " + projectName);
      final SourceZip srcZip = new SourceZip(project);
      final File zipFile = new File(sourceDir, projectName + ".src.zip");
      try {
        srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
      } catch(final IOException e) {
        SLLogger.getLogger().log(Level.SEVERE,
            "Unable to create source zip for project " + projectName, e);
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
   * @param progress
   * @return Whether instrumentation was canceled.
   */
  private boolean instrumentClassfiles(final SubMonitor progress) {
    runOutputDir.mkdirs();
    PrintWriter logOut = null;
    try {
      logOut = new PrintWriter(logFile);
      final RewriteMessenger messenger = new PrintWriterMessenger(logOut);
      final Configuration rewriterConfig = new Configuration();
      
//      final Properties p = new Properties();
//      Configuration.writeDefaultProperties(p);
//      p.put(Configuration.STORE_CLASS_NAME_PROPERTY, "com/surelogic/_flashlight/rewriter/test/DebugStore");
//      final Configuration rewriterConfig = new Configuration(p);
      
      final RewriteManager manager =
        new VMRewriteManager(rewriterConfig, messenger,
            fieldsFile, sitesFile, progress);
      for (final String nonInstruntedEntry: everythingElse) {
        final File asFile = new File(nonInstruntedEntry);
        if (asFile.isDirectory()) {
          manager.addClasspathDir(asFile);
        } else {
          manager.addClasspathJar(asFile);
        }
      }
      for (final Map.Entry<String, String> entry : bootEntries.entrySet()) {
        manager.addDirToJar(new File(entry.getKey()), new File(entry.getValue()), null);
      }
      for (final Map.Entry<String, String> entry : userJars.entrySet()) {
        manager.addJarToJar(new File(entry.getKey()), new File(entry.getValue()), null);
      }
      for (final Map.Entry<String, String> entry : userEntries.entrySet()) {
        manager.addDirToJar(new File(entry.getKey()), new File(entry.getValue()), null);
      }
      
      try {
        manager.execute();
      } catch (final CanceledException e) {
        /* Do nothing, the user canceled the launch early.
         * Caught to prevent propagation of the local exception being used
         * as a control flow hack.
         */
      }
    } catch(final FileNotFoundException e) {
      SLLogger.getLogger().log(Level.SEVERE,
          "Unable to create instrumentation log file " + logFile.getAbsolutePath(), e);
    } finally {
      if (logOut != null) {
        logOut.close();
      }
    }
    
    return false;
  }
  
  private String[] updateClassPath(final VMRunnerConfiguration configuration) {
    /* (1) Replace each project "binary output directory" with
     * its corresponding instrumented directory.
     */
    final String[] classPath = configuration.getClassPath();
    final List<String> newClassPathList = new ArrayList<String>(classPath.length+1);
    for (int i = 0; i < classPath.length; i++) {
      final String oldEntry = classPath[i];
      final String newEntry = userEntries.get(oldEntry);
      if (newEntry != null) {
        newClassPathList.add(newEntry);
      } else {
        final String jarEntry = userJars.get(oldEntry);
        newClassPathList.add(jarEntry != null ? jarEntry : oldEntry);
      }
    }
    
    /* (2) Also add the flashlight jar file to the classpath, unless the 
     * bootclasspath is non-empty.  If it's not empty we add the
     * flashlight lib to the bootclasspath so it is accessible to the
     * instrumented bootclasspath items. 
     */
    if (bootEntries.isEmpty()) {
      newClassPathList.add(pathToFlashlightLib);
    }
    
    final String[] newClassPath = new String[newClassPathList.size()];
    return newClassPathList.toArray(newClassPath);
  }
  
  private String[] updateBootClassPath(final VMRunnerConfiguration configuration) {
    /* (1) Replace each project "binary output directory" with
     * its corresponding instrumented directory.
     */
    final String[] classPath = configuration.getBootClassPath();
    if (classPath != null && classPath.length != 0) {
      final List<String> newClassPathList = new ArrayList<String>(classPath.length+1);
      for (int i = 0; i < classPath.length; i++) {
        final String oldEntry = classPath[i];
        final String newEntry = bootEntries.get(oldEntry);
        newClassPathList.add((newEntry == null) ? oldEntry : newEntry);
      }
      
      /* (2) Also add the flashlight jar file to the classpath, if the 
       * bootclasspath is non-empty
       */
      if (!bootEntries.isEmpty()) {
        newClassPathList.add(pathToFlashlightLib);
      }
      
      final String[] newClassPath = new String[newClassPathList.size()];
      return newClassPathList.toArray(newClassPath);
    } else {
      return classPath;
    }
  }
  
  private VMRunnerConfiguration updateRunnerConfiguration(
      final VMRunnerConfiguration original, ILaunchConfiguration launch,
      final String[] newClassPath, final String[] newBootClassPath) {
    // Create a new configuration and update the class path
    final VMRunnerConfiguration newConfig =
      new VMRunnerConfiguration(original.getClassToLaunch(), newClassPath);
    
    // Update the VM arguments: We need to add parameters for the Flashlight Store
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
      final int lastChar = maxHeapSetting.charAt(maxHeapSetting.length()-1);
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
    final List<String> newVmArgsList = new ArrayList<String>(vmArgs.length + 11);
    try {
    final Preferences prefs = Activator.getDefault().getPluginPreferences();
    final int rawQSize = launch.getAttribute(PreferenceConstants.P_RAWQ_SIZE, 
                                             prefs.getInt(PreferenceConstants.P_RAWQ_SIZE));
    final int refSize = launch.getAttribute(PreferenceConstants.P_REFINERY_SIZE, 
                                            prefs.getInt(PreferenceConstants.P_REFINERY_SIZE));
    final int outQSize = launch.getAttribute(PreferenceConstants.P_OUTQ_SIZE, 
                                             prefs.getInt(PreferenceConstants.P_OUTQ_SIZE));
    final int cPort = launch.getAttribute(PreferenceConstants.P_CONSOLE_PORT, 
                                          prefs.getInt(PreferenceConstants.P_CONSOLE_PORT));
    final String useBinary = launch.getAttribute(PreferenceConstants.P_OUTPUT_TYPE, 
    		                                      prefs.getString(PreferenceConstants.P_OUTPUT_TYPE));
    final boolean compress = launch.getAttribute(PreferenceConstants.P_COMPRESS_OUTPUT, 
                                                 prefs.getBoolean(PreferenceConstants.P_COMPRESS_OUTPUT));
    final boolean useSpy = launch.getAttribute(PreferenceConstants.P_USE_SPY, 
                                               prefs.getBoolean(PreferenceConstants.P_USE_SPY));
    final boolean useRefinery = launch.getAttribute(PreferenceConstants.P_USE_REFINERY, 
                                                    prefs.getBoolean(PreferenceConstants.P_USE_REFINERY));
    final boolean useFiltering = launch.getAttribute(PreferenceConstants.P_USE_FILTERING, 
                                                     prefs.getBoolean(PreferenceConstants.P_USE_FILTERING));
    if (useFiltering) {
    	PrintWriter out = null;
		try {
			out = new PrintWriter(filtersFile);
	    	for(Object o : launch.getAttributes().entrySet()) {
	    		Map.Entry e = (Map.Entry) o;
	    		String key  = (String) e.getKey();
	    		Object val  = e.getValue();
	    		if (key.startsWith(PreferenceConstants.P_FILTER_PKG_PREFIX) && 
	    			Boolean.TRUE.equals(val)) {
	    			//System.out.println(key+": "+e.getValue());
	    			out.println(key.substring(PreferenceConstants.P_FILTER_PKG_PREFIX.length()));
	    		}
	    	}
	    	out.println();
	
	    	newVmArgsList.add("-D"+FL_FILTERS_FILE+"=" + filtersFile.getAbsolutePath());
		} catch (FileNotFoundException ex) {
			SLLogger.getLogger().log(Level.SEVERE, "Couldn't create filters file: "+filtersFile.getAbsolutePath(), ex);
		} finally {
			if (out != null) {
				out.close();
			}
		}

    }
    newVmArgsList.add("-DFL_RUN=" + mainTypeName);
    newVmArgsList.add("-D"+FL_DIR+"=" + runOutputDir.getAbsolutePath());
    newVmArgsList.add("-D"+FL_FIELDS_FILE+"=" + fieldsFile.getAbsolutePath());
    newVmArgsList.add("-D"+FL_SITES_FILE+"=" + sitesFile.getAbsolutePath());
    newVmArgsList.add("-D"+FL_RAWQ_SIZE+"=" + rawQSize);
    newVmArgsList.add("-D"+FL_REFINERY_SIZE+"=" + refSize);
    newVmArgsList.add("-D"+FL_OUTQ_SIZE+"=" + outQSize);
    newVmArgsList.add("-D"+FL_CONSOLE_PORT+"=" + cPort);
    newVmArgsList.add("-D"+FL_DATE_OVERRIDE+"=" + datePostfix);
    newVmArgsList.add("-D"+FL_OUTPUT_TYPE+"="+OutputType.get(useBinary, compress));
    if (!useRefinery) {
    	newVmArgsList.add("-D"+FL_REFINERY_OFF+"=true");
    }
    if (!useSpy) newVmArgsList.add("-D"+FL_NO_SPY+"=true");
    } catch(CoreException e) {
    	SLLogger.getLogger().log(Level.SEVERE, "Couldn't setup launch for "+launch.getName(), e);
    	return null;
    }
    
    if (PreferenceConstants.getAutoIncreaseHeapAtLaunch()) {
      final long maxSystemHeapSize = ((long) MemoryUtility.computeMaxMemorySizeInMb()) << 20;
      final long newHeapSizeRaw = Math.min(3 * maxHeapSize, maxSystemHeapSize);
      newVmArgsList.add(MAX_HEAP_PREFIX + Long.toString(newHeapSizeRaw));
      SLLogger.getLogger().log(Level.INFO,
          "Increasing maximum heap size for launched application to "
              + newHeapSizeRaw + " bytes from " + maxHeapSize + " bytes");
      
      // Add the original arguments afterwards, skipping the original -Xmx setting
      for (int i = 0; i < vmArgs.length; i++) {
        if (i != heapSettingPos) newVmArgsList.add(vmArgs[i]);
      }
    } else {
      // Add the original arguments unchanged
      for (int i = 0; i < vmArgs.length; i++) {
        newVmArgsList.add(vmArgs[i]);
      }
    }
    // Get the new array of vm arguments
    String[] newVmArgs = new String[newVmArgsList.size()];
    newConfig.setVMArguments(newVmArgsList.toArray(newVmArgs));
    
    // Copy the rest of the arguments unchanged
    newConfig.setBootClassPath(newBootClassPath);
    newConfig.setEnvironment(original.getEnvironment());
    newConfig.setProgramArguments(original.getProgramArguments());
    newConfig.setResumeOnStartup(original.isResumeOnStartup());
    newConfig.setVMSpecificAttributesMap(updateVMSpecificAttributesMap(original.getVMSpecificAttributesMap()));
    newConfig.setWorkingDirectory(original.getWorkingDirectory());
    return newConfig;
  }
  
  private Map updateVMSpecificAttributesMap(final Map original) {
    if (original == null) {
      return null;
    }
    
    final Map updated = new HashMap();
    final String[] originalPrepend = (String[]) original.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND);
    final String[] originalBootpath = (String[]) original.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH);
    final String[] originalAppend = (String[]) original.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_APPEND);
    
    boolean needsFlashlightLib = false;
    if (originalPrepend != null) {
      final List<String> newPrependList = new ArrayList(originalPrepend.length);
      needsFlashlightLib |= updateBootpathArray(originalPrepend, newPrependList);
      if (newPrependList.size() != 0) {
        final String[] newPrepend = new String[originalPrepend.length];
        updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, newPrependList.toArray(newPrepend));
      }
    }
    if (originalBootpath != null) {
      final List<String> newBootpathList = new ArrayList(originalBootpath.length);
      needsFlashlightLib |= updateBootpathArray(originalBootpath, newBootpathList);
      for (final String entry : originalBootpath) {
        final String newEntry = bootEntries.get(entry);
        if (newEntry != null) {
          newBootpathList.add(newEntry);
          needsFlashlightLib = true;
        } else {
          newBootpathList.add(entry);
        }
      }
      final String[] newBootpath = new String[originalBootpath.length];
      updated.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH, newBootpathList.toArray(newBootpath));
    }
    List<String> newAppendList = null;
    if (originalAppend != null) {
      newAppendList = new ArrayList(originalAppend.length+1);
      for (final String entry : originalAppend) {
        final String newEntry = bootEntries.get(entry);
        if (newEntry != null) {
          newAppendList.add(newEntry);
          needsFlashlightLib = true;
        } else {
          newAppendList.add(entry);
        }
      }
    }
    if (needsFlashlightLib) {
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
  
  private boolean updateBootpathArray(final String[] originalBoothpath, final List<String> newBootpath) {
    boolean needsFlashlightLib = false;
    for (final String entry : originalBoothpath) {
      final String newEntry = bootEntries.get(entry);
      if (newEntry != null) {
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
    
    public VMRewriteManager(
        final Configuration c, final RewriteMessenger m, final File ff,
        final File sf, final SubMonitor sub) {
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
    protected void postInstrument(final String srcPath, final String destPath) {
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
    protected void exceptionInstrument(
        final String srcPath, final String destPath, final IOException e) {
      SLLogger.getLogger().log(Level.SEVERE,
          "Error instrumenting classfiles in " + srcPath, e);
    }
  
    @Override
    protected void exceptionCreatingFieldsFile(
        final File fieldsFile, final FileNotFoundException e) {
      SLLogger.getLogger().log(Level.SEVERE,
          "Unable to create fields file " + fieldsFile.getAbsolutePath(), e);
    }

    @Override
    protected void exceptionCreatingSitesFile(final File sitesFile,
        final FileNotFoundException e) {
      SLLogger.getLogger().log(Level.SEVERE,
          "Unable to create sites file " + sitesFile.getAbsolutePath(), e);
    }
  }
}
