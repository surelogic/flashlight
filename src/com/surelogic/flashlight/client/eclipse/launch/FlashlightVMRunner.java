package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
//import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.EngineMessenger;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic.common.eclipse.SourceZip;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

final class FlashlightVMRunner implements IVMRunner {
  private static final String LOG_FILE_NAME = "instrumentation.log";
  private static final String FIELDS_FILE_NAME = "fields.txt";
  private static final String SITES_FILE_NAME = "sites.txt";
  
  private final IVMRunner delegateRunner;
  private final File runOutputDir;
  private final String mainTypeName;
  private final File fieldsFile;
  private final File sitesFile;
  private final File logFile;
  private final Map<String, String> projectEntries;
  private final Set<IProject> interestingProjects;
  private final String datePostfix;
  
  /* We are guaranteed that the outDir exists in the file system already */
  public FlashlightVMRunner(
      final IVMRunner other, final File outDir, final Map<String, String> dirs,
      final Set<IProject> prjs, final String main, final String date) {
    delegateRunner = other;
    runOutputDir = outDir;
    projectEntries = dirs;
    interestingProjects = prjs;
    mainTypeName = main;
    datePostfix = date;
    
    fieldsFile = new File(runOutputDir, FIELDS_FILE_NAME);
    sitesFile = new File(runOutputDir, SITES_FILE_NAME);
    logFile = new File(runOutputDir, LOG_FILE_NAME);
  }
  
  public void run(final VMRunnerConfiguration configuration, final ILaunch launch,
      final IProgressMonitor monitor) throws CoreException {
    
    /*
     * Amount of work is 1 for each project we need to zip, 2 for each directory
     * we need to process, plus 1 remaining unit for the delegate.
     */
    final int totalWork = interestingProjects.size() + (2 * projectEntries.size()) + 1;
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
    
    /* Create an updated runner configuration. */
    final VMRunnerConfiguration newConfig = updateRunnerConfiguration(configuration, newClassPath);
    
    /* Done with our set up, call the real runner */
    delegateRunner.run(newConfig, launch, monitor);
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
        srcZip.generateSourceZip(zipFile.getAbsolutePath(), project, false);
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
      final EngineMessenger messenger = new PrintWriterMessenger(logOut);
      final Configuration rewriterConfig = new Configuration();
      
//      final Properties p = new Properties();
//      Configuration.writeDefaultProperties(p);
//      p.put(Configuration.STORE_CLASS_NAME_PROPERTY, "com/surelogic/_flashlight/rewriter/test/DebugStore");
//      final Configuration rewriterConfig = new Configuration(p);
      
      final RewriteManager manager =
        new VMRewriteManager(rewriterConfig, messenger,
            fieldsFile, sitesFile, progress);
      
      for (final Map.Entry<String, String> entry : projectEntries.entrySet()) {
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
  
  private String[] updateClassPath(final VMRunnerConfiguration configuration) 
      throws CoreException {
    /* (1) Replace each project "binary output directory" with
     * its corresponding instrumented directory.
     */
    final String[] classPath = configuration.getClassPath();
    final List<String> newClassPathList = new ArrayList<String>(classPath.length + 1);
    for (int i = 0; i < classPath.length; i++) {
      final String newEntry = projectEntries.get(classPath[i]);
      if (newEntry != null) newClassPathList.add(newEntry);
      else newClassPathList.add(classPath[i]);
    }
    
    /* (2) Also add the flashlight jar file to the classpath.
     */
    final IPath bundleBase = Activator.getDefault().getBundleLocation();
    if (bundleBase != null) {
      final IPath jarLocation = bundleBase.append("lib/flashlight-runtime.jar");
      newClassPathList.add(jarLocation.toOSString());
    } else {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
          "No bundle location found for the Flashlight plug-in."));
    }
    
    final String[] newClassPath = new String[newClassPathList.size()];
    return newClassPathList.toArray(newClassPath);
  }
  
  private VMRunnerConfiguration updateRunnerConfiguration(
      final VMRunnerConfiguration original, final String[] newClassPath) {
    // Create a new configuration and update the class path
    final VMRunnerConfiguration newConfig = new VMRunnerConfiguration(original.getClassToLaunch(), newClassPath);
    
    // Update the VM arguments: We need to add parameters for the Flashlight Store
    final String[] vmArgs = original.getVMArguments();
    // We will add at most ten arguments, but maybe less
    final List<String> newVmArgsList = new ArrayList<String>(vmArgs.length + 10);
    
    final String rawQSize = Activator.getDefault().getPluginPreferences().getString(PreferenceConstants.P_RAWQ_SIZE);
    final String refSize = Activator.getDefault().getPluginPreferences().getString(PreferenceConstants.P_REFINERY_SIZE);
    final String outQSize = Activator.getDefault().getPluginPreferences().getString(PreferenceConstants.P_OUTQ_SIZE);
    final String cPort = Activator.getDefault().getPluginPreferences().getString(PreferenceConstants.P_CONSOLE_PORT);
    final boolean useSpy = Activator.getDefault().getPluginPreferences().getBoolean(PreferenceConstants.P_USE_SPY);
    newVmArgsList.add("-DFL_RUN=" + mainTypeName);
    newVmArgsList.add("-DFL_DIR=" + runOutputDir.getAbsolutePath());
    newVmArgsList.add("-DFL_FIELDS_FILE=" + fieldsFile.getAbsolutePath());
    newVmArgsList.add("-DFL_SITES_FILE=" + sitesFile.getAbsolutePath());
    newVmArgsList.add("-DFL_RAWQ_SIZE=" + rawQSize);
    newVmArgsList.add("-DFL_REFINERY_SIZE=" + refSize);
    newVmArgsList.add("-DFL_OUTQ_SIZE=" + outQSize);
    newVmArgsList.add("-DFL_CONSOLE_PORT=" + cPort);
    newVmArgsList.add("-DFL_DATE_OVERRIDE=" + datePostfix);
    
    if (!useSpy) newVmArgsList.add("-DFL_NO_SPY=true");
    // Add the original arguments afterwards
    newVmArgsList.addAll(Arrays.asList(vmArgs));
    // Get the new array of vm arguments
    String[] newVmArgs = new String[newVmArgsList.size()];
    newConfig.setVMArguments(newVmArgsList.toArray(newVmArgs));
    
    // Copy the rest of the arguments unchanged
    newConfig.setBootClassPath(original.getBootClassPath());
    newConfig.setEnvironment(original.getEnvironment());
    newConfig.setProgramArguments(original.getProgramArguments());
    newConfig.setResumeOnStartup(original.isResumeOnStartup());
    newConfig.setVMSpecificAttributesMap(original.getVMSpecificAttributesMap());
    newConfig.setWorkingDirectory(original.getWorkingDirectory());
    return newConfig;
  }
  
  
  
  private static final class CanceledException extends RuntimeException {
    public CanceledException() {
      super();
    }
  }



  private static final class VMRewriteManager extends RewriteManager {
    private final SubMonitor progress;
    
    public VMRewriteManager(
        final Configuration c, final EngineMessenger m, final File ff,
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
