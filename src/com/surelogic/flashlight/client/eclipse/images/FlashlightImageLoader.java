package com.surelogic.flashlight.client.eclipse.images;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.common.CommonImages;
import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.tooltip.ToolTip;

public class FlashlightImageLoader implements ToolTip.ImageLoader {

	public static final String PATH = "/com/surelogic/flashlight/client/eclipse/images/";

	private FlashlightImageLoader() {
		// Do nothing
	}

	private final Map<String, File> fileMap = new HashMap<String, File>();

	public File getImageFile(String imageName) {
		synchronized (fileMap) {
			File f = fileMap.get(imageName);
			if (f == null) {
				URL gifURL = getImageURL(imageName);
				if (gifURL == null) {
					gifURL = CommonImages.getImageURL(imageName);
				}
				if (gifURL != null) {
					try {
						f = File.createTempFile("IMAGE", "CACHE");
						f.deleteOnExit();
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
					FileUtility.copy(gifURL, f);
					return f.getAbsoluteFile();
				} else {
					return null;
				}
			} else {
				return f.getAbsoluteFile();
			}
		}
	}

	/**
	 * Gets the URL to the passed name within this package.
	 * 
	 * @param imageSymbolicName
	 *            the image's symbolic name.
	 * @return a URL or {@code null} if no resource was found.
	 */
	public static URL getImageURL(String imageSymbolicName) {
		final String path = PATH + imageSymbolicName;
		final URL url = Thread.currentThread().getContextClassLoader()
				.getResource(path);
		return url;
	}

	private static final FlashlightImageLoader INSTANCE = new FlashlightImageLoader();

	public static FlashlightImageLoader getInstance() {
		return INSTANCE;
	}

}
