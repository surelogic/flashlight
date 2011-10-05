package com.surelogic.flashlight.client.monitor.views;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.monitor.views.MonitorStatus.FieldStatus;
import com.surelogic.flashlight.client.monitor.views.MonitorStatus.LockStatus;

public class MonitorViewMediator implements MonitorListener {

    private final Composite f_status;
    private final Label f_statusImage;
    private final Label f_runText;
    private final Label f_startTimeText;

    private final Text f_fieldsSelector;
    private final Text f_edtSelector;

    private final TextViewer f_listing;

    private final Tree f_fields;
    private final Tree f_locks;
    private final Tree f_edtAlerts;

    private final Image f_notConnected;
    private final Image f_connected;
    private final Image f_error;
    private final Image f_done;

    private final Color f_protectedColor;
    private final Color f_sharedColor;
    private final Color f_dataRaceColor;
    private final Color f_unknownColor;
    private final Color f_unsharedColor;

    MonitorViewMediator(final Composite status, final Label statusImage,
            final Label runText, final Label startTimeText,
            final Text fieldsSelector, final Tree fieldsTree,
            final Tree locksTree, final Text edtSelector, final Tree edtTree,
            final TextViewer listing) {
        f_status = status;
        f_statusImage = statusImage;
        f_runText = runText;
        f_startTimeText = startTimeText;
        f_fieldsSelector = fieldsSelector;
        f_fields = fieldsTree;
        f_locks = locksTree;
        f_edtSelector = edtSelector;
        f_edtAlerts = edtTree;
        f_listing = listing;

        f_connected = SLImages.getImage(CommonImages.IMG_ASTERISK_ORANGE_100);
        f_notConnected = SLImages.getImage(CommonImages.IMG_ASTERISK_GRAY);
        f_error = SLImages.getImage(CommonImages.IMG_RED_X);
        f_done = SLImages.getImage(CommonImages.IMG_ASTERISK_DIAMOND_GRAY);

        Display display = statusImage.getDisplay();
        f_protectedColor = display.getSystemColor(SWT.COLOR_GREEN);
        f_sharedColor = display.getSystemColor(SWT.COLOR_YELLOW);
        f_dataRaceColor = display.getSystemColor(SWT.COLOR_RED);
        f_unknownColor = display.getSystemColor(SWT.COLOR_GRAY);
        f_unsharedColor = display.getSystemColor(SWT.COLOR_BLUE);
    }

    private static class SpecListener implements SelectionListener {

        private final String f_specType;

        SpecListener(final String specType) {
            f_specType = specType;
        }

        @Override
        public void widgetSelected(final SelectionEvent e) {
            // Do nothing, we only care about when enter is pressed.
        }

        @Override
        public void widgetDefaultSelected(final SelectionEvent e) {
            MonitorThread.sendCommand(String.format("set %s=%s", f_specType,
                    ((Text) e.widget).getText()));
        }

    }

    public void init() {
        MonitorThread.addListener(this);
        f_fieldsSelector.addSelectionListener(new SpecListener("fieldSpec"));
        f_edtSelector
                .addSelectionListener(new SpecListener("swingFieldAlerts"));
    }

    public void dispose() {
        // Nothing here
    }

    public void setFocus() {
        f_statusImage.setFocus();
    }

    @Override
    public void update(final MonitorStatus status) {
        final UIJob job = new UIJob("Update Monitor View") {
            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
                if (f_statusImage.isDisposed()) {
                    return Status.OK_STATUS;
                }
                switch (status.getState()) {
                case SEARCHING:
                    f_statusImage.setImage(f_notConnected);
                    f_runText.setText(status.getRunName());
                    f_startTimeText.setText(status.getRunTime());
                    f_status.layout();
                    f_fields.removeAll();
                    String clazz = null;
                    TreeItem clazzTree = null;
                    List<FieldStatus> fieldList = status.getFields();
                    for (FieldStatus f : fieldList) {
                        String newClazz = f.getClazz();
                        if (!newClazz.equals(clazz)) {
                            clazz = newClazz;
                            clazzTree = new TreeItem(f_fields, SWT.NONE);
                            clazzTree.setText(clazz);
                            clazzTree.setBackground(f_unknownColor);
                        }
                        TreeItem item = new TreeItem(clazzTree, SWT.NONE);
                        item.setText(f.getQualifiedFieldName());
                        item.setData(f);
                        item.setBackground(f_unknownColor);
                    }
                    break;
                case CONNECTED:
                    f_statusImage.setImage(f_connected);
                    int i = 0;
                    List<FieldStatus> fields = status.getFields();
                    for (TreeItem clazzItem : f_fields.getItems()) {
                        boolean allGray = true;
                        for (TreeItem item : clazzItem.getItems()) {
                            FieldStatus f = fields.get(i++);
                            item.setText(f.getQualifiedFieldName());
                            item.setData(f);
                            boolean gray = false;
                            if (f.hasDataRace()) {
                                item.setBackground(f_dataRaceColor);
                            } else if (f.isActivelyProtected()) {
                                item.setBackground(f_protectedColor);
                            } else if (f.isShared()) {
                                item.setBackground(f_sharedColor);
                            } else if (f.isUnshared()) {
                                item.setBackground(f_unsharedColor);
                            } else {
                                item.setBackground(f_unknownColor);
                                gray = true;
                            }
                            allGray &= gray;
                        }
                        if (allGray) {
                            clazzItem.setBackground(f_unknownColor);
                        } else {
                            clazzItem.setBackground(null);
                        }
                    }
                    f_locks.removeAll();
                    for (LockStatus l : status.getLocks()) {
                        TreeItem item = new TreeItem(f_locks, SWT.NONE);
                        if (l.isDeadlocked()) {
                            item.setForeground(f_dataRaceColor);
                        } else {
                            item.setForeground(f_unsharedColor);
                        }
                        item.setText(l.getName());
                    }
                    for (List<String> edges : status.getEdges()) {
                        TreeItem item = new TreeItem(f_locks, SWT.NONE);
                        item.setText(edges.toString());
                    }

                    f_edtAlerts.removeAll();
                    for (FieldStatus f : fields) {
                        if (f.isEDTAlert()) {
                            TreeItem item = new TreeItem(f_edtAlerts, SWT.NONE);
                            item.setText(f.getQualifiedFieldName());
                        }
                    }

                    Document d = new Document();
                    d.set(status.getListing());
                    f_listing.setDocument(d);
                    f_listing.refresh();
                    break;
                case NOTFOUND:
                    f_statusImage.setImage(f_error);
                    break;
                case TERMINATED:
                    f_statusImage.setImage(f_done);
                    break;
                }

                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

}
