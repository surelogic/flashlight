package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Level;

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
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;

final class FlashlightVMRunner implements IVMRunner {
  private final IVMRunner delegateRunner;
  private final File runOutputDir;
  private final Map<String, String> projectEntries;
  
  
  
  public FlashlightVMRunner(
      final IVMRunner other, final File outDir, final Map<String, String> dirs) {
    delegateRunner = other;
    runOutputDir = outDir;
    projectEntries = dirs;
  }
  
  public void run(final VMRunnerConfiguration configuration, final ILaunch launch,
      final IProgressMonitor monitor) throws CoreException {
    
    /* Amount of work is 2 for each directory we need to process, plus 1
     * remaining unit for the delegate.
     */
    final int totalWork = 2 * projectEntries.size() + 1;
    final SubMonitor progress = SubMonitor.convert(monitor, totalWork);

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

  /**
   * Instrument the classfiles.
   * @param progress
   * @return Whether instrumentation was canceled.
   */
  private boolean instrumentClassfiles(final SubMonitor progress) {
    runOutputDir.mkdirs();
    final File logFile = new File(runOutputDir, "instrumentation.log");
    final File fieldsFile = new File(runOutputDir, "fields.txt");
    PrintWriter logOut = null;
    try {
      logOut = new PrintWriter(logFile);
      final EngineMessenger messenger = new PrintWriterMessenger(logOut);
      final Configuration rewriterConfig = new Configuration();
      final RewriteManager manager = new VMRewriteManager(rewriterConfig, messenger, fieldsFile, progress);
      
      for (final Map.Entry<String, String> entry : projectEntries.entrySet()) {
        manager.addDirToDir(new File(entry.getKey()), new File(entry.getValue()));
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
    final String[] newClassPath = new String[classPath.length + 1];
    for (int i = 0; i < classPath.length; i++) {
      final String newEntry = projectEntries.get(classPath[i]);
      if (newEntry != null) newClassPath[i] = newEntry;
      else newClassPath[i] = classPath[i];
    }
    
    /* (2) Also add the flashlight jar file to the classpath.
     */
    final IPath bundleBase = Activator.getDefault().getBundleLocation();
    if (bundleBase != null) {
      final IPath jarLocation = bundleBase.append("lib/flashlight-all.jar");
      newClassPath[classPath.length] = jarLocation.toOSString();
    } else {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
          "No bundle location found for the Flashlight plug-in."));
    }
    
    return newClassPath;
  }
  
  private VMRunnerConfiguration updateRunnerConfiguration(
      final VMRunnerConfiguration original, final String[] newClassPath) {
    final VMRunnerConfiguration newConfig = new VMRunnerConfiguration(original.getClassToLaunch(), newClassPath);
    newConfig.setBootClassPath(original.getBootClassPath());
    newConfig.setEnvironment(original.getEnvironment());
    newConfig.setProgramArguments(original.getProgramArguments());
    newConfig.setResumeOnStartup(original.isResumeOnStartup());
    newConfig.setVMArguments(original.getVMArguments());
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
        final SubMonitor sub) {
      super(c, m, ff);
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
  }
}
