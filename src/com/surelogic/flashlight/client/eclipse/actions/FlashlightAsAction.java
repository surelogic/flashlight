package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.debug.ui.actions.LaunchShortcutsAction;

import com.surelogic.flashlight.client.eclipse.Activator;

public class FlashlightAsAction extends LaunchShortcutsAction {

    public FlashlightAsAction() {
        super(Activator.LAUNCH_GROUP);
    }

}
