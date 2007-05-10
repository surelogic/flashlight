package com.surelogic._flashlight;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * This class defines the interface into the Flashlight data store.
 * 
 * @policyLock Console is java.lang.System:out
 */
public final class Store {

	/**
	 * This <i>must</i> be declared first within this class so that it can
	 * avoid instrumented library calls made by the static initialization of
	 * this class to recursively reenter the class during static initialization.
	 * <P>
	 * Normally this field would need to be <code>volatile</code>, however
	 * since the class loader holds a lock during class initialization the final
	 * value of <code>false</code> should be publicized safely to the other
	 * program thread.
	 */
	private static boolean f_flashlightIsNotInitialized = true;

	/**
	 * Flashlight can be turned off by defining the system property
	 * <code>FL_OFF</code> (as any value). For example, adding
	 * <code>-DFL_OFF</code> as and argument to the Java virtual machine will
	 * turn Flashlight off.
	 * <P>
	 * This field is also used to indicate that all collection has been
	 * terminated by being set to <code>true</code> by the {@link #shutdown()}
	 * method.
	 * <P>
	 * It is an invariant of this field that it is monotonic towards
	 * <code>true</code>.
	 */
	private static final AtomicBoolean FL_OFF = new AtomicBoolean(System
			.getProperty("FL_OFF") != null);

	/**
	 * Output encoding.
	 */
	static final String ENCODING = "UTF-8";

	/**
	 * Non-null if Flashlight should log to the console, <code>null</code>
	 * otherwise.
	 */
	private final static PrintWriter f_log;

	/**
	 * Logs a message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 */
	static void log(final String msg) {
		if (f_log != null)
			f_log.println("[Flashlight] " + msg);
	}

	/**
	 * Tracks the number off problems reported by the store.
	 */
	private static final AtomicLong f_problemCount;

	/**
	 * Logs a problem message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 */
	static void logAProblem(final String msg) {
		try {
			throw new IllegalStateException();
		} catch (IllegalStateException e) {
			logAProblem(msg, e);
		}
	}

	/**
	 * Logs a problem message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 * @param e
	 *            reported exception.
	 */
	static void logAProblem(final String msg, final Exception e) {
		f_problemCount.incrementAndGet();
		if (f_log != null) {
			/*
			 * It is an undocumented lock policy that PrintStream locks on
			 * itself. To make all of our output appear together on the console
			 * we follow this policy.
			 */
			synchronized (f_log) {
				f_log.println("[Flashlight] !PROBLEM! " + msg);
				e.printStackTrace(f_log);
			}
		}
	}

	/**
	 * Flush the log.
	 */
	static void logFlush() {
		if (f_log != null)
			f_log.flush();
	}

	/**
	 * Closes the log.
	 */
	static void logComplete() {
		if (f_log != null)
			f_log.close();
	}

	/**
	 * The string value of the <tt>FL_RUN</tt> property or
	 * <tt>"flashlight"</tt> if this property is not set.
	 */
	private static final String f_run;

	/**
	 * Gets the string value of the <tt>FL_RUN</tt> property or
	 * <tt>"flashlight"</tt> if this property is not set.
	 * 
	 * @return the string value of the <tt>FL_RUN</tt> property or
	 *         <tt>"flashlight"</tt> if this property is not set.
	 */
	static String getRun() {
		return f_run;
	}

	/**
	 * The value of {@link System#nanoTime()} when we start collecting data.
	 */
	private static final long f_start_nano;

	/**
	 * A queue to buffer raw event records from the instrumented program to the
	 * {@link Refinery}.
	 */
	private static final BlockingQueue<Event> f_rawQueue;

	/**
	 * A queue to buffer refined event records from the {@link Refinery} to the
	 * {@link Depository}.
	 */
	private static final BlockingQueue<Event> f_outQueue;

	/**
	 * The refinery thread.
	 */
	private static final Refinery f_refinery;

	/**
	 * The depository thread.
	 */
	private static final Depository f_depository;

	/**
	 * The console thread.
	 */
	private static final Console f_console;

	/**
	 * A periodic task that checks to see if Flashlight should shutdown by
	 * spying on the running program's threads.
	 */
	private static final Spy f_spy;

	/**
	 * This thread-local (tl) flag is used to ensure that we to not, within a
	 * thread, reenter the store. This situation can occur if we call methods on
	 * the objects passed into the store and the implementation of those methods
	 * is part of the instrumented program.
	 */
	private final static ThreadLocal<Boolean> tl_withinStore;

	/**
	 * This method must be called as the first statement by each flashlight
	 * thread to ensure that it <i>never</i> has data about it collected by the
	 * store. The flashlight threads don't call program code, however, they do
	 * use the Java standard libraries so we need to be cautious about any
	 * instrumentation on the Java standard library called from within these
	 * treads.
	 * <P>
	 * The talk, <i>Java Instrumentation for Dynamic Analysis</i>, by Jeff
	 * Perkins and Michael Ernst cautioned that "The instrumentation itself
	 * should not use instrumented classes, i.e., If a heap analysis used an
	 * instrumented version of HashMap it would recursively call itself."
	 * Flashlight allows this dangerous situation to exist by setting the
	 * thread-local flag so that all our instrumentation entry points to the
	 * store immediately return.
	 */
	final static void flashlightThread() {
		tl_withinStore.set(Boolean.TRUE);
	}

	/*
	 * Flashlight startup code used to get everything running.
	 */
	static {
		if (!FL_OFF.get()) {
			/*
			 * Initialize final static fields. If Flashlight is off these fields
			 * are all set to null to save memory.
			 */
			final StringBuilder fileName = new StringBuilder();
			fileName.append(System.getProperty("FL_DIR", System
					.getProperty("java.io.tmpdir")));
			fileName.append(System.getProperty("file.separator"));
			f_run = System.getProperty("FL_RUN", "flashlight");
			fileName.append(f_run);
			final SimpleDateFormat dateFormat = new SimpleDateFormat(
					"_yyyy.MM.dd_'at'_H.mm.ss");
			fileName.append(dateFormat.format(new Date()));

			File logFile = new File(fileName.toString() + ".log");
			PrintWriter w = null;
			try {
				OutputStream stream = new FileOutputStream(logFile);
				stream = new BufferedOutputStream(stream);
				w = new PrintWriter(stream);
			} catch (IOException e) {
				System.err.println("[Flashlight] unable to log to \""
						+ logFile.getAbsolutePath() + "\"");
				e.printStackTrace(System.err);
				System.exit(1); // bail
			}
			f_log = w;
			// still incremented even if logging is off.
			f_problemCount = new AtomicLong();

			File dataFile = new File(fileName.toString() + ".data.xml.gz");
			w = null;
			try {
				OutputStream stream = new FileOutputStream(dataFile);
				stream = new GZIPOutputStream(stream, 4096);
				OutputStreamWriter osw = new OutputStreamWriter(stream,
						ENCODING);
				w = new PrintWriter(osw);
			} catch (IOException e) {
				logAProblem("unable to output to \""
						+ dataFile.getAbsolutePath() + "\"", e);
				System.exit(1); // bail
			}
			final EventVisitor outputStrategy = new OutputStrategyXML(w);

			final int rawQueueSize = getIntProperty("FL_RAWQ_SIZE", 500, 10);
			f_rawQueue = new ArrayBlockingQueue<Event>(rawQueueSize);
			putInQueue(f_rawQueue, new Time());
			final int outQueueSize = getIntProperty("FL_OUTQ_SIZE", 500, 10);
			f_outQueue = new ArrayBlockingQueue<Event>(outQueueSize);
			tl_withinStore = new ThreadLocal<Boolean>() {
				@Override
				protected Boolean initialValue() {
					return Boolean.FALSE;
				}
			};
			IdPhantomReference
					.addObserver(new IdPhantomReferenceCreationObserver() {
						public void notify(IdPhantomReference o) {
							/*
							 * Create an event to define this object.
							 */
							Store.putInQueue(f_rawQueue,
									new ObjectDefinition(o));
						}
					});
			final int refinerySize = getIntProperty("FL_REFINERY_SIZE", 5000,
					100);
			f_refinery = new Refinery(f_rawQueue, f_outQueue, refinerySize);
			f_refinery.start();
			f_depository = new Depository(f_outQueue, outputStrategy);
			f_depository.start();
			log("collection started (rawQ=" + rawQueueSize + " : refinery="
					+ refinerySize + " : outQ=" + outQueueSize + ")");
			log("           to \"" + dataFile.getAbsolutePath() + "\"");
			/*
			 * The spy periodically checks the state of the instrumented program
			 * and shuts down flashlight if the program is finished.
			 */
			final boolean noSpy = System.getProperty("FL_NO_SPY") != null;
			if (noSpy) {
				f_spy = null;
			} else {
				f_spy = new Spy();
				f_spy.start();
			}
			/*
			 * The shutdown hook is a last ditch effort to shutdown flashlight
			 * cleanly when an abrupt termination occurs (e.g., System.exit is
			 * invoked).
			 */
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					Store.shutdown();
				}
			});
			/*
			 * The console lets someone attach to flashlight and command it to
			 * shutdown.
			 */
			f_console = new Console();
			f_console.start();

			f_start_nano = System.nanoTime();
		} else {
			f_run = null;
			f_log = null;
			f_problemCount = null;
			f_rawQueue = null;
			f_outQueue = null;
			tl_withinStore = null;
			f_refinery = null;
			f_depository = null;
			f_console = null;
			f_spy = null;
			f_start_nano = 0;
		}
		f_flashlightIsNotInitialized = false;
	}

	static int getIntProperty(final String key, int def, final int min) {
		try {
			String intString = System.getProperty(key);
			if (intString != null)
				def = Integer.parseInt(intString);
		} catch (NumberFormatException e) {
			// ignore, go with the default
		}
		// ensure the result isn't less than the minimum
		return (def >= min ? def : min);
	}

	/**
	 * Records a read of a field within the instrumented program.
	 * 
	 * @param receiver
	 *            the object instance the field is part of the state of, or
	 *            <code>null</code> if the field is <code>static</code>.
	 * @param field
	 *            a field within the instrumented program.
	 * @param location
	 *            the source location of where the field read occurred, may be
	 *            <code>null</code>.
	 */
	public static void fieldRead(final Object receiver, final Field field,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (field == null) {
				logAProblem("field cannot be null...bug");
				return;
			}
			final ObservedField oField = ObservedField.getInstance(field,
					f_rawQueue);
			final Event e;
			if (oField.isStatic()) {
				e = new FieldReadStatic(oField, location);
			} else {
				if (receiver == null) {
					logAProblem("instance field " + oField
							+ " read reported with a null receiver...bug");
					return;
				}
				e = new FieldReadInstance(receiver, oField, location);
			}
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Records a write of a field within the instrumented program.
	 * 
	 * @param receiver
	 *            the object instance the field is part of the state of, or
	 *            <code>null</code> if the field is <code>static</code>.
	 * @param field
	 *            a field within the instrumented program.
	 * @param location
	 *            the source location of where the field read occurred, may be
	 *            <code>null</code>.
	 */
	public static void fieldWrite(final Object receiver, final Field field,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (field == null) {
				logAProblem("field cannot be null...bug");
				return;
			}
			final ObservedField oField = ObservedField.getInstance(field,
					f_rawQueue);
			final Event e;
			if (oField.isStatic()) {
				e = new FieldWriteStatic(oField, location);
			} else {
				if (receiver == null) {
					logAProblem("instance field " + oField
							+ " write reported with a null receiver...bug");
					return;
				}
				e = new FieldWriteInstance(receiver, oField, location);
			}
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Records that the program is attempting to acquire an intrinsic lock. An
	 * intrinsic lock is a <code>synchronized</code> block or method.
	 * 
	 * @param lockObject
	 *            the object being synchronized (i.e., the lock).
	 * @param lockIsThis
	 *            <code>true</code> if the lock object is dynamically the same
	 *            as the receiver object, i.e., <code>this == on</code>.
	 * @param lockIsClass
	 *            <code>true</code> if the lock object is dynamically the same
	 *            as the class the method is declared within, <code>false</code>
	 *            otherwise.
	 * @param location
	 *            the source location of where the event occurred, may be
	 *            <code>null</code>.
	 */
	public static void beforeIntrinsicLockAcquisition(final Object lockObject,
			final boolean lockIsThis, final boolean lockIsClass,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				logAProblem("intrinsic lock object cannot be null...bug");
				return;
			}
			final Event e = new BeforeIntrinsicLockAcquisition(lockObject,
					lockIsThis, lockIsClass, location);
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Records that the program has acquired an intrinsic lock. An intrinsic
	 * lock is a <code>synchronized</code> block or method.
	 * 
	 * @param lockObject
	 *            the object being synchronized (i.e., the lock).
	 * @param location
	 *            the source location of where the event occurred, may be
	 *            <code>null</code>.
	 */
	public static void afterIntrinsicLockAcquisition(final Object lockObject,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				logAProblem("intrinsic lock object cannot be null...bug");
				return;
			}
			final Event e = new AfterIntrinsicLockAcquisition(lockObject,
					location);
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Records that the program is entering a call to one of the following
	 * methods:
	 * <ul>
	 * <li>{@link Object#wait()}</li>
	 * <li>{@link Object#wait(long)}</li>
	 * <li>{@link Object#wait(long, int)}</li>
	 * </ul>
	 * See the Java Language Specification (3rd edition) section 17.8 <i>Wait
	 * Sets and Notification</i> for the semantics of waiting on an intrinsic
	 * lock. An intrinsic lock is a <code>synchronized</code> block or method.
	 * 
	 * @param lockObject
	 *            the object being waited on (i.e., the thread should be holding
	 *            a lock on this object).
	 * @param location
	 *            the source location of where the event occurred, may be
	 *            <code>null</code>.
	 */
	public static void beforeIntrinsicLockWait(final Object lockObject,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				logAProblem("intrinsic lock object cannot be null...bug");
				return;
			}
			final Event e = new BeforeIntrinsicLockWait(lockObject, location);
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Records that the program is completing a call to one of the following
	 * methods:
	 * <ul>
	 * <li>{@link Object#wait()}</li>
	 * <li>{@link Object#wait(long)}</li>
	 * <li>{@link Object#wait(long, int)}</li>
	 * </ul>
	 * See the Java Language Specification (3rd edition) section 17.8 <i>Wait
	 * Sets and Notification</i> for the semantics of waiting on an intrinsic
	 * lock. An intrinsic lock is a <code>synchronized</code> block or method.
	 * 
	 * @param lockObject
	 *            the object being waited on (i.e., the thread should be holding
	 *            a lock on this object).
	 * @param location
	 *            the source location of where the event occurred, may be
	 *            <code>null</code>.
	 */
	public static void afterIntrinsicLockWait(final Object lockObject,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				logAProblem("intrinsic lock object cannot be null...bug");
				return;
			}
			final Event e = new AfterIntrinsicLockWait(lockObject, location);
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Records that the program has released an intrinsic lock. An intrinsic
	 * lock is a <code>synchronized</code> block or method.
	 * 
	 * @param lockObject
	 *            the object being synchronized (i.e., the lock).
	 * @param location
	 *            the source location of where the event occurred, may be
	 *            <code>null</code>.
	 */
	public static void afterIntrinsicLockRelease(final Object lockObject,
			final SrcLoc location) {
		if (f_flashlightIsNotInitialized)
			return;
		if (FL_OFF.get())
			return;
		if (tl_withinStore.get().booleanValue())
			return;
		tl_withinStore.set(Boolean.TRUE);
		try {
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				logAProblem("intrinsic lock object cannot be null...bug");
				return;
			}
			final Event e = new AfterIntrinsicLockRelease(lockObject, location);
			putInQueue(f_rawQueue, e);
		} finally {
			tl_withinStore.set(Boolean.FALSE);
		}
	}

	/**
	 * Stops collection of events about the instrumented program. This method
	 * may be called from within the following thread contexts:
	 * <ul>
	 * <li>A direct call from a program thread, i.e., a call was added to the
	 * program code </li>
	 * <li>The {@link Spy} thread if it detected the instrumented program
	 * completed and only flashlight threads remain running.</li>
	 * <li>A client handler thread created by the {@link Console} thread that
	 * was told to shutdown flashlight via socket.</li>
	 * <li>The thread created to run our shutdown hook.</li>
	 * </ul>
	 */
	public static void shutdown() {
		if (f_flashlightIsNotInitialized) {
			System.err.println("[Flashlight] !SERIOUS ERROR! "
					+ "Store.shutdown() invoked "
					+ "before the Store class is initialized");
			return;
		}
		/*
		 * The below getAndSet(true) ensures that only one thread shuts down
		 * Flashlight.
		 */
		if (FL_OFF.getAndSet(true))
			return;
		Thread.yield();

		/*
		 * Finish up data output.
		 */
		putInQueue(f_rawQueue, FinalEvent.FINAL_EVENT);
		join(f_refinery);
		join(f_depository);

		/*
		 * Note that a client handler for the console could have been the thread
		 * that called this method (i.e., we are running within a client handler
		 * thread of the console...not the listener thread).
		 */
		f_console.requestShutdown();

		/*
		 * Note that the spy thread could have been the thread that called this
		 * method.
		 */
		if (f_spy != null)
			f_spy.requestShutdown();

		final long problemCount = f_problemCount.get();
		final String duration = " (duration of collection was "
				+ (System.nanoTime() - f_start_nano) + " nanoseconds)";
		if (problemCount < 1)
			log("collection shutdown" + duration);
		else
			log("collection shutdown with " + problemCount
					+ " problem(s) reported" + duration);
		logComplete();
	}

	/**
	 * Only used for testing, this method sets the output strategy of the
	 * depository thread.
	 * 
	 * @param outputStrategy
	 *            an output strategy.
	 */
	static void setOutputStrategy(EventVisitor outputStrategy) {
		assert outputStrategy != null;
		f_depository.setOutputStrategy(outputStrategy);
	}

	/**
	 * Puts an event into a blocking queue. This operation will block if the
	 * queue is full and it will ignore any interruptions.
	 * 
	 * @param queue
	 *            the blocking queue to put the event into.
	 * @param e
	 *            the event to put into the raw queue.
	 */
	static void putInQueue(final BlockingQueue<Event> queue, final Event e) {
		boolean done = false;
		while (!done) {
			try {
				queue.put(e);
				done = true;
			} catch (InterruptedException e1) {
				/*
				 * We are within a program thread, so another program thread
				 * interrupted us. I think it is OK to ignore this, however, we
				 * do need to ensure the event gets put into the raw queue.
				 */
				logAProblem("queue.put(e) was interrupted", e1);
			}
		}
	}

	/**
	 * Joins on the given thread ignoring any interruptions.
	 * 
	 * @param t
	 *            the thread to join on.
	 */
	private static void join(final Thread t) {
		boolean done = false;
		while (!done) {
			try {
				t.join();
				done = true;
			} catch (InterruptedException e1) {
				// ignore, we expect to be interrupted
			}
		}
	}

	private Store() {
		// no instances
	}
}
