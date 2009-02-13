package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.dialogs.ConfirmPerspectiveSwitchDialog;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.CommonImages;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public final class ConfirmPerspectiveSwitch {

	/**
	 * Checks if the Flashlight perspective should be opened.
	 * 
	 * @param shell
	 *            a shell.
	 * @return {@code true} if the Flashlight perspective should be opened,
	 *         {@code false} otherwise.
	 */
	public static boolean toFlashlight(Shell shell) {
		if (PreferenceConstants.getPromptForPerspectiveSwitch()) {
			ConfirmPerspectiveSwitchDialog dialog = new ConfirmPerspectiveSwitchDialog(
					shell,
					SLImages.getImage(CommonImages.IMG_FL_LOGO),
					I18N
							.msg("common.confirm.perspective.switch.dialog.flashlight"));
			final boolean result = dialog.open() == Window.OK;
			final boolean rememberMyDecision = dialog.getRememberMyDecision();
			if (rememberMyDecision) {
				PreferenceConstants
						.setPromptForPerspectiveSwitch(!rememberMyDecision);
				PreferenceConstants.setAutoPerspectiveSwitch(result);
			}
			return result;
		} else {
			return PreferenceConstants.getAutoPerspectiveSwitch();
		}
	}
}
