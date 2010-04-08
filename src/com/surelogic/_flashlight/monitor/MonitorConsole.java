package com.surelogic._flashlight.monitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.surelogic._flashlight.StoreConfiguration;

class MonitorConsole extends Thread {
	private static final String STOP = "stop";
	private static final String EXIT = "exit";
	private static final String QUIT = "quit";
	private static final String LIST = "list";
	private static final String PING = "ping";
	private static final String ALERTS = "alerts";
	private static final String ALL_ALERTS = "allAlerts";
	private static final String DEADLOCKS = "deadlocks";
	private static final Pattern SET = Pattern.compile("set ([^=]*)=(.*)");
	private static final String FIELD_SPEC = "fieldSpec";
	private static final String EDT_FIELDS = "swingFieldAlerts";
	private static final String SHARED_FIELDS = "sharedFieldAlerts";
	private static final String LOCKSET_FIELDS = "lockSetAlerts";
	private long startTime;

	public MonitorConsole() {
		super("flashlight-console");
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
		MonitorStore.flashlightThread();
		startTime = System.currentTimeMillis();
		// start listening on a port
		boolean listening = false;
		int tryCount = 0;
		int port = f_port;
		do {
			try {
				port = f_port + tryCount;
				f_socket = new ServerSocket(port);
				listening = true;
			} catch (final IOException e) {
				tryCount++;
			}
		} while (!listening && tryCount <= 100);
		if (!listening) {
			MonitorStore.logAProblem("unable to listen on any port between "
					+ f_port + " and " + port
					+ " (i.e., Flashlight cannot be shutdown via a console)");
			return;
		}
		MonitorStore.log("console server listening on port " + port);

		// until told to shutdown, listen for and handle client connections
		while (!f_shutdownRequested) {
			try {
				final Socket client = f_socket.accept(); // wait for a client
				final InetAddress address = client.getInetAddress();
				final ClientHandler handler = new ClientHandler(client);
				MonitorStore.log("console connect from "
						+ (address == null ? "UNKNOWN" : address
								.getCanonicalHostName()) + " ("
						+ handler.getName() + ")");
				final WeakReference<ClientHandler> p_handler = new WeakReference<ClientHandler>(
						handler);
				f_handlers.add(p_handler);
				handler.start();
			} catch (final SocketException e) {
				/*
				 * ignore, this is normal behavior during a shutdown, i.e.,
				 * another thread has called gameSocket.close() via our
				 * requestShutdown() method.
				 */
			} catch (final IOException e) {
				MonitorStore.logAProblem(
						"failure listening for client connections " + port, e);
			}
		}
		/*
		 * Shutdown all the client handler threads. By doing this here, and not
		 * within the shutdown() method, we can keep the f_handlers list
		 * thread-local.
		 */
		for (final WeakReference<ClientHandler> p_handler : f_handlers) {
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
		} catch (final IOException e) {
			MonitorStore.logAProblem("unable to close the socket used by "
					+ getName(), e);
		}
	}

	/**
	 * Counts the number of client handlers created.
	 */
	static AtomicInteger f_instanceCount = new AtomicInteger();

	/**
	 * A thread to handle request from console connections. More than one client
	 * can be connected so there could be several instance of this class.
	 */
	class ClientHandler extends Thread {

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
			MonitorStore.flashlightThread();

			try {
				// create input and output connections
				final BufferedReader inputStream = new BufferedReader(
						new InputStreamReader(f_client.getInputStream()));
				final BufferedWriter outputStream = new BufferedWriter(
						new OutputStreamWriter(f_client.getOutputStream()));
				sendResponse(outputStream, "Welcome to Flashlight! \""
						+ MonitorStore.getRun() + "\"");
				sendResponse(outputStream, "(type \"" + STOP
						+ "\" to shutdown collection)");

				while (!f_shutdownRequested) {
					String nextLine = prompt(inputStream, outputStream); // blocks
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
						if (nextLine.equalsIgnoreCase(STOP)) {
							sendResponse(outputStream,
									"Flashlight is shutting down...");
							MonitorStore.shutdown();
						} else if (nextLine.equalsIgnoreCase(EXIT)
								|| nextLine.equalsIgnoreCase(QUIT)) {
							f_shutdownRequested = true;
							f_client.close();
						} else if (nextLine.equalsIgnoreCase(PING)) {
							sendResponse(outputStream, String.format(
									"Uptime: %d", System.currentTimeMillis()
											- startTime));
						} else if (nextLine.equalsIgnoreCase(LIST)) {
							sendResponse(outputStream, Analysis.getAnalysis()
									.toString());
						} else if (nextLine.equalsIgnoreCase(ALL_ALERTS)) {
							final AlertInfo alerts = Analysis.getAnalysis()
									.getAlerts();
							sendResponse(outputStream, alerts.toString());
						} else if (nextLine.equalsIgnoreCase(ALERTS)) {
							AlertInfo alerts = Analysis.getAnalysis()
									.getAlerts();
							sendResponse(outputStream, new Date() + "\n\r"
									+ alerts.toString());
							for (;;) {
								try {
									Thread.sleep(1000L);
								} catch (final InterruptedException e) {
									// Do nothing
								}
								final AlertInfo old = alerts;
								alerts = Analysis.getAnalysis().getAlerts();
								final AlertInfo fresh = alerts.alertsSince(old);
								if (!fresh.isEmpty()) {
									sendResponse(outputStream, new Date()
											+ "\n\r" + alerts.toString());
								}
							}
						} else if (nextLine.equalsIgnoreCase(DEADLOCKS)) {
							sendResponse(outputStream, Analysis.getAnalysis()
									.getDeadlocks().toString());
						} else {
							final Matcher m = SET.matcher(nextLine);
							if (m.matches()) {
								final String prop = m.group(1);
								final String val = m.group(2);
								if (FIELD_SPEC.equalsIgnoreCase(prop)) {
									sendResponse(outputStream, String.format(
											"Changing fieldSpec to be %s", val));
									Analysis
											.reviseSpec(new MonitorSpec(
													val,
													MonitorStore
															.getFieldDefinitions()));
								} else if (EDT_FIELDS.equalsIgnoreCase(prop)) {
									sendResponse(
											outputStream,
											String
													.format(
															"Monitoring fields matching %s for Swing policy violations.",
															val));
									Analysis.reviseAlerts(new AlertSpec(val,
											null, null, MonitorStore
													.getFieldDefinitions()));

								} else if (SHARED_FIELDS.equalsIgnoreCase(prop)) {
									sendResponse(
											outputStream,
											String
													.format(
															"Ensuring fields matching %s are not shared.",
															val));
									Analysis.reviseAlerts(new AlertSpec(null,
											val, null, MonitorStore
													.getFieldDefinitions()));
								} else if (LOCKSET_FIELDS
										.equalsIgnoreCase(prop)) {
									sendResponse(
											outputStream,
											String
													.format(
															"Ensuring fields matching %s always have a lock set.",
															val));
									Analysis.reviseAlerts(new AlertSpec(null,
											null, val, MonitorStore
													.getFieldDefinitions()));
								}
							} else {
								sendResponse(
										outputStream,
										"invalid command...please use \""
												+ STOP
												+ "\" when you want to halt collection");
							}
						}
					}
				}
				MonitorStore.log("console disconnect (" + getName() + ")");
			} catch (final SocketException e) {
				/*
				 * ignore, this is normal behavior during a shutdown, i.e.,
				 * another thread has called f_client.close() via our
				 * requestShutdown() method.
				 */

			} catch (final IOException e) {
				MonitorStore
						.logAProblem("general I/O failure on socket used by "
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
				final String response) {
			try {
				outputStream.write(response + "\n\r");
				outputStream.flush();
			} catch (final IOException e) {
				MonitorStore.logAProblem(
						"general I/O failure writing to socket used by "
								+ getName(), e);
			}
		}

		/**
		 * Display a prompt to the user
		 * 
		 * @param inputStream
		 * 
		 * @param outputStream
		 */
		private String prompt(final BufferedReader inputStream,
				final BufferedWriter outputStream) {
			try {
				outputStream.write(">\r\n");
				outputStream.flush();
				return inputStream.readLine(); // blocks
			} catch (final IOException e) {
				MonitorStore.logAProblem(
						"general I/O failure writing to socket used by "
								+ getName(), e);
			}
			return null;
		}

		/**
		 * Signals that this client handler should be shutdown. This method
		 * returns immediately.
		 */
		void requestShutdown() {
			f_shutdownRequested = true;
			try {
				this.interrupt(); // wake up
				f_client.close();
			} catch (final IOException e) {
				MonitorStore.logAProblem("unable to close the socket used by "
						+ getName(), e);
			}
		}
	}
}
