package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.rewriter.runtime.IIdObject;
import com.surelogic._flashlight.rewriter.runtime.IdObject;

public abstract class IdPhantomReference extends PhantomReference {
  static final ConcurrentReferenceHashMap.Hasher hasher = new ConcurrentReferenceHashMap.Hasher() {

    public int hashCode(final Object o) {
      if (o instanceof IIdObject) {
        return ((IIdObject) o).identity$HashCode();
      } else {
        return System.identityHashCode(o);
      }
    }

    public boolean useReferenceEquality() {
      return true;
    }
  };

  private final long f_id;

  public long getId() {
    return f_id;
  }

  /**
   * Sometimes duplicates occur, we mark them and ignore them.
   */
  private volatile boolean f_isDuplicate = false;

  void setIsDuplicate() {
    f_isDuplicate = true;
  }

  boolean isDuplicate() {
    return f_isDuplicate;
  }

  protected IdPhantomReference(final Object referent, final ReferenceQueue q) {
    this(referent, q, IdObject.getNewId());
  }

  @SuppressWarnings("unchecked")
  protected IdPhantomReference(final Object referent, final ReferenceQueue q, final long id) {
    super(referent, q);
    f_id = id == Phantom.NO_PREASSIGNED_ID ? IdObject.getNewId() : id;
  }

  /**
   * Use a thread-safe set to hold our observers.
   */
  static final Set<IdPhantomReferenceCreationObserver> f_observers = new CopyOnWriteArraySet<IdPhantomReferenceCreationObserver>();

  static class Unnotified {
    final ClassPhantomReference type;
    final IdPhantomReference ref;

    Unnotified(final ClassPhantomReference t, final IdPhantomReference r) {
      type = t;
      ref = r;
    }
  }

  static List<Unnotified> unnotified = new ArrayList<Unnotified>();

  static void addObserver(final IdPhantomReferenceCreationObserver o) {
    f_observers.add(o);
    List<Unnotified> refs = null;
    synchronized (IdPhantomReference.class) {
      if (unnotified != null) {
        refs = unnotified;
        unnotified = null;
      }
    }
    if (refs != null) {
      for (final Unnotified u : refs) {
        u.ref.notifyObservers(u.type);
      }
    }
  }

  static void removeObserver(final IdPhantomReferenceCreationObserver o) {
    f_observers.remove(o);
  }

  protected void notifyObservers(final ClassPhantomReference type) {
    if (f_observers.isEmpty()) {
      synchronized (IdPhantomReference.class) {
        if (unnotified != null) {
          unnotified.add(new Unnotified(type, this));
        }
        return;
      }
    }
    for (final IdPhantomReferenceCreationObserver o : f_observers) {
      o.notify(type, this);
    }
  }

  interface RefFactory<K, V extends IdPhantomReference> {
    V newReference(K o, ReferenceQueue q, long id);

    V removeSpecialId(final K o);

    void recordSpecialId(final K o, final V pr);
  }

  static class RefNode<K, V extends IdPhantomReference> {
    final K obj;
    final V phantom;
    RefNode<K, V> next;

    RefNode(final K o, final V pr, final RefNode<K, V> next) {
      obj = o;
      phantom = pr;
      this.next = next;
    }
  }

  static abstract class AbstractRefFactory<K, V extends IdPhantomReference> implements RefFactory<K, V> {
    RefNode<K, V> root;

    public synchronized final V removeSpecialId(final K o) {
      RefNode<K, V> last = null;
      RefNode<K, V> here = root;
      while (here != null) {
        if (here.obj == o) {
          // Remove entry
          if (last == null) {
            root = here.next;
          } else {
            last.next = here.next;
          }
          return here.phantom;
        }
        last = here;
        here = here.next;
      }
      return null;
    }

    public synchronized final void recordSpecialId(final K o, final V pr) {
      final RefNode<K, V> n = new RefNode<K, V>(o, pr, root);
      root = n;
    }
  }

  static <K, V extends IdPhantomReference> V getInstance(final K o, final ReferenceQueue q, final long id,
      final ConcurrentMap<K, V> map, final RefFactory<K, V> factory) {
    boolean phantomExisted;
    V pr;
    if (id != Phantom.NO_PREASSIGNED_ID) {
      pr = factory.removeSpecialId(o);
      if (pr != null) {
        // Already allocated
        return pr;
      }
      // Must be new
      phantomExisted = false;
      pr = factory.newReference(o, q, id);

    } else {
      if (o instanceof IIdObject) {
        final IIdObject ido = (IIdObject) o;
        @SuppressWarnings("unchecked")
        final V tempPr = (V) ido.flPhantom$Reference();
        pr = tempPr;
        if (pr != null) {
          return pr;
        }
        /*
         * If it gets here, it's because a superclass called an overridden
         * method and the code below will allocate a phantom reference
         */
        pr = factory.newReference(o, q, id);
        factory.recordSpecialId(o, pr);
        notifyOnCreation(o, pr, q);
        return pr;
      } else {
        pr = map.get(o);
      }
      phantomExisted = pr != null;
      if (!phantomExisted) {
        final V pr2 = factory.newReference(o, q, id);
        pr = map.putIfAbsent(o, pr2);
        if (pr != null) {
          // Created an extra phantom, so kill the extra
          phantomExisted = true;
          pr2.setIsDuplicate();
        } else {
          pr = pr2;
        }
      }
    }
    /*
     * We want to release the lock before we notify observers because, well, who
     * knows what they will do and we wouldn't want to deadlock.
     */
    if (!phantomExisted) {
      notifyOnCreation(o, pr, q);
    }
    return pr;
  }

  private static <K, V extends IdPhantomReference> void notifyOnCreation(final K o, final V pr, final ReferenceQueue q) {
    final ClassPhantomReference type = o instanceof Class ? null : ClassPhantomReference.getInstance(o.getClass(), q);
    pr.notifyObservers(type);
  }

  /**
   * Accepts this phantom reference on the passed visitor.
   * 
   * @param v
   *          the visitor for this phantom reference.
   */
  abstract void accept(final ObjectDefinition defn, final IdPhantomReferenceVisitor v);
}
