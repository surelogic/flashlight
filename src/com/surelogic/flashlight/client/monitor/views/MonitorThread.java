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
                    } else if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                        Set<String> fieldSet = shared ? f_status.getShared()
                                : f_status.getUnshared();
                        fieldSet.add(className + "."
                                + line.substring(line.lastIndexOf(' ') + 1));
                    } else {
                        className = line;
                    }
                    line = reader.readLine();
                } while (line != null && !line.startsWith(DELIMITER));
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
