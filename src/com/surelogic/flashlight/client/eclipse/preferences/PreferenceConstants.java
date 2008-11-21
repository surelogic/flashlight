package com.surelogic.flashlight.client.eclipse.preferences;

import com.surelogic.flashlight.client.eclipse.Activator;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {
	public static final String P_FL_PREFIX = "com.surelogic.flashlight.";
	
	public static final String P_FILTER_PKG_PREFIX = P_FL_PREFIX+"filter.";
	
	public static final String P_USE_FILTERING = P_FL_PREFIX+"use.filtering";
	
	public static final String P_OUTPUT_TYPE = P_FL_PREFIX+"output.type";
	
	public static final String P_COMPRESS_OUTPUT = P_FL_PREFIX+"compress.output";
	
	public static final String P_USE_REFINERY = P_FL_PREFIX+"use.refinery";
	
	public static final String P_RAWQ_SIZE = P_FL_PREFIX+"rawq.size";

	public static final String P_OUTQ_SIZE = P_FL_PREFIX+"outq.size";

	public static final String P_REFINERY_SIZE = P_FL_PREFIX+"refinery.size";

	public static final String P_USE_SPY = P_FL_PREFIX+"use.spy";

	public static final String P_CONSOLE_PORT = P_FL_PREFIX+"console.port";

	public static final String P_MAX_ROWS_PER_QUERY = P_FL_PREFIX+"max.rows.per.query";

	public static final String P_PROMPT_PERSPECTIVE_SWITCH = P_FL_PREFIX+"perspective.switch.prompt";
	
	public static boolean getPromptForPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_PROMPT_PERSPECTIVE_SWITCH);
	}

	public static void setPromptForPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_PROMPT_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_PERSPECTIVE_SWITCH = "com.surelogic.flashlight.perspective.switch.auto";

	public static boolean getAutoPerspectiveSwitch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_PERSPECTIVE_SWITCH);
	}

	public static void setAutoPerspectiveSwitch(boolean value) {
		Activator.getDefault().getPluginPreferences().setValue(
				P_AUTO_PERSPECTIVE_SWITCH, value);
	}

	public static final String P_AUTO_INCREASE_HEAP_AT_LAUNCH = "com.surelogic.flashlight.auto.increase.heap.at.launch";

	public static boolean getAutoIncreaseHeapAtLaunch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_INCREASE_HEAP_AT_LAUNCH);
	}
}
