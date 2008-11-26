package com.surelogic.flashlight.client.eclipse.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.eclipse.preferences.LabeledScaleFieldEditor;
import com.surelogic.common.i18n.I18N;

import static com.surelogic._flashlight.common.InstrumentationConstants.*;

public class FlashlightInstrumentationWidgets {
	private final DialogPage page;
	private final IPreferenceStore prefs;
	private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	
	private final IntegerFieldEditor f_consolePort;
	private final ScaleFieldEditor f_outQSize;
	private final ScaleFieldEditor f_rawQSize;
	private final ScaleFieldEditor f_refinerySize;
	private final BooleanFieldEditor f_useSpyThread;
	
	public FlashlightInstrumentationWidgets(DialogPage page, IPreferenceStore prefs,
            Composite group) {
		this(page, prefs, group, group, group);
	}
	
	public FlashlightInstrumentationWidgets(DialogPage page, IPreferenceStore prefs,
			                                Composite group0, Composite group1, Composite group2) {
		this.page = page;
		this.prefs = prefs;
		
		BooleanFieldEditor f_filter = 
			new BooleanFieldEditor(PreferenceConstants.P_USE_FILTERING,
				I18N.msg("flashlight.preference.page.useFiltering"), group0);
		finishSetup(group0, f_filter);
		
		RadioGroupFieldEditor f_outputType = 
			new RadioGroupFieldEditor(PreferenceConstants.P_OUTPUT_TYPE,
					I18N.msg("flashlight.preference.page.outputType"), 2, 
					new String[][] {
				      {"XML", "false"},
					  {"Binary", "true"}
			        }, 
			        group1);
		finishSetup(group1, f_outputType);
		
		BooleanFieldEditor f_compress = 
			new BooleanFieldEditor(PreferenceConstants.P_COMPRESS_OUTPUT,
				I18N.msg("flashlight.preference.page.compressOutput"), group1);
		finishSetup(group1, f_compress);
		
		BooleanFieldEditor f_useRefinery = 
			new BooleanFieldEditor(PreferenceConstants.P_USE_REFINERY,
				I18N.msg("flashlight.preference.page.useRefinery"), group2);
		finishSetup(group2, f_useRefinery);

		f_rawQSize = new LabeledScaleFieldEditor(PreferenceConstants.P_RAWQ_SIZE,
				I18N.msg("flashlight.preference.page.rawQSize"), group2);
		finishScaleSetup(group2, f_rawQSize, FL_QUEUE_SIZE_MIN, FL_QUEUE_SIZE_MAX);

		f_refinerySize = new LabeledScaleFieldEditor(
				PreferenceConstants.P_REFINERY_SIZE, I18N
						.msg("flashlight.preference.page.refinerySize"), group2);
		finishScaleSetup(group2, f_refinerySize, FL_QUEUE_SIZE_MIN, FL_QUEUE_SIZE_MAX);

		f_outQSize = new LabeledScaleFieldEditor(PreferenceConstants.P_OUTQ_SIZE,
				I18N.msg("flashlight.preference.page.outQSize"), group2);
		finishScaleSetup(group2, f_outQSize, FL_QUEUE_SIZE_MIN, FL_QUEUE_SIZE_MAX);

		f_useSpyThread = new BooleanFieldEditor(PreferenceConstants.P_USE_SPY,
				I18N.msg("flashlight.preference.page.useSpyThread"), group2);
		finishSetup(group2, f_useSpyThread);
		
		f_consolePort = new IntegerFieldEditor(
				PreferenceConstants.P_CONSOLE_PORT, I18N
						.msg("flashlight.preference.page.consolePort"), group2);
		finishIntSetup(group2, f_consolePort, 1024, 65535);		
	}
	
	private void finishScaleSetup(Composite parent, ScaleFieldEditor edit, int min, int max) {
		edit.setMinimum(min);
		edit.setMaximum(max);
		edit.setIncrement(1);
		finishSetup(parent, edit);
	}
	
	private void finishIntSetup(Composite parent, IntegerFieldEditor edit, int min, int max) {
		edit.setValidRange(min, max);
		finishSetup(parent, edit);
	}
	
	private void finishSetup(Composite parent, FieldEditor edit) {
		edit.fillIntoGrid(parent, 3);
		edit.setPage(page);
		edit.setPreferenceStore(prefs);
		f_editors.add(edit);
	}

	public Collection<FieldEditor> getEditors() {
		return f_editors;
	}
}
