package com.surelogic.flashlight.common.prep;

import java.util.Iterator;

/**
 * Represents a lock trace in a tree of lock traces.
 * 
 * @author nathan
 * 
 */
class LockTrace {
    private final long id;
    private final long lock;
    private final long trace;
    private final LockType type;

    private final LockTrace parent;
    private LockTrace sibling;
    private LockTrace child;

    private LockTrace(long id, long lock, long trace, LockType type,
            LockTrace parent, LockTrace sibling) {
        this.id = id;
        this.lock = lock;
        this.trace = trace;
        this.type = type;
        this.parent = parent;
        this.sibling = sibling;
    }

    private LockTrace(long id, long lock, long trace, LockType type) {
        this(id, lock, trace, type, null, null);
    }

    /**
     * Create a new root node with no parent.
     * 
     * @param id
     * @param lock
     * @param trace
     * @return
     */
    static LockTrace newRootLockTrace(long id, long lock, long trace,
            LockType type) {
        return new LockTrace(id, lock, trace, type);
    }

    /**
     * Push a new lock trace node onto the tree.
     * 
     * @param id
     * @param lock
     * @param trace
     * @return
     */
    public LockTrace pushLockTrace(long id, long lock, long trace, LockType type) {
        child = new LockTrace(id, lock, trace, type, this, child);
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
        throw new IllegalStateException(
                "Could not find self in parent to expunge");
    }

    public long getId() {
        return id;
    }

    public long getLock() {
        return lock;
    }

    public long getTrace() {
        return trace;
    }

    public LockType getType() {
        return type;
    }

    public LockTrace getParent() {
        return parent;
    }

    boolean matches(long lock, long trace) {
        return this.lock == lock && this.trace == trace;
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
