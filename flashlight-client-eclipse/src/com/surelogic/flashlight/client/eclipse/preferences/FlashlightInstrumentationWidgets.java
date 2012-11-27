package com.surelogic.flashlight.client.eclipse.preferences;

import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTQ_SIZE_MIN;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_QUEUE_SIZE_MAX;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RAWQ_SIZE_MIN;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE_MAX;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE_MIN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.preferences.LabeledScaleFieldEditor;

public class FlashlightInstrumentationWidgets {
    private final DialogPage f_page;
    private final IPreferenceStore f_preferences;
    private final List<FieldEditor> f_editors = new ArrayList<FieldEditor>();

    private final IntegerFieldEditor f_consolePort;
    private final ScaleFieldEditor f_outQSize;
    private final ScaleFieldEditor f_rawQSize;
    private final ScaleFieldEditor f_refinerySize;
    private final BooleanFieldEditor f_useSpyThread;

    public FlashlightInstrumentationWidgets(final DialogPage page,
            final IPreferenceStore prefs, final Composite group) {
        this(page, prefs, group, group, group);
    }

    public FlashlightInstrumentationWidgets(final DialogPage page,
            final IPreferenceStore prefs, final Composite group3,
            final Composite group1, final Composite group2) {
        f_page = page;
        f_preferences = prefs;

        final BooleanFieldEditor f_postmortem = new BooleanFieldEditor(
                FlashlightPreferencesUtility.POSTMORTEM_MODE,
                I18N.msg("flashlight.preference.page.usePostmortem"), group3);
        finishSetup(group3, f_postmortem);

        f_consolePort = new IntegerFieldEditor(
                FlashlightPreferencesUtility.CONSOLE_PORT,
                I18N.msg("flashlight.preference.page.consolePort"), group3);
        finishIntSetup(group3, f_consolePort, 1024, 65535);

        final RadioGroupFieldEditor f_collectionType = new RadioGroupFieldEditor(
                FlashlightPreferencesUtility.COLLECTION_TYPE,
                I18N.msg("flashlight.preference.page.collectionType"), 2,
                new String[][] { { "All", "ALL" },
                        { "Only lock info", "ONLY_LOCKS" } }, group1);
        finishSetup(group1, f_collectionType);

        final BooleanFieldEditor f_compress = new BooleanFieldEditor(
                FlashlightPreferencesUtility.COMPRESS_OUTPUT,
                I18N.msg("flashlight.preference.page.compressOutput"), group1);
        finishSetup(group1, f_compress);

        final BooleanFieldEditor f_useRefinery = new BooleanFieldEditor(
                FlashlightPreferencesUtility.USE_REFINERY,
                I18N.msg("flashlight.preference.page.useRefinery"), group2);
        finishSetup(group2, f_useRefinery);

        f_rawQSize = new LabeledScaleFieldEditor(
                FlashlightPreferencesUtility.RAWQ_SIZE,
                I18N.msg("flashlight.preference.page.rawQSize"), group2);
        finishScaleSetup(group2, f_rawQSize, FL_RAWQ_SIZE_MIN,
                FL_QUEUE_SIZE_MAX);

        f_refinerySize = new LabeledScaleFieldEditor(
                FlashlightPreferencesUtility.REFINERY_SIZE,
                I18N.msg("flashlight.preference.page.refinerySize"), group2);
        finishScaleSetup(group2, f_refinerySize, FL_REFINERY_SIZE_MIN,
                FL_REFINERY_SIZE_MAX);

        f_outQSize = new LabeledScaleFieldEditor(
                FlashlightPreferencesUtility.OUTQ_SIZE,
                I18N.msg("flashlight.preference.page.outQSize"), group2);
        finishScaleSetup(group2, f_outQSize, FL_OUTQ_SIZE_MIN,
                FL_QUEUE_SIZE_MAX);

        f_useSpyThread = new BooleanFieldEditor(
                FlashlightPreferencesUtility.USE_SPY,
                I18N.msg("flashlight.preference.page.useSpyThread"), group2);
        finishSetup(group2, f_useSpyThread);

    }

    private void finishScaleSetup(final Composite parent,
            final ScaleFieldEditor edit, final int min, final int max) {
        edit.setMinimum(min);
        edit.setMaximum(max);
        edit.setIncrement(1);
        finishSetup(parent, edit);
    }

    private void finishIntSetup(final Composite parent,
            final IntegerFieldEditor edit, final int min, final int max) {
        edit.setValidRange(min, max);
        finishSetup(parent, edit);
    }

    private void finishSetup(final Composite parent, final FieldEditor edit) {
        edit.fillIntoGrid(parent, 3);
        edit.setPage(f_page);
        edit.setPreferenceStore(f_preferences);
        f_editors.add(edit);
    }

    public Collection<FieldEditor> getEditors() {
        return f_editors;
    }
}
