package com.surelogic.flashlight.client.eclipse.jobs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;

public class SendCommandToFlashlightMonitorJob extends AbstractSLJob {

    private final String f_command;
    private final MonitorStatus f_status;

    public SendCommandToFlashlightMonitorJob(final MonitorStatus status,
            final String command) {
        super(String.format("Sending %s command to %s", command,
                status.getRunId()));
        f_command = command;
        f_status = status;
    }

    @Override
    public SLStatus run(final SLProgressMonitor monitor) {
        try {
            final Socket s = new Socket();
            final BufferedReader portReader = new BufferedReader(
                    new FileReader(f_status.getPortFile()));
            int port;
            try {
                port = Integer.parseInt(portReader.readLine());
            } finally {
                portReader.close();
            }
            s.connect(new InetSocketAddress("localhost", port));
            try {
                final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(s.getInputStream()));
                final PrintWriter writer = new PrintWriter(s.getOutputStream());
                WatchFlashlightMonitorJob.readUpTo(reader,
                        WatchFlashlightMonitorJob.DELIMITER);
                WatchFlashlightMonitorJob.writeCommand(writer, f_command);
                WatchFlashlightMonitorJob.readUpTo(reader,
                        WatchFlashlightMonitorJob.DELIMITER);
            } finally {
                s.close();
            }
        } catch (final IOException e) {
            SLLogger.getLoggerFor(SendCommandToFlashlightMonitorJob.class).log(
                    Level.WARNING,
                    String.format("Could not send command to %s.",
                            f_status.getRunId()), e);
        }
        return SLStatus.OK_STATUS;
    }

}
