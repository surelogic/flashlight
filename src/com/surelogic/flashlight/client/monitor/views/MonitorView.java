package com.surelogic.flashlight.client.monitor.views;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public class MonitorView extends ViewPart {

    private MonitorViewMediator f_mediator;

    @Override
    public void createPartControl(final Composite parent) {
        final GridLayout layout = new GridLayout();
        parent.setLayout(layout);
        final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.setLayoutData(layoutData);

        /*
         * Status Bar
         */
        final Composite status = new Composite(parent, SWT.NONE);
        final GridLayout statusLayout = new GridLayout();
        statusLayout.marginHeight = 0;
        statusLayout.marginWidth = 0;
        statusLayout.numColumns = 3;
        statusLayout.verticalSpacing = 0;
        status.setLayout(statusLayout);
        final GridData statusLayoutData = new GridData(SWT.FILL, SWT.FILL,
                true, false);
        status.setLayoutData(statusLayoutData);

        final Label statusImage = new Label(status, SWT.NONE);
        statusImage.setImage(SLImages.getImage(CommonImages.IMG_GRAY_CIRCLE));

        final Label startTimeText = new Label(status, SWT.NONE);
        final Label runText = new Label(status, SWT.NONE);

        /*
         * Tab folder
         */
        final TabFolder folder = new TabFolder(parent, SWT.NONE);
        folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /*
         * Fields Tab
         */
        final TabItem fieldsTab = new TabItem(folder, SWT.NONE);
        fieldsTab.setText("Fields");
        final Composite fieldsBody = new Composite(folder, SWT.NONE);
        fieldsBody.setLayout(new GridLayout());
        fieldsBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Text fieldsSelector = new Text(fieldsBody, SWT.SINGLE);
        fieldsSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                false));

        final Tree fieldsTree = new Tree(fieldsBody, SWT.VIRTUAL);
        fieldsTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        fieldsTab.setControl(fieldsBody);
        fieldsBody.layout();

        /*
         * Locks Tab
         */
        final TabItem locksTab = new TabItem(folder, SWT.NONE);
        locksTab.setText("Locks");

        Composite locksBody = new Composite(folder, SWT.NONE);
        locksBody.setLayout(new GridLayout());
        locksBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Tree locksTree = new Tree(locksBody, SWT.VIRTUAL);
        locksTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        locksTab.setControl(locksBody);

        /*
         * EDT Tab
         */
        final TabItem edtTab = new TabItem(folder, SWT.NONE);
        edtTab.setText("EDT");

        final Composite edtBody = new Composite(folder, SWT.NONE);
        edtBody.setLayout(new GridLayout());
        edtBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Text edtSelector = new Text(edtBody, SWT.SINGLE);
        edtSelector
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        final Tree edtTree = new Tree(edtBody, SWT.NONE);
        edtTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        edtTab.setControl(edtBody);

        /*
         * Listing tab, we aren't keeping this.
         */
        final TabItem removeThis = new TabItem(folder, SWT.NONE);
        removeThis.setText("List");
        final TextViewer tv = new TextViewer(folder, SWT.BORDER | SWT.V_SCROLL
                | SWT.H_SCROLL);

        removeThis.setControl(tv.getControl());
        f_mediator = new MonitorViewMediator(status, statusImage, runText,
                startTimeText, fieldsSelector, fieldsTree, locksTree,
                edtSelector, edtTree, tv);

        f_mediator.init();
    }

    @Override
    public void dispose() {
        /*
         * We need to check for null because Eclipse gives us no guarantee that
         * createPartControl() was called.
         */
        if (f_mediator != null) {
            f_mediator.dispose();
            f_mediator = null;
        }
        super.dispose();
    }

    @Override
    public void setFocus() {
        /*
         * This is a good point to refresh the contents of the view.
         */
        if (f_mediator != null) {
            f_mediator.setFocus();
        }
    }
}
