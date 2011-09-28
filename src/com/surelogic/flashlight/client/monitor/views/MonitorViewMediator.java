package com.surelogic.flashlight.client.monitor.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.flashlight.client.monitor.views.MonitorStatus.FieldStatus;

public class MonitorViewMediator implements MonitorListener {

    private final Composite f_parent;

    private final Label f_runText;
    private final Label f_startTimeText;

    private final Text f_fieldsSelector;
    private final TextViewer f_listing;

    private final Tree f_fields;

    private final Color f_green;
    private final Color f_yellow;
    private final Color f_red;
    private final Color f_gray;

    MonitorViewMediator(final Composite parent, final Label runText,
            final Label startTimeText, final Text fieldsSelector,
            final Tree tree, final TextViewer listing) {
        f_parent = parent;
        f_runText = runText;
        f_startTimeText = startTimeText;
        f_fieldsSelector = fieldsSelector;
        f_fields = tree;
        f_listing = listing;
        Display display = parent.getDisplay();
        f_green = display.getSystemColor(SWT.COLOR_GREEN);
        f_yellow = display.getSystemColor(SWT.COLOR_YELLOW);
        f_red = display.getSystemColor(SWT.COLOR_RED);
        f_gray = display.getSystemColor(SWT.COLOR_GRAY);

    }

    public void init() {
        MonitorThread.addListener(this);
        f_fieldsSelector.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                // TODO
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                MonitorThread.sendCommand("set fieldSpec="
                        + ((Text) e.widget).getText());
            }
        });
    }

    public void dispose() {
        // Nothing here
    }

    public void setFocus() {
        f_parent.setFocus();
    }

    @Override
    public void update(final MonitorStatus status) {
        final UIJob job = new UIJob("Update Monitor View") {
            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
                if (f_parent.isDisposed()) {
                    return Status.OK_STATUS;
                }
                switch (status.getState()) {
                case SEARCHING:
                    f_runText.setText(status.getRunName());
                    f_startTimeText.setText(status.getRunTime());
                    f_parent.layout();
                    f_fields.removeAll();
                    for (FieldStatus f : status.getFields()) {
                        TreeItem item = new TreeItem(f_fields, SWT.NONE);
                        item.setText(f.getQualifiedFieldName());
                        item.setData(f);
                        item.setBackground(f_gray);
                    }
                    break;
                case CONNECTED:
                    int i = 0;
                    for (FieldStatus f : status.getFields()) {
                        final TreeItem item = f_fields.getItem(i++);
                        if (!((FieldStatus) item.getData())
                                .getQualifiedFieldName().equals(
                                        f.getQualifiedFieldName())) {
                            // FIXME this is test code, it should never happen
                            throw new IllegalStateException();
                        }
                        item.setText(f.getQualifiedFieldName());
                        item.setData(f);
                        if (f.isShared() && f.isUnshared()) {
                            item.setBackground(f_yellow);
                        } else if (f.isShared()) {
                            item.setBackground(f_red);
                        } else if (f.isUnshared()) {
                            item.setBackground(f_green);
                        } else {
                            item.setBackground(f_gray);
                        }
                    }
                    Document d = new Document();
                    d.set(status.getListing());
                    f_listing.setDocument(d);
                    f_listing.refresh();
                    break;
                case NOTFOUND:
                    // TODO
                    break;
                case TERMINATED:
                    // TODO
                    break;
                }

                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

}
