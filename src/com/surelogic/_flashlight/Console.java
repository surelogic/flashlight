package com.surelogic._flashlight;

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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Console extends Thread {

	Console() {
		super("flashlight-console");
	}

	private volatile boolean f_shutdownRequested = false;

	private final int f_port = Store.getIntProperty("FL_CONSOLE_PORT", 43524,
			1024);

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
			Store.logAProblem("unable to listen on any port between " + f_port
					+ " and " + port
					+ " (i.e., Flashlight cannot be shutdown via a console)");
			return;
		}
		Store.log("console server listening on port " + port);

		// until told to shutdown, listen for and handle client connections
		while (!f_shutdownRequested) {
			try {
				final Socket client = f_socket.accept(); // wait for a client
				InetAddress address = client.getInetAddress();
				final ClientHandler handler = new ClientHandler(client);
				Store
						.log("console connect from "
								+ (address == null ? "UNKNOWN" : address
										.getCanonicalHostName()) + " ("
								+ handler + ")");
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
				Store.logAProblem("failure listening for client connections "
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
			f_socket.close();
		} catch (IOException e) {
			Store.logAProblem(
					"unable to close the socket used by " + getName(), e);
		}
	}

	/**
	 * A thread to handle request from console connections. More than one client
	 * can be connected so there could be several instance of this class.
	 */
	static private class ClientHandler extends Thread {

		static AtomicInteger f_instanceCount = new AtomicInteger();

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
				sendResponse(outputStream, "Welcome to Flashlight! \""
						+ Store.getRun() + "\"");
				sendResponse(outputStream,
						"(type \"stop\" to shutdown collection)");
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
						if (nextLine.equalsIgnoreCase("stop")) {
							sendResponse(outputStream,
									"Flashlight is shutting down...");
							Store.shutdown();
						} else if (nextLine.equalsIgnoreCase("exit")
								|| nextLine.equalsIgnoreCase("quit")) {
							f_shutdownRequested = true;
							f_client.close();
						} else {
							sendResponse(outputStream,
									"invalid command...please use \"stop\" when you want to halt collection");
						}
					}
				}
				Store.log("console disconnect (" + this + ")");
			} catch (SocketException e) {
				/*
				 * ignore, this is normal behavior during a shutdown, i.e.,
				 * another thread has called f_client.close() via our
				 * requestShutdown() method.
				 */
			} catch (IOException e) {
				Store.logAProblem("general I/O failure on socket used by "
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
		private void sendResponse(BufferedWriter outputStream, String response) {
			try {
				outputStream.write(response + "\n\r");
				outputStream.flush();
			} catch (IOException e) {
				Store.logAProblem(
						"general I/O failure writing to socket used by "
								+ getName(), e);
			}
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
			} catch (IOException e) {
				Store.logAProblem("unable to close the socket used by "
						+ getName(), e);
			}
		}
	}
}
