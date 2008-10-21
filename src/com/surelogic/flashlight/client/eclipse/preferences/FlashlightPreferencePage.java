package com.surelogic.flashlight.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.Activator;

public class FlashlightPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public FlashlightPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Use this page to customize Flashlight.");

		UsageMeter.getInstance().tickUse(
				"Flashlight FlashlightPreferencePage opened");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new ScaleFieldEditor(PreferenceConstants.P_RAWQ_SIZE,
				"Raw queue size:", getFieldEditorParent(), 1000, 50000, 1000,
				1000));
		addField(new ScaleFieldEditor(PreferenceConstants.P_REFINERY_SIZE,
				"Refinery cache size:", getFieldEditorParent(), 1000, 50000,
				1000, 1000));
		addField(new ScaleFieldEditor(PreferenceConstants.P_OUTQ_SIZE,
				"Out queue size:", getFieldEditorParent(), 1000, 50000, 1000,
				1000));

		IntegerFieldEditor ife = new IntegerFieldEditor(
				PreferenceConstants.P_CONSOLE_PORT, "Console port:",
				getFieldEditorParent());
		ife.setValidRange(1024, 65535);
		addField(ife);

		addField(new BooleanFieldEditor(PreferenceConstants.P_USE_SPY,
				"Use a spy thread to detect program termination",
				getFieldEditorParent()));

		IntegerFieldEditor mre = new IntegerFieldEditor(
				PreferenceConstants.P_MAX_ROWS_PER_QUERY,
				"Maximum rows per query:", getFieldEditorParent());
		mre.setValidRange(1024, 65535);
		addField(mre);

		addField(new BooleanFieldEditor(
				PreferenceConstants.P_PROMPT_PERSPECTIVE_SWITCH,
				"Prompt to change to the Flashlight perspective on instrumented launch",
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_PERSPECTIVE_SWITCH,
				"If no prompt, automatically change to the Flashlight perspective on instrumented launch",
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_INCREASE_HEAP_AT_LAUNCH,
				"Automatically increase the maximum heap memory for each instrumented launch",
				getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
		// nothing extra to do here
	}
}