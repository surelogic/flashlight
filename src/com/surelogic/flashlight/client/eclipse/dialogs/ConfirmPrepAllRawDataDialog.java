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
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

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
		if (PreferenceConstants.getPromptToPrepAllRawData()) {
			final ConfirmPrepAllRawDataDialog dialog = new ConfirmPrepAllRawDataDialog(
					SWTUtility.getShell(), SLImages
							.getImage(CommonImages.IMG_FL_LOGO), I18N
							.msg("flashlight.dialog.prep.all.text"));
			final boolean result = dialog.open() == Window.OK;
			final boolean rememberMyDecision = dialog.getRememberMyDecision();
			if (rememberMyDecision) {
				PreferenceConstants
						.setPromptToPrepAllRawData(!rememberMyDecision);
				PreferenceConstants.setAutoPrepAllRawData(result);
			}
			return result;
		} else {
			return PreferenceConstants.getAutoPrepAllRawData();
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
	protected Control createCustomArea(Composite parent) {
		final Button rememberMyDecision = new Button(parent, SWT.CHECK);
		rememberMyDecision.setText(I18N
				.msg("flashlight.dialog.prep.all.remember"));
		rememberMyDecision.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				f_rememberMyDecision = rememberMyDecision.getSelection();
			}
		});
		return super.createCustomArea(parent);
	}

	public ConfirmPrepAllRawDataDialog(Shell parentShell,
			Image dialogTitleImage, String dialogMessage) {
		super(parentShell, I18N.msg("flashlight.dialog.prep.all.title"),
				dialogTitleImage, dialogMessage, QUESTION, new String[] {
						"Yes", "No" }, 0);
	}
}
