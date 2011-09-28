package com.surelogic.flashlight.client.monitor.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.monitor.views.MonitorStatus.ConnectionState;

public class MonitorThread extends Thread {

    private static final String DELIMITER = "**********";

    private static boolean f_go = false;
    private static MonitorThread f_instance;

    private static final CopyOnWriteArrayList<MonitorListener> f_listeners = new CopyOnWriteArrayList<MonitorListener>();

    private static final ConcurrentLinkedQueue<String> f_queue = new ConcurrentLinkedQueue<String>();

    private final MonitorStatus f_status;

    private MonitorThread(final MonitorStatus status) {
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
                    s = new Socket();
                    s.connect(new InetSocketAddress("localhost", 43524));
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
            while (true) {
                synchronized (MonitorThread.class) {
                    if (!f_go) {
                        f_instance = null;
                        return;
                    }
                }
                for (String val = f_queue.poll(); val != null; val = f_queue
                        .poll()) {
                    writer.println(val);
                    writer.flush();
                    readUpTo(reader, DELIMITER);
                }
                writer.println("shared");
                writer.flush();
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                boolean shared = true;
                String className = "";
                do {
                    if (line.isEmpty()) {
                        // Do nothing
                    } else if (line.startsWith("Shared Fields")) {
                        shared = true;
                    } else if (line.startsWith("Unshared Fields")) {
                        shared = false;
                    } else if (isField(line)) {
                        Set<String> fieldSet = shared ? f_status.getShared()
                                : f_status.getUnshared();
                        fieldSet.add(getField(className, line));
                    } else {
                        className = line;
                    }
                    line = reader.readLine();
                } while (line != null && !line.startsWith(DELIMITER));
                writer.println("locksets");
                writer.flush();
                line = reader.readLine();
                if (line.isEmpty()) {
                    // TODO Not sure where this is coming from
                    line = reader.readLine();
                }
                f_status.getRaces().clear();
                if (line.startsWith("Potential Race Conditions")) {
                    String clazz = "";
                    while (!line.startsWith("Actively protected fields")) {
                        line = reader.readLine();
                        if (isField(line)) {
                            f_status.getRaces().add(getField(clazz, line));
                        } else {
                            clazz = line;
                        }

                    }
                } else {
                    SLLogger.getLoggerFor(MonitorThread.class).warning(
                            "Could not read output from locksets command.");
                }
                f_status.getActivelyProtected().clear();
                if (line.startsWith("Actively protected fields")) {
                    String clazz = "";
                    while (!line.startsWith("Garbage Collected Fields")) {
                        line = reader.readLine();
                        if (isField(line)) {
                            f_status.getActivelyProtected().add(
                                    getField(clazz, line));
                        } else {
                            clazz = line;
                        }
                    }
                } else {
                    SLLogger.getLoggerFor(MonitorThread.class).warning(
                            "Could not read output from locksets command.");
                }
                readUpTo(reader, DELIMITER);
                writer.println("list");
                writer.flush();
                f_status.setListing(readUpTo(reader, DELIMITER));
                notifyListeners();
                try {
                    Thread.sleep(5000L);
                } catch (final InterruptedException e) {
                    // Do nothing
                }
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

    public static void begin(final MonitorStatus status) {
        synchronized (MonitorThread.class) {
            if (f_go) {
                // Already started
                throw new IllegalStateException(
                        "Monitor thread is already running.");
            }
            f_queue.clear();
            f_instance = new MonitorThread(status);
            f_instance.start();
            f_go = true;
        }
    }

    private String readUpTo(final BufferedReader reader, final String marker)
            throws IOException {
        final StringBuilder text = new StringBuilder();
        for (String line = reader.readLine(); line != null
                && !line.startsWith(marker); line = reader.readLine()) {
            text.append(line + '\n');
        }
        return text.toString();
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
