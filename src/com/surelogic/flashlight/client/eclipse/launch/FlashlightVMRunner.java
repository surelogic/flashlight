package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter; //import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.eclipse.MemoryUtility;
import com.surelogic.common.eclipse.SourceZip;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.FlashlightEclipseUtility;
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

	private final File projectOutputDir;
	private final File externalOutputDir;
	private final File sourceDir;
	private final File fieldsFile;
	private final File sitesFile;
	private final File logFile;
	private final File filtersFile;

	private final String datePostfix;
	private final String pathToFlashlightLib;

	private final List<String> user;
	private final List<String> boot;
	private final List<String> system;
	private final List<String> instrumentUser;
	private final List<String> instrumentBoot;
	
	private final boolean ALWAYS_APPEND_TO_BOOT = true;

	public FlashlightVMRunner(final IVMRunner other, final String mainType,
			final List<String> user, final List<String> boot,
			final List<String> system, final List<String> iUser,
			final List<String> iBoot) throws CoreException {
		delegateRunner = other;
		this.user = user;
		this.boot = boot;
		this.system = system;
		instrumentUser = iUser;
		instrumentBoot = iBoot;

		// Get the path to the flashlight-runtime.jar
		final IPath bundleBase = Activator.getDefault().getBundleLocation();
		if (bundleBase != null) {
			final IPath jarLocation = bundleBase
					.append("lib/flashlight-runtime.jar");
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
		final File flashlightDataDir = FlashlightEclipseUtility
				.getFlashlightDataDirectory();
		runOutputDir = new File(flashlightDataDir, runName);
		if (!runOutputDir.exists())
			runOutputDir.mkdirs();

		/* Init references to the different components of the output directory */
		projectOutputDir = new File(runOutputDir, "projects");
		externalOutputDir = new File(runOutputDir, "external");
		sourceDir = new File(runOutputDir, "source");
		fieldsFile = new File(runOutputDir, FIELDS_FILE_NAME);
		sitesFile = new File(runOutputDir, SITES_FILE_NAME);
		logFile = new File(runOutputDir, LOG_FILE_NAME);
		filtersFile = new File(runOutputDir, FILTERS_FILE_NAME);
		if (!projectOutputDir.exists())
			projectOutputDir.mkdir();
		if (!externalOutputDir.exists())
			externalOutputDir.mkdir();
		if (!sourceDir.exists())
			sourceDir.mkdir();
	}

	public void run(final VMRunnerConfiguration configuration,
			final ILaunch launch, final IProgressMonitor monitor)
			throws CoreException {
		/*
		 * Build the set of projects used by the application being run, and
		 * build the map of original to instrumented names.
		 */
		final Set<IProject> interestingProjects = new HashSet<IProject>();
		final Map<String, String> classpathEntryMap = new HashMap<String, String>();
		getInterestingProjectsAndBuildEntryMap(interestingProjects,
				classpathEntryMap);

		/*
		 * Amount of work is 1 for each project we need to zip, 2 for each
		 * directory we need to process, plus 1 remaining unit for the delegate.
		 */
		final int totalWork = interestingProjects.size() + // source zips
				user.size() + boot.size() + system.size() + // scanning
				instrumentUser.size() + instrumentBoot.size() + // instrumenting
				1; // running
		final SubMonitor progress = SubMonitor.convert(monitor, totalWork);

		/* Create the source zip */
		if (createSourceZips(interestingProjects, progress)) {
			// Canceled, abort early
			return;
		}

		/*
		 * Build the instrumented class files. First we scan each directory to
		 * the build the field database, and then we instrument each directory.
		 */
		if (instrumentClassfiles(launch.getLaunchConfiguration(), classpathEntryMap, progress)) {
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

		/*
		 * Create and launch a job that detects when the instrumented run
		 * terminates, and switches to the flashlight perspective on
		 * termination.
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

	private boolean createSourceZips(final Set<IProject> projects,
			final SubMonitor progress) {
		for (final IProject project : projects) {
			final String projectName = project.getName();
			progress.subTask("Creating source zip for " + projectName);
			final SourceZip srcZip = new SourceZip(project);
			final File zipFile = new File(sourceDir, projectName + ".src.zip");
			try {
				srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
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
	 */
	@SuppressWarnings("cast")
  private boolean instrumentClassfiles(final ILaunchConfiguration launch,
	    final Map<String, String> entryMap,
			final SubMonitor progress) {
		runOutputDir.mkdirs();
		PrintWriter logOut = null;
		try {
			logOut = new PrintWriter(logFile);
			final RewriteMessenger messenger = new PrintWriterMessenger(logOut);

			// Read the property file
			Properties flashlightProps = new Properties();
			final File flashlightPropFile =
			  new File(System.getProperty("user.home"),
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
  			configBuilder.setIndirectUseDefault(
  			    launch.getAttribute(
  			        PreferenceConstants.P_USE_DEFAULT_INDIRECT_ACCESS_METHODS, true));
			} catch (final CoreException e) {
			  // eat it
			}
      try {
        final List<String> xtraMethods = (List<String>) launch.getAttribute(
            PreferenceConstants.P_ADDITIONAL_INDIRECT_ACCESS_METHDOS,
            Collections.emptyList());
        for (final String s : xtraMethods) {
          configBuilder.addAdditionalMethods(new File(s));
        }
      } catch (final CoreException e) {
        // eat it
      }
			
      try {
        final List<String> blacklist = (List<String>) launch.getAttribute(
            PreferenceConstants.P_CLASS_BLACKLIST, Collections.emptyList());
        for (final String internalTypeName : blacklist) {
          configBuilder.addToBlacklist(internalTypeName);
        }
      } catch (final CoreException e) {
        // eat it
      }
      
			final RewriteManager manager =
			  new VMRewriteManager(configBuilder.getConfiguration(),
					messenger, fieldsFile, sitesFile, progress);

			// Scan everything on the classpath
			addToScan(manager, user);
			addToScan(manager, boot);
			addToScan(manager, system);

			// Add the entries to be instrumented
			addToInstrument(manager, instrumentUser, entryMap);
			addToInstrument(manager, instrumentBoot, entryMap);

			try {
				manager.execute();
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

	/**
	 * Add each entry in the given classpath segment to the scan-only list if it
	 * is NOT selected to be instrumented.
	 */
	private void addToScan(final RewriteManager manager,
			final List<String> classpath) {
		for (final String entry : classpath) {
			if (!instrumentUser.contains(entry)
					&& !instrumentBoot.contains(entry)) {
				final File asFile = new File(entry);
				if (asFile.isDirectory()) {
					manager.addClasspathDir(asFile);
				} else {
					manager.addClasspathJar(asFile);
				}
			}
		}
	}

	private void addToInstrument(final RewriteManager manager,
			final List<String> toBeInstrumented,
			final Map<String, String> entryMap) {
		for (final String instrument : toBeInstrumented) {
			final File asFile = new File(instrument);
			String mapped = entryMap.get(instrument);
			if (mapped == null) {
				System.out.println("No mapping for " + instrument);
			}
			final File destFile = new File(mapped);
			if (asFile.isDirectory()) {
				manager.addDirToJar(asFile, destFile, null);
			} else {
				manager.addJarToJar(asFile, destFile, null);
			}
		}
	}

	private String[] updateClassPath(final VMRunnerConfiguration configuration,
			final Map<String, String> entryMap) {
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
			final Map<String, String> entryMap) {
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
			final Map<String, String> entryMap,
			final List<String> newClassPathList, final List<String> toInstrument) {
		for (int i = 0; i < classPath.length; i++) {
			final String oldEntry = classPath[i];
			if (toInstrument.contains(oldEntry)) {
				final String newEntry = entryMap.get(oldEntry);
				newClassPathList.add(newEntry);
			} else {
				newClassPathList.add(oldEntry);
			}
		}
	}

	private VMRunnerConfiguration updateRunnerConfiguration(
			final VMRunnerConfiguration original, ILaunchConfiguration launch,
			final String[] newClassPath, final String[] newBootClassPath,
			final Map<String, String> entryMap) {
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
		final List<String> newVmArgsList = new ArrayList<String>(
				vmArgs.length + 11);
		try {
			final Preferences prefs = Activator.getDefault()
					.getPluginPreferences();
			final int rawQSize = launch.getAttribute(
					PreferenceConstants.P_RAWQ_SIZE, prefs
							.getInt(PreferenceConstants.P_RAWQ_SIZE));
			final int refSize = launch.getAttribute(
					PreferenceConstants.P_REFINERY_SIZE, prefs
							.getInt(PreferenceConstants.P_REFINERY_SIZE));
			final int outQSize = launch.getAttribute(
					PreferenceConstants.P_OUTQ_SIZE, prefs
							.getInt(PreferenceConstants.P_OUTQ_SIZE));
			final int cPort = launch.getAttribute(
					PreferenceConstants.P_CONSOLE_PORT, prefs
							.getInt(PreferenceConstants.P_CONSOLE_PORT));
			final String useBinary = launch.getAttribute(
					PreferenceConstants.P_OUTPUT_TYPE, prefs
							.getString(PreferenceConstants.P_OUTPUT_TYPE));
			final boolean compress = launch.getAttribute(
					PreferenceConstants.P_COMPRESS_OUTPUT, prefs
							.getBoolean(PreferenceConstants.P_COMPRESS_OUTPUT));
			final boolean useSpy = launch.getAttribute(
					PreferenceConstants.P_USE_SPY, prefs
							.getBoolean(PreferenceConstants.P_USE_SPY));
			final boolean useRefinery = launch.getAttribute(
					PreferenceConstants.P_USE_REFINERY, prefs
							.getBoolean(PreferenceConstants.P_USE_REFINERY));
			final boolean useFiltering = launch.getAttribute(
					PreferenceConstants.P_USE_FILTERING, prefs
							.getBoolean(PreferenceConstants.P_USE_FILTERING));
			if (useFiltering) {
				PrintWriter out = null;
				try {
					out = new PrintWriter(filtersFile);
					for (Object o : launch.getAttributes().entrySet()) {
						Map.Entry e = (Map.Entry) o;
						String key = (String) e.getKey();
						Object val = e.getValue();
						if (key
								.startsWith(PreferenceConstants.P_FILTER_PKG_PREFIX)
								&& Boolean.TRUE.equals(val)) {
							// System.out.println(key+": "+e.getValue());
							out
									.println(key
											.substring(PreferenceConstants.P_FILTER_PKG_PREFIX
													.length()));
						}
					}
					out.println();

					newVmArgsList.add("-D" + FL_FILTERS_FILE + "="
							+ filtersFile.getAbsolutePath());
				} catch (FileNotFoundException ex) {
					SLLogger.getLogger().log(
							Level.SEVERE,
							"Couldn't create filters file: "
									+ filtersFile.getAbsolutePath(), ex);
				} finally {
					if (out != null) {
						out.close();
					}
				}

			}
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
			newVmArgsList.add("-D" + FL_OUTPUT_TYPE + "="
					+ OutputType.get(useBinary, compress));
			if (!useRefinery) {
				newVmArgsList.add("-D" + FL_REFINERY_OFF + "=true");
			}
			if (!useSpy)
				newVmArgsList.add("-D" + FL_NO_SPY + "=true");
		} catch (CoreException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Couldn't setup launch for " + launch.getName(), e);
			return null;
		}

		if (PreferenceConstants.getAutoIncreaseHeapAtLaunch()) {
			final long maxSystemHeapSize = ((long) MemoryUtility
					.computeMaxMemorySizeInMb()) << 20;
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
				if (i != heapSettingPos)
					newVmArgsList.add(vmArgs[i]);
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
		newConfig.setVMSpecificAttributesMap(updateVMSpecificAttributesMap(
				original.getVMSpecificAttributesMap(), entryMap));
		newConfig.setWorkingDirectory(original.getWorkingDirectory());
		return newConfig;
	}

	private Map updateVMSpecificAttributesMap(final Map originalMap,
			final Map<String, String> entryMap) {
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

	private boolean updateBootpathArray(final Map<String, String> entryMap,
			final String[] originalBoothpath, final List<String> newBootpath) {
		boolean needsFlashlightLib = false;
		for (final String entry : originalBoothpath) {
			if (instrumentBoot.contains(entry)) {
				final String newEntry = entryMap.get(entry);
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
			SLLogger.getLogger().log(
					Level.SEVERE, "Problem loading indirect access methods", e);
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
				final FileNotFoundException e) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Unable to create sites file "
							+ sitesFile.getAbsolutePath(), e);
		}
	}

	private void getInterestingProjectsAndBuildEntryMap(
			final Set<IProject> interestingProjects,
			final Map<String, String> classpathEntryMap) {
		// Get the list of open projects in the workpace
		final List<IProject> openProjects = getOpenProjects();

		/*
		 * For each classpath entry we see if it is from a workspace project. If
		 * so, we add the project to the list of interesting projects. Also, we
		 * compute the location of the instrumentation for the classpath entry.
		 */
		scanProjects(user, openProjects, interestingProjects, classpathEntryMap);
		scanProjects(boot, openProjects, interestingProjects, classpathEntryMap);
		scanProjects(system, openProjects, interestingProjects,
				classpathEntryMap);
	}

	private static List<IProject> getOpenProjects() {
		final List<IProject> openProjects = new ArrayList<IProject>();
		for (final IProject p : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			if (p.isOpen())
				openProjects.add(p);
		}
		return openProjects;
	}

	private void scanProjects(final List<String> classpathEntries,
			final List<IProject> projects,
			final Set<IProject> interestingProjects,
			final Map<String, String> classpathEntryMap) {
		for (final String entry : classpathEntries) {
			final boolean isJar = !(new File(entry)).isDirectory();
			boolean foundProject = false;
			for (final IProject project : projects) {
				final String projectLoc = project.getLocation().toOSString();
				if (isFromProject(projectLoc, entry)) {
					final File newEntry = buildInstrumentedName(entry,
							projectLoc, isJar);
					classpathEntryMap.put(entry, newEntry.getAbsolutePath());
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
				String correctedEntry = fixLeadingDriveLetter(entry);
				final File newLocation = new File(externalOutputDir,
						isJar ? correctedEntry : (correctedEntry + ".jar"));
				classpathEntryMap.put(entry, newLocation.getAbsolutePath());
			}
		}
	}

	private static boolean isFromProject(final String projectLoc,
			final String entry) {
		return entry.startsWith(projectLoc)
				&& entry.charAt(projectLoc.length()) == File.separatorChar;
	}

	private File buildInstrumentedName(final String entry,
			final String projectLoc, final boolean isJar) {
		final String projectDirName = projectLoc.substring(projectLoc
				.lastIndexOf(File.separatorChar) + 1);
		final String binaryName = entry.substring(projectLoc.length() + 1);
		final String jarName = !isJar ? binaryName + ".jar" : binaryName;
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
