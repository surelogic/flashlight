package com.surelogic.flashlight;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.surelogic.flashlight.db.Data;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.surelogic.flashlight";

	public static final String SCHEMA_FILE = "/lib/database/schema.sql";

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
		// find the schema file within this plug-in
		IPath p = Path.fromOSString(SCHEMA_FILE);
		final URL schemaURL = FileLocator.find(getBundle(), p, null);
		if (schemaURL != null) {
			// startup the database and (if necessary) load its schema
			Data.bootAndCheckSchema(schemaURL);
		} else {
			throw new CoreException(
					FLog
							.createErrorStatus("Unable to find the Flashlight schema file, "
									+ SCHEMA_FILE
									+ ", relative to the plug-in path."));
		}
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

	/**
	 * Shows the view identified by the given view id in this page and gives it
	 * focus. If there is a view identified by the given view id (and with no
	 * secondary id) already open in this page, it is given focus.
	 * <P>
	 * This method must be called from a UI thread or it will throw a
	 * {@link NullPointerException}. *
	 * 
	 * @param viewId
	 *            the id of the view extension to use
	 * @return the shown view or <code>null</code>.
	 */
	public static IViewPart showView(final String viewId) {
		try {
			final IViewPart view = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.showView(viewId);
			return view;
		} catch (PartInitException e) {
			FLog.logError("Unable to open the view identified by " + viewId
					+ ".", e);
		}
		return null;
	}

	public static IViewPart showView(final String viewId,
			final String secondaryId, final int mode) {
		try {
			final IViewPart view = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().showView(
							viewId, secondaryId, mode);
			return view;
		} catch (PartInitException e) {
			FLog.logError("Unable to open the view identified by " + viewId
					+ " " + secondaryId + ".", e);
		}
		return null;
	}
}
