package com.surelogic._flashlight;

import java.util.Collection;
import java.util.List;

/**
 * A {@link StoreListener} listens for events from the Flashlight
 * instrumentation. It expects to be created, initialized, and shutdown by the
 * {@link Store} class. Any implementor of {@link StoreListener} is expected to
 * be thread-safe in all respects. The {@link StoreListener} should expect to be
 * provided with only two guarantees:
 * 
 * <ol>
 * <li>There is a happens-before relationship between init and any other method
 * of the listener interface
 * <li>No methods will be called after shutdown is called
 * </ol>
 * 
 * @author nathan
 * 
 */
public interface StoreListener {

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
	void instanceFieldAccess(final boolean read, final Object receiver,
			final int fieldID, final long siteId,
			final ClassPhantomReference dcPhantom, final Class<?> declaringClass);

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
	void staticFieldAccess(final boolean read, final int fieldID,
			final long siteId, final ClassPhantomReference dcPhantom,
			final Class<?> declaringClass);

	/**
	 * Record that the given object was accessed indirectly (via method call) at
	 * the given site
	 * 
	 * @param receiver
	 *            non-null
	 */
	void indirectAccess(final Object receiver, final long siteId);

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
	void arrayAccess(final boolean read, final Object receiver,
			final int index, final long siteId);

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
	void classInit(final boolean before, final Class<?> clazz);

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
	void constructorCall(final boolean before, final long siteId);

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
	void constructorExecution(final boolean before, final Object receiver,
			final long siteId);

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
	void methodCall(final boolean before, final Object receiver,
			final long siteId);

	void methodExecution(boolean before, long siteid);

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
	void beforeIntrinsicLockAcquisition(final Object lockObject,
			final boolean lockIsThis, final boolean lockIsClass,
			final long siteId);

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
	void afterIntrinsicLockAcquisition(final Object lockObject,
			final long siteId);

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
	void intrinsicLockWait(final boolean before, final Object lockObject,
			final long siteId);

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
	void afterIntrinsicLockRelease(final Object lockObject, final long siteId);

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
	void beforeUtilConcurrentLockAcquisitionAttempt(final Object lockObject,
			final long siteId);

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
	void afterUtilConcurrentLockAcquisitionAttempt(final boolean gotTheLock,
			final Object lockObject, final long siteId);

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
	void afterUtilConcurrentLockReleaseAttempt(final boolean releasedTheLock,
			final Object lockObject, final long siteId);

	/**
	 * Prepare for the collection of events about the instrumented program.
	 * 
	 * XXX The holder of this StoreListener guarantees that there will be a
	 * happens before relationship between this initialization and any calls to
	 * other methods of this interface.
	 */
	void init(RunConf conf);

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
	void shutdown();

	void instanceFieldInit(final Object receiver, final int fieldId,
			final Object value);

	void staticFieldInit(final int fieldId, final Object value);

	/**
	 * This event is called periodically to report all recently garbage
	 * collected objects. Listeners are *not* required to be non-blocking, but
	 * they should process data in a timely fashion, as the phantom reference
	 * queue will not be polled again until this completes.
	 * 
	 * @param references
	 *            a list of references to garbage collected objects
	 */
	void garbageCollect(final List<? extends IdPhantomReference> references);

	/**
	 * This will be called by the user of this listener. The listener should
	 * return a list of commands that a user could use to modify or query this
	 * listener.
	 * 
	 * @return a list of commands for this store listener.
	 */
	Collection<? extends ConsoleCommand> getCommands();

}
