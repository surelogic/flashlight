package com.surelogic._flashlight.monitor;

import java.util.Map;

import com.surelogic._flashlight.FLStore;

/**
 * A thread that periodically spies on the threads within the instrumented
 * program and shuts down flashlight if the program has completed.
 * <p>
 * Note that the spy implementation is dependent upon
 * <ul>
 * <li>all the flashlight thread names being prefixed with <tt>flashlight-</tt>,
 * and</li>
 * <li>the VM cleanup thread being named <tt>DestroyJavaVM</tt>.
 * </ul>
 * If one or both of these are not true than the spy will not work properly. Our
 * implementation controls the first (i.e., if it is not true we have a bug),
 * but the VM controls the second.
 */
public final class MonitorSpy extends Thread {

	MonitorSpy() {
		super("flashlight-spy");
	}

	private volatile boolean f_shutdownRequested = false;

	/**
	 * Spy every five seconds.
	 */
	private static final int PERIODIC = 5000;

	@Override
	public void run() {
		FLStore.flashlightThread();

		while (true) {
			try {
				Thread.sleep(PERIODIC);
			} catch (final InterruptedException e) {
				// ignore, likely during a shutdown that this thread doesn't
				// initiate.
			}
			if (f_shutdownRequested) {
				break;
			}
			final Map<Thread, StackTraceElement[]> threadToStackTrace = Thread
					.getAllStackTraces();
			boolean timeToShutdown = true; // assumption
			for (final Thread t : threadToStackTrace.keySet()) {
				if (!t.isDaemon()) {
					final boolean current = t == Thread.currentThread();

					final boolean flashlightThread = t.getName().startsWith(
							"flashlight-");

					/*
					 * The "DestroyJavaVM" thread seems to hang out waiting
					 * until the program is done. This check is flaky...and
					 * probably not all that reliable, portable, etc.
					 */
					final boolean destroyJavaVM = t.getName().equalsIgnoreCase(
							"DestroyJavaVM");

					if (!current && !flashlightThread && !destroyJavaVM) {
						timeToShutdown = false;
					}
				}
			}
			if (timeToShutdown) {
				/*
				 * StringBuilder sb = new StringBuilder();
				 * sb.append("Shutting down with these threads:\n"); for (Thread
				 * t : threadToStackTrace.keySet()) { ThreadGroup group =
				 * t.getThreadGroup(); String groupName = group == null ? "n/a"
				 * : group.getName();
				 * sb.append('\t').append(t.getName()).append(
				 * ' ').append(groupName).append('\n'); }
				 * System.out.println(sb); System.out.flush();
				 */
				MonitorStore.shutdown();
			} else {
				MonitorStore.logFlush();
			}
		}
	}

	/**
	 * Signals that this spy thread should be shutdown. This method returns
	 * immediately.
	 */
	void requestShutdown() {
		f_shutdownRequested = true;
		this.interrupt(); // wake up
	}
}
