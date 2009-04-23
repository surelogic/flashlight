package com.surelogic.flashlight.client.eclipse;

import java.io.File;

import com.surelogic.flashlight.common.FlashlightUtility;

public final class FlashlightEclipseUtility {
	/**
	 * This method returns the anchor for the Flashlight data directory. Clients
	 * typically will not use this method to get the Flashlight data directory,
	 * instead they would use the method {@link #getFlashlightDataDirectory()}.
	 * 
	 * @return the non-null anchor for the Flashlight data directory.
	 */
	static public File getFlashlightDataDirectoryAnchor() {
		return FlashlightUtility.getFlashlightDataDirectoryAnchor();
	}
	
	/**
	 * This method gets the Flashlight data directory. It ensures the returned
	 * directory exists.
	 */
	static public File getFlashlightDataDirectory() {
		return FlashlightUtility.getFlashlightDataDirectory();
	}
}
