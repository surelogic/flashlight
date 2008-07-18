package com.surelogic.flashlight.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.flashlight.Activator;

public class FlashlightPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public FlashlightPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Use this page to customize Flashlight.");
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
	}

	public void init(IWorkbench workbench) {
		// nothing extra to do here
	}
}