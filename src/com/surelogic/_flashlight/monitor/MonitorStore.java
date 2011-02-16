package com.surelogic._flashlight.monitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.surelogic._flashlight.ClassPhantomReference;
import com.surelogic._flashlight.ConsoleCommand;
import com.surelogic._flashlight.FieldDef;
import com.surelogic._flashlight.FieldDefs;
import com.surelogic._flashlight.IdPhantomReference;
import com.surelogic._flashlight.ObjectPhantomReference;
import com.surelogic._flashlight.Phantom;
import com.surelogic._flashlight.RunConf;
import com.surelogic._flashlight.Spy;
import com.surelogic._flashlight.StoreListener;
import com.surelogic._flashlight.ThreadPhantomReference;
import com.surelogic._flashlight.UtilConcurrent;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.ReferenceType;
import com.surelogic._flashlight.trace.TraceNode;

/**
 * This class defines the interface into the Flashlight data store.
 * 
 * @policyLock Console is java.lang.System:out
 */
public final class MonitorStore implements StoreListener {

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

	private volatile MonitorSpec f_spec;

	private final ThreadLocal<ThreadLocks> tl_lockSet;
	final CopyOnWriteArrayList<ThreadLocks> f_lockSets;

	CopyOnWriteArrayList<ThreadLocks> threadLocks() {
		return f_lockSets;
	}

	private UtilConcurrent f_knownRWLocks;
	private RunConf f_conf;

	private Analysis f_activeAnalysis;
	private final Lock f_analysisLock = new ReentrantLock();

	/**
	 * Revise the set of alerts used by the active analysis.
	 * 
	 * @param spec
	 */
	void reviseAlerts(final AlertSpec spec) {
		f_analysisLock.lock();
		try {
			f_activeAnalysis.setAlerts(spec);
		} finally {
			f_analysisLock.unlock();
		}
	}

	/**
	 * Change the monitor specification used by the active analysis.
	 * 
	 * @param spec
	 */
	void reviseSpec(final MonitorSpec spec) {
		f_analysisLock.lock();
		try {
			if (f_activeAnalysis != null) {
				f_activeAnalysis.wrapUp();
				try {
					f_activeAnalysis.join();
				} catch (final InterruptedException e) {
					f_conf.logAProblem("Exception while changing analyses", e);
				}
			}
			f_activeAnalysis = new Analysis(this, f_conf);
			f_spec = spec;
			f_activeAnalysis.start();
		} finally {
			f_analysisLock.unlock();
		}
	}

	/**
	 * Get the active analysis. This is the only thread-safe way for a console
	 * to get the active analysis.
	 * 
	 * @return
	 */
	Analysis getAnalysis() {
		f_analysisLock.lock();
		try {
			return f_activeAnalysis;
		} finally {
			f_analysisLock.unlock();
		}
	}

	/*
	 * Flashlight startup code used to get everything running.
	 */
	public MonitorStore() {
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

	}

	public void init(final RunConf conf) {
		f_spec = new MonitorSpec(System.getProperty("com.surelogic.fieldSpec",
				""), conf.getFieldDefs());
		f_knownRWLocks = new UtilConcurrent();
		f_conf = conf;

		// Start up looking at no fields.
		reviseSpec(f_spec);
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
		// Do nothing
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
		final long lockId = Phantom.of(lockObject).getId();
		if (!f_lockNames.containsKey(lockId)) {
			f_lockNames.put(lockId, lockObject.getClass().toString()
					+ lockObject.hashCode());
		}
		tl_lockSet.get().enterLock(lockId);
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
		// Do nothing
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
		final IdPhantomReference lockPhantom = Phantom.of(lockObject);
		tl_lockSet.get().leaveLock(lockPhantom.getId());
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
		// Do nothing
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
		if (lockObject instanceof Lock) {
			if (gotTheLock) {
				final ObjectPhantomReference lockPhantom = Phantom
						.ofObject(lockObject);
				final long id = lockPhantom.getId();
				tl_lockSet.get().enterLock(id);
			}
		} else {
			final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by LockSetStore.afterUtilConcurrentLockAcquisitionAttempt(%s, lockObject=%s, location=%s)";
			f_conf.logAProblem(String.format(fmt, gotTheLock ? "holding"
					: "failed-to-acquire", lockObject, siteId));
			return;
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
		if (lockObject instanceof Lock) {
			if (releasedTheLock) {
				final ObjectPhantomReference lockPhantom = Phantom
						.ofObject(lockObject);
				final long id = lockPhantom.getId();
				tl_lockSet.get().leaveLock(id);
			}
		} else {
			final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by LockSetStore.afterUtilConcurrentLockReleaseAttempt(%s, lockObject=%s, location=%s)";
			f_conf.logAProblem(String.format(fmt, releasedTheLock ? "released"
					: "failed-to-release", lockObject, siteId));
			return;
		}
	}

	public void instanceFieldInit(final Object receiver, final int fieldId,
			final Object value) {
		// Do nothing
	}

	public void staticFieldInit(final int fieldId, final Object value) {
		// Do nothing
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
		f_analysisLock.lock();
		try {
			f_activeAnalysis.wrapUp();
		} finally {
			f_analysisLock.unlock();
		}
	}

	class ListCommand implements ConsoleCommand {
		private static final String LIST = "list";

		public String getDescription() {
			return LIST + "- displays all results of the latest ";
		}

		public String handle(final String command) {
			if ("list".equalsIgnoreCase(command)) {
				return getAnalysis().toString();
			}
			return null;
		}

	}

	class AlertsCommand implements ConsoleCommand {
		private static final String ALERTS = "alerts";

		public String getDescription() {
			return ALERTS
					+ " - display any and all alerts that have been triggered.";
		}

		public String handle(final String command) {
			if (ALERTS.equalsIgnoreCase(command)) {
				return getAnalysis().getAlerts().toString();
			}
			return null;
		}

	}

	class DeadlocksCommand implements ConsoleCommand {
		private static final String DEADLOCKS = "deadlocks";

		public String getDescription() {
			return DEADLOCKS + " - display all detected potential deadlocks";
		}

		public String handle(final String command) {
			if (DEADLOCKS.equalsIgnoreCase(command)) {
				return getAnalysis().getDeadlocks().toString();
			}
			return null;
		}

	}

	class LockSetsCommand implements ConsoleCommand {
		private static final String LOCKSETS = "lockSets";

		public String getDescription() {
			return LOCKSETS
					+ " - display known lock set information for all observed fields";
		}

		public String handle(final String command) {
			if (LOCKSETS.equalsIgnoreCase(command)) {
				return getAnalysis().getLockSets().toString();
			}
			return null;
		}

	}

	class RaceConditionsCommand implements ConsoleCommand {
		private static final String RACECONDITIONS = "dataRaces";

		public String getDescription() {
			return RACECONDITIONS
					+ " - display all detected potential race conditions";
		}

		public String handle(final String command) {
			if (RACECONDITIONS.equalsIgnoreCase(command)) {
				return getAnalysis().getLockSets().raceInfo();
			}
			return null;
		}

	}

	class SharedCommand implements ConsoleCommand {
		private static final String SHARED = "shared";

		public String getDescription() {
			return SHARED
					+ " - display all observed fields that are shared between threads";
		}

		public String handle(final String command) {
			if (SHARED.equalsIgnoreCase(command)) {
				return getAnalysis().getShared().toString();
			}
			return null;
		}

	}

	class SetCommand implements ConsoleCommand {
		private static final String SET = "set <prop>=<val>";

		private static final String FIELD_SPEC = "fieldSpec";
		private static final String EDT_FIELDS = "swingFieldAlerts";
		private static final String SHARED_FIELDS = "sharedFieldAlerts";
		private static final String LOCKSET_FIELDS = "lockSetAlerts";

		private final Pattern SETP = Pattern.compile("set ([^=]*)=(.*)");

		public String getDescription() {
			return SET
					+ "- Set one of the following properties: ["
					+ new String[] { FIELD_SPEC, EDT_FIELDS, SHARED_FIELDS,
							LOCKSET_FIELDS };
		}

		public String handle(final String command) {
			final Matcher m = SETP.matcher(command);
			if (m.matches()) {
				final String prop = m.group(1);
				final String val = m.group(2);
				if (FIELD_SPEC.equalsIgnoreCase(prop)) {
					reviseSpec(new MonitorSpec(val, f_conf.getFieldDefs()));
					return String.format("Changing fieldSpec to be %s", val);
				} else if (EDT_FIELDS.equalsIgnoreCase(prop)) {
					reviseAlerts(new AlertSpec(val, null, null,
							f_conf.getFieldDefs()));
					String.format(
							"Monitoring fields matching %s for Swing policy violations.",
							val);

				} else if (SHARED_FIELDS.equalsIgnoreCase(prop)) {
					reviseAlerts(new AlertSpec(null, val, null,
							f_conf.getFieldDefs()));
					String.format(
							"Ensuring fields matching %s are not shared.", val);

				} else if (LOCKSET_FIELDS.equalsIgnoreCase(prop)) {
					reviseAlerts(new AlertSpec(null, null, val,
							f_conf.getFieldDefs()));
					String.format(
							"Ensuring fields matching %s always have a lock set.",
							val);
				} else {
					return String.format("%s is not a valid property", prop);
				}
			}
			return null;
		}

	}

	class DescribeCommand implements ConsoleCommand {
		private static final String DESCRIBE = "describe <field-regex>";
		private final Pattern DESCRIBEP = Pattern.compile("describe (.*)");

		public String getDescription() {
			return DESCRIBE
					+ " - describe all observed fields in the programming matching the given regular expression";
		}

		public String handle(final String command) {
			final Matcher d = DESCRIBEP.matcher(command);
			if (d.matches()) {
				final Pattern test = Pattern.compile(d.group(1));
				final LockSetInfo lockSets2 = getAnalysis().getLockSets();
				final FieldDefs defs = f_conf.getFieldDefs();
				StringBuilder response = new StringBuilder();
				for (final FieldDef def : defs.values()) {
					if (test.matcher(def.getQualifiedFieldName()).matches()) {
						response.append(lockSets2.lockSetInfo(def));
					}
				}
				return response.toString();
			}
			return null;
		}

	}

	public Collection<? extends ConsoleCommand> getCommands() {
		return Arrays.asList(new ConsoleCommand[] { new ListCommand(),
				new AlertsCommand(), new DeadlocksCommand(),
				new LockSetsCommand(), new RaceConditionsCommand(),
				new SharedCommand(), new SetCommand(), new DescribeCommand() });
	}

}
