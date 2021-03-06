package com.surelogic._flashlight;

import java.util.ArrayList;
import java.util.List;

import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.monitor.MonitorStore;

/**
 * This class defines the interface into the Flashlight data store.
 *
 * @policyLock Console is java.lang.System:out
 */
public class Store {

  /**
   * This <i>must</i> be declared first within this class so that it can avoid
   * instrumented library calls made by the static initialization of this class
   * to recursively reenter the class during static initialization.
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
   * A periodic task that checks to see if Flashlight should shutdown by spying
   * on the running program's threads.
   */
  private static final Spy f_spy;

  private static final GCThread f_gc;

  static final RunConf f_conf;

  static final List<StoreListener> f_listeners;

  /**
   * This thread-local (tl) flag is used to ensure that we to not, within a
   * thread, reenter the store. This situation can occur if we call methods on
   * the objects passed into the store and the implementation of those methods
   * is part of the instrumented program.
   */
  private final static ThreadLocal<State> tl_withinStore;

  static class State {
    boolean inside;
  }

  static {
    // Check if FL is on (and shutoff)
    if (IdConstants.enableFlashlightToggle || !StoreDelegate.FL_OFF.getAndSet(true)) {
      f_conf = new RunConf();

      // TODO add listeners based on properties

      f_listeners = new ArrayList<StoreListener>(2);
      f_listeners.add(new MonitorStore());
      if (StoreConfiguration.isPostmortemMode()) {
        f_listeners.add(new PostMortemStore());
      }
      List<ConsoleCommand> commands = new ArrayList<ConsoleCommand>();
      commands.add(new ShutdownCommand());
      commands.add(new PingCommand());
      for (StoreListener l : f_listeners) {
        l.init(f_conf);
        commands.addAll(l.getCommands());
      }

      /*
       * The console lets someone attach to flashlight and command it to
       * shutdown.
       */
      f_console = new Console(f_conf, commands);
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
       * The shutdown hook is a last ditch effort to shutdown flashlight cleanly
       * when an abrupt termination occurs (e.g., System.exit is invoked).
       */
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          shutdown();
        }
      });
      /*
       * Start garbage collection processing
       */
      f_gc = new GCThread();
      f_gc.start();

      f_flashlightIsNotInitialized = false;
      StoreDelegate.FL_OFF.set(false);
    } else {
      f_listeners = null;
      f_console = null;
      f_spy = null;
      f_conf = null;
      f_gc = null;
      tl_withinStore = null;
    }
  }

  /**
   * This method must be called as the first statement by each flashlight thread
   * to ensure that it <i>never</i> has data about it collected by the store.
   * The flashlight threads don't call program code, however, they do use the
   * Java standard libraries so we need to be cautious about any instrumentation
   * on the Java standard library called from within these treads.
   * <P>
   * The talk, <i>Java Instrumentation for Dynamic Analysis</i>, by Jeff Perkins
   * and Michael Ernst cautioned that "The instrumentation itself should not use
   * instrumented classes, i.e., If a heap analysis used an instrumented version
   * of HashMap it would recursively call itself." Flashlight allows this
   * dangerous situation to exist by setting the thread-local flag so that all
   * our instrumentation entry points to the store immediately return.
   */
  public final static void flashlightThread() {
    tl_withinStore.get().inside = false;
  }

  /**
   * Garbage collection thread. It polls the phantom reference queues and
   * reports garbage collection events to the {@link StoreListener} listeners.
   *
   * @author nathan
   *
   */
  static class GCThread extends Thread {

    private final List<IdPhantomReference> references = new ArrayList<IdPhantomReference>();
    private volatile boolean keepAlive = true;

    GCThread() {
      super("flashlight-gc");
    }

    @Override
    public void run() {
      while (keepAlive) {
        Phantom.drainTo(references);
        if (references.size() > 0) {
          for (StoreListener l : f_listeners) {
            l.garbageCollect(references);
          }
          references.clear();
        }
        Thread.yield();
      }
    }

    public void requestShutdown() {
      keepAlive = false;
    }
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

  public static void instanceFieldAccess(final boolean read, final Object receiver, final int fieldID, final long siteId,
      final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
    if (!StoreConfiguration.processFieldAccesses()) {
      return;
    }
    if (checkInside()) {
      try {
        /*
         * if the field is not from an instrumented class then force creation of
         * the phantom class object. Declaring class is null if the field is not
         * from an instrumented class. We need to force the creation of the
         * phantom object so that a listener somewhere else dumps the field
         * definitions into the .fl file.
         * 
         * XXX This check is not redundant. Edwin already removed this once
         * before and broke things.
         */
        if (declaringClass != null) {
          Phantom.ofClass(declaringClass);
        }
        for (StoreListener l : f_listeners) {
          l.instanceFieldAccess(read, receiver, fieldID, siteId, dcPhantom, declaringClass);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void staticFieldAccess(final boolean read, final int fieldID, final long siteId,
      final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
    if (!StoreConfiguration.processFieldAccesses()) {
      return;
    }
    if (checkInside()) {
      try {
        /*
         * if the field is not from an instrumented class then force creation of
         * the phantom class object. Declaring class is null if the field is not
         * from an instrumente class. We need to force the creation of the
         * phantom object so that a listener somewhere else dumps the field
         * definitions into the .fl file.
         * 
         * XXX This check is not redundant. Edwin already removed this once
         * before and broke things.
         */
        if (declaringClass != null) {
          Phantom.ofClass(declaringClass);
        }
        for (StoreListener l : f_listeners) {
          l.staticFieldAccess(read, fieldID, siteId, dcPhantom, declaringClass);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void indirectAccess(final Object receiver, final long siteId) {
    if (!StoreConfiguration.getCollectionType().processIndirectAccesses()) {
      return;
    }
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "LockSetStore.indirectAccess(%n\t\treceiver=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, StoreDelegate.safeToString(receiver), siteId));
        }
        for (StoreListener l : f_listeners) {
          l.indirectAccess(receiver, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void arrayAccess(final boolean read, final Object receiver, final int index, final long siteId) {
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

  public static void classInit(final boolean before, final Class<?> clazz) {
    if (checkInside()) {
      try {
        final ClassPhantomReference p = Phantom.ofClass(clazz);
        p.setUnderConstruction(before);
        if (f_conf.isDebug()) {
          final String fmt = "Store.classInit(%n\t\tbefore=%s%n\t\tclass=%s)";
          f_conf.log(String.format(fmt, before ? "true" : "false", clazz.getName()));
        }
        for (StoreListener l : f_listeners) {
          l.classInit(before, clazz);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void constructorCall(final boolean before, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.constructorCall(%n\t\t%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, before ? "before" : "after", siteId));
        }
        for (StoreListener l : f_listeners) {
          l.constructorCall(before, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void constructorExecution(final boolean before, final Object receiver, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.constructorExecution(%n\t\t%s%n\t\treceiver=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, before ? "before" : "after", StoreDelegate.safeToString(receiver), siteId));
        }
        /*
         * Check that the parameters are valid, gather needed information, and
         * put an event in the raw queue.
         */
        if (receiver == null) {
          final String fmt = "constructor cannot be null...instrumentation bug detected by Store.constructorExecution(%s, receiver=%s, location=%s)";
          f_conf.logAProblem(String.format(fmt, before ? "before" : "after", StoreDelegate.safeToString(receiver), siteId));
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

  public static void methodCall(final boolean before, final Object receiver, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.methodCall(%n\t\t%s%n\t\treceiver=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, before ? "before" : "after", StoreDelegate.safeToString(receiver), siteId));
        }
        for (StoreListener l : f_listeners) {
          l.methodCall(before, receiver, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void methodExecution(final boolean before, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "";
          f_conf.log(String.format(fmt, before ? "before" : "after", siteId));
        }
        for (StoreListener l : f_listeners) {
          l.methodExecution(before, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  /**
   * Called just after closure object is created. Assumes the that the closure
   * is implemented by wrapping a method call to another method that implements
   * the true body of the closure. How the wrapped method is called is indicated
   * by the <code>behavior</code> argument, using constants described in Section
   * 5.4.3.5 of <cite>The Java Virtual Machine Specification, Java 8
   * Edition</cite>.
   *
   * <table>
   * <tr>
   * <th>Behavior
   * <th>Interpretation
   * <tr>
   * <td>5
   * <td>invokevirtual <i>owner</i>.<i>method</i>
   * <tr>
   * <td>6
   * <td>invokestatic <i>owner</i>.<i>method</i>
   * <tr>
   * <td>7
   * <td>invokespecial <i>owner</i>.<i>method</i>
   * <tr>
   * <td>8
   * <td>new <i>owner</i>; dup; invokespecial <i>owner</i>.&lt;init&gt;
   * <tr>
   * <td>9
   * <td>invokeinterface <i>owner</i>.<i>method</i>
   * </table>
   *
   * @param closure
   *          The closure object. This is the object that implements a
   *          <em>functional interface</em>.
   * @param functionalInterface
   *          The internal class name of the functional interface implemented by
   *          the closure
   * @param methodName
   *          The name of the abstract method of the functional interface that
   *          is implemented by the closure.
   * @param methodDesc
   *          The signature of the abstract method.
   * @param behavior
   *          How the method wrapped by the closure is invoked. See above.
   * @param owner
   *          The internal class name of the owner of the wrapped method.
   * @param name
   *          The name of the wrapped method.
   * @param desc
   *          The type signature of the wrapped method.
   */
  public static void closureCreation(final Object closure, final String functionalInterface, final String methodName,
      final String methodDesc, final int behavior, final String owner, final String name, final String desc) {
    // fill this in
  }

  public static void beforeIntrinsicLockAcquisition(final Object lockObject, final boolean lockIsThis, final boolean lockIsClass,
      final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.beforeIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlockIsThis=%b%n\t\tlockIsClass=%b%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, StoreDelegate.safeToString(lockObject), lockIsThis, lockIsClass, siteId));
        }
        /*
         * Check that the parameters are valid, gather needed information, and
         * put an event in the raw queue.
         */
        if (lockObject == null) {
          final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.beforeIntrinsicLockAcquisition(lockObject=%s, lockIsThis=%b, lockIsClass=%b, location=%s)";
          f_conf.logAProblem(String.format(fmt, StoreDelegate.safeToString(lockObject), lockIsThis, lockIsClass, siteId));
          return;
        }
        for (StoreListener l : f_listeners) {
          l.beforeIntrinsicLockAcquisition(lockObject, lockIsThis, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void afterIntrinsicLockAcquisition(final Object lockObject, final boolean lockIsThis, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.afterIntrinsicLockAcquisition(%n\t\tlockObject=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, StoreDelegate.safeToString(lockObject), siteId));
        }
        /*
         * Check that the parameters are valid, gather needed information, and
         * put an event in the raw queue.
         */
        if (lockObject == null) {
          final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.afterIntrinsicLockAcquisition(lockObject=%s, location=%s)";
          f_conf.logAProblem(String.format(fmt, StoreDelegate.safeToString(lockObject), siteId));
          return;
        }
        for (StoreListener l : f_listeners) {
          l.afterIntrinsicLockAcquisition(lockObject, lockIsThis, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void intrinsicLockWait(final boolean before, final Object lockObject, final boolean lockIsThis, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.intrinsicLockWait(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, before ? "before" : "after", StoreDelegate.safeToString(lockObject), siteId));
        }
        /*
         * Check that the parameters are valid, gather needed information, and
         * put an event in the raw queue.
         */
        if (lockObject == null) {
          final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.intrinsicLockWait(%s, lockObject=%s, location=%s)";
          f_conf.logAProblem(String.format(fmt, before ? "before" : "after", StoreDelegate.safeToString(lockObject), siteId));
          return;
        }
        for (StoreListener l : f_listeners) {
          l.intrinsicLockWait(before, lockObject, lockIsThis, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void afterIntrinsicLockRelease(final Object lockObject, final boolean lockIsThis, final long siteId) {
    if (checkInside()) {
      try {
        if (RunConf.DEBUG) {
          final String fmt = "Store.afterIntrinsicLockRelease(%n\t\tlockObject=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, StoreDelegate.safeToString(lockObject), siteId));
        }
        /*
         * Check that the parameters are valid, gather needed information, and
         * put an event in the raw queue.
         */
        if (lockObject == null) {
          final String fmt = "intrinsic lock object cannot be null...instrumentation bug detected by Store.afterIntrinsicLockRelease(lockObject=%s, location=%s)";
          f_conf.logAProblem(String.format(fmt, StoreDelegate.safeToString(lockObject), siteId));
          return;
        }
        for (StoreListener l : f_listeners) {
          l.afterIntrinsicLockRelease(lockObject, lockIsThis, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void beforeUtilConcurrentLockAcquisitionAttempt(final Object lockObject, final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          /*
           * Implementation note: We are counting on the implementer of the
           * util.concurrent Lock object to not have a bad toString() method.
           */
          final String fmt = "Store.beforeUtilConcurrentLockAcquisitionAttempt(%n\t\tlockObject=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, lockObject, siteId));
        }
        for (StoreListener l : f_listeners) {
          l.beforeUtilConcurrentLockAcquisitionAttempt(lockObject, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void afterUtilConcurrentLockAcquisitionAttempt(final boolean gotTheLock, final Object lockObject,
      final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          /*
           * Implementation note: We are counting on the implementer of the
           * util.concurrent Lock object to not have a bad toString() method.
           */
          final String fmt = "Store.afterUtilConcurrentLockAcquisitionAttempt(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, gotTheLock ? "holding" : "failed-to-acquire", lockObject, siteId));
        }
        for (StoreListener l : f_listeners) {
          l.afterUtilConcurrentLockAcquisitionAttempt(gotTheLock, lockObject, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void afterUtilConcurrentLockReleaseAttempt(final boolean releasedTheLock, final Object lockObject,
      final long siteId) {
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          /*
           * Implementation note: We are counting on the implementer of the
           * util.concurrent Lock object to not have a bad toString() method.
           */
          final String fmt = "Store.afterUtilConcurrentLockReleaseAttempt(%n\t\t%s%n\t\tlockObject=%s%n\t\tlocation=%s)";
          f_conf.log(String.format(fmt, releasedTheLock ? "released" : "failed-to-release", lockObject, siteId));
        }
        for (StoreListener l : f_listeners) {
          l.afterUtilConcurrentLockReleaseAttempt(releasedTheLock, lockObject, siteId);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void instanceFieldInit(final Object receiver, final int fieldId, final Object value) {
    if (!StoreConfiguration.processFieldAccesses()) {
      return;
    }
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.instanceFieldInit%n\t\treceiver=%s%n\t\field=%s%n\t\tvalue=%s)";
          f_conf.log(String.format(fmt, StoreDelegate.safeToString(receiver), fieldId, StoreDelegate.safeToString(value)));
        }
        // Ignore null assignments
        if (value == null) {
          return;
        }
        for (StoreListener l : f_listeners) {
          l.instanceFieldInit(receiver, fieldId, value);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void staticFieldInit(final int fieldId, final Object value) {
    if (!StoreConfiguration.processFieldAccesses()) {
      return;
    }
    if (checkInside()) {
      try {
        if (f_conf.isDebug()) {
          final String fmt = "Store.instanceFieldInit%n\t\field=%s%n\t\tvalue=%s)";
          f_conf.log(String.format(fmt, fieldId, StoreDelegate.safeToString(value)));
        }
        // Ignore null assignments
        if (value == null) {
          return;
        }
        for (StoreListener l : f_listeners) {
          l.staticFieldInit(fieldId, value);
        }
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  /**
   * @param id
   *          the identifier for the triggered happens-before rule
   * @param typeName
   *          <code>null</code> if the method definitely matches a happens
   *          before method; otherwise it is the fully qualified type name of
   *          the type that we need to see if callee is assignable to.
   */
  public static void happensBeforeThread(final long nanoTime, final Thread callee, String id, final long siteId,
      final String typeName, final boolean isCallIn) {
    if (checkInside()) {
      try {
        if (typeName == null || Class.forName(typeName).isAssignableFrom(Thread.currentThread().getClass())) {
          for (StoreListener l : f_listeners) {
            l.happensBeforeThread(id, callee, siteId, typeName, nanoTime);
          }
        }
      } catch (ClassNotFoundException e) {
        f_conf.logAProblem(e.getMessage(), e);
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  /**
   * @param id
   *          the identifier for the triggered happens-before rule
   * @param typeName
   *          <code>null</code> if the method definitely matches a happens
   *          before method; otherwise it is the fully qualified type name of
   *          the type that we need to see if callee is assignable to.
   */
  public static void happensBeforeObject(final long nanoTime, final Object object, String id, final long siteId,
      final String typeName, final boolean isCallIn) {
    if (checkInside()) {
      try {
        if (typeName == null || Class.forName(typeName).isAssignableFrom(object.getClass())) {
          for (StoreListener l : f_listeners) {
            l.happensBeforeObject(id, object, siteId, typeName, nanoTime);
          }
        }
      } catch (ClassNotFoundException e) {
        f_conf.logAProblem(e.getMessage(), e);
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  /**
   * @param id
   *          the identifier for the triggered happens-before rule
   * @param typeName
   *          <code>null</code> if the method definitely matches a happens
   *          before method; otherwise it is the fully qualified type name of
   *          the type that we need to see if callee is assignable to.
   */
  public static void happensBeforeCollection(final long nanoTime, final Object item, final Object collection, String id,
      final long siteId, final String typeName, final boolean isCallIn) {
    if (checkInside()) {
      try {
        if (typeName == null || Class.forName(typeName).isAssignableFrom(collection.getClass())) {
          for (StoreListener l : f_listeners) {
            l.happensBeforeCollection(id, collection, item, siteId, typeName, nanoTime);
          }
        }
      } catch (ClassNotFoundException e) {
        f_conf.logAProblem(e.getMessage(), e);
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  public static void happensBeforeExecutor(final long nanoTime, final Object object, String id, final long siteId,
      final String typeName, final boolean isCallIn) {
    if (checkInside()) {
      try {
        if (typeName == null || Class.forName(typeName).isAssignableFrom(object.getClass())) {
          for (StoreListener l : f_listeners) {
            l.happensBeforeExecutor(id, object, siteId, typeName, nanoTime);
          }
        }
      } catch (ClassNotFoundException e) {
        f_conf.logAProblem(e.getMessage(), e);
      } finally {
        tl_withinStore.get().inside = false;
      }
    }
  }

  /**
   * Stops collection of events about the instrumented program. This method may
   * be called from within the following thread contexts:
   * <ul>
   * <li>A direct call from a program thread, i.e., a call was added to the
   * program code</li>
   * <li>The {@link Spy} thread if it detected the instrumented program
   * completed and only flashlight threads remain running.</li>
   * <li>A client handler thread created by the {@link Console} thread that was
   * told to shutdown flashlight via socket.</li>
   * <li>The thread created to run our shutdown hook.</li>
   * </ul>
   */
  public static void shutdown() {
    if (f_flashlightIsNotInitialized) {
      System.err.println("[Flashlight] !SERIOUS ERROR! " + "Store.shutdown() invoked " + "before the Store class is initialized");
      return;
    }
    /*
     * The below getAndSet(true) ensures that only one thread shuts down
     * Flashlight.
     */
    if (StoreDelegate.FL_OFF.getAndSet(true)) {
      return;
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

    f_gc.requestShutdown();

    /*
     * Shutdown store listeners
     */
    for (StoreListener l : f_listeners) {
      l.shutdown();
    }

    final long totalTime = System.nanoTime() - f_conf.getStartNanoTime();
    final StringBuilder sb = new StringBuilder(" (duration of collection was ");
    formatNanoTime(sb, totalTime);
    sb.append(')');
    final String duration = sb.toString();
    final long problemCount = f_conf.getProblemCount();
    if (problemCount < 1) {
      f_conf.log("collection shutdown" + duration);
    } else {
      f_conf.log("collection shutdown with " + problemCount + " problem(s) reported" + duration);
    }

    f_conf.logComplete();
  }

  /**
   * Get the phantom object reference for the given {@code Class} object. Cannot
   * use {@link Phantom#ofClass(Class)} directly because we need to make sure
   * the store is loaded and initialized before creating phantom objects.
   */
  public static ClassPhantomReference getClassPhantom(final Class<?> c) {
    return StoreDelegate.getClassPhantom(c);
  }

  public static ObjectPhantomReference getObjectPhantom(final Object o, final long id) {
    return StoreDelegate.getObjectPhantom(o, id);
  }

  /**
   * Return the id associated with the given field in the fields.txt file. This
   * method is used during object deserialization. DO NOT REMOVE
   *
   * @param clazz
   *          the fully-qualified class name.
   * @param field
   * @return a positive integer, or -1 if the field is not found
   */
  public static int getFieldId(final String clazz, final String field) {
    return f_conf.getFieldId(clazz, field);
  }

  private static void formatNanoTime(final StringBuilder sb, final long totalTime) {
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

  static class ShutdownCommand implements ConsoleCommand {
    private static final String STOP = "stop";

    @Override
    public String getDescription() {
      return "stop - shutdown the instrumentation";
    }

    @Override
    public String handle(final String command) {
      if (STOP.equals(command)) {
        shutdown();
        return "Flashlight instrumentation has shut down.";
      }
      return null;
    }
  }

  static class PingCommand implements ConsoleCommand {

    @Override
    public String handle(final String command) {
      if ("ping".equalsIgnoreCase(command)) {
        return String.format("Uptime: %d", System.currentTimeMillis() - f_conf.getStartTime().getTime());
      }
      return null;
    }

    @Override
    public String getDescription() {
      return "ping - displays instrumentation uptime";
    }
  }

}
