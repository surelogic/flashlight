package com.android.ide.eclipse.adt.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.android.SdkConstants;
import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidPrintStream;
import com.android.ide.eclipse.adt.internal.build.AaptExecException;
import com.android.ide.eclipse.adt.internal.build.AaptParser;
import com.android.ide.eclipse.adt.internal.build.AaptResultException;
import com.android.ide.eclipse.adt.internal.build.BuildHelper;
import com.android.ide.eclipse.adt.internal.build.BuildHelper.ResourceMarker;
import com.android.ide.eclipse.adt.internal.build.DexException;
import com.android.ide.eclipse.adt.internal.build.Messages;
import com.android.ide.eclipse.adt.internal.build.NativeLibInJarException;
import com.android.ide.eclipse.adt.internal.build.builders.PostCompilerBuilder;
import com.android.ide.eclipse.adt.internal.launch.FlashlightAndroidLaunchConfigurationDelegate.FLData;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs.BuildVerbosity;
import com.android.ide.eclipse.adt.internal.project.ApkInstallManager;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.sdk.ProjectState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.internal.build.DebugKeyProvider.KeytoolException;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteManager.AlreadyInstrumentedException;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.dialogs.ShowTextDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.launch.LaunchHelper;
import com.surelogic.flashlight.client.eclipse.launch.LaunchUtils;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.schema.SchemaResources;

@SuppressWarnings({ "deprecation", "restriction" })
public class AndroidBuildUtil {

  private AndroidBuildUtil() {
    // Do nothing
  }

  public static FlashlightAndroidLaunchConfigurationDelegate.FLData doFullIncrementalDebugBuildForTest(RunId runId,
      final ILaunchConfiguration launchConfig, final IProject project, final IProgressMonitor monitor) throws CoreException {
    List<IJavaProject> androidProjectList = new ArrayList<>();
    try {
      androidProjectList = ProjectHelper.getAndroidProjectDependencies(BaseProjectHelper.getJavaProject(project));
    } catch (JavaModelException e) {
      AdtPlugin.printErrorToConsole(project, e);
    }
    // Recursively build dependencies
    FLData data = null;
    for (IJavaProject dependency : androidProjectList) {
      data = doFullIncrementalDebugBuild(runId, launchConfig, dependency.getProject(), monitor);
    }

    // Do an incremental build to pick up all the deltas
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

    // If the preferences indicate not to use post compiler optimization
    // then the incremental build will have done everything necessary,
    // otherwise,
    // we have to run the final builder manually (if requested).
    if (AdtPrefs.getPrefs().getBuildSkipPostCompileOnFileSave()) {
      // Create the map to pass to the PostC builder
      Map<String, String> args = new TreeMap<>();
      args.put(PostCompilerBuilder.POST_C_REQUESTED, ""); //$NON-NLS-1$

      // call the post compiler manually, forcing FULL_BUILD otherwise
      // Eclipse won't
      // call the builder since the delta is empty.
      project.build(IncrementalProjectBuilder.FULL_BUILD, PostCompilerBuilder.ID, args, monitor);
    }

    // because the post compiler builder does a delayed refresh due to
    // library not picking the refresh up if it's done during the build,
    // we want to force a refresh here as this call is generally asking for
    // a build to use the apk right after the call.
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    return data;
  }

  public static FlashlightAndroidLaunchConfigurationDelegate.FLData doFullIncrementalDebugBuild(RunId runId,
      final ILaunchConfiguration launchConfig, final IProject project, final IProgressMonitor monitor) throws CoreException {
    // First have android do their full build
    ProjectHelper.doFullIncrementalDebugBuild(project, monitor);

    // Get list of projects that we depend on
    List<IJavaProject> androidProjectList = new ArrayList<>();
    try {
      androidProjectList = ProjectHelper.getAndroidProjectDependencies(BaseProjectHelper.getJavaProject(project));
    } catch (JavaModelException e) {
      AdtPlugin.printErrorToConsole(project, e);
    }
    // Recursively build dependencies
    for (IJavaProject dependency : androidProjectList) {
      ProjectHelper.doFullIncrementalDebugBuild(dependency.getProject(), monitor);
    }

    // Do an incremental build to pick up all the deltas
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

    // If the preferences indicate not to use post compiler optimization
    // then the incremental build will have done everything necessary,
    // otherwise,
    // we have to run the final builder manually (if requested).
    if (AdtPrefs.getPrefs().getBuildSkipPostCompileOnFileSave()) {
      // Create the map to pass to the PostC builder
      Map<String, String> args = new TreeMap<>();
      args.put(PostCompilerBuilder.POST_C_REQUESTED, ""); //$NON-NLS-1$

      // call the post compiler manually, forcing FULL_BUILD otherwise
      // Eclipse won't
      // call the builder since the delta is empty.
      project.build(IncrementalProjectBuilder.FULL_BUILD, PostCompilerBuilder.ID, args, monitor);
    }

    // because the post compiler builder does a delayed refresh due to
    // library not picking the refresh up if it's done during the build,
    // we want to force a refresh here as this call is generally asking for
    // a build to use the apk right after the call.
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    // list of referenced projects. This is a mix of java projects and
    // library projects
    // and is computed below.
    IProject[] allRefProjects = null;

    ProjectState projectState = Sdk.getProjectState(project);
    if (projectState == null) {
      return null;
    }
    BuildToolInfo buildToolInfo = projectState.getBuildToolInfo();
    if (buildToolInfo == null) {
      buildToolInfo = Sdk.getCurrent().getLatestBuildTool();
      if (buildToolInfo == null) {
        AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
            "No \"Build Tools\" package available; use SDK Manager to install one.");
        throw new IllegalStateException("No \"Build Tools\" package available; use SDK Manager to install one.");
      } else {
        AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project,
            String.format("Using default Build Tools revision %s", buildToolInfo.getRevision()));
      }
    }
    boolean isLibrary = projectState.isLibrary();

    List<IProject> libProjects = projectState.getFullLibraryProjects();

    IJavaProject javaProject = JavaCore.create(project);

    // get the list of referenced projects.
    // get the list of referenced projects.
    List<IProject> javaProjects = ProjectHelper.getReferencedProjects(project);
    List<IJavaProject> referencedJavaProjects = BuildHelper.getJavaProjects(javaProjects);

    // mix the java project and the library projecst
    final int size = libProjects.size() + javaProjects.size();
    ArrayList<IProject> refList = new ArrayList<>(size);
    refList.addAll(libProjects);
    refList.addAll(javaProjects);
    allRefProjects = refList.toArray(new IProject[size]);

    // get the android output folder
    IFolder androidOutputFolder = BaseProjectHelper.getAndroidOutputFolder(project);
    IFolder resOutputFolder = androidOutputFolder.getFolder(SdkConstants.FD_RES);

    // now we need to get the classpath list
    List<IPath> sourceList = BaseProjectHelper.getSourceClasspaths(javaProject);

    AndroidPrintStream mOutStream = new AndroidPrintStream(project, null /* prefix */, AdtPlugin.getOutStream());
    AndroidPrintStream mErrStream = new AndroidPrintStream(project, null /* prefix */, AdtPlugin.getOutStream());

    ResourceMarker mResourceMarker = new ResourceMarker() {
      @Override
      public void setWarning(final IResource resource, final String message) {
        BaseProjectHelper.markResource(resource, AdtConstants.MARKER_PACKAGING, message, IMarker.SEVERITY_WARNING);
      }
    };
    // public BuildHelper(@com.android.annotations.NonNull
    // com.android.ide.eclipse.adt.internal.sdk.ProjectState projectState,
    // @com.android.annotations.NonNull com.android.sdklib.BuildToolInfo
    // buildToolInfo, @com.android.annotations.NonNull
    // com.android.ide.eclipse.adt.AndroidPrintStream outStream,
    // @com.android.annotations.NonNull
    // com.android.ide.eclipse.adt.AndroidPrintStream errStream, boolean
    // forceJumbo, boolean disableDexMerger, boolean debugMode, boolean
    // verbose,
    // com.android.ide.eclipse.adt.internal.build.BuildHelper.ResourceMarker
    // resMarker)
    BuildHelper helper = new BuildHelper(projectState, buildToolInfo, mOutStream, mErrStream,
        false /* jumbo mode doesn't matter here */,
        false /*
               * dex merger doesn't matter here
               */, true /* debugMode */, AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE, mResourceMarker);

    IPath androidBinLocation = androidOutputFolder.getLocation();
    String osAndroidBinPath = androidBinLocation.toOSString();

    String classesDexPath = osAndroidBinPath + File.separator + SdkConstants.FN_APK_CLASSES_DEX;

    String finalPackageName = ProjectHelper.getApkFilename(project, null /* config */);
    String osFinalPackagePath = osAndroidBinPath + File.separator + finalPackageName;
    // Delete old APK
    new File(osFinalPackagePath).delete();

    Collection<String> dxInputPaths = helper.getCompiledCodePaths();
    // Now we instrument all of the code in dxInputPaths and replace it with
    // ours.
    IFile manifestFile = project.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
    FlashlightAndroidLaunchConfigurationDelegate.FLData data = null;
    try {
      data = AndroidBuildUtil.instrumentClasses(runId, launchConfig, project, dxInputPaths);
      LaunchUtils.createSourceZips(null, data.allProjects, data.sourceDir, null);
      try {
        dxInputPaths = data.getClasspathEntries();
        helper.packageResources(manifestFile, libProjects, null, 0, osAndroidBinPath, AdtConstants.FN_RESOURCES_AP_);

        helper.executeDx(javaProject, dxInputPaths, classesDexPath);

        helper.finalDebugPackage(osAndroidBinPath + File.separator + AdtConstants.FN_RESOURCES_AP_, classesDexPath,
            osFinalPackagePath, libProjects, mResourceMarker);
      } finally {
        data.deleteTempFiles();
      }
    } catch (DexException e) {
      String message = e.getMessage();

      AdtPlugin.printErrorToConsole(project, message);
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, message, IMarker.SEVERITY_ERROR);

      Throwable cause = e.getCause();

      if (cause instanceof NoClassDefFoundError || cause instanceof NoSuchMethodError) {
        AdtPlugin.printErrorToConsole(project, Messages.Incompatible_VM_Warning, Messages.Requires_1_5_Error);
      }
      return null;

    } catch (AaptResultException e) {
      // attempt to parse the error output
      String[] aaptOutput = e.getOutput();
      boolean parsingError = AaptParser.parseOutput(aaptOutput, project);

      // if we couldn't parse the output we display it in the console.
      if (parsingError) {
        AdtPlugin.printErrorToConsole(project, (Object[]) aaptOutput);

        // if the exec failed, and we couldn't parse the error output
        // (and
        // therefore not all files that should have been marked, were
        // marked),
        // we put a generic marker on the project and abort.
        BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, Messages.Unparsed_AAPT_Errors,
            IMarker.SEVERITY_ERROR);
      }
      return null;
    } catch (AaptExecException e) {
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, e.getMessage(), IMarker.SEVERITY_ERROR);
      return null;
    } catch (KeytoolException e) {
      String eMessage = e.getMessage();

      // mark the project with the standard message
      String msg = String.format(Messages.Final_Archive_Error_s, eMessage);
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);

      // output more info in the console
      AdtPlugin.printErrorToConsole(project, msg, String.format(Messages.ApkBuilder_JAVA_HOME_is_s, e.getJavaHome()),
          Messages.ApkBuilder_Update_or_Execute_manually_s, e.getCommandLine());

      return null;
    } catch (ApkCreationException e) {
      String eMessage = e.getMessage();

      // mark the project with the standard message
      String msg = String.format(Messages.Final_Archive_Error_s, eMessage);
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
    } catch (AndroidLocationException e) {
      String eMessage = e.getMessage();

      // mark the project with the standard message
      String msg = String.format(Messages.Final_Archive_Error_s, eMessage);
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
    } catch (NativeLibInJarException e) {
      String msg = e.getMessage();

      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);

      AdtPlugin.printErrorToConsole(project, (Object[]) e.getAdditionalInfo());
    } catch (CoreException e) {
      // mark project and return
      String msg = String.format(Messages.Final_Archive_Error_s, e.getMessage());
      AdtPlugin.printErrorToConsole(project, msg);
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg, IMarker.SEVERITY_ERROR);
    } catch (DuplicateFileException e) {
      String msg1 = String.format("Found duplicate file for APK: %1$s\nOrigin 1: %2$s\nOrigin 2: %3$s", e.getArchivePath(),
          e.getFile1(), e.getFile2());
      String msg2 = String.format(Messages.Final_Archive_Error_s, msg1);
      AdtPlugin.printErrorToConsole(project, msg2);
      BaseProjectHelper.markResource(project, AdtConstants.MARKER_PACKAGING, msg2, IMarker.SEVERITY_ERROR);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    // we are done.

    // refresh the bin folder content with no recursion.
    androidOutputFolder.refreshLocal(IResource.DEPTH_ONE, monitor);

    // reset the installation manager to force new installs of this project
    ApkInstallManager.getInstance().resetInstallationFor(project);

    return data;
  }

  static FlashlightAndroidLaunchConfigurationDelegate.FLData instrumentClasses(RunId runId, final ILaunchConfiguration launchConfig,
      final IProject project, final Collection<String> dxInputPaths) throws IOException, CoreException {
    ConfigurationBuilder configBuilder = LaunchHelper.buildConfigurationFromPreferences(launchConfig);

    /* Get the entries that the user does not want instrumented */
    final List<String> noInstrumentUser = launchConfig
        .getAttribute(FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT, Collections.<String> emptyList());
    final List<String> noInstrumentBoot = launchConfig.getAttribute(FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
        Collections.<String> emptyList());

    FlashlightAndroidLaunchConfigurationDelegate.FLData data = new FlashlightAndroidLaunchConfigurationDelegate.FLData(runId,
        launchConfig, project, dxInputPaths);
    PrintWriter logWriter = new PrintWriter(data.log);
    try {
      RewriteManager rm = new AndroidRewriteManager(configBuilder.getConfiguration(), new PrintWriterMessenger(logWriter),
          data.fieldsFile, data.sitesFile, data.classesFile, data.hbFile);
      String runtimePath = AndroidBuildUtil.getRuntimeJarPath();
      List<String> instrumentLast = LaunchHelper.sanitizeInstrumentationList(data.originalClasspaths);
      List<Integer> toInstrument = new ArrayList<>();
      for (int i = 0; i < data.originalClasspaths.size(); i++) {
        String fromPath = data.originalClasspaths.get(i);
        File from = new File(fromPath);
        File to = new File(data.classpaths.get(i));
        boolean ignore = noInstrumentBoot.contains(fromPath) || noInstrumentUser.contains(fromPath);
        if (!ignore && instrumentLast.contains(fromPath)) {
          toInstrument.add(i);
        }
        if (from.isDirectory()) {
          if (ignore) {
            rm.addClasspathDir(from);
          } else {
            rm.addDirToDir(from, to);
          }
        } else if (from.exists()) {
          if (ignore) {
            rm.addClasspathJar(from);
          } else {
            rm.addJarToJar(from, to, runtimePath);
          }
        } else {
          FlashlightAndroidLaunchConfigurationDelegate.getLog().warning(from.getAbsolutePath().toString()
              + " could not be found on the classpath could not be found when trying to instrument this project with Flashlight.");
        }
      }
      for (int i : toInstrument) {
        String fromPath = data.originalClasspaths.get(i);
        File from = new File(fromPath);
        File to = new File(data.classpaths.get(i));
        if (from.isDirectory()) {
          rm.addDirToDir(from, to);
        } else if (from.exists()) {
          rm.addJarToJar(from, to, runtimePath);
        } else {
          FlashlightAndroidLaunchConfigurationDelegate.getLog().warning(from.getAbsolutePath().toString()
              + " could not be found on the classpath could not be found when trying to instrument this project with Flashlight.");
        }
      }
      // We check the classpath to see if there are any entries that
      // aren't
      // being exported, but that we will need in order to fully
      // instrument
      // the project.
      IJavaProject javaProject = JavaCore.create(project);
      for (IClasspathEntry cpe : javaProject.getResolvedClasspath(true)) {
        if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
          String path = cpe.getPath().toOSString();
          if (!data.originalClasspaths.contains(path)) {
            File pathFile = new File(path);
            if (pathFile.isDirectory()) {
              rm.addClasspathDir(pathFile);
            } else if (pathFile.exists()) {
              rm.addClasspathJar(pathFile);
            }
          }
        }
      }
      try {
        rm.execute();
      } catch (AlreadyInstrumentedException e) {
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
        return null;
      }
    } finally {
      logWriter.close();
    }
    data.createInfoClasses();
    return data;
  }

  public static String getRuntimeJarPath() {
    final File bundleBase = EclipseUtility.getInstallationDirectoryOf(SchemaResources.PLUGIN_ID);
    if (bundleBase != null) {
      final File jarLocation = new File(bundleBase, SchemaResources.RUNTIME_JAR);
      return jarLocation.getAbsolutePath();
    } else {
      throw new IllegalStateException(
          "No bundle location (null returned) found for the Flashlight common plug-in:" + SchemaResources.PLUGIN_ID);
    }
  }

}
