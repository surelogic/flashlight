package com.surelogic.flashlight.client.eclipse.jobs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus.ConnectionState;

public class SendCommandToFlashlightMonitorJob extends AbstractSLJob {

    private final String f_command;
    private final MonitorStatus f_status;

    public SendCommandToFlashlightMonitorJob(final MonitorStatus status,
            final String command) {
        super(String.format("Sending %s command to %s - %s", command,
                status.getRunName(), status.getRunTime()));
        f_command = command;
        f_status = status;
    }

    @Override
    public SLStatus run(final SLProgressMonitor monitor) {
        try {
            Socket s = new Socket();
            BufferedReader portReader = new BufferedReader(new FileReader(
                    f_status.getPortFile()));
            int port;
            try {
                port = Integer.parseInt(portReader.readLine());
            } finally {
                portReader.close();
            }
            s.connect(new InetSocketAddress("localhost", port));
            try {
                f_status.setState(ConnectionState.CONNECTED);
                final PrintWriter writer = new PrintWriter(s.getOutputStream());
                writer.println(f_command);
                writer.flush();
            } finally {
                s.close();
            }
        } catch (IOException e) {
            SLLogger.getLoggerFor(SendCommandToFlashlightMonitorJob.class).log(
                    Level.WARNING,
                    String.format("Could not send command to %s - %s.",
                            f_status.getRunName(), f_status.getRunTime()), e);
        }
        return SLStatus.OK_STATUS;
    }

}
