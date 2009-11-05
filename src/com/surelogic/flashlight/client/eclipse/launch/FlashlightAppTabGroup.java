package com.surelogic.flashlight.client.eclipse.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;

/* Currently unused because I learned how to use the 
 * "org.eclipse.debug.ui.launchConfigurationTabs" extension point
 * instead of the "org.eclipse.debug.ui.launchConfigurationTabGroups"
 * extension point.
 */
public class FlashlightAppTabGroup extends AbstractLaunchConfigurationTabGroup {
	public void createTabs(final ILaunchConfigurationDialog dialog,
			final String mode) {
		final ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new JavaMainTab(), new JavaArgumentsTab(),
				new FlashlightInstrumentationTab(), new FlashlightTab(),
				new FlashlightMethodsTab(), new FlashlightFieldsTab(),
				new JavaJRETab(), new JavaClasspathTab(),
				new SourceLookupTab(), new EnvironmentTab(), new CommonTab() };
		setTabs(tabs);
	}
}
