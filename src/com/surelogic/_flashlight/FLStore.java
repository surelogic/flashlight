package com.surelogic._flashlight;

import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic._flashlight.common.IdConstants;

/**
 * This class defines the interface into the Flashlight data store.
 * 
 * @policyLock Console is java.lang.System:out
 */
public class FLStore {

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
	 * The console thread.
	 */
	private static final Console f_console;

	/**
	 * A periodic task that checks to see if Flashlight should shutdown by
	 * spying on the running program's threads.
	 */
	private static final Spy f_spy;

	private static final RunConf f_conf;

	private static final CopyOnWriteArrayList<StoreListener> f_listeners = new CopyOnWriteArrayList<StoreListener>();

	/**
	 * This thread-local (tl) flag is used to ensure that we to not, within a
	 * thread, reenter the store. This situation can occur if we call methods on
	 * the objects passed into the store and the implementation of those methods
	 * is part of the instrumented program.
	 */
	private final static ThreadLocal<State> tl_withinStore;

	private static class State {
		private boolean inside;
	}

	static {
		// Check if FL is on (and shutoff)
		if (IdConstants.enableFlashlightToggle
				|| !StoreDelegate.FL_OFF.getAndSet(true)) {
			f_conf = new RunConf();
			// TODO add listeners
			for (StoreListener l : f_listeners) {
				l.init(f_conf);
			}

			/*
			 * The console lets someone attach to flashlight and command it to
			 * shutdown.
			 */
			f_console = new Console();
			f_console.start();
			tl_withinStore = new ThreadLocal<State>() {

				@Override
				protected State initialValue() {
					return new State();
				}

			};
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
			f_flashlightIsNotInitialized = false;
			StoreDelegate.FL_OFF.set(false);
		} else {
			f_console = null;
			f_spy = null;
			f_conf = null;
			tl_withinStore = null;
		}
	}

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
	public final static void flashlightThread() {
		tl_withinStore.get().inside = false;
	}

	/**
	 * Checks to see whether or not we are inside the Flashlight instrumentation
	 * already. If not, we flag the thread as being inside the Flashlight
	 * instrumentation. Also checks to make sure that Flashlight is not turned
	 * off.
	 * 
	 * @return <code>true</code> if we are on and not yet flagged as inside
	 *         flashlight
	 */
	private static final boolean checkInside() {
		if (StoreDelegate.FL_OFF.get()) {
			return false;
		}
		final State flState = tl_withinStore.get();
		if (flState.inside) {
			return false;
		}
		flState.inside = true;
		return true;
	}

	public void instanceFieldAccess(final boolean read, final Object receiver,
			final int fieldID, final long siteId,
			final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
		if (!StoreConfiguration.processFieldAccesses()) {
			return;
		}
		if (checkInside()) {
			try {
				/*
				 * if the field is not from an instrumented class then force
				 * creation of the phantom class object. Declaring class is null
				 * if the field is not from an instrumented class. We need to
				 * force the creation of the phantom object so that a listener
				 * somewhere else dumps the field definitions into the .fl file.
				 * 
				 * XXX This check is not redundant. Edwin already removed this
				 * once before and broke things.
				 */
				if (declaringClass != null) {
					Phantom.ofClass(declaringClass);
				}
				for (StoreListener l : f_listeners) {
					l.instanceFieldAccess(read, receiver, fieldID, siteId,
							dcPhantom, declaringClass);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void staticFieldAccess(final boolean read, final int fieldID,
			final long siteId, final ClassPhantomReference dcPhantom,
			final Class<?> declaringClass) {
		if (!StoreConfiguration.processFieldAccesses()) {
			return;
		}
		if (checkInside()) {
			try {
				/*
				 * if the field is not from an instrumented class then force
				 * creation of the phantom class object. Declaring class is null
				 * if the field is not from an instrumente class. We need to
				 * force the creation of the phantom object so that a listener
				 * somewhere else dumps the field definitions into the .fl file.
				 * 
				 * XXX This check is not redundant. Edwin already removed this
				 * once before and broke things.
				 */
				if (declaringClass != null) {
					Phantom.ofClass(declaringClass);
				}
				for (StoreListener l : f_listeners) {
					l.staticFieldAccess(read, fieldID, siteId, dcPhantom,
							declaringClass);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void indirectAccess(final Object receiver, final long siteId) {
		if (!StoreConfiguration.getCollectionType().processIndirectAccesses()) {
			return;
		}
		if (checkInside()) {
			try {
				if (f_conf.isDebug()) {
					final String fmt = "LockSetStore.indirectAccess(%n\t\treceiver=%s%n\t\tlocation=%s)";
					f_conf.log(String.format(fmt,
							StoreDelegate.safeToString(receiver), siteId));
				}
				for (StoreListener l : f_listeners) {
					l.indirectAccess(receiver, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void arrayAccess(final boolean read, final Object receiver,
			final int index, final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.arrayAccess(read, receiver, index, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void classInit(final boolean before, final Class<?> clazz) {
		if (checkInside()) {
			try {
				final ClassPhantomReference p = Phantom.ofClass(clazz);
				p.setUnderConstruction(before);
				if (f_conf.isDebug()) {
					final String fmt = "Store.classInit(%n\t\tbefore=%s%n\t\tclass=%s)";
					f_conf.log(String.format(fmt, before ? "true" : "false",
							clazz.getName()));
				}
				for (StoreListener l : f_listeners) {
					l.classInit(before, clazz);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void constructorCall(final boolean before, final long siteId) {
		if (checkInside()) {
			try {
				if (f_conf.isDebug()) {
					final String fmt = "Store.constructorCall(%n\t\t%s%n\t\tlocation=%s)";
					f_conf.log(String.format(fmt, before ? "before" : "after",
							siteId));
				}
				for (StoreListener l : f_listeners) {
					l.constructorCall(before, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void constructorExecution(final boolean before,
			final Object receiver, final long siteId) {
		if (checkInside()) {
			try {
				if (f_conf.isDebug()) {
					final String fmt = "Store.constructorExecution(%n\t\t%s%n\t\treceiver=%s%n\t\tlocation=%s)";
					f_conf.log(String.format(fmt, before ? "before" : "after",
							StoreDelegate.safeToString(receiver), siteId));
				}
				/*
				 * Check that the parameters are valid, gather needed
				 * information, and put an event in the raw queue.
				 */
				if (receiver == null) {
					final String fmt = "constructor cannot be null...instrumentation bug detected by Store.constructorExecution(%s, receiver=%s, location=%s)";
					f_conf.logAProblem(String.format(fmt, before ? "before"
							: "after", StoreDelegate.safeToString(receiver),
							siteId));
				} else {
					final ObjectPhantomReference p = Phantom.ofObject(receiver);
					p.setUnderConstruction(before);
					for (StoreListener l : f_listeners) {
						l.constructorExecution(before, receiver, siteId);
					}
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void methodCall(final boolean before, final Object receiver,
			final long siteId) {
		if (checkInside()) {
			try {
				if (f_conf.isDebug()) {
					final String fmt = "Store.methodCall(%n\t\t%s%n\t\treceiver=%s%n\t\tlocation=%s)";
					f_conf.log(String.format(fmt, before ? "before" : "after",
							StoreDelegate.safeToString(receiver), siteId));
				}
				for (StoreListener l : f_listeners) {
					l.methodCall(before, receiver, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void beforeIntrinsicLockAcquisition(final Object lockObject,
			final boolean lockIsThis, final boolean lockIsClass,
			final long siteId) {
		if (checkInside()) {
			try {
				if (f_conf.isDebug()) {
					final String fmt = "Store.beforeIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlockIsThis=%b%n\t\tlockIsClass=%b%n\t\tlocation=%s)";
					f_conf.log(String.format(fmt,
							StoreDelegate.safeToString(lockObject), lockIsThis,
							lockIsClass, siteId));
				}
				/*
				 * Check that the parameters are valid, gather needed
				 * information, and put an event in the raw queue.
				 */
				if (lockObject == null) {
					final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.beforeIntrinsicLockAcquisition(lockObject=%s, lockIsThis=%b, lockIsClass=%b, location=%s)";
					f_conf.logAProblem(String.format(fmt,
							StoreDelegate.safeToString(lockObject), lockIsThis,
							lockIsClass, siteId));
					return;
				}
				for (StoreListener l : f_listeners) {
					l.beforeIntrinsicLockAcquisition(lockObject, lockIsThis,
							lockIsClass, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void afterIntrinsicLockAcquisition(final Object lockObject,
			final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.afterIntrinsicLockAcquisition(lockObject, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void intrinsicLockWait(final boolean before,
			final Object lockObject, final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.intrinsicLockWait(before, lockObject, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void afterIntrinsicLockRelease(final Object lockObject,
			final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.afterIntrinsicLockRelease(lockObject, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void beforeUtilConcurrentLockAcquisitionAttempt(
			final Object lockObject, final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.beforeUtilConcurrentLockAcquisitionAttempt(lockObject,
							siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void afterUtilConcurrentLockAcquisitionAttempt(
			final boolean gotTheLock, final Object lockObject, final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.afterUtilConcurrentLockAcquisitionAttempt(gotTheLock,
							lockObject, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void afterUtilConcurrentLockReleaseAttempt(
			final boolean releasedTheLock, final Object lockObject,
			final long siteId) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.afterUtilConcurrentLockReleaseAttempt(releasedTheLock,
							lockObject, siteId);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void instanceFieldInit(final Object receiver, final int fieldId,
			final Object value) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.instanceFieldInit(receiver, fieldId, value);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

	public void staticFieldInit(final int fieldId, final Object value) {
		if (checkInside()) {
			try {
				for (StoreListener l : f_listeners) {
					l.staticFieldInit(fieldId, value);
				}
			} finally {
				tl_withinStore.get().inside = false;
			}
		}
	}

}
