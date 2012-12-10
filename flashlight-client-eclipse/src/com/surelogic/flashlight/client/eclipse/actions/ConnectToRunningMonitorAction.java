package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;

public class ConnectToRunningMonitorAction extends Action {

    public ConnectToRunningMonitorAction() {
        setText(I18N.msg("flashlight.monitor.connectToRun"));
        setToolTipText(I18N.msg("flashlight.monitor.connectToRun.tooltip"));
        setImageDescriptor(SLImages
                .getImageDescriptor(CommonImages.IMG_QUERY_BACK));
        setDisabledImageDescriptor(SLImages
                .getImageDescriptor(CommonImages.IMG_QUERY_GRAY));
    }

    @Override
    public void run() {
        DirectoryDialog dialog = new DirectoryDialog(
                EclipseUIUtility.getShell());
        dialog.setText(I18N.msg("flashlight.monitor.connectToRun.dialog"));
        dialog.setMessage(I18N
                .msg("flashlight.monitor.connectToRun.dialog.msg"));
        final String result = dialog.open();
        if (result != null) {
            File runDir = new File(result);
            if (runDir.isDirectory()) {
                final String runName = runDir.getName();
                int split = runName.indexOf('-');
                String name;
                Date date;
                if (split > 0) {
                    name = runName.substring(0, split);
                    DateFormat format = new SimpleDateFormat(
                            InstrumentationConstants.DATE_FORMAT);
                    try {
                        date = format.parse(runName.substring(split));
                    } catch (ParseException e) {
                        date = new Date();
                    }
                } else {
                    name = result;
                    date = new Date();
                }
                MonitorStatus status = new MonitorStatus(
                        RunManager.getRunIdStringFrom(runDir));
                /* Let the monitor thread know it should expect a launch */
                final Job job = EclipseUtility
                        .toEclipseJob(new WatchFlashlightMonitorJob(status));
                job.setSystem(true);
                job.schedule();
            }
        }
    }
}
