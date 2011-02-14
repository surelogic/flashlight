package com.surelogic._flashlight.monitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.swing.SwingUtilities;

import com.surelogic._flashlight.ClassPhantomReference;
import com.surelogic._flashlight.IdPhantomReference;
import com.surelogic._flashlight.ObjectPhantomReference;
import com.surelogic._flashlight.Phantom;
import com.surelogic._flashlight.RunConf;
import com.surelogic._flashlight.Spy;
import com.surelogic._flashlight.StoreConfiguration;
import com.surelogic._flashlight.StoreDelegate;
import com.surelogic._flashlight.StoreListener;
import com.surelogic._flashlight.ThreadPhantomReference;
import com.surelogic._flashlight.UtilConcurrent;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.ReferenceType;
import com.surelogic._flashlight.trace.TraceNode;

/**
 * This class defines the interface into the Flashlight data store.
 * 
 * @policyLock Console is java.lang.System:out
 */
public final class MonitorStore implements StoreListener {

	/**
	 * The console thread.
	 */
	private final MonitorConsole f_console;

	/**
	 * A periodic task that checks to see if Flashlight should shutdown by
	 * spying on the running program's threads.
	 */
	private final MonitorSpy f_spy;

	private final ConcurrentMap<Long, String> f_lockNames;

	ConcurrentMap<Long, String> getLockNames() {
		return f_lockNames;
	}

	private final ConcurrentMap<Long, ReadWriteLockIds> f_rwLocks;

	ConcurrentMap<Long, ReadWriteLockIds> getRWLocks() {
		return f_rwLocks;
	}

	/**
	 * This thread-local (tl) flag is used to ensure that we to not, within a
	 * thread, reenter the store. This situation can occur if we call methods on
	 * the objects passed into the store and the implementation of those methods
	 * is part of the instrumented program.
	 * 
	 * It also holds the thread-local lock set values.
	 */
	private final ThreadLocal<State> tl_withinStore;

	public static final class State {
		final ThreadPhantomReference thread;
		public final TraceNode.Header traceHeader;

		public State() {
			thread = Phantom.ofThread(Thread.currentThread());
			traceHeader = TraceNode.makeHeader();
		}

	}

	void updateSpec(final MonitorSpec spec) {
		f_spec = spec;
	}

	private volatile MonitorSpec f_spec;

	private final ThreadLocal<ThreadLocks> tl_lockSet;
	final CopyOnWriteArrayList<ThreadLocks> f_lockSets;

	CopyOnWriteArrayList<ThreadLocks> threadLocks() {
		return f_lockSets;
	}

	private static final boolean useLocks = true;

	private UtilConcurrent f_knownRWLocks;
	private RunConf f_conf;

	/*
	 * Flashlight startup code used to get everything running.
	 */
	MonitorStore() {
		f_lockNames = new ConcurrentReferenceHashMap<Long, String>(
				ReferenceType.STRONG, ReferenceType.STRONG,
				ConcurrentReferenceHashMap.STANDARD_HASH);

		f_rwLocks = new ConcurrentReferenceHashMap<Long, ReadWriteLockIds>(
				ReferenceType.STRONG, ReferenceType.STRONG,
				ConcurrentReferenceHashMap.STANDARD_HASH);

		tl_withinStore = new ThreadLocal<State>() {
			@Override
			protected State initialValue() {
				return new State();
			}
		};
		/*
		 * Initialize lock set analysis thread locals
		 */
		tl_lockSet = new ThreadLocal<ThreadLocks>() {

			@Override
			protected ThreadLocks initialValue() {
				final ThreadPhantomReference thread = tl_withinStore.get().thread;
				final ThreadLocks ls = new ThreadLocks(thread.getName(),
						thread.getId(), SwingUtilities.isEventDispatchThread(),
						f_rwLocks);
				f_lockSets.add(ls);
				return ls;
			}

		};
		f_lockSets = new CopyOnWriteArrayList<ThreadLocks>();

		/*
		 * The spy periodically checks the state of the instrumented program and
		 * shuts down flashlight if the program is finished.
		 */
		final boolean noSpy = StoreConfiguration.getNoSpy();
		if (noSpy) {
			f_spy = null;
		} else {
			f_spy = new MonitorSpy();
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
				MonitorStore.this.shutdown();
			}
		});
		/*
		 * The console lets someone attach to flashlight and command it to
		 * shutdown.
		 */
		f_console = new MonitorConsole();
		f_console.start();
		// Start up looking at no fields.
		Analysis.reviseSpec(f_spec);
	}

	public void init(final RunConf conf) {
		f_spec = new MonitorSpec(System.getProperty("com.surelogic.fieldSpec",
				""), conf.getFieldDefs());
		f_knownRWLocks = new UtilConcurrent();
		f_conf = conf;
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
	ClassPhantomReference getClassPhantom(final Class<?> c) {
		return StoreDelegate.getClassPhantom(c);
	}

	ObjectPhantomReference getObjectPhantom(final Object o, final long id) {
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
	 *            access when {@code dcPhantom} is null} . If the
	 *            {@code dcPhantom} is non- null} then this is null} .
	 */
	public void instanceFieldAccess(final boolean read, final Object receiver,
			final int fieldID, final long siteId,
			final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
		final ObjectPhantomReference rec = Phantom.ofObject(receiver);
		final long receiverId = rec.getId();
		if (f_spec.isMonitoring(fieldID)) {
			tl_lockSet.get().field(fieldID, receiverId,
					rec.isUnderConstruction());
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
	 *            access when {@code dcPhantom} is null} . If the
	 *            {@code dcPhantom} is non- null} then this is null} .
	 */
	public void staticFieldAccess(final boolean read, final int fieldID,
			final long siteId, final ClassPhantomReference dcPhantom,
			final Class<?> declaringClass) {
		boolean underConstruction = false;
		if (dcPhantom != null) {
			underConstruction = dcPhantom.isUnderConstruction();
		}

		if (f_spec.isMonitoring(fieldID)) {
			tl_lockSet.get().field(fieldID, underConstruction);
		}
	}

	/**
	 * Record that the given object was accessed indirectly (via method call) at
	 * the given site
	 * 
	 * @param receiver
	 *            non-null
	 */
	public void indirectAccess(final Object receiver, final long siteId) {

		// Do nothing
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
	public void arrayAccess(final boolean read, final Object receiver,
			final int index, final long siteId) {
		// Do nothing
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
	public void classInit(final boolean before, final Class<?> clazz) {
		// Do nothing
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
	public void constructorCall(final boolean before, final long siteId) {
		State state = tl_withinStore.get();
		/*
		 * Check that the parameters are valid, gather needed information, and
		 * put an event in the raw queue.
		 */
		if (before) {
			TraceNode.pushTraceNode(siteId, state);
		} else {
			TraceNode.popTraceNode(siteId, state);
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
	public void constructorExecution(final boolean before,
			final Object receiver, final long siteId) {
		// Do nothing
	}

	/**
	 * Records that a method call occurred within the instrumented program.
	 * Typically this is a call to a method from another method, however, the
	 * call could originate in a constructor or an initializer.
	 * <p>
	 * This method also dispatches this event properly if the method call is to
	 * an <i>interesting</i> method with regard to the program's concurrency.
	 * Interesting methods include calls to {@link Object#wait()},
	 * {@link Object#wait(long)}, {@link Object#wait(long, int)}, and
	 * {@code java.util.concurrent} locks.
	 * 
	 * @param before
	 *            {@code true} indicates <i>before</i> the method call,
	 *            {@code false} indicates <i>after</i> the method call.
	 * @param receiver
	 *            the object instance the method is being called on, or
	 *            {@code null} if the method is {@code static}.
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
	public void methodCall(final boolean before, final Object receiver,
			final long siteId) {

		/*
		 * Special handling for ReadWriteLocks
		 */
		if (receiver instanceof ReadWriteLock) {
			/*
			 * Define the structure of the ReadWriteLock in an event.
			 */
			final ReadWriteLock rwl = (ReadWriteLock) receiver;
			final ObjectPhantomReference p = Phantom.ofObject(rwl);
			if (f_knownRWLocks.addReadWriteLock(p)) {
				if (f_conf.isDebug()) {
					final String fmt = "Defined ReadWriteLock id=%d";
					f_conf.log(String.format(fmt, p.getId()));
				}
				final long id = p.getId();
				final long read = Phantom.ofObject(rwl.readLock()).getId();
				final long write = Phantom.ofObject(rwl.writeLock()).getId();
				final ReadWriteLockIds ids = new ReadWriteLockIds(id, read,
						write);
				f_rwLocks.put(read, ids);
				f_rwLocks.put(write, ids);
			}
		}
		State state = tl_withinStore.get();
		/*
		 * Record this call in the trace.
		 */
		if (before) {
			TraceNode.pushTraceNode(siteId, state);
		} else {
			TraceNode.popTraceNode(siteId, state);
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
	public void beforeIntrinsicLockAcquisition(final Object lockObject,
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
				final String fmt = "LockSetStore.beforeIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlockIsThis=%b%n\t\tlockIsClass=%b%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(lockObject), lockIsThis,
						lockIsClass, siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by LockSetStore.beforeIntrinsicLockAcquisition(lockObject=%s, lockIsThis=%b, lockIsClass=%b, location=%s)";
				logAProblem(String.format(fmt, safeToString(lockObject),
						lockIsThis, lockIsClass, siteId));
				return;
			}
			// Do nothing right now
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
	public void afterIntrinsicLockAcquisition(final Object lockObject,
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
				final String fmt = "LockSetStore.afterIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(lockObject), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by LockSetStore.afterIntrinsicLockAcquisition(lockObject=%s, location=%s)";
				logAProblem(String
						.format(fmt, safeToString(lockObject), siteId));
				return;
			}
			final long lockId = Phantom.of(lockObject).getId();
			if (!f_lockNames.containsKey(lockId)) {
				f_lockNames.put(lockId, lockObject.getClass().toString()
						+ lockObject.hashCode());
			}
			tl_lockSet.get().enterLock(lockId);
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
	 *            {@code true} indicates <i>before</i> the method call,
	 *            {@code false} indicates <i>after</i> the method call.
	 * @param lockObject
	 *            the object being waited on (i.e., the thread should be holding
	 *            a lock on this object).
	 * @param withinClass
	 *            the class where the event occurred, may be {@code null}.
	 * @param line
	 *            the line number where the event occurred.
	 */
	public void intrinsicLockWait(final boolean before,
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
				final String fmt = "LockSetStore.intrinsicLockWait(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, before ? "before" : "after",
						safeToString(lockObject), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by LockSetStore.intrinsicLockWait(%s, lockObject=%s, location=%s)";
				logAProblem(String.format(fmt, before ? "before" : "after",
						safeToString(lockObject), siteId));
				return;
			}
			if (before) {
				// Do nothing
			} else {
				// Do nothing
			}
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
	public void afterIntrinsicLockRelease(final Object lockObject,
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
				final String fmt = "LockSetStore.afterIntrinsicLockRelease(%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, safeToString(lockObject), siteId));
			}
			/*
			 * Check that the parameters are valid, gather needed information,
			 * and put an event in the raw queue.
			 */
			if (lockObject == null) {
				final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by LockSetStore.afterIntrinsicLockRelease(lockObject=%s, location=%s)";
				logAProblem(String
						.format(fmt, safeToString(lockObject), siteId));
				return;
			}
			final IdPhantomReference lockPhantom = Phantom.of(lockObject);
			tl_lockSet.get().leaveLock(lockPhantom.getId());
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
	public void beforeUtilConcurrentLockAcquisitionAttempt(
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
				final String fmt = "LockSetStore.beforeUtilConcurrentLockAcquisitionAttempt(%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, lockObject, siteId));
			}
			if (lockObject instanceof Lock) {
				// Do nothing
			} else {
				final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by LockSetStore.beforeUtilConcurrentLockAcquisitionAttempt(lockObject=%s, location=%s)";
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
	public void afterUtilConcurrentLockAcquisitionAttempt(
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
				final String fmt = "LockSetStore.afterUtilConcurrentLockAcquisitionAttempt(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, gotTheLock ? "holding"
						: "failed-to-acquire", lockObject, siteId));
			}
			if (lockObject instanceof Lock) {
				if (gotTheLock) {
					final ObjectPhantomReference lockPhantom = Phantom
							.ofObject(lockObject);
					final long id = lockPhantom.getId();
					tl_lockSet.get().enterLock(id);
				}
			} else {
				final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by LockSetStore.afterUtilConcurrentLockAcquisitionAttempt(%s, lockObject=%s, location=%s)";
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
	public void afterUtilConcurrentLockReleaseAttempt(
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
				final String fmt = "LockSetStore.afterUtilConcurrentLockReleaseAttempt(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
				log(String.format(fmt, releasedTheLock ? "released"
						: "failed-to-release", lockObject, siteId));
			}
			if (lockObject instanceof Lock) {
				if (releasedTheLock) {
					final ObjectPhantomReference lockPhantom = Phantom
							.ofObject(lockObject);
					final long id = lockPhantom.getId();
					tl_lockSet.get().leaveLock(id);
				}
			} else {
				final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by LockSetStore.afterUtilConcurrentLockReleaseAttempt(%s, lockObject=%s, location=%s)";
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
	 * <li>A client handler thread created by the {@link MonitorConsole} thread
	 * that was told to shutdown flashlight via socket.</li>
	 * <li>The thread created to run our shutdown hook.</li>
	 * </ul>
	 */
	public void shutdown() {
		if (f_flashlightIsNotInitialized) {
			System.err.println("[Flashlight] !SERIOUS ERROR! "
					+ "LockSetStore.shutdown() invoked "
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
		final File f = new File("/tmp/locks");
		if (f.exists()) {
			f.delete();
		}

		final Analysis analysis = Analysis.getAnalysis();
		analysis.wrapUp();
		join(analysis);

		PrintWriter p;
		try {
			p = new PrintWriter(f);
			try {
				p.println(analysis.toString());
			} finally {
				p.close();
			}
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
		}

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

	public void instanceFieldInit(final Object receiver, final int fieldId,
			final Object value) {
		if (!StoreConfiguration.processFieldAccesses()) {
			return;
		}
		if (DEBUG) {
			final String fmt = "LockSetStore.instanceFieldInit%n\t\treceiver=%s%n\t\field=%s%n\t\tvalue=%s)";
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
			// Do nothing
		} finally {
			state.inside = false;
		}
	}

	public void staticFieldInit(final int fieldId, final Object value) {
		if (!StoreConfiguration.processFieldAccesses()) {
			return;
		}
		if (DEBUG) {
			final String fmt = "LockSetStore.instanceFieldInit%n\t\field=%s%n\t\tvalue=%s)";
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
			// Do nothing
		} finally {
			state.inside = false;
		}
	}

}
