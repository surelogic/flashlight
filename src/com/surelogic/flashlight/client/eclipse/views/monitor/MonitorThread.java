package com.surelogic.flashlight.client.eclipse.views.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.logging.SLLogger;

public class MonitorThread extends Thread {
	private static boolean f_go = false;

	@Override
	public void run() {
		Socket s = null;
		while (f_go) {
			try {
				boolean connected = false;
				while (!connected) {
					try {
						update("Checking");
						s = new Socket();
						s.connect(new InetSocketAddress("localhost", 43524));
						update("Connecting");
						connected = true;
					} catch (final SocketException e) {
						update("Monitor not available");
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
				for (String line = reader.readLine(); line != null
						&& !line.startsWith(">"); line = reader.readLine()) {
					// Do nothing
				}

				while (f_go) {
					writer.println("list");
					writer.flush();
					final StringBuilder msg = new StringBuilder();
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					do {
						msg.append(line);
						msg.append("\n");
						line = reader.readLine();
					} while (line != null && !line.startsWith(">"));
					update(msg.toString());
					try {
						Thread.sleep(5000L);
					} catch (final InterruptedException e) {
						// Do nothing
					}
				}
				s.close();
			} catch (final Exception e) {
				SLLogger.getLoggerFor(MonitorThread.class).log(Level.INFO,
						e.getMessage(), e);
			}
		}
	}

	static void update(final String msg) {
		new UIJob("Update Monitor Thread") {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				MonitorViewMediator.setText(msg);
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	public static void begin() {
		if (f_go) {
			// Already started
			throw new IllegalStateException("Monitor thread already started");
		}
		f_go = true;
		new MonitorThread().start();
	}

	static void end() {
		f_go = false;
	}
}
