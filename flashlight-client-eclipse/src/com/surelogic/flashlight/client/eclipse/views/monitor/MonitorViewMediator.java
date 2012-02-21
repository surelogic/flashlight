package com.surelogic.flashlight.client.eclipse.views.monitor;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.surelogic.common.XUtil;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.flashlight.client.eclipse.jobs.SendCommandToFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus.ConnectionState;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus.FieldStatus;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus.LockStatus;

public class MonitorViewMediator {

    private static final String EDT_ALERTS = "swingFieldAlerts";
    private static final String FIELD_SPEC = "fieldSpec";

    private final Composite f_status;
    private final Label f_statusImage;
    private final Label f_runText;
    private final Label f_startTimeText;

    private final Text f_fieldsSelector;
    private final Text f_edtSelector;

    private final Button f_fieldsButton;
    private final Button f_edtButton;

    private final TextViewer f_listing;

    private final Tree f_fields;
    private final Tree f_locks;
    private final Tree f_edtAlerts;

    private final MonitorImages f_im;

    private MonitorStatus f_monitorStatus;

    MonitorViewMediator(final Composite status, final Label statusImage,
            final Label runText, final Label startTimeText,
            final Text fieldsSelector, final Button fieldsButton,
            final Tree fieldsTree, final Tree locksTree,
            final Text edtSelector, final Button edtButton, final Tree edtTree,
            final TextViewer listing) {
        f_status = status;
        f_statusImage = statusImage;
        f_runText = runText;
        f_startTimeText = startTimeText;
        f_fieldsSelector = fieldsSelector;
        f_fieldsButton = fieldsButton;
        f_fields = fieldsTree;
        f_locks = locksTree;
        f_edtSelector = edtSelector;
        f_edtButton = edtButton;
        f_edtAlerts = edtTree;
        f_listing = listing;
        f_im = new MonitorImages(statusImage.getDisplay());
    }

    private class SpecListener implements SelectionListener, ModifyListener {

        private final String f_specType;
        private final Text f_widget;
        private final Button f_button;

        SpecListener(final String specType, final Text widget,
                final Button button) {
            f_specType = specType;
            f_widget = widget;
            f_button = button;
        }

        @Override
        public void widgetSelected(final SelectionEvent e) {
            EclipseJob.getInstance().schedule(
                    new SendCommandToFlashlightMonitorJob(f_monitorStatus,
                            String.format("set %s=%s", f_specType,
                                    f_widget.getText())));
            f_button.setEnabled(false);
        }

        @Override
        public void widgetDefaultSelected(final SelectionEvent e) {
            EclipseJob.getInstance().schedule(
                    new SendCommandToFlashlightMonitorJob(f_monitorStatus,
                            String.format("set %s=%s", f_specType,
                                    f_widget.getText())));
            f_button.setEnabled(false);
        }

        @Override
        public void modifyText(final ModifyEvent e) {
            f_button.setEnabled(true);
        }

    }

    public void init() {
        WatchFlashlightMonitorJob.setMediator(this);
        SpecListener l = new SpecListener(FIELD_SPEC, f_fieldsSelector,
                f_fieldsButton);
        f_fieldsSelector.addSelectionListener(l);
        f_fieldsSelector.addModifyListener(l);
        f_fieldsButton.addSelectionListener(l);
        f_fieldsButton.setEnabled(false);
        l = new SpecListener(EDT_ALERTS, f_edtSelector, f_edtButton);
        f_edtSelector.addSelectionListener(l);
        f_edtSelector.addModifyListener(l);
        f_edtButton.addSelectionListener(l);
        f_edtButton.setEnabled(false);
    }

    public void dispose() {
        WatchFlashlightMonitorJob.setMediator(null);
        f_im.dispose();
    }

    public void setFocus() {
        f_statusImage.setFocus();
    }

    private void searching(final MonitorStatus status) {
        f_statusImage.setImage(f_im.getConnecting());
        f_runText.setText(status.getRunName());
        f_startTimeText.setText(status.getRunTime());
        f_status.layout();
        f_fieldsSelector.setText("");
        f_fieldsButton.setEnabled(false);
        f_edtSelector.setText("");
        f_edtButton.setEnabled(false);
        List<FieldStatus> fieldList = status.getFields();
        f_fields.removeAll();
        String clazz = null;
        String pakkage = null;
        TreeItem pakkageTree = null;
        TreeItem clazzTree = null;
        for (FieldStatus f : fieldList) {
            String qualifiedClazz = f.getClazz();
            int split = qualifiedClazz.lastIndexOf('.');
            String newPakkage, newClazz;
            if (split != -1) {
                newPakkage = qualifiedClazz.substring(0, split);
                newClazz = qualifiedClazz.substring(split + 1);
            } else {
                newPakkage = "";
                newClazz = qualifiedClazz;
            }
            if (!newPakkage.equals(pakkage)) {
                pakkage = newPakkage;
                clazz = null;
                pakkageTree = new TreeItem(f_fields, SWT.None);
                pakkageTree.setText(pakkage);
                pakkageTree.setBackground(f_im.getUnknownColor());
                pakkageTree.setImage(f_im.getPackage());
            }
            if (!newClazz.equals(clazz)) {
                clazz = newClazz;
                clazzTree = new TreeItem(pakkageTree, SWT.NONE);
                clazzTree.setText(clazz);
                clazzTree.setBackground(f_im.getUnknownColor());
                clazzTree.setImage(f_im.getClazz());
            }
            TreeItem item = new TreeItem(clazzTree, SWT.NONE);
            item.setText(f.getField());
            item.setData(f);
            item.setBackground(f_im.getUnknownColor());
        }
        clazz = null;
        pakkage = null;
        pakkageTree = null;
        clazzTree = null;
        f_edtAlerts.removeAll();
        for (FieldStatus f : fieldList) {
            String qualifiedClazz = f.getClazz();
            int split = qualifiedClazz.lastIndexOf('.');

            String newPakkage, newClazz;
            if (split != -1) {
                newPakkage = qualifiedClazz.substring(0, split);
                newClazz = qualifiedClazz.substring(split + 1);
            } else {
                newPakkage = "";
                newClazz = qualifiedClazz;
            }
            if (!newPakkage.equals(pakkage)) {
                pakkage = newPakkage;
                clazz = null;
                pakkageTree = new TreeItem(f_edtAlerts, SWT.None);
                pakkageTree.setText(pakkage);
                pakkageTree.setBackground(f_im.getUnknownColor());
                pakkageTree.setImage(f_im.getPackage());
            }

            if (!newClazz.equals(clazz)) {
                clazz = newClazz;
                clazzTree = new TreeItem(pakkageTree, SWT.NONE);
                clazzTree.setText(clazz);
                clazzTree.setBackground(f_im.getUnknownColor());
                clazzTree.setImage(f_im.getClazz());
            }
            TreeItem item = new TreeItem(clazzTree, SWT.NONE);
            item.setText(f.getField());
            item.setData(f);
            item.setBackground(f_im.getUnknownColor());
        }
        f_locks.removeAll();
    }

    private void connected(final MonitorStatus status) {
        f_statusImage.setImage(f_im.getConnected());
        if (!f_fieldsButton.isEnabled()) {
            String spec = status.getProperty(FIELD_SPEC);
            if (spec != null) {
                f_fieldsSelector.setText(spec);
                f_fieldsButton.setEnabled(false);
            }
        }
        f_fieldsSelector.setEnabled(true);
        if (!f_edtButton.isEnabled()) {
            String spec = status.getProperty(EDT_ALERTS);
            if (spec != null) {
                f_edtSelector.setText(spec);
                f_edtButton.setEnabled(false);
            }
        }
        f_edtSelector.setEnabled(true);
        int i = 0;
        List<FieldStatus> fields = status.getFields();
        for (TreeItem packageItem : f_fields.getItems()) {
            boolean allGrayPackage = true;
            for (TreeItem clazzItem : packageItem.getItems()) {
                boolean allGrayClass = true;
                for (TreeItem item : clazzItem.getItems()) {
                    FieldStatus f = fields.get(i++);
                    boolean gray = false;
                    if (f.hasDataRace()) {
                        item.setBackground(f_im.getDataRaceColor());
                    } else if (f.isActivelyProtected()) {
                        item.setBackground(f_im.getProtectedColor());
                    } else if (f.isShared()) {
                        item.setBackground(f_im.getSharedColor());
                    } else if (f.isUnshared()) {
                        item.setBackground(f_im.getUnsharedColor());
                    } else {
                        item.setBackground(f_im.getUnknownColor());
                        gray = true;
                    }
                    allGrayClass &= gray;
                }
                if (allGrayClass) {
                    clazzItem.setBackground(f_im.getUnknownColor());
                } else {
                    clazzItem.setBackground(null);
                }
                allGrayPackage &= allGrayClass;
            }
            if (allGrayPackage) {
                packageItem.setBackground(f_im.getUnknownColor());
            } else {
                packageItem.setBackground(null);
            }
        }
        i = 0;
        for (TreeItem packageItem : f_edtAlerts.getItems()) {
            boolean allGrayPackage = true;
            for (TreeItem clazzItem : packageItem.getItems()) {
                boolean allGrayClass = true;
                for (TreeItem item : clazzItem.getItems()) {
                    FieldStatus f = fields.get(i++);
                    boolean gray = false;
                    if (f.isEDTAlert()) {
                        item.setBackground(f_im.getEdtAlertColor());
                    } else {
                        gray = true;
                    }
                    allGrayClass &= gray;
                }
                if (allGrayClass) {
                    clazzItem.setBackground(f_im.getUnknownColor());
                } else {
                    clazzItem.setBackground(null);
                }
                allGrayPackage &= allGrayClass;
            }
            if (allGrayPackage) {
                packageItem.setBackground(f_im.getUnknownColor());
            } else {
                packageItem.setBackground(null);
            }
        }
        f_locks.removeAll();
        for (LockStatus l : status.getLocks()) {
            TreeItem item = new TreeItem(f_locks, SWT.NONE);
            if (l.isDeadlocked()) {
                item.setForeground(f_im.getDataRaceColor());
            } else {
                item.setForeground(f_im.getUnsharedColor());
            }
            item.setText(l.getName());
        }

        Document d = new Document();
        d.set(status.getListing());
        if (XUtil.useExperimental()) {
            f_listing.setDocument(d);
            f_listing.refresh();
        }

    }

    private void finished(final MonitorStatus status) {
        f_edtSelector.setEnabled(false);
        f_edtButton.setEnabled(false);
        f_fieldsSelector.setEnabled(false);
        f_fieldsButton.setEnabled(false);
        if (status.getState() == ConnectionState.NOTFOUND) {
            f_statusImage.setImage(f_im.getError());
        } else {
            f_statusImage.setImage(f_im.getDone());
        }
    }

    /**
     * Update the monitor view mediator. Only call this from the UI thread.
     */
    public void update(final MonitorStatus status) {
        f_monitorStatus = status;
        if (f_statusImage.isDisposed()) {
            return;
        }
        switch (status.getState()) {
        case SEARCHING:
            searching(status);
            break;
        case CONNECTED:
            connected(status);
            break;
        case NOTFOUND:
        case TERMINATED:
            finished(status);
            break;
        }
    }

}
