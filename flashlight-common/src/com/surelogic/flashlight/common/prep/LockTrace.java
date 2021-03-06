package com.surelogic.flashlight.common.prep;

import java.util.Iterator;

import com.surelogic.flashlight.common.LockId;
import com.surelogic.flashlight.common.LockType;

/**
 * Represents a lock trace in a tree of lock traces.
 */
class LockTrace {
  final long id;
  final LockId lock;
  final long trace;

  final LockTrace parent;
  LockTrace sibling;
  LockTrace child;

  private LockTrace(long id, LockId lock, long trace, LockTrace parent, LockTrace sibling) {
    this.id = id;
    this.lock = lock;
    this.trace = trace;
    this.parent = parent;
    this.sibling = sibling;
  }

  private LockTrace(long id, LockId lock, long trace) {
    this(id, lock, trace, null, null);
  }

  /**
   * Create a new root node with no parent.
   * 
   * @param id
   * @param lock
   * @param trace
   * @return
   */
  static LockTrace newRootLockTrace(long id, LockId lock, long trace) {
    return new LockTrace(id, lock, trace);
  }

  /**
   * Push a new lock trace node onto the tree.
   * 
   * @param id
   * @param lock
   * @param trace
   * @return
   */
  public LockTrace pushLockTrace(long id, LockId lock, long trace) {
    child = new LockTrace(id, lock, trace, this, child);
    return child;
  }

  /**
   * Remove this lock trace node and all of its children.from the lock trace
   * tree.
   */
  public void expunge() {
    if (parent == null) {
      return;
    }
    if (parent.child == this) {
      parent.child = sibling;
    }
    for (LockTrace sib : parent.children()) {
      if (sib.sibling == this) {
        sib.sibling = sibling;
        return;
      }
    }
    throw new IllegalStateException("Could not find self in parent to expunge");
  }

  public long getId() {
    return id;
  }

  public LockId getLockNode() {
    return lock;
  }

  public long getLockId() {
    return lock.getId();
  }

  public long getTrace() {
    return trace;
  }

  public LockType getType() {
    return lock.getType();
  }

  public LockTrace getParent() {
    return parent;
  }

  boolean matches(LockId lock, long trace) {
    return this.lock.equals(lock) && this.trace == trace;
  }

  public Iterable<LockTrace> children() {
    return new Iterable<LockTrace>() {

      @Override
      public Iterator<LockTrace> iterator() {
        return new SibIter(child);
      }
    };

  }

  private static class SibIter implements Iterator<LockTrace> {
    LockTrace trace;

    SibIter(LockTrace first) {
      trace = first;
    }

    @Override
    public boolean hasNext() {
      return trace != null;
    }

    @Override
    public LockTrace next() {
      LockTrace ret = trace;
      trace = trace.sibling;
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

}
