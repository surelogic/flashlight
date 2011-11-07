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

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.DialogTouchNotificationUI;
import com.surelogic.flashlight.client.eclipse.jobs.PromptToPrepAllRawData;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    public static final String LAUNCH_GROUP = "com.surelogic.flashlight.launchGroup.flashlight";

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
            throw new IllegalStateException(Activator.class.getName()
                    + " instance already exits, it should be a singleton.");
        }
        plugin = this;
    }

    /**
     * Gets the identifier for this plug in.
     * 
     * @return an identifier, such as <tt>com.surelogic.common</tt>. In rare
     *         cases, for example bad plug in XML, it may be {@code null}.
     * @see Bundle#getSymbolicName()
     */
    public String getPlugInId() {
        return plugin.getBundle().getSymbolicName();
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        /*
         * "Touch" common-core-eclipse so the logging gets Eclipse-ified.
         */
        SLEclipseStatusUtility.touch(new DialogTouchNotificationUI());

        /*
         * "Touch" the JSure preference initialization.
         */
        FlashlightPreferencesUtility.initializeDefaultScope();

        /*
         * Get the data directory and ensure that it actually exists.
         */
        final File dataDir = FlashlightPreferencesUtility
                .getFlashlightDataDirectory();

        EclipseUtility.getProductReleaseDateJob(SLLicenseProduct.FLASHLIGHT,
                this).schedule();
        RunManager.getInstance().setDataDirectory(dataDir);
        PromptToPrepAllRawData.start();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            PromptToPrepAllRawData.stop();
            AdHocDataSource.getInstance().dispose();
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
