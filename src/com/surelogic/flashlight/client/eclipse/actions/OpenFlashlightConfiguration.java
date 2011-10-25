package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;

import com.surelogic.flashlight.client.eclipse.Activator;

public class OpenFlashlightConfiguration extends OpenLaunchDialogAction {

    public OpenFlashlightConfiguration() {
        super(Activator.LAUNCH_GROUP);
    }
}
