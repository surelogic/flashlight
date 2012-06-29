package com.surelogic._flashlight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.surelogic._flashlight.common.InstrumentationConstants;

class Console extends Thread {

    private static final String QUIT = "quit";
    private static final String EXIT = "exit";

    private static final String DELIMIT = "************************************************************";

    private final RunConf f_conf;

    private final List<ConsoleCommand> f_command;

    Console(final RunConf conf, final List<ConsoleCommand> commands) {
        super("flashlight-console");
        f_conf = conf;
        f_command = commands;
    }

    private volatile boolean f_shutdownRequested = false;

    private final int f_port = StoreConfiguration.getConsolePort();

    /**
     * The socket this game server is listening on.
     */
    private ServerSocket f_socket;

    /**
     * Maintains a list of the client handlers. {@link WeakReference}s are used
     * to allow dead threads to be garbage collected. Note that we hang onto all
     * the weak references until the program ends (this could probably be
     * improved with a reference queue...but we don't expects a lot of
     * connections).
     */
    private final List<WeakReference<ClientHandler>> f_handlers = new ArrayList<WeakReference<ClientHandler>>();

    @Override
    public void run() {
        Store.flashlightThread();

        // start listening on a port
        boolean listening = false;
        int tryCount = 0;
        int port = f_port;
        do {
            try {
                port = f_port + tryCount;
                f_socket = new ServerSocket(port);
                listening = true;
            } catch (IOException e) {
                tryCount++;
            }
        } while (!listening && tryCount <= 100);
        if (!listening) {
            f_conf.logAProblem("unable to listen   on any port between "
                    + f_port + " and " + port
                    + " (i.e., Flashlight cannot be shutdown via a console)");
            return;
        }
        f_conf.log("console server listening on port " + port);
        if (StoreConfiguration.getDirectory() != null) {
            // We write out the port file we are using to our local run folder,
            // if it exists.
            File portFile = new File(StoreConfiguration.getDirectory(),
                    InstrumentationConstants.FL_PORT_FILE_NAME);
            try {
                PrintWriter writer = new PrintWriter(portFile);
                try {
                    writer.println(port);
                } finally {
                    writer.close();
                }
            } catch (FileNotFoundException e) {
                f_conf.log("Could not write to instrumentation's port file.");
            }
        }
        // until told to shutdown, listen for and handle client connections
        while (!f_shutdownRequested) {
            try {
                final Socket client = f_socket.accept(); // wait for a client
                InetAddress address = client.getInetAddress();
                final ClientHandler handler = new ClientHandler(client);
                f_conf.log("console connect from "
                        + (address == null ? "UNKNOWN" : address
                                .getCanonicalHostName()) + " ("
                        + handler.getName() + ")");
                final WeakReference<ClientHandler> p_handler = new WeakReference<ClientHandler>(
                        handler);
                f_handlers.add(p_handler);
                handler.start();
            } catch (SocketException e) {
                /*
                 * ignore, this is normal behavior during a shutdown, i.e.,
                 * another thread has called gameSocket.close() via our
                 * requestShutdown() method.
                 */
            } catch (IOException e) {
                f_conf.logAProblem("failure listening for client connections "
                        + port, e);
            }
        }
        /*
         * Shutdown all the client handler threads. By doing this here, and not
         * within the shutdown() method, we can keep the f_handlers list
         * thread-local.
         */
        for (WeakReference<ClientHandler> p_handler : f_handlers) {
            final ClientHandler handler = p_handler.get();
            if (handler != null) {
                handler.requestShutdown();
            }
        }
    }

    /**
     * Signals that this console and all open client handler threads should be
     * shutdown. This method returns immediately.
     */
    void requestShutdown() {
        f_shutdownRequested = true;
        try {
            if (f_socket != null) {
                f_socket.close();
            }
        } catch (IOException e) {
            f_conf.logAProblem("unable to close the socket used by "
                    + getName(), e);
        }
    }

    static AtomicInteger f_instanceCount = new AtomicInteger();

    /**
     * A thread to handle request from console connections. More than one client
     * can be connected so there could be several instance of this class.
     */
    private class ClientHandler extends Thread {

        private volatile boolean f_shutdownRequested = false;

        final Socket f_client;

        ClientHandler(final Socket client) {
            super("flashlight-console-client-handler "
                    + f_instanceCount.incrementAndGet());
            assert client != null;
            f_client = client;
        }

        @Override
        public void run() {
            Store.flashlightThread();

            try {
                // create input and output connections
                BufferedReader inputStream = new BufferedReader(
                        new InputStreamReader(f_client.getInputStream()));
                BufferedWriter outputStream = new BufferedWriter(
                        new OutputStreamWriter(f_client.getOutputStream()));
                sendResponse(outputStream,
                        "Welcome to Flashlight! \"" + f_conf.getRun() + "\"");
                sendResponse(outputStream,
                        "Type quit or exit to end this session.");
                sendResponse(outputStream, "Available commands: ");
                for (ConsoleCommand c : f_command) {
                    sendResponse(outputStream, c.getDescription());
                }
                sendResponse(outputStream, DELIMIT);
                while (!f_shutdownRequested) {
                    String nextLine = inputStream.readLine(); // blocks
                    if (nextLine == null) {
                        /*
                         * This condition appears to occur when the client
                         * abruptly terminates its connection to the server (I
                         * found no documentation to support this, however). To
                         * make the server behave normally when an abrupt
                         * termination occurs, we need to "pretend" we received
                         * a quit command.
                         */
                        f_shutdownRequested = true;
                    } else {
                        // process the command
                        nextLine = nextLine.trim();
                        if (nextLine.equalsIgnoreCase(EXIT)
                                || nextLine.equalsIgnoreCase(QUIT)) {
                            f_shutdownRequested = true;
                            f_client.close();
                        } else {
                            boolean matched = false;
                            Iterator<ConsoleCommand> iter = f_command
                                    .iterator();
                            while (!matched && iter.hasNext()) {
                                String response = iter.next().handle(nextLine);
                                if (response != null) {
                                    matched = true;
                                    sendResponse(outputStream, response);
                                }
                            }
                            if (!matched) {
                                sendResponse(outputStream, "invalid command...");
                                sendResponse(outputStream,
                                        "Available commands: ");
                                for (ConsoleCommand c : f_command) {
                                    sendResponse(outputStream,
                                            c.getDescription());
                                }
                            }
                            sendResponse(outputStream, DELIMIT);
                        }
                    }
                }
                f_conf.log("console disconnect (" + getName() + ")");
            } catch (SocketException e) {
                /*
                 * ignore, this is normal behavior during a shutdown, i.e.,
                 * another thread has called f_client.close() via our
                 * requestShutdown() method.
                 */
            } catch (IOException e) {
                f_conf.logAProblem("general I/O failure on socket used by "
                        + getName(), e);
            }
        }

        /**
         * Sends the provided response String followed by a newline to the given
         * output steam. Then {@link BufferedWriter#flush()} is called on the
         * output steam.
         * 
         * @param outputStream
         *            the stream to output to.
         * @param response
         *            the data to write to the stream.
         */
        private void sendResponse(final BufferedWriter outputStream,
                final String response) throws IOException {
                outputStream.write(response + "\n\r");
                outputStream.flush();
        }

        /**
         * Signals that this client handler should be shutdown. This method
         * returns immediately.
         */
        void requestShutdown() {
            f_shutdownRequested = true;
            try {
                interrupt(); // wake up
                f_client.close();
            } catch (IOException e) {
                f_conf.logAProblem("unable to close the socket used by "
                        + getName(), e);
            }
        }
    }

}
