package com.surelogic.flashlight.client.eclipse.launch;

import org.eclipse.debug.ui.*;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.*;

/* Currently unused because I learned how to use the 
 * "org.eclipse.debug.ui.launchConfigurationTabs" extension point
 * instead of the "org.eclipse.debug.ui.launchConfigurationTabGroups"
 * extension point.
 */
public class FlashlightAppTabGroup extends AbstractLaunchConfigurationTabGroup {
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new JavaMainTab(),
				new FlashlightTab(),
				new JavaArgumentsTab(),
				new JavaJRETab(),
				new JavaClasspathTab(),
				new SourceLookupTab(),
				new EnvironmentTab(),
				new CommonTab()
		};
		setTabs(tabs);
	}
}
