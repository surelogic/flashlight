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
import com.surelogic._flashlight.rewriter.RewriteEngine;
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
    
    // Build the instrumented class files

    /* Amount of work is 2 for each directory we need to process, plus 1
     * remaining unit for the delegate.
     */
    final int totalWork = 2 * projectEntries.size() + 1;
    final SubMonitor progress = SubMonitor.convert(monitor, totalWork);
    
    runOutputDir.mkdirs();
    final File logFile = new File(runOutputDir, "instrumentation.log");
    final File fieldsFile = new File(runOutputDir, "fields.txt");
    PrintWriter logOut = null;
    try {
      logOut = new PrintWriter(logFile);

      final Configuration rewriterConfig = new Configuration();
      PrintWriter fieldsOut = null;
      try {
        fieldsOut = new PrintWriter(fieldsFile);
        final EngineMessenger messenger = new PrintWriterMessenger(logOut);
        final RewriteEngine engine = new RewriteEngine(rewriterConfig, messenger, fieldsOut);
        
        for (Map.Entry<String, String> entry : projectEntries.entrySet()) {
          final String srcDir = entry.getKey();
          progress.subTask("Scanning " + srcDir);
          messenger.info("Scanning " + srcDir);
          try {
            messenger.increaseNesting();
            engine.scanDirectory(new File(srcDir));
            
            // check for cancellation
            if (progress.isCanceled()) {
              return;
            }
          } catch (final IOException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                "Error scanning classfiles in directory " + srcDir, e);
          } finally {
            messenger.decreaseNesting();
            progress.worked(1);
          }
        }

        for (Map.Entry<String, String> entry : projectEntries.entrySet()) {
          final String srcDir = entry.getKey();
          final String destDir = entry.getValue();
          progress.subTask("Instrumenting " + srcDir);
          messenger.info("Instrumenting " + srcDir + " to " + new File(destDir));
          try {
            messenger.increaseNesting();
            engine.rewriteDirectoryToDirectory(new File(srcDir), new File(destDir));

            // check for cancellation
            if (progress.isCanceled()) {
              return;
            }   
          } catch (final IOException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                "Error instrumenting classfiles in directory " + srcDir, e);
          } finally {
            messenger.decreaseNesting();
            progress.worked(1);
          }
        }
      } catch(final FileNotFoundException e) {
        SLLogger.getLogger().log(Level.SEVERE,
            "Unable to create fields file " + fieldsFile.getAbsolutePath(), e);
      } finally {
        if (fieldsOut != null) {
          fieldsOut.close();
        }
      }
    } catch(final FileNotFoundException e) {
      SLLogger.getLogger().log(Level.SEVERE,
          "Unable to create instrumentation log file " + logFile.getAbsolutePath(), e);
    } finally {
      if (logOut != null) {
        logOut.close();
      }
    }
    
    // Fix up the classpath
    final String[] classPath = configuration.getClassPath();
    final String[] newClassPath = new String[classPath.length + 1];
    for (int i = 0; i < classPath.length; i++) {
      final String newEntry = projectEntries.get(classPath[i]);
      if (newEntry != null) {
        System.out.println("Updated to " + newEntry);
        newClassPath[i] = newEntry;
      } else {
        newClassPath[i] = classPath[i];
        System.out.println(newClassPath[i]);
      }
    }
    
    final IPath bundleBase = Activator.getDefault().getBundleLocation();
    if (bundleBase != null) {
      IPath jarLocation = bundleBase.append("lib/flashlight-all.jar");
      newClassPath[classPath.length] = jarLocation.toOSString();
    } else {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
          "No bundle location found for the Flashlight plug-in."));
    }
    
    final VMRunnerConfiguration newConfig = new VMRunnerConfiguration(configuration.getClassToLaunch(), newClassPath);
    newConfig.setBootClassPath(configuration.getBootClassPath());
    newConfig.setEnvironment(configuration.getEnvironment());
    newConfig.setProgramArguments(configuration.getProgramArguments());
    newConfig.setResumeOnStartup(configuration.isResumeOnStartup());
    newConfig.setVMArguments(configuration.getVMArguments());
    newConfig.setVMSpecificAttributesMap(configuration.getVMSpecificAttributesMap());
    newConfig.setWorkingDirectory(configuration.getWorkingDirectory());
    
    /* Done with our set up, call the real runner */
    delegateRunner.run(newConfig, launch, monitor);
  }

}
