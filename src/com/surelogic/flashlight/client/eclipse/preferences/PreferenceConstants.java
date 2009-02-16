package com.surelogic.flashlight.client.eclipse.preferences;

import com.surelogic.flashlight.client.eclipse.Activator;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {

	private static final String PREFIX = "com.surelogic.flashlight.";

	public static final String P_FILTER_PKG_PREFIX = PREFIX + "filter.";

	public static final String P_USE_FILTERING = PREFIX + "use.filtering";

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
			+ "perspective.switch.prompt";

	public static boolean getPromptForPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_PROMPT_PERSPECTIVE_SWITCH);
	}

	public static void setPromptForPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_PROMPT_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_PERSPECTIVE_SWITCH = PREFIX
			+ "perspective.switch.auto";

	public static boolean getAutoPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_PERSPECTIVE_SWITCH);
	}

	public static void setAutoPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_AUTO_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_INCREASE_HEAP_AT_LAUNCH = PREFIX
			+ "auto.increase.heap.at.launch";

	public static boolean getAutoIncreaseHeapAtLaunch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_INCREASE_HEAP_AT_LAUNCH);
	}
}
