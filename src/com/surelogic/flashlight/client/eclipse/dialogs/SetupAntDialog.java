package com.surelogic.flashlight.client.eclipse.dialogs;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.surelogic.common.CommonImages;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;

public class SetupAntDialog extends TitleAreaDialog {
    private static final String FL_ANT_ZIP = "/lib/flashlight-ant.zip";
    private static final String FLASHLIGHT_DIR = "flashlight-ant";

    private static final int CONTENTS_WIDTH_HINT = 200;
    private static final int CONTENTS_HEIGHT_HINT = 40;

    private final String logo;
    private final String help;
    private Text chooseFileText;

    /**
     * Used to open the dialog
     * 
     * @param logo
     *            a {@link CommonImages} location that corresponds to the
     *            application logo
     * @param shell
     *            a shell.
     * @param href
     *            the help document location of the tutorial section.
     */
    public static void open(final Shell shell, final String logo,
            final String href) {
        final SetupAntDialog dialog = new SetupAntDialog(shell, logo, href);
        dialog.open();
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(I18N.msg("flashlight.dialog.installAnt.title"));
        newShell.setImage(SLImages.getImage(logo));
    }

    protected SetupAntDialog(final Shell parentShell, final String logo,
            final String href) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        this.logo = logo;
        this.help = href;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite contents = (Composite) super.createDialogArea(parent);
        final Composite container = new Composite(contents, SWT.NONE);
        final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = CONTENTS_WIDTH_HINT;
        data.heightHint = CONTENTS_HEIGHT_HINT;
        container.setLayoutData(data);

        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        container.setLayout(gridLayout);

        final Label chooseFileLabel = new Label(container, SWT.NONE);
        chooseFileLabel.setText("Install To:");
        chooseFileLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
                false, 1, 1));
        chooseFileText = new Text(container, SWT.None);
        chooseFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false, 1, 1));
        final Button chooseFileButton = new Button(container, SWT.PUSH);
        chooseFileButton.setText("Browse...");
        chooseFileButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                false, false, 1, 1));
        chooseFileButton.addListener(SWT.Selection, new Listener() {
            private DirectoryDialog fd;

            @Override
            public void handleEvent(final Event event) {
                if (fd == null) {
                    fd = new DirectoryDialog(getShell());
                    fd.setText("Install Directory");
                }
                final String fileName = chooseFileText.getText();
                if (fileName.length() > 0) {
                    fd.setFilterPath(fileName);
                }
                final String selectedFilename = fd.open();
                if (selectedFilename != null) {
                    chooseFileText.setText(selectedFilename);
                }
            }
        });

        setTitle(I18N.msg("flashlight.dialog.installAnt.title"));
        setMessage(I18N.msg("flashlight.dialog.installAnt.info"),
                IMessageProvider.INFORMATION);
        Dialog.applyDialogFont(container);

        return container;
    }

    @Override
    protected void okPressed() {
        String text = chooseFileText.getText();
        File file = new File(text);
        if (file.exists()) {
            File target = new File(file, FLASHLIGHT_DIR);
            if (target.exists()) {
                MessageDialog
                        .openInformation(
                                EclipseUIUtility.getShell(),
                                "Directory already exists.",
                                String.format(
                                        "A directory named %s already exists in %s.  The Flashlight Ant tasks have most likely already been unzipped here.",
                                        FLASHLIGHT_DIR, file.toString()));
            } else {
                target.mkdir();
                URL url = Thread.currentThread().getContextClassLoader()
                        .getResource(FL_ANT_ZIP);
                try {
                    File tmp = File.createTempFile("fla", "zip");
                    try {
                        FileUtility.copy(url, tmp);
                        FileUtility.unzipFile(tmp, target);
                        MessageDialog
                                .openInformation(
                                        EclipseUIUtility.getShell(),
                                        "Install Successful",
                                        String.format(
                                                "The Flashlight Ant tasks can be found in the %s folder.",
                                                target.toString()));
                    } finally {
                        tmp.delete();
                    }
                } catch (IOException e) {
                    target.delete();
                    e.printStackTrace();
                }
            }
        }
        super.okPressed();
    }
}
