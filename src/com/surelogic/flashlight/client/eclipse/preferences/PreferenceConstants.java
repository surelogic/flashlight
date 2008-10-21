package com.surelogic.flashlight.client.eclipse.preferences;

import com.surelogic.flashlight.client.eclipse.Activator;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {

	public static final String P_RAWQ_SIZE = "com.surelogic.flashlight.rawq.size";

	public static final String P_OUTQ_SIZE = "com.surelogic.flashlight.outq.size";

	public static final String P_REFINERY_SIZE = "com.surelogic.flashlight.refinery.size";

	public static final String P_USE_SPY = "com.surelogic.flashlight.use.spy";

	public static final String P_CONSOLE_PORT = "com.surelogic.flashlight.console.port";

	public static final String P_MAX_ROWS_PER_QUERY = "com.surelogic.flashlight.max.rows.per.query";

	public static final String P_PROMPT_PERSPECTIVE_SWITCH = "com.surelogic.flashlight.perspective.switch.prompt";

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

	public static boolean getAutoIncreasHeapAtLaunch() {
		return Activator.getDefault().getPluginPreferences().getBoolean(
				P_AUTO_INCREASE_HEAP_AT_LAUNCH);
	}
}
