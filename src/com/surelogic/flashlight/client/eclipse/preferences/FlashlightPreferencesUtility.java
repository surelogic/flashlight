package com.surelogic.flashlight.client.eclipse.preferences;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.surelogic._flashlight.common.InstrumentationConstants.*;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.preferences.AutoPerspectiveSwitchPreferences;

/**
 * Defines preference constants for the Flashlight tool.
 * <p>
 * The preferences are manipulated using the API in {@link EclipseUtility}.
 * Eclipse UI code that uses an <tt>IPreferenceStore</tt> may obtain one that
 * accesses the Flashlight preferences by calling
 * <tt>EclipseUIUtility.getPreferences()</tt>.
 */
public final class FlashlightPreferencesUtility {

	private static final String PREFIX = "com.surelogic.flashlight.";

	private static final AtomicBoolean f_initializationNeeded = new AtomicBoolean(
			true);

	public static final String CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX
			+ "classpathEntriesToNotInstrument";
	public static final String BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX
			+ "bootpathEntriesToNotInstrument";
	public static final String FIELD_FILTER = PREFIX + "filter.fields";
	public static final String FIELD_FILTER_PACKAGES = PREFIX
			+ "filter.fields.inPackages";
	public static final String CLASS_BLACKLIST = PREFIX + "classBlacklist";
	public static final String USE_DEFAULT_INDIRECT_ACCESS_METHODS = PREFIX
			+ "useDefaultIndirectAccessMethods";
	public static final String ADDITIONAL_INDIRECT_ACCESS_METHODS = PREFIX
			+ "additionalIndirectAccessMethods";
	public static final String COLLECTION_TYPE = PREFIX + "collection.type";
	public static final String OUTPUT_TYPE = PREFIX + "output.type";
	public static final String COMPRESS_OUTPUT = PREFIX + "compress.output";
	public static final String USE_REFINERY = PREFIX + "use.refinery";
	public static final String RAWQ_SIZE = PREFIX + "rawq.size";
	public static final String OUTQ_SIZE = PREFIX + "outq.size";
	public static final String REFINERY_SIZE = PREFIX + "refinery.size";
	public static final String USE_SPY = PREFIX + "use.spy";
	public static final String CONSOLE_PORT = PREFIX + "console.port";
	public static final String MAX_ROWS_PER_QUERY = PREFIX
			+ "max.rows.per.query";
	public static final String PREP_OBJECT_WINDOW_SIZE = PREFIX
			+ "object.window.size";
	public static final String AUTO_INCREASE_HEAP_AT_LAUNCH = PREFIX
			+ "auto.increase.heap.at.launch";
	public static final String PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES = PREFIX
			+ "prompt.about.lots.of.saved.queries";
	public static final String PROMPT_TO_PREP_ALL_RAW_DATA = PREFIX
			+ "prompt.to.prep.all.raw.data";
	public static final String AUTO_PREP_ALL_RAW_DATA = PREFIX
			+ "auto.prep.all.raw.data";

	private static final String FLASHLIGHT_DATA_DIRECTORY = PREFIX
			+ "data.directory";

	/**
	 * Sets up the default values for the JSure tool.
	 * <p>
	 * <b>WARNING:</b> Because this class exports strings that are declared to
	 * be {@code public static final} simply referencing these constants may not
	 * trigger Eclipse to load the containing plug-in. This is because the
	 * constants are copied by the Java compiler into using class files. This
	 * means that each using plug-in <b>must</b> invoke
	 * {@link #initializeDefaultScope()} in its plug-in activator's
	 * {@code start} method.
	 */
	public static void initializeDefaultScope() {
		if (f_initializationNeeded.compareAndSet(true, false)) {
			int cpuCount = Runtime.getRuntime().availableProcessors();
			if (cpuCount < 1)
				cpuCount = 1;
			EclipseUtility.setDefaultIntPreference(RAWQ_SIZE,
					FL_RAWQ_SIZE_DEFAULT);
			EclipseUtility.setDefaultIntPreference(OUTQ_SIZE,
					FL_OUTQ_SIZE_DEFAULT);
			EclipseUtility.setDefaultIntPreference(REFINERY_SIZE,
					FL_REFINERY_SIZE_DEFAULT);
			EclipseUtility.setDefaultBooleanPreference(USE_SPY, true);
			EclipseUtility.setDefaultIntPreference(CONSOLE_PORT,
					FL_CONSOLE_PORT_DEFAULT);
			EclipseUtility.setDefaultIntPreference(MAX_ROWS_PER_QUERY, 5000);
			EclipseUtility.setDefaultBooleanPreference(
					AUTO_INCREASE_HEAP_AT_LAUNCH, true);
			EclipseUtility.setDefaultBooleanPreference(USE_REFINERY, true);
			EclipseUtility.setDefaultStringPreference(COLLECTION_TYPE,
					FL_COLLECTION_TYPE_DEFAULT.name());
			EclipseUtility.setDefaultBooleanPreference(OUTPUT_TYPE,
					FL_OUTPUT_TYPE_DEFAULT.isBinary());
			EclipseUtility.setDefaultBooleanPreference(COMPRESS_OUTPUT,
					FL_OUTPUT_TYPE_DEFAULT.isCompressed());

			EclipseUtility.setDefaultBooleanPreference(
					PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, true);
			EclipseUtility.setDefaultBooleanPreference(
					PROMPT_TO_PREP_ALL_RAW_DATA, true);

			EclipseUtility.setDefaultIntPreference(PREP_OBJECT_WINDOW_SIZE,
					300000);

			EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences()
					.getPromptPerspectiveSwitchConstant(), true);
			EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences()
					.getAutoPerspectiveSwitchConstant(), true);

			EclipseUtility
					.setDefaultStringPreference(
							FLASHLIGHT_DATA_DIRECTORY,
							EclipseUtility
									.getADataDirectoryPath(FileUtility.FLASHLIGHT_DATA_PATH_FRAGMENT));
			/*
			 * We'll take the default-default for the other preferences.
			 */
		}
	}

	/**
	 * Gets the Flashlight data directory. This method ensures that the
	 * directory does exist on the disk. It checks that is is there and, if not,
	 * tries to create it. If it can't be created the method throws an
	 * exception.
	 * 
	 * @return the Flashlight data directory.
	 * 
	 * @throws IllegalStateException
	 *             if the Flashlight data directory doesn't exist on the disk
	 *             and can't be created.
	 */
	public static File getFlashlightDataDirectory() {
		final String path = EclipseUtility
				.getStringPreference(FLASHLIGHT_DATA_DIRECTORY);
		final File result = new File(path);
		FileUtility.ensureDirectoryExists(path);
		return result;
	}

	/**
	 * Sets the Flashlight data directory to an existing directory.
	 * <p>
	 * This method simply changes the preference it does not move data from the
	 * old directory (or even delete the old directory).
	 * 
	 * @param dir
	 *            the new Flashlight data directory (must exist and be a
	 *            directory).
	 * 
	 * @throws IllegalArgumentException
	 *             if the passed {@link File} is not a directory or doesn't
	 *             exist.
	 */
	public static void setFlashlightDataDirectory(final File dir) {
		if (dir != null && dir.isDirectory()) {
			EclipseUtility.setStringPreference(FLASHLIGHT_DATA_DIRECTORY,
					dir.getAbsolutePath());
		} else {
			throw new IllegalArgumentException("Bad Flashlight data directory "
					+ dir + " it doesn't exist on the disk");
		}
	}

	/**
	 * Gets the switch-to-the-Flashlight-perspective preferences.
	 * 
	 * @return the switch-to-the-Flashlight-perspective preferences.
	 */
	public static AutoPerspectiveSwitchPreferences getSwitchPreferences() {
		return new AutoPerspectiveSwitchPreferences() {
			@Override
			public String getConstant(String suffix) {
				return PREFIX + suffix;
			}
		};
	}

	private FlashlightPreferencesUtility() {
		// utility
	}
}
