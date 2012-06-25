package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorImages;

public class ShowMonitorLegendDialog extends Dialog {

    protected ShowMonitorLegendDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(I18N.msg("flashlight.monitor.legend.dialog"));
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        parent = (Composite) super.createDialogArea(parent);

        final Composite legend = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout();
        legend.setLayout(layout);
        final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        legend.setLayoutData(layoutData);
        layout.numColumns = 1;
        layout.marginLeft = 5;
        final GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, false);

        MonitorImages im = new MonitorImages(parent.getDisplay());

        StyledText fieldSection = makeHeader(legend,
                I18N.msg("flashlight.monitor.legend.fieldSection"));
        fieldSection.setLayoutData(labelData);

        Label drColor = new Label(legend, SWT.NONE);
        drColor.setLayoutData(labelData);
        drColor.setBackground(im.getDataRaceColor());
        drColor.setText(I18N.msg("flashlight.monitor.legend.dataRace"));

        Label protectedColor = new Label(legend, SWT.NONE);
        protectedColor.setLayoutData(labelData);
        protectedColor.setBackground(im.getProtectedColor());
        protectedColor.setText(I18N.msg("flashlight.monitor.legend.protected"));

        Label sharedColor = new Label(legend, SWT.NONE);
        sharedColor.setLayoutData(labelData);
        sharedColor.setBackground(im.getSharedColor());
        sharedColor.setText(I18N.msg("flashlight.monitor.legend.shared"));

        Label unknownColor = new Label(legend, SWT.NONE);
        unknownColor.setLayoutData(labelData);
        unknownColor.setBackground(im.getUnknownColor());
        unknownColor.setText(I18N.msg("flashlight.monitor.legend.unknown"));

        Label unsharedColor = new Label(legend, SWT.NONE);
        unsharedColor.setLayoutData(labelData);
        unsharedColor.setBackground(im.getUnsharedColor());
        unsharedColor.setText(I18N.msg("flashlight.monitor.legend.unshared"));

        StyledText lockSection = makeHeader(legend,
                I18N.msg("flashlight.monitor.legend.lockSection"));
        lockSection.setLayoutData(labelData);

        Label lockColor = new Label(legend, SWT.NONE);
        lockColor.setLayoutData(labelData);
        lockColor.setBackground(im.getDataRaceColor());
        lockColor.setText(I18N.msg("flashlight.monitor.legend.deadlock"));

        Label noLockColor = new Label(legend, SWT.NONE);
        noLockColor.setLayoutData(labelData);
        noLockColor.setBackground(im.getUnsharedColor());
        noLockColor.setText(I18N.msg("flashlight.monitor.legend.noDeadlock"));

        StyledText edtSection = makeHeader(legend,
                I18N.msg("flashlight.monitor.legend.edtSection"));
        edtSection.setLayoutData(labelData);

        Label edtColor = new Label(legend, SWT.NONE);
        edtColor.setLayoutData(labelData);
        edtColor.setBackground(im.getEdtAlertColor());
        edtColor.setText(I18N.msg("flashlight.monitor.legend.edtViolation"));

        Label noEdtColor = new Label(legend, SWT.NONE);
        noEdtColor.setLayoutData(labelData);
        noEdtColor.setBackground(im.getUnknownColor());
        noEdtColor.setText(I18N.msg("flashlight.monitor.legend.noEdt"));
        return parent;
    }

    private static StyledText makeHeader(final Composite parent,
            final String text) {
        StyledText widget = new StyledText(parent, SWT.SINGLE);
        widget.setText(text);
        StyleRange style = new StyleRange();
        style.start = 0;
        style.length = text.length();
        style.fontStyle = SWT.BOLD;
        widget.setStyleRange(style);
        widget.setEditable(false);
        widget.setEnabled(false);
        widget.setLineAlignment(0, 1, SWT.CENTER);
        widget.setBackground(parent.getBackground());
        return widget;
    }

    public static int show() {
        return new ShowMonitorLegendDialog(EclipseUIUtility.getShell()).open();
    }
}
