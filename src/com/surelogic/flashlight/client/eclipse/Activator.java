package com.surelogic.flashlight.client.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.jobs.FlashlightCleanupJob;
import com.surelogic.flashlight.client.eclipse.jobs.PromptToPrepAllRawData;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.surelogic.flashlight";

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
		if (plugin != null) {
			throw new IllegalStateException(PLUGIN_ID + " class instance ("
					+ Activator.class.getName() + ") already exits");
		}
		plugin = this;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

		/*
		 * "Touch" common-eclipse so the logging gets Eclipse-ified.
		 */
		SLEclipseStatusUtility.touch();

		UsageMeter.getInstance().tickUse("Flashlight Eclipse plug-in loaded");

		/*
		 * Get the data directory and ensure that it actually exists.
		 */
		final String path = getPluginPreferences().getString(
				PreferenceConstants.P_DATA_DIRECTORY);
		if (path == null)
			throw new IllegalStateException(I18N.err(44, "P_DATA_DIRECTORY"));
		final File dataDir = new File(path);
		FileUtility.createDirectory(dataDir);

		RunManager.getInstance().setDataDirectory(dataDir);
		new FlashlightCleanupJob().schedule();
		PromptToPrepAllRawData.start();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		try {
			PromptToPrepAllRawData.stop();
			AdHocDataSource.getInstance().dispose();
			UsageMeter.getInstance().persist();
			plugin = null;
		} finally {
			super.stop(context);
		}
	}

	public IPath getBundleLocation() {
		final Bundle bundle = getBundle();
		if (bundle == null) {
			return null;
		}
		URL local = null;
		try {
			local = FileLocator.toFileURL(bundle.getEntry("/"));
		} catch (final IOException e) {
			return null;
		}
		final String fullPath = new File(local.getPath()).getAbsolutePath();
		return Path.fromOSString(fullPath);
	}
}
