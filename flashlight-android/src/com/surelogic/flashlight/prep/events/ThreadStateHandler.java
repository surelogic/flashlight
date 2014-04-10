package com.surelogic.flashlight.prep.events;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.util.LinkedList;

import com.surelogic.flashlight.common.LockId;

public class ThreadStateHandler implements EventHandler {

    // thread id -> thread info
    private final TLongObjectMap<ThreadState> activeThreads;
    private final ClassHandler classes;
    private final TraceHandler traces;

    private int peakThreads;
    private long startedThreads;

    ThreadStateHandler(ClassHandler classes, TraceHandler traces) {
        activeThreads = new TLongObjectHashMap<ThreadState>();
        this.classes = classes;
        this.traces = traces;
    }

    public int getThreadCount() {
        return activeThreads.size();
    }

    public int getPeakThreadCount() {
        return peakThreads;
    }

    public void resetPeakThreadCount() {
        peakThreads = getThreadCount();

    }

    public long getTotalStartedThreadCount() {
        return startedThreads;
    }

    public long[] getAllThreadIds() {
        return activeThreads.keys();
    }

    public ThreadState getThreadState(long id) {
        return getState(id);
    }

    public ThreadState[] getThreadState(long[] ids) {
        ThreadState[] arr = new ThreadState[ids.length];
        for (int i = 0; i < ids.length; i++) {
            arr[i] = getThreadState(ids[i]);
        }
        return arr;
    }

    @Override
    public void handle(Event e) {
        ThreadState inThread = null;
        if (e instanceof TracedEvent) {
            TracedEvent te = (TracedEvent) e;
            inThread = getState(te);
            inThread.lastTrace = te.getTrace();
        }
        switch (e.getEventType()) {
        case THREADDEFINITION:
            ThreadDefinition td = (ThreadDefinition) e;
            getState(td.getId()).setName(td.getName());
            peakThreads = Math.max(peakThreads, activeThreads.size());
            startedThreads++;
            break;
        case GARBAGECOLLECTEDOBJECT:
            GCObject gc = (GCObject) e;
            long id = gc.getId();
            activeThreads.remove(id);
            break;
        case AFTERINTRINSICLOCKACQUISITION:
        case AFTERUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            final LockEvent lea = (LockEvent) e;
            if (lea.isSuccess()) {
                inThread.acquireLock(lea);
            }
            break;
        case AFTERINTRINSICLOCKRELEASE:
        case AFTERUTILCONCURRENTLOCKRELEASEATTEMPT:
            final LockEvent ler = (LockEvent) e;
            if (ler.isSuccess()) {
                inThread.releaseLock(ler);
            }
            break;
        case BEFOREINTRINSICLOCKWAIT:
            final LockEvent lebw = (LockEvent) e;
            inThread.waitLock(lebw);
            break;
        case AFTERINTRINSICLOCKWAIT:
            final LockEvent leaw = (LockEvent) e;
            inThread.unwaitLock(leaw);
            break;
        case BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT:
        case BEFOREINTRINSICLOCKACQUISITION:
            final LockEvent leba = (LockEvent) e;
            inThread.beforeLock(leba);
            break;
        default:
            break;
        }

    }

    ThreadState getState(TracedEvent e) {
        return getState(e.getInThread());
    }

    ThreadState getState(long thread) {
        ThreadState state = activeThreads.get(thread);
        if (state == null) {
            state = new ThreadState(thread);
            activeThreads.put(thread, state);
        }
        return state;
    }

    static class Trace {
        String classname;
        String methodName;
        String fileName;
        int lineNumber;
        boolean nativeMethod;
    }

    enum Status {
        IDLE, BLOCKING, WAITING;

        Status beforeAcquisition() {
            if (this == IDLE) {
                return BLOCKING;
            } else {
                throw new IllegalStateException();
            }
        }

        Status afterAcquisition() {
            if (this == BLOCKING) {
                return IDLE;
            } else {
                throw new IllegalStateException();
            }
        }

        Status waitObject() {
            if (this == IDLE) {
                return WAITING;
            } else {
                throw new IllegalStateException();
            }
        }

        Status unwaitObject() {
            if (this == WAITING) {
                return IDLE;
            } else {
                throw new IllegalStateException();
            }
        }

        Thread.State toState() {
            switch (this) {
            case BLOCKING:
                return State.BLOCKED;
            case IDLE:
                return State.RUNNABLE;
            case WAITING:
                return State.WAITING;
            default:
                throw new IllegalStateException("Unreachable code");
            }
        }
    }

    public class ThreadState {

        private final long id;
        private String threadName;

        private long lastTrace;
        private long lastEventNanos;
        private long blockedTime;
        private long blockedCount;
        private long waitedTime;
        private long waitedCount;

        private Status status;

        public ThreadState(long id) {
            this.id = id;
        }

        LinkedList<LockId> locks;

        void beforeLock(LockEvent le) {
            lastEventNanos = le.getNanoTime();
            status = status.beforeAcquisition();
        }

        void acquireLock(LockEvent le) {
            blockedTime = le.getNanoTime() - lastEventNanos;
            blockedCount++;
            locks.push(le.getLockId());
            status = status.afterAcquisition();
        }

        void releaseLock(LockEvent le) {
            locks.remove(le.getLockId());
        }

        void waitLock(LockEvent le) {
            waitedTime = le.getNanoTime() - lastEventNanos;
            waitedCount++;
            lastEventNanos = le.getNanoTime();
            status = status.waitObject();
        }

        void unwaitLock(LockEvent le) {
            status = status.unwaitObject();
        }

        StackTraceElement[] getTrace() {
            return traces.trace(lastTrace);
        }

        public long getId() {
            return id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (id ^ id >>> 32);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ThreadState other = (ThreadState) obj;
            if (id != other.id) {
                return false;
            }
            return true;
        }

        public long getBlockedCount() {
            return blockedCount;
        }

        public long getBlockedTime() {
            return blockedTime;
        }

        public LockInfo getLockInfo() {
            if (locks.size() > 0) {
                LockId first = locks.getFirst();
                return new LockInfo(classes.getClassNameFromObject(first
                        .getId()),
                        (first.getId() + first.getType().toString()).hashCode());
            }
            return null;
        }

        public String getName() {
            return threadName;
        }

        public void setName(String name) {
            threadName = name;
        }

        public State getState() {
            return status.toState();
        }

        public long getWaitedCount() {
            return waitedCount;
        }

        public long getWaitedTime() {
            return waitedTime;
        }
    }

}
