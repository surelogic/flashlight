package com.surelogic._flashlight;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

/**
 * This class defines the interface into the Flashlight data store.
 * 
 * @policyLock Console is java.lang.System:out
 */
public final class Store {

	/**
	 * This <i>must</i> be declared first within this class so that it can avoid
	 * instrumented library calls made by the static initialization of this
	 * class to recursively reenter the class during static initialization.
	 * <P>
	 * Normally this field would need to be <code>volatile</code>, however since
	 * the class loader holds a lock during class initialization the final value
	 * of <code>false</code> should be publicized safely to the other program
	 * thread.
	 */
	// (Re-)using StoreDelegate.FL_OFF for normal checks
	private static boolean f_flashlightIsNotInitialized = true;

	/**
	 * Output encoding.
	 */
	static final String ENCODING = "UTF-8";

	/**
	 * Non-null if Flashlight should log to the console, <code>null</code>
	 * otherwise.
	 */
	private static final PrintWriter f_log;

	/**
	 * Flags if helpful debug information should be output to the console log.
	 * This flag generates a lot of output and should only be set to {@code
	 * true} for small test programs.
	 */
	public static final boolean DEBUG = false;

	/**
	 * Logs a message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 */
	static void log(final String msg) {
		if (f_log != null) {
			f_log.println("[Flashlight] " + msg);
		}
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
		logAProblem(msg, new Exception());
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
		if (f_log != null) {
			f_log.flush();
		}
	}

	/**
	 * Closes the log.
	 */
	static void logComplete() {
		if (f_log != null) {
			f_log.close();
		}
	}

	/**
	 * The string value of the <tt>FL_RUN</tt> property or <tt>"flashlight"</tt>
	 * if this property is not set.
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
	private static final BlockingQueue<List<Event>> f_rawQueue;

	/**
	 * A queue to buffer refined event records from the {@link Refinery} to the
	 * {@link Depository}.
	 */
	private static final BlockingQueue<List<Event>> f_outQueue;

	/**
	 * The refinery thread.
	 */
	private static final AbstractRefinery f_refinery;

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
	private final static ThreadLocal<State> tl_withinStore;

	public static final class State {
		boolean inside = false;
		final ThreadPhantomReference thread;
		public final TraceNode.Header traceHeader;
		final List<Event> eventQueue;
		final BlockingQueue<List<Event>> rawQueue;
		final List<IdPhantomReference> locksHeld = IdConstants.trackLocks ? new ArrayList<IdPhantomReference>()
				: null;

		State(final BlockingQueue<List<Event>> q, final List<Event> l,
				final boolean flashlightThread) {
			rawQueue = q;
			eventQueue = l;
			if (flashlightThread) {
				inside = true;
				thread = null;
				traceHeader = null;
			} else {
				thread = Phantom.ofThread(Thread.currentThread());
				traceHeader = TraceNode.makeHeader();
			}
		}

		TraceNode getCurrentTrace(final long siteId) {
			return traceHeader.getCurrentNode(siteId, this);
		}

		TraceNode getCurrentTrace() {
			return traceHeader.getCurrentNode(this);
		}
	}

	private static final boolean useLocks = true;

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
	final static State flashlightThread() {
		final State s = createState(true);
		tl_withinStore.set(s);
		return s;
	}

	private static State createState(final boolean flashlightThread) {
		final List<Event> l = new ArrayList<Event>(LOCAL_QUEUE_MAX);
		synchronized (localQueueList) {
			localQueueList.add(l);
		}
		return new State(f_rawQueue, l, flashlightThread);
	}

	/*
	 * Flashlight startup code used to get everything running.
	 */
	static {
		// Check if FL is on (and shutoff)
		if (IdConstants.enableFlashlightToggle
				|| !StoreDelegate.FL_OFF.getAndSet(true)) {
			// System.out.println("Store");
			// new
			// Throwable("Store CL = "+Store.class.getClassLoader()).printStackTrace(System.out);

			/*
			 * Initialize final static fields. If Flashlight is off these fields
			 * are all set to null to save memory.
			 */
			final File flashlightDir = new File(StoreConfiguration
					.getDirectory());
			if (!flashlightDir.exists()) {
				flashlightDir.mkdirs();
			}
			// ??? What to do if mkdirs() fails???
			final StringBuilder fileName = new StringBuilder();
			fileName.append(flashlightDir);
			fileName.append(System.getProperty("file.separator"));
			f_run = StoreConfiguration.getRun();
			fileName.append(f_run);

			/*
			 * Get the start time of the data collection. This time is embedded
			 * in the time event in the output as well as in the names of the
			 * data and log files. The time can be provided in the configuration
			 * parameter "date override" so that we use the same start time in
			 * the data file as we use in the name of the flashlight run
			 * directory as constructed by the executing IDE.
			 */
			final SimpleDateFormat dateFormat = new SimpleDateFormat(
					InstrumentationConstants.DATE_FORMAT);
			final String dateOverride = StoreConfiguration.getDateOverride();
			Date startTime;
			if (dateOverride == null) {
				/* No time is provided, use the current time */
				startTime = new Date();
			} else {
				/*
				 * We have an externally provided time. Try to parse it. If we
				 * cannot parse it, use the current time.
				 */
				try {
					startTime = dateFormat.parse(dateOverride);
				} catch (final ParseException e) {
					System.err
							.println("[Flashlight] couldn't parse date string \""
									+ dateOverride + "\"");
					startTime = new Date();
				}
			}
			final Time timeEvent = new Time(startTime);
			fileName.append(dateFormat.format(timeEvent.getDate()));

			final File logFile = new File(fileName.toString() + ".flog");
			PrintWriter w = null;
			try {
				OutputStream stream = new FileOutputStream(logFile);
				stream = new BufferedOutputStream(stream);
				w = new PrintWriter(stream);
			} catch (final IOException e) {
				System.err.println("[Flashlight] unable to log to \""
						+ logFile.getAbsolutePath() + "\"");
				e.printStackTrace(System.err);
				System.exit(1); // bail
			}
			f_log = w;
			// still incremented even if logging is off.
			f_problemCount = new AtomicLong();

			final OutputType outType = StoreConfiguration.getOutputType();
			w = null;
			if (StoreConfiguration.debugOn()) {
				System.err.println("Output XML = " + !outType.isBinary());
			}
			final File dataFile = EventVisitor.createStreamFile(fileName
					.toString(), outType);
			EventVisitor outputStrategy = null;
			try {
				final PrintWriter headerW = new PrintWriter(fileName.toString()
						+ ".flh");
				OutputStrategyXML.outputHeader(headerW, timeEvent, outType
						.isBinary() ? OutputStrategyBinary.version
						: OutputStrategyXML.version);
				headerW.close();

				if (StoreConfiguration.debugOn()) {
					System.err.println("Compress stream = "
							+ outType.isCompressed());
				}
				final EventVisitor.Factory factory = outType.isBinary() ? OutputStrategyBinary.factory
						: OutputStrategyXML.factory;
				if (StoreConfiguration.useSeparateStreams()) {
					outputStrategy = new OutputStreamsStrategy(fileName
							.toString(), ENCODING, timeEvent, factory);
				} else {
					final OutputStream stream = EventVisitor.createStream(
							fileName.toString(), outType);
					outputStrategy = factory
							.create(stream, ENCODING, timeEvent);
				}
			} catch (final IOException e) {
				logAProblem("unable to output to \""
						+ dataFile.getAbsolutePath() + "\"", e);
				System.exit(1); // bail
			}

			final int rawQueueSize = StoreConfiguration.getRawQueueSize();
			final int outQueueSize = StoreConfiguration.getOutQueueSize();
			f_rawQueue = new ArrayBlockingQueue<List<Event>>(StoreConfiguration
					.isRefineryOff() ? outQueueSize : rawQueueSize);
			putInQueue(f_rawQueue, singletonList(timeEvent));
			putInQueue(f_rawQueue, singletonList(new SelectedPackage("*")));

			if (StoreConfiguration.debugOn()) {
				System.err.println("Using refinery = "
						+ !StoreConfiguration.isRefineryOff());
			}
			if (!StoreConfiguration.isRefineryOff()) {
				f_outQueue = new ArrayBlockingQueue<List<Event>>(outQueueSize);
			} else {
				f_outQueue = null;
			}
			tl_withinStore = new ThreadLocal<State>() {
				@Override
				protected State initialValue() {
					return createState(false);
				}
			};
			IdPhantomReference
					.addObserver(new IdPhantomReferenceCreationObserver() {
						public void notify(final ClassPhantomReference o,
								final IdPhantomReference r) {
							/*
							 * Create an event to define this object.
							 */
							Store.putInQueue(tl_withinStore.get(),
									new ObjectDefinition(o, r));
						}
					});
			final int refinerySize = StoreConfiguration.getRefinerySize();
			if (!StoreConfiguration.isRefineryOff()) {
				f_refinery = new Refinery(f_rawQueue, f_outQueue, refinerySize);
				f_refinery.start();
				f_depository = new Depository(f_outQueue, outputStrategy);
			} else {
				f_refinery = new MinimalRefinery();
				f_refinery.start();
				f_depository = new Depository(f_rawQueue, outputStrategy);
			}
			f_depository.start();
			log("collection started (rawQ=" + rawQueueSize + " : refinery="
					+ refinerySize + " : outQ=" + outQueueSize + ")");
			log("to \"" + dataFile.getAbsolutePath() + "\"");
			/*
			 * The spy periodically checks the state of the instrumented program
			 * and shuts down flashlight if the program is finished.
			 */
			final boolean noSpy = StoreConfiguration.getNoSpy();
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

			f_flashlightIsNotInitialized = false;
			StoreDelegate.FL_OFF.set(false);
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
			f_flashlightIsNotInitialized = false;
		}
	}

	public static BlockingQueue<List<Event>> getRawQueue() {
		return f_rawQueue;
	}

	static int getIntProperty(final String key, int def, final int min) {
		try {
			final String intString = System.getProperty(key);
			if (intString != null) {
				def = Integer.parseInt(intString);
			}
		} catch (final NumberFormatException e) {
			// ignore, go with the default
		}
		// ensure the result isn't less than the minimum
		return def >= min ? def : min;
	}

	/**
	 * Get the phantom object reference for the given {@code Class} object.
	 * Cannot use {@link Phantom#ofClass(Class)} directly because we need to
	 * make sure the store is loaded and initialized before creating phantom
	 * objects.
	 */
	public static ClassPhantomReference getClassPhantom(final Class<?> c) {
		return StoreDelegate.getClassPhantom(c);
	}

	public static ObjectPhantomReference getObjectPhantom(final Object o,
			final long id) {
		return StoreDelegate.getObjectPhantom(o, id);
	}

	/**
	 * Records that a statically numbered instance field was accessed within the
	 * instrumented program.
	 * 
	 * @param read
	 *            {@code true} indicates a field <i>read</i>, {@code false}
	 *            indicates a field <i>write</i>.
	 * @param receiver
	 *            the object instance the field is part of the state of.
	 * @param fieldID
	 *            the statically assigned id for the accessed field.
	 * @param siteId
	 *            The id of the program location that is accessing the field
	 * @param dcPhantom
	 *            The phantom class object for the class that declares the field
	 *            being accessed, or null} if the field is declared in an
	 *            uninstrumented class.
	 * @param declaringClass
	 *            The class object for the class that declares the field being
	 *            access when {@code dcPhantom} is null} . If the {@code
	 *            dcPhantom} is non- null} then this is null} .
	 */
	public static void instanceFieldAccess(final boolean read,
			final Object receiver, final int fieldID, final long siteId,
			final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
		if (!StoreConfiguration.processFieldAccesses()) {
			// System.out.println("Omitting field access");
			return;
		}
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}

		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		// System.out.println("Handling field access");

		flState.inside = true;
		try {
			/*
			 * if (DEBUG) { final String fmt =
			 * "Store.instanceFieldAccessLookup(%n\t\t%s%n\t\treceiver=%s%n\t\tfield=%s%n\t\tlocation=%s)"
			 * ; log(String.format(fmt, read ? "read" : "write",
			 * safeToString(receiver), clazz.getName()+'.'+fieldName,
			 * SrcLoc.toString(withinClass, line))); }
			 */

			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			/*
			 * if (oField == null) { final String fmt =
			 * "field cannot be null...instrumentation bug detected by Store.instanceFieldAccessLookup(%s, receiver=%s, field=%s, withinClass, line=%s)"
			 * ; logAProblem(String.format(fmt, read ? "read" : "write",
			 * safeToString(receiver), clazz.getName()+'.'+fieldName,
			 * SrcLoc.toString(withinClass, line))); return; }
			 */
			final Event e;
			/*
			 * if the field is not from an instrumented class then force
			 * creation of the phantom class object. Declaring class is null if
			 * the field is not from an instrumented class. We need to force the
			 * creation of the phantom object so that a listener somewhere else
			 * dumps the field definitions into the .fl file.
			 * 
			 * XXX This check is not redundant. Edwin already removed this once
			 * before and broke things.
			 */
			if (declaringClass != null) {
				Phantom.ofClass(declaringClass);
			}
			if (read) {
				e = new FieldReadInstance(receiver, fieldID, siteId, flState);
			} else {
				e = new FieldWriteInstance(receiver, fieldID, siteId, flState);
			}
			putInQueue(flState, e);
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that a statically numbered static field was accessed within the
	 * instrumented program.
	 * 
	 * @param read
	 *            {@code true} indicates a field <i>read</i>, {@code false}
	 *            indicates a field <i>write</i>.
	 * @param fieldID
	 *            the statically assigned id for the accessed field.
	 * @param siteId
	 *            The id of the program location that is accessing the field
	 * @param dcPhantom
	 *            The phantom class object for the class that declares the field
	 *            being accessed, or null} if the field is declared in an
	 *            uninstrumented class.
	 * @param declaringClass
	 *            The class object for the class that declares the field being
	 *            access when {@code dcPhantom} is null} . If the {@code
	 *            dcPhantom} is non- null} then this is null} .
	 */
	public static void staticFieldAccess(final boolean read, final int fieldID,
			final long siteId, final ClassPhantomReference dcPhantom,
			final Class<?> declaringClass) {
		if (!StoreConfiguration.processFieldAccesses()) {
			// System.out.println("Omitting field access");
			return;
		}

		if (StoreDelegate.FL_OFF.get()) {
			return;
		}

		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */

		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		// System.out.println("Handling field access");

		flState.inside = true;
		try {
			/*
			 * if (DEBUG) { final String fmt =
			 * "Store.staticFieldAccessLookup(%n\t\t%s%n\t\tfield=%s%n\t\tlocation=%s)"
			 * ; log(String.format(fmt, read ? "read" : "write",
			 * clazz.getName()+'.'+fieldName, SrcLoc.toString(withinClass,
			 * line))); }
			 */

			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			/*
			 * if (oField == null) { final String fmt =
			 * "field cannot be null...instrumentation bug detected by Store.staticFieldAccessLookup(%s, field=%s, location=%s)"
			 * ; logAProblem(String.format(fmt, read ? "read" : "write",
			 * clazz.getName()+'.'+fieldName, SrcLoc.toString(withinClass,
			 * line))); return; }
			 */
			boolean underConstruction = false;
			if (dcPhantom != null) {
				underConstruction = dcPhantom.isUnderConstruction();
			}
			/*
			 * if the field is not from an instrumented class then force
			 * creation of the phantom class object. Declaring class is null if
			 * the field is not from an instrumente class. We need to force the
			 * creation of the phantom object so that a listener somewhere else
			 * dumps the field definitions into the .fl file.
			 * 
			 * XXX This check is not redundant. Edwin already removed this once
			 * before and broke things.
			 */
			if (declaringClass != null) {
				Phantom.ofClass(declaringClass);
			}
			final Event e;
			if (read) {
				e = new FieldReadStatic(fieldID, siteId, flState,
						underConstruction);
			} else {
				e = new FieldWriteStatic(fieldID, siteId, flState,
						underConstruction);
			}
			putInQueue(flState, e);
		} finally {
			flState.inside = false;
		}
	}

	// /**
	// * Records that a instance field that needs to be assigned a dynamic field
	// id
	// * was accessed within the instrumented program.
	// *
	// * @param read
	// * {@code true} indicates a field <i>read</i>, {@code false}
	// * indicates a field <i>write</i>.
	// * @param receiver
	// * the object instance the field is part of the state of.
	// * @param clazz
	// * The class object of the class in which the search
	// * for the field should begin.
	// * @param fieldName
	// * the name of the field.
	// * @param withinClass
	// * the phantom class object for the class where the event occurred, may be
	// {@code null}.
	// * @param line
	// * the line number where the event occurred.
	// */
	// public static void instanceFieldAccessLookup(
	// final boolean read, final Object receiver,
	// final Class clazz, final String fieldName, final long siteId) {
	// if (!IdConstants.useFieldAccesses) {
	// return;
	// }
	// if (f_flashlightIsNotInitialized)
	// return;
	// if (StoreDelegate.FL_OFF.get())
	// return;
	// final State flState = tl_withinStore.get();
	// if (flState.inside)
	// return;
	// flState.inside = true;
	// try {
	// if (filterOutFieldAccess(clazz, siteId)) {
	// return;
	// }
	// if (DEBUG) {
	// final String fmt =
	// "Store.instanceFieldAccessLookup(%n\t\t%s%n\t\treceiver=%s%n\t\tfield=%s%n\t\tlocation=%s)";
	// log(String.format(fmt, read ? "read" : "write",
	// safeToString(receiver), clazz.getName()+'.'+fieldName, siteId));
	// }
	// final ObservedField oField = ObservedField.getInstance(clazz, fieldName,
	// flState);
	// /*
	// * Check that the parameters are valid, gather needed information,
	// * and put an event in the raw queue.
	// */
	// if (oField == null) {
	// final String fmt =
	// "field cannot be null...instrumentation bug detected by Store.instanceFieldAccessLookup(%s, receiver=%s, field=%s, withinClass, line=%s)";
	// logAProblem(String.format(fmt, read ? "read" : "write",
	// safeToString(receiver), clazz.getName()+'.'+fieldName, siteId));
	// return;
	// }
	// final Event e;
	// if (receiver == null) {
	// final String fmt =
	// "instance field %s access reported with a null receiver...instrumentation bug detected by Store.instanceFieldAccessLookup(%s, receiver=%s, field=%s, location=%s)";
	// logAProblem(String.format(fmt, oField, read ? "read"
	// : "write", safeToString(receiver), clazz.getName()+'.'+fieldName,
	// siteId));
	// return;
	// }
	// if (read)
	// e = new FieldReadInstance(receiver, oField.getId(), siteId, flState);
	// else
	// e = new FieldWriteInstance(receiver, oField.getId(), siteId, flState);
	// putInQueue(flState, e);
	// } finally {
	// flState.inside = false;
	// }
	// }

	// /**
	// * Records that a instance field that needs to be assigned a dynamic field
	// id
	// * was accessed within the instrumented program.
	// *
	// * @param read
	// * {@code true} indicates a field <i>read</i>, {@code false}
	// * indicates a field <i>write</i>.
	// * @param clazz
	// * The class object of the class in which the search
	// * for the field should begin.
	// * @param fieldName
	// * the name of the field.
	// * @param withinClass
	// * the phantom class object for the class where the event occurred, may be
	// {@code null}.
	// * @param line
	// * the line number where the event occurred.
	// */
	// public static void staticFieldAccessLookup(final boolean read,
	// final Class clazz, final String fieldName, final long siteId) {
	// if (!IdConstants.useFieldAccesses) {
	// return;
	// }
	// if (f_flashlightIsNotInitialized)
	// return;
	// if (StoreDelegate.FL_OFF.get())
	// return;
	// final State flState = tl_withinStore.get();
	// if (flState.inside)
	// return;
	// flState.inside = true;
	// try {
	// if (filterOutFieldAccess(clazz, siteId)) {
	// return;
	// }
	// if (DEBUG) {
	// final String fmt =
	// "Store.staticFieldAccessLookup(%n\t\t%s%n\t\tfield=%s%n\t\tlocation=%s)";
	// log(String.format(fmt, read ? "read" : "write",
	// clazz.getName()+'.'+fieldName, siteId));
	// }
	// final ObservedField oField = ObservedField.getInstance(clazz, fieldName,
	// flState);
	// /*
	// * Check that the parameters are valid, gather needed information,
	// * and put an event in the raw queue.
	// */
	// if (oField == null) {
	// final String fmt =
	// "field cannot be null...instrumentation bug detected by Store.staticFieldAccessLookup(%s, field=%s, location=%s)";
	// logAProblem(String.format(fmt, read ? "read" : "write",
	// clazz.getName()+'.'+fieldName, siteId));
	// return;
	// }
	//
	// final Event e;
	// if (read)
	// e = new FieldReadStatic(oField.getId(), siteId, flState);
	// else
	// e = new FieldWriteStatic(oField.getId(), siteId, flState);
	// putInQueue(flState, e);
	// } finally {
	// flState.inside = false;
	// }
	// }

	/**
	 * Record that the given object was accessed indirectly (via method call) at
	 * the given site
	 * 
	 * @param receiver
	 *            non-null
	 */
	public static void indirectAccess(final Object receiver, final long siteId) {
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		if (!StoreConfiguration.getCollectionType().processIndirectAccesses()) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */

		// System.out.println("indirectAccess");
		// System.out.println("  receiver = " + receiver.getClass().getName() +
		// "@"
		// + Integer.toHexString(System.identityHashCode(receiver)));
		// System.out.println("  siteID = " + siteId);
		// System.out.flush();

		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.indirectAccess(%n\t\treceiver=%s%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(receiver), siteId));
			}
			/*
			 * Record this access in the trace.
			 */
			putInQueue(flState, new IndirectAccess(receiver, siteId, flState));
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that an array element is being accessed.
	 * 
	 * @param read
	 *            {@code true} indicates the element is being <i>read</i>,
	 *            {@code false} indicates the element is being <i>written</i>.
	 * @param receiver
	 *            the array instance whose element is being accessed.
	 * @param index
	 *            The index of the array element being accessed.
	 * @param siteId
	 *            The site in the code of the array access.
	 */
	public static void arrayAccess(final boolean read, final Object receiver,
			final int index, final long siteId) {
		// System.out.println("arrayAccess");
		// System.out.println("  read = " + read);
		// System.out.println("  receiver = " + receiver);
		// System.out.println("  index = " + index);
		// System.out.println("  siteId = " + siteId);
	}

	/**
	 * Records that a class initializer is about to begin execution, or has
	 * completed execution.
	 * 
	 * @param before
	 *            true} if the the class initializer is about to begin
	 *            execution; false} if the class initializers has already
	 *            completed execution.
	 * @param class The class object of the class being initialized.
	 */
	public static void classInit(final boolean before, final Class<?> clazz) {
		final ClassPhantomReference p = Phantom.ofClass(clazz);
		p.setUnderConstruction(before);
		if (DEBUG) {
			final String fmt = "Store.classInit(%n\t\tbefore=%s%n\t\tclass=%s)";
			System.out.println(String.format(fmt, before ? "true" : "false",
					clazz.getName()));
		}
	}

	/**
	 * Records that a constructor call occurred within the instrumented program.
	 * 
	 * @param before
	 *            {@code true} indicates <i>before</i> the constructor call,
	 *            {@code false} indicates <i>after</i> the constructor call.
	 * @param enclosingFileName
	 *            the name of the file where the constructor call occurred.
	 * @param enclosingLocationName
	 *            the name of the method, constructor, or initializer where the
	 *            constructor call occurred.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void constructorCall(final boolean before, final long siteId) {
		/*
		 * if (!IdConstants.useTraces) { return; }
		 */

		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.constructorCall(%n\t\t%s%n\t\tlocation=%s)";
				log(String.format(fmt, before ? "before" : "after", siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (before) {
				TraceNode.pushTraceNode(siteId, flState);
			} else {
				TraceNode.popTraceNode(siteId, flState);
			}
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that a constructor is executing within the instrumented program.
	 * Constructor executions are used to track objects that are under
	 * construction. Unlike constructor call records, many pairs of constructor
	 * executions may be reported during the construction of an object. This is
	 * because a pair of constructor executions is reported for each block of
	 * constructor code that is executed. Therefore, explicit or implicit calls
	 * to {@code super(..)} or {@code this(..)} can cause multiple blocks of
	 * code to execute during object construction. The receiver object reported
	 * will, of course, be the same for all pairs of constructor executions
	 * reported for the construction of an object.
	 * 
	 * @param before
	 *            {@code true} indicates <i>before</i> the constructor
	 *            execution, {@code false} indicates <i>after</i> the
	 *            constructor execution.
	 * @param receiver
	 *            the object under construction.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void constructorExecution(final boolean before,
			final Object receiver, final long siteId) {
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.constructorExecution(%n\t\t%s%n\t\treceiver=%s%n\t\tlocation=%s)";
				log(String.format(fmt, before ? "before" : "after",
						safeToString(receiver), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (receiver == null) {
				final String fmt = "constructor cannot be null...instrumentation bug detected by Store.constructorExecution(%s, receiver=%s, location=%s)";
				logAProblem(String.format(fmt, before ? "before" : "after",
						safeToString(receiver), siteId));
			} else {
				final ObjectPhantomReference p = Phantom.ofObject(receiver);
				p.setUnderConstruction(before);
			}
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that a method call occurred within the instrumented program.
	 * Typically this is a call to a method from another method, however, the
	 * call could originate in a constructor or an initializer.
	 * <p>
	 * This method also dispatches this event properly if the method call is to
	 * an <i>interesting</i> method with regard to the program's concurrency.
	 * Interesting methods include calls to {@link Object#wait()},
	 * {@link Object#wait(long)}, {@link Object#wait(long, int)}, and {@code
	 * java.util.concurrent} locks.
	 * 
	 * @param before
	 *            {@code true} indicates <i>before</i> the method call, {@code
	 *            false} indicates <i>after</i> the method call.
	 * @param receiver
	 *            the object instance the method is being called on, or {@code
	 *            null} if the method is {@code static}.
	 * @param enclosingFileName
	 *            the name of the file where the method call occurred.
	 * @param enclosingLocationName
	 *            the name of the method, constructor, or initializer where the
	 *            method call occurred.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void methodCall(final boolean before, final Object receiver,
			final long siteId) {
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		/*
		 * if (!IdConstants.useTraces) { return; }
		 */

		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */

		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.methodCall(%n\t\t%s%n\t\treceiver=%s%n\t\tlocation=%s)";
				log(String.format(fmt, before ? "before" : "after",
						safeToString(receiver), siteId));
			}

			/*
			 * Special handling for ReadWriteLocks
			 */
			if (receiver instanceof ReadWriteLock) {
				/*
				 * Define the structure of the ReadWriteLock in an event.
				 */
				final ReadWriteLock rwl = (ReadWriteLock) receiver;
				final ObjectPhantomReference p = Phantom.ofObject(rwl);
				if (UtilConcurrent.addReadWriteLock(p)) {
					if (DEBUG) {
						final String fmt = "Defined ReadWriteLock id=%d";
						log(String.format(fmt, p.getId()));
					}
					final Event e = new ReadWriteLockDefinition(p, Phantom
							.ofObject(rwl.readLock()), Phantom.ofObject(rwl
							.writeLock()));
					putInQueue(flState, e);
				}
			}

			/*
			 * Record this call in the trace.
			 */
			if (before) {
				TraceNode.pushTraceNode(siteId, flState);
			} else {
				TraceNode.popTraceNode(siteId, flState);
			}
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that the instrumented program is attempting to acquire an
	 * intrinsic lock. An intrinsic lock is a {@code synchronized} block or
	 * method.
	 * 
	 * @param lockObject
	 *            the object being synchronized (i.e., the lock).
	 * @param lockIsThis
	 *            {@code true} if the lock object is dynamically the same as the
	 *            receiver object, i.e., {@code this == on}.
	 * @param lockIsClass
	 *            {@code true} if the lock object is dynamically the same as the
	 *            class the method is declared within, {@code false} otherwise.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void beforeIntrinsicLockAcquisition(final Object lockObject,
			final boolean lockIsThis, final boolean lockIsClass,
			final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.beforeIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlockIsThis=%b%n\t\tlockIsClass=%b%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(lockObject), lockIsThis,
						lockIsClass, siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.beforeIntrinsicLockAcquisition(lockObject=%s, lockIsThis=%b, lockIsClass=%b, location=%s)";
				logAProblem(String.format(fmt, safeToString(lockObject),
						lockIsThis, lockIsClass, siteId));
				return;
			}
			final Event e = new BeforeIntrinsicLockAcquisition(lockObject,
					lockIsThis, lockIsClass, siteId, flState);
			putInQueue(flState, e, true);
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that the instrumented program has acquired an intrinsic lock. An
	 * intrinsic lock is a {@code synchronized} block or method.
	 * 
	 * @param lockObject
	 *            the object being synchronized (i.e., the lock).
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void afterIntrinsicLockAcquisition(final Object lockObject,
			final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.afterIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(lockObject), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.afterIntrinsicLockAcquisition(lockObject=%s, location=%s)";
				logAProblem(String
						.format(fmt, safeToString(lockObject), siteId));
				return;
			}
			if (IdConstants.trackLocks) {
				final IdPhantomReference lockPhantom = Phantom.of(lockObject);
				flState.locksHeld.add(lockPhantom);
			}
			final Event e = new AfterIntrinsicLockAcquisition(lockObject,
					siteId, flState);
			putInQueue(flState, e);
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that the instrumented program is entering a call to one of the
	 * following methods:
	 * <ul>
	 * <li>{@link Object#wait()}</li>
	 * <li>{@link Object#wait(long)}</li>
	 * <li>
	 * {@link Object#wait(long, int)}</li>
	 * </ul>
	 * See the Java Language Specification (3rd edition) section 17.8 <i>Wait
	 * Sets and Notification</i> for the semantics of waiting on an intrinsic
	 * lock. An intrinsic lock is a {@code synchronized} block or method.
	 * 
	 * @param before
	 *            {@code true} indicates <i>before</i> the method call, {@code
	 *            false} indicates <i>after</i> the method call.
	 * @param lockObject
	 *            the object being waited on (i.e., the thread should be holding
	 *            a lock on this object).
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void intrinsicLockWait(final boolean before,
			final Object lockObject, final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.intrinsicLockWait(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, before ? "before" : "after",
						safeToString(lockObject), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.intrinsicLockWait(%s, lockObject=%s, location=%s)";
				logAProblem(String.format(fmt, before ? "before" : "after",
						safeToString(lockObject), siteId));
				return;
			}
			final Event e;
			if (before) {
				e = new BeforeIntrinsicLockWait(lockObject, siteId, flState);
			} else {
				e = new AfterIntrinsicLockWait(lockObject, siteId, flState);
			}
			putInQueue(flState, e, true);
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that the program has released an intrinsic lock. An intrinsic
	 * lock is a {@code synchronized} block or method.
	 * 
	 * @param lockObject
	 *            the object being synchronized (i.e., the lock).
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void afterIntrinsicLockRelease(final Object lockObject,
			final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				final String fmt = "Store.afterIntrinsicLockRelease(%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(lockObject), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.afterIntrinsicLockRelease(lockObject=%s, location=%s)";
				logAProblem(String
						.format(fmt, safeToString(lockObject), siteId));
				return;
			}
			if (IdConstants.trackLocks) {
				final IdPhantomReference lockPhantom = Phantom.of(lockObject);
				flState.locksHeld.remove(lockPhantom);
			}
			final Event e = new AfterIntrinsicLockRelease(lockObject, siteId,
					flState);
			putInQueue(flState, e);
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records that the instrumented program is attempting to acquire a
	 * {@link Lock}.
	 * 
	 * @param lockObject
	 *            the {@link Lock} object in use.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void beforeUtilConcurrentLockAcquisitionAttempt(
			final Object lockObject, final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				/*
				 * Implementation note: We are counting on the implementer of
				 * the util.concurrent Lock object to not have a bad toString()
				 * method.
				 */
				final String fmt = "Store.beforeUtilConcurrentLockAcquisitionAttempt(%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, lockObject, siteId));
			}
			if (lockObject instanceof Lock) {
				final Lock ucLock = (Lock) lockObject;
				final Event e = new BeforeUtilConcurrentLockAcquisitionAttempt(
						ucLock, siteId, flState);
				putInQueue(flState, e, true);
			} else {
				final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.beforeUtilConcurrentLockAcquisitionAttempt(lockObject=%s, location=%s)";
				logAProblem(String.format(fmt, lockObject, siteId));
				return;
			}
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records the result of the instrumented program's attempt to acquire a
	 * {@link Lock}.
	 * 
	 * @param gotTheLock
	 *            {@code true} indicates the attempt succeeded and the lock was
	 *            obtained, {@code false} indicates the attempt failed and the
	 *            lock was not obtained (due to an exception or a false return
	 *            from a {@link Lock#tryLock()} or
	 *            {@link Lock#tryLock(long, java.util.concurrent.TimeUnit)}).
	 * @param lockObject
	 *            the {@link Lock} object in use.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void afterUtilConcurrentLockAcquisitionAttempt(
			final boolean gotTheLock, final Object lockObject, final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				/*
				 * Implementation note: We are counting on the implementer of
				 * the util.concurrent Lock object to not have a bad toString()
				 * method.
				 */
				final String fmt = "Store.afterUtilConcurrentLockAcquisitionAttempt(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, gotTheLock ? "holding"
						: "failed-to-acquire", lockObject, siteId));
			}
			if (lockObject instanceof Lock) {
				if (IdConstants.trackLocks && gotTheLock) {
					final ObjectPhantomReference lockPhantom = Phantom
							.ofObject(lockObject);
					flState.locksHeld.add(lockPhantom);
				}
				final Lock ucLock = (Lock) lockObject;
				final Event e = new AfterUtilConcurrentLockAcquisitionAttempt(
						gotTheLock, ucLock, siteId, flState);
				putInQueue(flState, e);
			} else {
				final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.afterUtilConcurrentLockAcquisitionAttempt(%s, lockObject=%s, location=%s)";
				logAProblem(String.format(fmt, gotTheLock ? "holding"
						: "failed-to-acquire", lockObject, siteId));
				return;
			}
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Records the result of the instrumented program's attempt to release a
	 * {@link Lock}.
	 * 
	 * @param releasedTheLock
	 *            {@code true} indicates the attempt succeeded and the lock was
	 *            released, {@code false} indicates the attempt failed and the
	 *            lock was not released (due to an exception that was likely
	 *            caused because the thread was not holding the lock).
	 * @param lockObject
	 *            the {@link Lock} object in use.
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public static void afterUtilConcurrentLockReleaseAttempt(
			final boolean releasedTheLock, final Object lockObject,
			final long siteId) {
		if (!useLocks) {
			return;
		}
		/*
		 * if (f_flashlightIsNotInitialized) { return; }
		 */
		if (StoreDelegate.FL_OFF.get()) {
			return;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return;
		}
		flState.inside = true;
		try {
			if (DEBUG) {
				/*
				 * Implementation note: We are counting on the implementer of
				 * the util.concurrent Lock object to not have a bad toString()
				 * method.
				 */
				final String fmt = "Store.afterUtilConcurrentLockReleaseAttempt(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, releasedTheLock ? "released"
						: "failed-to-release", lockObject, siteId));
			}
			if (lockObject instanceof Lock) {
				if (IdConstants.trackLocks && releasedTheLock) {
					final ObjectPhantomReference lockPhantom = Phantom
							.ofObject(lockObject);
					flState.locksHeld.remove(lockPhantom);
				}

				final Lock ucLock = (Lock) lockObject;
				final Event e = new AfterUtilConcurrentLockReleaseAttempt(
						releasedTheLock, ucLock, siteId, flState);
				putInQueue(flState, e);
			} else {
				final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.afterUtilConcurrentLockReleaseAttempt(%s, lockObject=%s, location=%s)";
				logAProblem(String.format(fmt, releasedTheLock ? "released"
						: "failed-to-release", lockObject, siteId));
				return;
			}
		} finally {
			flState.inside = false;
		}
	}

	/**
	 * Stops collection of events about the instrumented program. This method
	 * may be called from within the following thread contexts:
	 * <ul>
	 * <li>A direct call from a program thread, i.e., a call was added to the
	 * program code</li>
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
		// System.out.println("FL_OFF = "+StoreDelegate.FL_OFF.hashCode()+" "+StoreDelegate.FL_OFF.get());

		/*
		 * The below getAndSet(true) ensures that only one thread shuts down
		 * Flashlight.
		 */
		if (StoreDelegate.FL_OFF.getAndSet(true)) {
			return;
		}

		// new Throwable("Calling shutdown()").printStackTrace(System.out);
		// System.out.flush();

		/*
		 * Finish up data output.
		 */
		if (StoreConfiguration.isRefineryOff()) {
			// Need to shutdown the minimal refinery in a different way than
			// the normal refinery
			f_refinery.requestShutdown();
		}
		Thread.yield();

		putInQueue(f_rawQueue, flushLocalQueues());
		putInQueue(f_rawQueue, singletonList(FinalEvent.FINAL_EVENT));
		if (!StoreConfiguration.isRefineryOff()) {
			join(f_refinery);
		}
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
		if (f_spy != null) {
			f_spy.requestShutdown();
		}

		final long endTime = System.nanoTime();
		final long totalTime = endTime - f_start_nano;
		final StringBuilder sb = new StringBuilder(
				" (duration of collection was ");
		formatNanoTime(sb, totalTime);
		sb.append(')');
		final String duration = sb.toString();
		final long problemCount = f_problemCount.get();
		if (problemCount < 1) {
			log("collection shutdown" + duration);
		} else {
			log("collection shutdown with " + problemCount
					+ " problem(s) reported" + duration);
		}

		final File done = new File(StoreConfiguration.getDirectory(),
				InstrumentationConstants.FL_COMPLETE_RUN);
		try {
			final FileWriter w = new FileWriter(done);
			sb.delete(0, sb.length()); // clear
			sb.append("Completed: ");
			formatNanoTime(sb, endTime);
			w.write(sb.toString());
			w.close();
		} catch (final IOException e) {
			log(e.getMessage() + ", while writing final file");
		}
		logComplete();
	}

	private static void formatNanoTime(final StringBuilder sb,
			final long totalTime) {
		final long nsPerSecond = 1000000000L;
		final long nsPerMinute = 60000000000L;
		final long nsPerHour = 3600000000000L;

		long timeLeft = totalTime;
		final long totalHours = timeLeft / nsPerHour;
		timeLeft -= totalHours * nsPerHour;

		final long totalMins = timeLeft / nsPerMinute;
		timeLeft -= totalMins * nsPerMinute;

		final float totalSecs = timeLeft / (float) nsPerSecond;

		sb.append(totalHours).append(':');
		if (totalMins < 10) {
			sb.append('0');
		}
		sb.append(totalMins).append(':');
		if (totalSecs < 10) {
			sb.append('0');
		}
		sb.append(totalSecs);
	}

	/**
	 * Only used for testing, this method sets the output strategy of the
	 * depository thread.
	 * 
	 * @param outputStrategy
	 *            an output strategy.
	 */
	static void setOutputStrategy(final EventVisitor outputStrategy) {
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
	static <T> void putInQueue(final BlockingQueue<T> queue, final T e) {
		boolean done = false;
		while (!done) {
			try {
				queue.put(e);
				done = true;
			} catch (final InterruptedException e1) {
				/*
				 * We are within a program thread, so another program thread
				 * interrupted us. I think it is OK to ignore this, however, we
				 * do need to ensure the event gets put into the raw queue.
				 */
				logAProblem("queue.put(e) was interrupted", e1);
			}
		}
	}

	static final int LOCAL_QUEUE_MAX = 256;

	/**
	 * Used by the refinery to flush all the local queues upon shutdown
	 */
	static final List<List<Event>> localQueueList = new ArrayList<List<Event>>();

	public static void putInQueue(final State state, final Event e) {
		putInQueue(state, e, false);
	}

	static void putInQueue(final State state, final Event e, final boolean flush) {
		/*
		 * if (e instanceof ObjectDefinition) { ObjectDefinition od =
		 * (ObjectDefinition) e; if (od.getObject() instanceof
		 * ClassPhantomReference) {
		 * System.err.println("Local queue: "+od.getObject()); } }
		 */
		/*
		 * if (state == null) { state = tl_withinStore.get(); }
		 */
		final List<Event> localQ = state.eventQueue;
		List<Event> copy = null;
		synchronized (localQ) {
			localQ.add(e);
			if (/* flush || */localQ.size() >= LOCAL_QUEUE_MAX) {
				copy = new ArrayList<Event>(localQ);
				localQ.clear();
			}
		}
		if (copy != null) {
			/*
			 * for(Event ev : copy) { if (ev instanceof ObjectDefinition) {
			 * ObjectDefinition od = (ObjectDefinition) ev; if (od.getObject()
			 * instanceof ClassPhantomReference) {
			 * System.err.println("Queuing: "+od.getObject()); } } }
			 */
			putInQueue(state.rawQueue, copy);
		}
	}

	static List<Event> flushLocalQueues() {
		final List<Event> buf = new ArrayList<Event>(LOCAL_QUEUE_MAX);
		synchronized (localQueueList) {
			for (final List<Event> q : localQueueList) {
				synchronized (q) {
					buf.addAll(q);
					q.clear();
				}
			}
		}
		/*
		 * for(Event ev : buf) { if (ev instanceof ObjectDefinition) {
		 * ObjectDefinition od = (ObjectDefinition) ev; if (od.getObject()
		 * instanceof ClassPhantomReference) {
		 * System.err.println("Flushing: "+od.getObject()); } } }
		 */
		return buf;
	}

	static List<Event> singletonList(final Event e) {
		final List<Event> l = new ArrayList<Event>(1);
		l.add(e);
		return l;
	}

	/**
	 * Joins on the given thread ignoring any interruptions.
	 * 
	 * @param t
	 *            the thread to join on.
	 */
	private static void join(final Thread t) {
		if (t == null) {
			return;
		}
		boolean done = false;
		while (!done) {
			try {
				t.join();
				done = true;
			} catch (final InterruptedException e1) {
				// ignore, we expect to be interrupted
			}
		}
	}

	/**
	 * Produces a safe string representation of any object. Some overrides of
	 * {@link Object#toString()} throw exceptions and behave badly. This method
	 * avoids those problems by building the same string that would be built for
	 * the object if {@link Object#toString()} was not overridden.
	 * <p>
	 * In a dynamic analysis, like Flashlight, it is not safe to be calling the
	 * {@link Object#toString()} methods of objects where the class is unknown.
	 * 
	 * @param o
	 *            the object to return a string representation of.
	 * @return a string representing the passed object.
	 */
	private static String safeToString(final Object o) {
		if (o == null) {
			return "null";
		} else {
			return o.getClass().getName() + "@"
					+ Integer.toHexString(o.hashCode());
		}
	}

	private Store() {
		// no instances
	}

	public static void instanceFieldInit(final Object receiver,
			final int fieldId, final Object value) {
		if (!StoreConfiguration.processFieldAccesses()) {
			return;
		}
		if (DEBUG) {
			final String fmt = "Store.instanceFieldInit%n\t\treceiver=%s%n\t\field=%s%n\t\tvalue=%s)";
			log(String.format(fmt, safeToString(receiver), fieldId,
					safeToString(value)));
		}
		// Ignore null assignments
		if (value == null) {
			return;
		}
		final State state = tl_withinStore.get();
		if (state.inside) {
			return;
		}
		state.inside = true;
		try {
			putInQueue(state, new FieldAssignment(receiver, fieldId, value));
		} finally {
			state.inside = false;
		}
	}

	public static void staticFieldInit(final int fieldId, final Object value) {
		if (!StoreConfiguration.processFieldAccesses()) {
			return;
		}
		if (DEBUG) {
			final String fmt = "Store.instanceFieldInit%n\t\field=%s%n\t\tvalue=%s)";
			log(String.format(fmt, fieldId, safeToString(value)));
		}
		// Ignore null assignments
		if (value == null) {
			return;
		}
		final State state = tl_withinStore.get();
		if (state.inside) {
			return;
		}
		state.inside = true;
		try {
			putInQueue(state, new FieldAssignment(fieldId, value));
		} finally {
			state.inside = false;
		}
	}
}
