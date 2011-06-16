package com.surelogic.flashlight.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public final class LibResources {
	/**
	 * The name of the current version Flashlight Ant tasks. This is used for
	 * the directory created by the tool.
	 */
	public static final String ANT_TASK_VERSION = "flashlight-ant-4.0.0";

	/**
	 * The name of the archive that contains the Flashlight Ant tasks.
	 */
	public static final String ANT_TASK_ZIP = "flashlight-ant.zip";

	public static final String PATH = "/lib/";
	public static final String ANT_TASK_ZIP_PATHNAME = PATH + ANT_TASK_ZIP;

	public static InputStream getAntTaskZip() throws IOException {
		final URL url = LibResources.class.getResource(ANT_TASK_ZIP_PATHNAME);
		final InputStream is = url.openStream();
		return is;
	}
}
