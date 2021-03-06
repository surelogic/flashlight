package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.ReferenceType;

/**
 * @region private static ThreadPRInstanceMap
 * @lock ThreadPRInstanceMapLock is f_threadToPhantom protects
 *       ThreadPRInstanceMap
 */
public final class ThreadPhantomReference extends ObjectPhantomReference {

  /**
   * Map from a {@link Thread} instance to its associated
   * {@link ClassPhantomReference}. The key of this map is not prevented from
   * being garbage collected. Sadly, the weak identity hash map we are using is
   * implemented for JDK 1.4, however, that makes this class easy to back port.
   * 
   * @unique
   * @aggregate Instance into ThreadPRInstanceMap
   */
  private static final ConcurrentMap<Thread, ThreadPhantomReference> f_threadToPhantom = new ConcurrentReferenceHashMap<Thread, ThreadPhantomReference>(
      ReferenceType.WEAK, ReferenceType.STRONG, hasher);

  private static final RefFactory<Thread, ThreadPhantomReference> f_factory = new AbstractRefFactory<Thread, ThreadPhantomReference>() {
    public ThreadPhantomReference newReference(final Thread o, final ReferenceQueue q, final long id) {
      return new ThreadPhantomReference(o, q, id);
    }
  };

  private final String f_threadName;

  /**
   * Gets the name of this thread.
   * 
   * @return the name of this thread.
   */
  public String getName() {
    return f_threadName;
  }

  protected ThreadPhantomReference(final Thread referent, final ReferenceQueue q, final long id) {
    super(referent, q, id);
    f_threadName = referent.getName();
  }

  static ThreadPhantomReference getInstance(final Thread c, final ReferenceQueue q, final long id) {
    return getInstance(c, q, id, f_threadToPhantom, f_factory);
  }

  @Override
  void accept(final ObjectDefinition defn, final IdPhantomReferenceVisitor v) {
    v.visit(defn, this);
  }

  @Override
  public String toString() {
    return "[ThreadPhantom: id=" + getId() + " name=" + getName()
    /* + " type=" + getType() */ + "]";
  }
}
