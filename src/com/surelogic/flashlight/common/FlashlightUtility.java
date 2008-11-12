package com.surelogic.flashlight.common;

import java.io.File;

import com.surelogic.common.FileUtility;

/**
 * A utility for the Flashlight product.
 */
public final class FlashlightUtility {

	private FlashlightUtility() {
		// no instances
	}

	/**
	 * This method returns the anchor for the Flashlight data directory. Clients
	 * typically will not use this method to get the Flashlight data directory,
	 * instead they would use the method {@link #getFlashlightDataDirectory()}.
	 * 
	 * @return the non-null anchor for the Flashlight data directory.
	 * @see FileUtility#getDataDirectory(File)
	 * @see #getFlashlightDataDirectory()
	 */
	static public File getFlashlightDataDirectoryAnchor() {
		return new File(System.getProperty("user.home") + File.separator
				+ ".flashlight-data");
	}

	/**
	 * This method gets the Flashlight data directory. It ensures the directory
	 * exists.
	 * <p>
	 * This method is the same as calling
	 * 
	 * <pre>
	 * getDataDirectory(getFlashlightDataDirectoryAnchor())
	 * </pre>
	 * 
	 * @return the Flashlight data directory.
	 * @see FileUtility#getDataDirectory(File)
	 * @see #getFlashlightDataDirectoryAnchor()
	 */
	static public File getFlashlightDataDirectory() {
		return FileUtility.getDataDirectory(getFlashlightDataDirectoryAnchor());
	}

}
