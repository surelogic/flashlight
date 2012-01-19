package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

/**
 * Confirms with the user, using a dialog, if all raw data should be prepared to
 * be queried.
 */
public final class ConfirmPrepAllRawDataDialog extends MessageDialog {

	/**
	 * Called to check if the user should be prompted to prepare all raw data to
	 * be queried. This method uses preferences to skip the user prompt if the
	 * user requested that his or her choice be remembered.
	 * <p>
	 * This method must be invoked from the UI thread.
	 * 
	 * @return {@code true} if a prep job for all unprepped runs should be
	 *         started, {@code false} otherwise.
	 */
	public static boolean check() {
		if (EclipseUtility
				.getBooleanPreference(FlashlightPreferencesUtility.PROMPT_TO_PREP_ALL_RAW_DATA)) {
			final ConfirmPrepAllRawDataDialog dialog = new ConfirmPrepAllRawDataDialog(
					EclipseUIUtility.getShell(),
					SLImages.getImage(CommonImages.IMG_FL_LOGO),
					I18N.msg("flashlight.dialog.prep.all.text"));
			final boolean result = dialog.open() == Window.OK;
			final boolean rememberMyDecision = dialog.getRememberMyDecision();
			if (rememberMyDecision) {
				EclipseUtility
						.setBooleanPreference(
								FlashlightPreferencesUtility.PROMPT_TO_PREP_ALL_RAW_DATA,
								!rememberMyDecision);
				EclipseUtility.setBooleanPreference(
						FlashlightPreferencesUtility.AUTO_PREP_ALL_RAW_DATA,
						result);
			}
			return result;
		} else {
			return EclipseUtility
					.getBooleanPreference(FlashlightPreferencesUtility.AUTO_PREP_ALL_RAW_DATA);
		}
	}

	private boolean f_rememberMyDecision = false;

	/**
	 * Gets if the user has asked that his or her decision be remembered in the
	 * future rather than being prompted by this dialog.
	 * 
	 * @return {@code true} if the user has asked that his or her decision be
	 *         remembered in the future rather than being prompted by this
	 *         dialog, {@code false} otherwise.
	 */
	public boolean getRememberMyDecision() {
		return f_rememberMyDecision;
	}

	@Override
	protected Control createCustomArea(final Composite parent) {
		final Button rememberMyDecision = new Button(parent, SWT.CHECK);
		rememberMyDecision.setText(I18N
				.msg("flashlight.dialog.prep.all.remember"));
		rememberMyDecision.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				f_rememberMyDecision = rememberMyDecision.getSelection();
			}
		});
		return super.createCustomArea(parent);
	}

	public ConfirmPrepAllRawDataDialog(final Shell parentShell,
			final Image dialogTitleImage, final String dialogMessage) {
		super(parentShell, I18N.msg("flashlight.dialog.prep.all.title"),
				dialogTitleImage, dialogMessage, QUESTION, new String[] {
						"Yes", "No" }, 0);
	}
}
