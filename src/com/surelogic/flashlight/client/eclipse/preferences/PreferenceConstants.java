package com.surelogic.flashlight.client.eclipse.preferences;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.eclipse.preferences.IPreferenceConstants;
import com.surelogic.flashlight.client.eclipse.Activator;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants implements IPreferenceConstants {

	private static final String PREFIX = "com.surelogic.flashlight.";

//	public static final String P_FILTER_PKG_PREFIX = PREFIX + "filter.";
//
//	public static final String P_USE_FILTERING = PREFIX + "use.filtering";
//
	public static final String P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX + "classpathEntriesToNotInstrument";

  public static final String P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX + "bootpathEntriesToNotInstrument";
  
  public static final String P_FIELD_FILTER = PREFIX + "filter.fields";
  
  public static final String P_FIELD_FILTER_PACKAGES = PREFIX + "filter.fields.inPackages";
  
  public static final String P_CLASS_BLACKLIST = PREFIX + "classBlacklist";

	public static final String P_USE_DEFAULT_INDIRECT_ACCESS_METHODS = PREFIX + "useDefaultIndirectAccessMethods";
	
	public static final String P_ADDITIONAL_INDIRECT_ACCESS_METHODS = PREFIX + "additionalIndirectAccessMethods";
	
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

	public static final String P_PROMPT_PERSPECTIVE_SWITCH = PREFIX
			+ PROMPT_PERSPECTIVE_SWITCH;

	public boolean getPromptForPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_PROMPT_PERSPECTIVE_SWITCH);
	}

	public void setPromptForPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_PROMPT_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_PERSPECTIVE_SWITCH = PREFIX
			+ AUTO_PERSPECTIVE_SWITCH;

	public boolean getAutoPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_PERSPECTIVE_SWITCH);
	}

	public void setAutoPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_AUTO_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_INCREASE_HEAP_AT_LAUNCH = PREFIX
			+ "auto.increase.heap.at.launch";

	public static boolean getAutoIncreaseHeapAtLaunch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_INCREASE_HEAP_AT_LAUNCH);
	}

	public static final String P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES = PREFIX
			+ "prompt.about.lots.of.saved.queries";

	public static boolean getPromptAboutLotsOfSavedQueries() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES);
	}

	public static void setPromptAboutLotsOfSavedQueries(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, value);
	}

	
	public static final String P_DATA_DIRECTORY_ANCHOR = PREFIX + "data-directory-anchor";
	
	public static File getFlashlightDataDirectoryAnchor() {
		if (Activator.getDefault() == null) {
			throw new IllegalStateException("No Activator");
		}
		String path = Activator.getDefault().getPluginPreferences().getString(
				P_DATA_DIRECTORY_ANCHOR);
		if (path.length() == 0 || path == null) {
			return null;
		}
		return new File(path);
	}
	
	public static void setFlashlightDataDirectoryAnchor(File dir) {
		if (dir != null && dir.exists() && dir.isDirectory()) {
			Activator.getDefault().getPluginPreferences().setValue(
					P_DATA_DIRECTORY_ANCHOR, dir.getAbsolutePath());
		} else {
			throw new IllegalArgumentException("Bad directory: "+dir);
		}
	}
	
	private PreferenceConstants() {
		// Nothing to do
	}

	public static final PreferenceConstants prototype = new PreferenceConstants();

	public String getPrefConstant(String suffix) {
		return PREFIX + suffix;
	}

	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
