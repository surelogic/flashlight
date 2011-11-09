package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.dialogs.AbstractConfirmPerspectiveSwitch;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class ConfirmPerspectiveSwitch extends
		AbstractConfirmPerspectiveSwitch {

	private static final ConfirmPerspectiveSwitch prototype = new ConfirmPerspectiveSwitch();

	private ConfirmPerspectiveSwitch() {
		super(FlashlightPerspective.class.toString(),
				FlashlightPreferencesUtility.getSwitchPreferences());
	}

	@Override
	protected String getLogo() {
		return CommonImages.IMG_FL_LOGO;
	}

	@Override
	protected String getShortPrefix() {
		return "flashlight.";
	}

	/**
	 * Checks if the Flashlight perspective should be opened.
	 * 
	 * @param shell
	 *            a shell.
	 * @return {@code true} if the Flashlight perspective should be opened,
	 *         {@code false} otherwise.
	 */
	public static boolean toFlashlight(Shell shell) {
		return prototype.toPerspective(shell);
	}
}
