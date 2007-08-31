package com.surelogic.flashlight;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.surelogic.flashlight";

	public static final String XML_ENCODING = "UTF-8";

	// The shared instance
	private static Activator plugin;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public Activator() {
		if (plugin != null)
			throw new IllegalStateException(PLUGIN_ID + " class instance ("
					+ Activator.class.getName() + ") already exits");
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// startup the database and ensure its schema is up to date
		Data.bootAndCheckSchema();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public IPath getBundleLocation() {
		Bundle bundle = getBundle();
		if (bundle == null)
			return null;
		URL local = null;
		try {
			local = FileLocator.toFileURL(bundle.getEntry("/"));
		} catch (IOException e) {
			return null;
		}
		String fullPath = new File(local.getPath()).getAbsolutePath();
		return Path.fromOSString(fullPath);
	}
}
