package com.surelogic.flashlight.client.views.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.views.monitor.MonitorStatus.ConnectionState;

public class MonitorThread extends Thread {

    private static final String DELIMITER = "**********";
    private static final Pattern LIST_PATTERN = Pattern.compile("^\\[|, |\\]$");
    private static final Pattern LOCK_EDGE_DELIM = Pattern.compile(" -> ");

    private static boolean f_go = false;
    private static MonitorThread f_instance;

    private static final CopyOnWriteArrayList<MonitorListener> f_listeners = new CopyOnWriteArrayList<MonitorListener>();

    private static final ConcurrentLinkedQueue<String> f_queue = new ConcurrentLinkedQueue<String>();

    private final MonitorStatus f_status;

    private MonitorThread(final MonitorStatus status) {
        super("Flashlight Monitor");
        f_status = status;
    }

    @Override
    public void run() {
        Socket s = null;
        try {
            boolean connected = false;
            while (!connected) {
                try {
                    f_status.setState(ConnectionState.SEARCHING);
                    notifyListeners();
                    File portFile = f_status.getPortFile();
                    int timeout = 0;
                    while (!portFile.exists() && timeout++ < 60) {
                        Thread.sleep(1000);
                    }
                    if (timeout >= 60) {
                        f_status.setState(ConnectionState.NOTFOUND);
                        notifyListeners();
                        synchronized (MonitorThread.class) {
                            f_go = false;
                        }
                        return;
                    }
                    BufferedReader portReader = new BufferedReader(
                            new FileReader(portFile));
                    int port = Integer.parseInt(portReader.readLine());
                    s = new Socket();
                    s.connect(new InetSocketAddress("localhost", port));
                    f_status.setState(ConnectionState.CONNECTED);
                    notifyListeners();
                    connected = true;
                } catch (final SocketException e) {
                    f_status.setState(ConnectionState.NOTFOUND);
                    notifyListeners();
                    try {
                        Thread.sleep(5000L);
                    } catch (final InterruptedException e1) {
                        // Do nothing
                    }
                }
            }

            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            final PrintWriter writer = new PrintWriter(s.getOutputStream());
            readUpTo(reader, DELIMITER);
            try {
                while (true) {
                    synchronized (MonitorThread.class) {
                        if (!f_go) {
                            f_instance = null;
                            throw new DoneException();
                        }
                    }
                    for (String val = f_queue.poll(); val != null; val = f_queue
                            .poll()) {
                        writeCommand(writer, val);
                        readUpTo(reader, DELIMITER);
                    }

                    writeCommand(writer, "shared");
                    List<String> sharedResult = readUpTo(reader, DELIMITER);
                    boolean shared = true;
                    String className = "";
                    for (String line : sharedResult) {
                        if (line.isEmpty()) {
                            // Do nothing
                        } else if (line.startsWith("Shared Fields")) {
                            shared = true;
                        } else if (line.startsWith("Unshared Fields")) {
                            shared = false;
                        } else if (isField(line)) {
                            Set<String> fieldSet = shared ? f_status
                                    .getShared() : f_status.getUnshared();
                            fieldSet.add(getField(className, line));
                        } else {
                            className = line;
                        }
                    }

                    writeCommand(writer, "locksets");
                    readUpTo(reader, "Potential Race Conditions");
                    List<String> locksetResults = readUpTo(reader,
                            "Actively protected fields");
                    String clazz = "";
                    for (String line : locksetResults) {
                        if (isField(line)) {
                            f_status.getRaces().add(getField(clazz, line));
                        } else {
                            clazz = line;
                        }
                    }
                    List<String> protectedResults = readUpTo(reader,
                            "Garbage Collected Fields");
                    f_status.getActivelyProtected().clear();
                    for (String line : protectedResults) {
                        if (isField(line)) {
                            f_status.getActivelyProtected().add(
                                    getField(clazz, line));
                        } else {
                            clazz = line;
                        }
                    }
                    readUpTo(reader, DELIMITER);

                    writeCommand(writer, "deadlocks");
                    readUpTo(reader, "Lock Orderings");
                    List<String> orderingResults = readUpTo(reader,
                            "Potential Deadlocks");
                    for (String line : orderingResults) {
                        String[] split = LOCK_EDGE_DELIM.split(line);
                        final List<String> edge = new ArrayList<String>(
                                split.length - 1);
                        for (int i = 1; i < split.length; i++) {
                            edge.add(split[i]);
                        }
                        f_status.getEdges().add(edge);
                    }

                    f_status.getDeadlocks().clear();
                    String deadlockLine = reader.readLine();
                    if (deadlockLine == null) {
                        throw new DoneException();
                    }
                    String[] split = LIST_PATTERN.split(deadlockLine);
                    for (String deadlock : split) {
                        if (!deadlock.isEmpty()) {
                            f_status.getDeadlocks().add(deadlock);
                        }
                    }
                    readUpTo(reader, DELIMITER);

                    writeCommand(writer, "alerts");
                    readUpTo(reader, "Swing Event Dispatch Thread");

                    f_status.getAlerts().clear();
                    List<String> swingAlerts = readUpTo(reader, "Shared Field");
                    for (String line : swingAlerts) {
                        if (isField(line)) {
                            f_status.getAlerts().add(getField(clazz, line));
                        } else {
                            clazz = line;
                        }
                    }
                    readUpTo(reader, DELIMITER);
                    writeCommand(writer, "props");
                    for (String propStr : readUpTo(reader, DELIMITER)) {
                        int eq = propStr.indexOf('=');
                        if (eq > 0) {
                            f_status.setProperty(propStr.substring(0, eq),
                                    propStr.substring(eq + 1));
                        }
                    }
                    writeCommand(writer, "list");
                    StringBuilder b = new StringBuilder();
                    for (String str : readUpTo(reader, DELIMITER)) {
                        b.append(str);
                        b.append('\n');
                    }
                    f_status.setListing(b.toString());
                    notifyListeners();
                    try {
                        Thread.sleep(5000L);
                    } catch (final InterruptedException e) {
                        // Do nothing
                    }

                }
            } catch (DoneException e) {
                f_status.setState(ConnectionState.TERMINATED);
                notifyListeners();
            }
            s.close();
        } catch (final Exception e) {
            SLLogger.getLoggerFor(MonitorThread.class).log(Level.WARNING,
                    e.getMessage(), e);
        }
    }

    private static boolean isField(final String line) {
        return line != null
                && (line.charAt(0) == ' ' || line.charAt(0) == '\t');
    }

    private static String getField(final String clazz, final String line) {
        return clazz + '.' + line.substring(line.lastIndexOf(' ') + 1);
    }

    public static boolean begin(final MonitorStatus status) {
        synchronized (MonitorThread.class) {
            if (f_go) {
                // Already started
                return false;
            }
            f_queue.clear();
            f_instance = new MonitorThread(status);
            f_instance.start();
            f_go = true;
            return true;
        }
    }

    private static class DoneException extends RuntimeException {
        //
    }

    private void writeCommand(final PrintWriter writer, final String command) {
        writer.println(command);
        writer.flush();
    }

    private List<String> readUpTo(final BufferedReader reader,
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

    private void notifyListeners() {
        for (MonitorListener l : f_listeners) {
            l.update(f_status);
        }
    }

    public static void sendCommand(final String command) {
        f_queue.add(command);
    }

    public static void addListener(final MonitorListener listener) {
        MonitorThread.f_listeners.add(listener);
    }

    public static void end() {
        synchronized (MonitorThread.class) {
            f_go = false;
        }
    }
}
