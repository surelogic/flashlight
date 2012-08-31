package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class LaunchHelper {
    private LaunchHelper() {
        // Do nothing
    }

    public static RuntimeConfig getRuntimeConfig(
            final ILaunchConfiguration launch) throws CoreException {
        return new RuntimeConfig(launch);
    }

    public static class RuntimeConfig {
        private final int rawQSize;
        private final int refSize;
        private final int outQSize;
        private final int cPort;
        private final String useBinary;
        private final boolean postmortem;
        private final boolean compress;
        private final String collectionType;
        private final boolean useSpy;
        private final boolean useRefinery;

        private RuntimeConfig(final ILaunchConfiguration launch)
                throws CoreException {
            IPreferenceStore prefs = EclipseUIUtility.getPreferences();
            rawQSize = launch.getAttribute(
                    FlashlightPreferencesUtility.RAWQ_SIZE,
                    prefs.getInt(FlashlightPreferencesUtility.RAWQ_SIZE));
            refSize = launch.getAttribute(
                    FlashlightPreferencesUtility.REFINERY_SIZE,
                    prefs.getInt(FlashlightPreferencesUtility.REFINERY_SIZE));
            outQSize = launch.getAttribute(
                    FlashlightPreferencesUtility.OUTQ_SIZE,
                    prefs.getInt(FlashlightPreferencesUtility.OUTQ_SIZE));
            cPort = launch.getAttribute(
                    FlashlightPreferencesUtility.CONSOLE_PORT,
                    prefs.getInt(FlashlightPreferencesUtility.CONSOLE_PORT));
            useBinary = launch.getAttribute(
                    FlashlightPreferencesUtility.OUTPUT_TYPE,
                    prefs.getString(FlashlightPreferencesUtility.OUTPUT_TYPE));
            postmortem = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.POSTMORTEM_MODE,
                            prefs.getBoolean(FlashlightPreferencesUtility.POSTMORTEM_MODE));
            compress = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.COMPRESS_OUTPUT,
                            prefs.getBoolean(FlashlightPreferencesUtility.COMPRESS_OUTPUT));
            collectionType = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.COLLECTION_TYPE,
                            prefs.getString(FlashlightPreferencesUtility.COLLECTION_TYPE));
            useSpy = launch.getAttribute(FlashlightPreferencesUtility.USE_SPY,
                    prefs.getBoolean(FlashlightPreferencesUtility.USE_SPY));
            useRefinery = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.USE_REFINERY,
                            prefs.getBoolean(FlashlightPreferencesUtility.USE_REFINERY));
        }

        public int getRawQueueSize() {
            return rawQSize;
        }

        public int getRefinerySize() {
            return refSize;
        }

        public int getOutQueueSize() {
            return outQSize;
        }

        public int getConsolePort() {
            return cPort;
        }

        public String getUseBinary() {
            return useBinary;
        }

        public boolean isPostmortem() {
            return postmortem;
        }

        public boolean isCompressed() {
            return compress;
        }

        public String getCollectionType() {
            return collectionType;
        }

        public boolean useSpy() {
            return useSpy;
        }

        public boolean useRefinery() {
            return useRefinery;
        }

    }

    public static List<String> sanitizeInstrumentationList(
            final List<String> allInstrument) throws CoreException {
        /*
         * Bug 1615: Sanity check the instrumented classpath entries first:
         * Check that no entry marked for instrumentation is a file system
         * parent of any other entry that is marked for instrumentation.
         * 
         * Could be expensive: O(n^2)
         * 
         * 2011-03-22: Jar files that are nested inside of directories are added
         * to a special list so that they can be forced to instructed last. This
         * way we make sure the handling of the directories doesn't overwrite
         * the instrumented jar file.
         */
        final List<String> toInstrument = new ArrayList<String>();
        final StringBuilder sb = new StringBuilder();
        for (final String potentialParent : allInstrument) {
            final String test = potentialParent + File.separator;
            for (final String potentialChild : allInstrument) {
                if (potentialChild.startsWith(test)) {
                    if (new File(potentialChild).isDirectory()) {
                        sb.append("Classpath entry ");
                        sb.append(potentialParent);
                        sb.append(" is instrumented and nests the instrumented classpath directory entry ");
                        sb.append(potentialChild);
                        sb.append("  ");
                    } else { // nested jar file
                        toInstrument.add(potentialChild);
                    }
                }
            }
        }
        if (sb.length() > 0) {
            throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
                    sb.toString()));
            // FIXME throw new FlashlightLaunchException(sb.toString());
        }
        return toInstrument;
    }

    public static ConfigurationBuilder buildConfigurationFromPreferences(
            final ILaunchConfiguration launch) {

        // Read the property file
        final Properties flashlightProps = new Properties();
        final File flashlightPropFile = new File(
                System.getProperty("user.home"),
                "flashlight-rewriter.properties");
        boolean failed = false;
        try {
            flashlightProps.load(new FileInputStream(flashlightPropFile));
        } catch (final IOException e) {
            failed = true;
        } catch (final IllegalArgumentException e) {
            failed = true;
        }

        final ConfigurationBuilder configBuilder;
        if (failed) {
            configBuilder = new ConfigurationBuilder();
        } else {
            configBuilder = new ConfigurationBuilder(flashlightProps);
        }
        try {
            configBuilder
                    .setIndirectUseDefault(launch
                            .getAttribute(
                                    FlashlightPreferencesUtility.USE_DEFAULT_INDIRECT_ACCESS_METHODS,
                                    true));
        } catch (final CoreException e) {
            // eat itI
        }
        try {
            final List<String> xtraMethods = launch
                    .getAttribute(
                            FlashlightPreferencesUtility.ADDITIONAL_INDIRECT_ACCESS_METHODS,
                            Collections.emptyList());
            for (final String s : xtraMethods) {
                configBuilder.addAdditionalMethods(new File(s));
            }
        } catch (final CoreException e) {
            // eat it
        }

        try {
            final List<String> blacklist = launch.getAttribute(
                    FlashlightPreferencesUtility.CLASS_BLACKLIST,
                    Collections.emptyList());
            for (final String internalTypeName : blacklist) {
                configBuilder.addToBlacklist(internalTypeName);
            }
        } catch (final CoreException e) {
            // eat it
        }

        try {
            final String filterName = launch.getAttribute(
                    FlashlightPreferencesUtility.FIELD_FILTER,
                    FieldFilter.NONE.name());
            configBuilder.setFieldFilter(Enum.valueOf(FieldFilter.class,
                    filterName));

            final List<String> filterPkgs = launch.getAttribute(
                    FlashlightPreferencesUtility.FIELD_FILTER_PACKAGES,
                    Collections.emptyList());
            configBuilder.getFilterPackages().clear();
            for (final String pkg : filterPkgs) {
                configBuilder.addToFilterPackages(pkg.replace('.', '/'));
            }

        } catch (final CoreException e) {
            // eat it
        }

        return configBuilder;
    }
}
