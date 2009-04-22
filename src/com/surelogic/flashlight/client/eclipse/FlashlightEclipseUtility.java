package com.surelogic.flashlight.client.eclipse;

import java.io.File;

import com.surelogic.flashlight.common.FlashlightUtility;

public final class FlashlightEclipseUtility {
	/**
	 * This method gets the Flashlight data directory. It ensures the returned
	 * directory exists.
	 */
	static public File getFlashlightDataDirectory() {
		return FlashlightUtility.getFlashlightDataDirectory();
	}
}
