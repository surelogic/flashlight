package com.surelogic.flashlight.client.eclipse.jobs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorStatus.ConnectionState;
import com.surelogic.flashlight.client.eclipse.views.monitor.MonitorView;
import com.surelogic.flashlight.common.model.RawFileUtility;

public class WatchFlashlightMonitorJob extends AbstractSLJob {

    static final String DELIMITER = "**********";
    private static final Pattern LIST_PATTERN = Pattern.compile("^\\[|, |\\]$");
    private static final Pattern LOCK_EDGE_DELIM = Pattern.compile(" -> ");

    private final MonitorStatus f_status;

    public WatchFlashlightMonitorJob(final MonitorStatus status) {
        super(String.format("Checking status of %s - %s", status.getRunName(),
                status.getRunTime()));
        f_status = status;
    }

    @Override
    public SLStatus run(final SLProgressMonitor monitor) {
        MonitorView view = (MonitorView) EclipseUIUtility
                .getView("com.surelogic.flashlight.client.monitor.views.MonitorView");
        if (view != null) {
            try {
                if (RawFileUtility.doneCollectingDataInto(f_status
                        .getRunDirectory())) {
                    f_status.setState(ConnectionState.TERMINATED);
                } else if (!f_status.getPortFile().exists()) {
                    final int timeout = f_status.getTimeout();
                    if (timeout < 60) {
                        f_status.setTimeout(timeout + 1);
                    } else {
                        f_status.setState(ConnectionState.NOTFOUND);
                    }
                } else {
                    final BufferedReader portReader = new BufferedReader(
                            new FileReader(f_status.getPortFile()));
                    int port;
                    try {
                        port = Integer.parseInt(portReader.readLine());
                    } finally {
                        portReader.close();
                    }
                    try {
                        updateStatus(port);
                    } catch (final DoneException e) {
                        f_status.setState(ConnectionState.TERMINATED);
                    }
                }
            } catch (final IOException e) {
                f_status.setState(ConnectionState.NOTFOUND);
            }
        }
        boolean callback = f_status.getState() != ConnectionState.NOTFOUND
                && f_status.getState() != ConnectionState.TERMINATED;
        new UpdateUIMonitorJob(new MonitorStatus(f_status), callback)
                .schedule();
        return SLStatus.OK_STATUS;
    }

    private void updateStatus(final int port) throws IOException {
        final Socket s = new Socket();
        s.connect(new InetSocketAddress("localhost", port));
        try {
            if (f_status.getState() == ConnectionState.SEARCHING) {
                // Make sure we trigger at least one searching event.
                new UpdateUIMonitorJob(new MonitorStatus(f_status), false)
                        .schedule();
            }
            f_status.setState(ConnectionState.CONNECTED);
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            final PrintWriter writer = new PrintWriter(s.getOutputStream());
            readUpTo(reader, DELIMITER);

            writeCommand(writer, "shared");
            final List<String> sharedResult = readUpTo(reader, DELIMITER);
            boolean shared = true;
            String className = "";
            for (final String line : sharedResult) {
                if (line.isEmpty()) {
                    // Do nothing
                } else if (line.startsWith("Shared Fields")) {
                    shared = true;
                } else if (line.startsWith("Unshared Fields")) {
                    shared = false;
                } else if (isField(line)) {
                    final Set<String> fieldSet = shared ? f_status.getShared()
                            : f_status.getUnshared();
                    fieldSet.add(getField(className, line));
                } else {
                    className = line;
                }
            }

            writeCommand(writer, "locksets");
            readUpTo(reader, "Potential Race Conditions");
            final List<String> locksetResults = readUpTo(reader,
                    "Actively protected fields");
            String clazz = "";
            for (final String line : locksetResults) {
                if (isField(line)) {
                    f_status.getRaces().add(getField(clazz, line));
                } else {
                    clazz = line;
                }
            }
            final List<String> protectedResults = readUpTo(reader,
                    "Garbage Collected Fields");
            f_status.getActivelyProtected().clear();
            for (final String line : protectedResults) {
                if (isField(line)) {
                    f_status.getActivelyProtected().add(getField(clazz, line));
                } else {
                    clazz = line;
                }
            }
            readUpTo(reader, DELIMITER);

            writeCommand(writer, "deadlocks");
            readUpTo(reader, "Lock Orderings");
            final List<String> orderingResults = readUpTo(reader,
                    "Potential Deadlocks");
            for (final String line : orderingResults) {
                final String[] split = LOCK_EDGE_DELIM.split(line);
                final List<String> edge = new ArrayList<String>(
                        split.length - 1);
                for (int i = 1; i < split.length; i++) {
                    edge.add(split[i]);
                }
                f_status.getEdges().add(edge);
            }

            f_status.getDeadlocks().clear();
            final String deadlockLine = reader.readLine();
            if (deadlockLine == null) {
                throw new DoneException();
            }
            final String[] split = LIST_PATTERN.split(deadlockLine);
            for (final String deadlock : split) {
                if (!deadlock.isEmpty()) {
                    f_status.getDeadlocks().add(deadlock);
                }
            }
            readUpTo(reader, DELIMITER);

            writeCommand(writer, "alerts");
            readUpTo(reader, "Swing Event Dispatch Thread");

            f_status.getAlerts().clear();
            final List<String> swingAlerts = readUpTo(reader, "Shared Field");
            for (final String line : swingAlerts) {
                if (isField(line)) {
                    f_status.getAlerts().add(getField(clazz, line));
                } else {
                    clazz = line;
                }
            }
            readUpTo(reader, DELIMITER);
            writeCommand(writer, "props");
            for (final String propStr : readUpTo(reader, DELIMITER)) {
                final int eq = propStr.indexOf('=');
                if (eq > 0) {
                    f_status.setProperty(propStr.substring(0, eq),
                            propStr.substring(eq + 1));
                }
            }
            writeCommand(writer, "list");
            final StringBuilder b = new StringBuilder();
            for (final String str : readUpTo(reader, DELIMITER)) {
                b.append(str);
                b.append('\n');
            }
            f_status.setListing(b.toString());
        } finally {
            s.close();
        }
    }

    static void writeCommand(final PrintWriter writer, final String command) {
        writer.println(command);
        writer.flush();
    }

    /**
     * Reads until it reaches a line that starts with the given marker.
     * 
     * @param reader
     * @param marker
     * @return
     * @throws IOException
     */
    static List<String> readUpTo(final BufferedReader reader,
            final String marker) throws IOException {
        final List<String> lines = new ArrayList<String>();
        String line;
        for (line = reader.readLine(); line != null && !line.startsWith(marker); line = reader
                .readLine()) {
            lines.add(line);
        }
        if (line == null) {
            throw new DoneException();
        }
        return lines;
    }

    private static class UpdateUIMonitorJob extends SLUIJob {

        private final MonitorStatus f_status;
        private final boolean f_callback;

        UpdateUIMonitorJob(final MonitorStatus status, final boolean callback) {
            f_status = status;
            f_callback = callback;
        }

        @Override
        public IStatus runInUIThread(final IProgressMonitor monitor) {
            MonitorView view = (MonitorView) EclipseUIUtility
                    .getView("com.surelogic.flashlight.client.monitor.views.MonitorView");
            if (view != null) {
                view.getMediator().update(f_status);
                if (f_callback) {
                    EclipseJob.getInstance().schedule(
                            new WatchFlashlightMonitorJob(f_status), 1000);
                }
            }
            return Status.OK_STATUS;
        }
    }

    @SuppressWarnings("serial")
    private static class DoneException extends RuntimeException {
        // nothing to add
    }

    private static boolean isField(final String line) {
        return line != null
                && (line.charAt(0) == ' ' || line.charAt(0) == '\t');
    }

    private static String getField(final String clazz, final String line) {
        return clazz + '.' + line.substring(line.lastIndexOf(' ') + 1);
    }
}
