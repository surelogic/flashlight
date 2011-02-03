package com.surelogic.flashlight.client.eclipse.preferences;

import java.io.File;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.preferences.IAutoPerspectiveSwitchPreferences;

public final class FlashlightPreferencesUtility {

	private static final String PREFIX = "com.surelogic.flashlight.";

	public static final String P_PROMPT_PERSPECTIVE_SWITCH = PREFIX
			+ IAutoPerspectiveSwitchPreferences.PROMPT_PERSPECTIVE_SWITCH;
	public static final String P_AUTO_PERSPECTIVE_SWITCH = PREFIX
			+ IAutoPerspectiveSwitchPreferences.AUTO_PERSPECTIVE_SWITCH;

	public static IAutoPerspectiveSwitchPreferences getSwitchPreferences() {
		return new IAutoPerspectiveSwitchPreferences() {

			@Override
			public void setPromptForPerspectiveSwitch(boolean value) {
				EclipseUtility.getPreferences().putBoolean(
						P_PROMPT_PERSPECTIVE_SWITCH, value);
			}

			@Override
			public boolean getPromptForPerspectiveSwitch() {
				return EclipseUtility.getPreferences().getBoolean(
						P_PROMPT_PERSPECTIVE_SWITCH, true);
			}

			@Override
			public void setAutoPerspectiveSwitch(boolean value) {
				EclipseUtility.getPreferences().putBoolean(
						P_AUTO_PERSPECTIVE_SWITCH, value);
			}

			@Override
			public boolean getAutoPerspectiveSwitch() {
				return EclipseUtility.getPreferences().getBoolean(
						P_AUTO_PERSPECTIVE_SWITCH, false);
			}

			@Override
			public String getPrefConstant(String suffix) {
				return PREFIX + suffix;
			}
		};
	}

	public static final String P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX
			+ "classpathEntriesToNotInstrument";

	public static final String P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX
			+ "bootpathEntriesToNotInstrument";

	public static final String P_FIELD_FILTER = PREFIX + "filter.fields";

	public static final String P_FIELD_FILTER_PACKAGES = PREFIX
			+ "filter.fields.inPackages";

	public static final String P_CLASS_BLACKLIST = PREFIX + "classBlacklist";

	public static final String P_USE_DEFAULT_INDIRECT_ACCESS_METHODS = PREFIX
			+ "useDefaultIndirectAccessMethods";

	public static final String P_ADDITIONAL_INDIRECT_ACCESS_METHODS = PREFIX
			+ "additionalIndirectAccessMethods";

	public static final String P_COLLECTION_TYPE = PREFIX + "collection.type";

	public static final String P_OUTPUT_TYPE = PREFIX + "output.type";

	public static final String P_COMPRESS_OUTPUT = PREFIX + "compress.output";

	public static final String P_USE_REFINERY = PREFIX + "use.refinery";

	public static final String P_RAWQ_SIZE = PREFIX + "rawq.size";

	public static final String P_OUTQ_SIZE = PREFIX + "outq.size";

	public static final String P_REFINERY_SIZE = PREFIX + "refinery.size";

	public static final String P_USE_SPY = PREFIX + "use.spy";

	public static final String P_CONSOLE_PORT = PREFIX + "console.port";

	public static final String P_MAX_ROWS_PER_QUERY = PREFIX
			+ "max.rows.per.query";

	public static final String P_PREP_OBJECT_WINDOW_SIZE = PREFIX
			+ "object.window.size";

	public static int getPrepObjectWindowSize() {
		return EclipseUtility.getPreferences().getInt(
				P_PREP_OBJECT_WINDOW_SIZE, 300000);
	}

	public static final String P_AUTO_INCREASE_HEAP_AT_LAUNCH = PREFIX
			+ "auto.increase.heap.at.launch";

	public static boolean getAutoIncreaseHeapAtLaunch() {
		return EclipseUtility.getPreferences().getBoolean(
				P_AUTO_INCREASE_HEAP_AT_LAUNCH, true);
	}

	public static final String P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES = PREFIX
			+ "prompt.about.lots.of.saved.queries";

	public static boolean getPromptAboutLotsOfSavedQueries() {
		return EclipseUtility.getPreferences().getBoolean(
				P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, true);
	}

	public static void setPromptAboutLotsOfSavedQueries(final boolean value) {
		EclipseUtility.getPreferences().putBoolean(
				P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, value);
	}

	public static int getMaxRowsPerQuery() {
		return EclipseUtility.getPreferences().getInt(
				FlashlightPreferencesUtility.P_MAX_ROWS_PER_QUERY, 5000);
	}

	public static final String P_DATA_DIRECTORY = PREFIX + "data.directory";

	public static File getFlashlightDataDirectory() {
		final String path = EclipseUtility
				.getPreferences()
				.get(P_DATA_DIRECTORY,
						EclipseUtility
								.getADataDirectoryPath(FileUtility.FLASHLIGHT_DATA_PATH_FRAGMENT));
		final File result = new File(path);
		FileUtility.ensureDirectoryExists(path);
		return result;
	}

	public static void setFlashlightDataDirectory(final File dir) {
		if (dir != null && dir.isDirectory()) {
			EclipseUtility.getPreferences().put(P_DATA_DIRECTORY,
					dir.getAbsolutePath());
		} else {
			throw new IllegalArgumentException(
					"Bad Flashlight data directory: " + dir);
		}
	}

	public static final String P_PROMPT_TO_PREP_ALL_RAW_DATA = PREFIX
			+ "prompt.to.prep.all.raw.data";

	public static boolean getPromptToPrepAllRawData() {
		return EclipseUtility.getPreferences().getBoolean(
				P_PROMPT_TO_PREP_ALL_RAW_DATA, true);
	}

	public static void setPromptToPrepAllRawData(final boolean value) {
		EclipseUtility.getPreferences().putBoolean(
				P_PROMPT_TO_PREP_ALL_RAW_DATA, value);
	}

	public static final String P_AUTO_PREP_ALL_RAW_DATA = PREFIX
			+ "auto.prep.all.raw.data";

	public static boolean getAutoPrepAllRawData() {
		return EclipseUtility.getPreferences().getBoolean(
				P_AUTO_PREP_ALL_RAW_DATA, false);
	}

	public static void setAutoPrepAllRawData(final boolean value) {
		EclipseUtility.getPreferences().putBoolean(P_AUTO_PREP_ALL_RAW_DATA,
				value);
	}

	private FlashlightPreferencesUtility() {
		// utility
	}
}
