package com.surelogic.flashlight.client.eclipse;

import java.io.File;

import com.surelogic.common.FileUtility;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
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
		File dir = PreferenceConstants.getFlashlightDataDirectoryAnchor();
		if (dir != null) {
			return dir;
		}
		return FlashlightUtility.getFlashlightDataDirectoryAnchor();
	}
	
	/**
	 * This method gets the Flashlight data directory. It ensures the returned
	 * directory exists.
	 */
	static public File getFlashlightDataDirectory() {
		return FileUtility.getDataDirectory(getFlashlightDataDirectoryAnchor());
	}
}
