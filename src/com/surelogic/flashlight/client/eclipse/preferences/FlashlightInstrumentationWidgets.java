package com.surelogic.flashlight.client.eclipse.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.i18n.I18N;

public class FlashlightInstrumentationWidgets {
	private final DialogPage page;
	private final IPreferenceStore prefs;
	private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	
	private final IntegerFieldEditor f_consolePort;
	private final IntegerFieldEditor f_outQSize;
	private final IntegerFieldEditor f_rawQSize;
	private final IntegerFieldEditor f_refinerySize;
	private final BooleanFieldEditor f_useSpyThread;
	
	public FlashlightInstrumentationWidgets(DialogPage page, IPreferenceStore prefs,
			                                Composite group1, Composite group2) {
		this.page = page;
		this.prefs = prefs;
		
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

		f_rawQSize = new IntegerFieldEditor(PreferenceConstants.P_RAWQ_SIZE,
				I18N.msg("flashlight.preference.page.rawQSize"), group2);
		finishIntSetup(group2, f_rawQSize, 1000, 50000);

		f_refinerySize = new IntegerFieldEditor(
				PreferenceConstants.P_REFINERY_SIZE, I18N
						.msg("flashlight.preference.page.refinerySize"), group2);
		finishIntSetup(group2, f_refinerySize, 1000, 50000);

		f_outQSize = new IntegerFieldEditor(PreferenceConstants.P_OUTQ_SIZE,
				I18N.msg("flashlight.preference.page.outQSize"), group2);
		finishIntSetup(group2, f_outQSize, 1000, 50000);

		f_useSpyThread = new BooleanFieldEditor(PreferenceConstants.P_USE_SPY,
				I18N.msg("flashlight.preference.page.useSpyThread"), group2);
		finishSetup(group2, f_useSpyThread);
		
		f_consolePort = new IntegerFieldEditor(
				PreferenceConstants.P_CONSOLE_PORT, I18N
						.msg("flashlight.preference.page.consolePort"), group2);
		finishIntSetup(group2, f_consolePort, 1024, 65535);		
	}
	
	private void finishIntSetup(Composite parent, IntegerFieldEditor edit, int min, int max) {
		edit.setValidRange(min, max);
		finishSetup(parent, edit);
	}
	
	private void finishSetup(Composite parent, FieldEditor edit) {
		edit.fillIntoGrid(parent, 2);
		edit.setPage(page);
		edit.setPreferenceStore(prefs);
		edit.load();
		f_editors.add(edit);
	}

	public Collection<FieldEditor> getEditors() {
		return f_editors;
	}
}
