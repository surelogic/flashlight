package com.surelogic.flashlight.prep.events;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;
import java.util.LinkedList;

import com.surelogic.flashlight.common.LockId;

public class ThreadStateHandler implements EventHandler {

    // thread id -> thread info
    private final TLongObjectMap<ThreadState> activeThreads;
    private final ClassHandler classes;
    private int peakThreads;
    private long startedThreads;

    ThreadStateHandler(ClassHandler classes) {
        activeThreads = new TLongObjectHashMap<ThreadState>();
        this.classes = classes;
    }

    public int getThreadCount() {
        return activeThreads.size();
    }

    public int getPeakThreadCount() {
        return peakThreads;
    }

    public long getTotalStartedThreadCount() {
        return startedThreads;
    }

    // We can't implement this one right now
    public int getDaemonThreadCount() {
        return -1;
    }

    public long[] getAllThreadIds() {
        return activeThreads.keys();
    }

    public ThreadInfo getThreadInfo(long id) {
        return getState(id).toThreadInfo();
    }

    public ThreadInfo[] getThreadInfo(long[] ids) {
        ThreadInfo[] arr = new ThreadInfo[ids.length];
        for (int i = 0; i < ids.length; i++) {
            arr[i] = getThreadInfo(ids[i]);
        }
        return arr;
    }

    @Override
    public void handle(Event e) {
        switch (e.getEventType()) {
        case THREADDEFINITION:
            ThreadDefinition td = (ThreadDefinition) e;
            ThreadState state = new ThreadState(td.getId(), td.getName());
            activeThreads.put(state.getId(), state);
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
                getState(lea).acquireLock(lea);
            }
            break;
        case AFTERINTRINSICLOCKRELEASE:
        case AFTERUTILCONCURRENTLOCKRELEASEATTEMPT:
            final LockEvent ler = (LockEvent) e;
            if (ler.isSuccess()) {
                getState(ler).releaseLock(ler);
            }
            break;
        case BEFOREINTRINSICLOCKWAIT:
            final LockEvent lebw = (LockEvent) e;
            getState(lebw).waitLock(lebw);
            break;
        case AFTERINTRINSICLOCKWAIT:
            final LockEvent leaw = (LockEvent) e;
            getState(leaw).unwaitLock(leaw);
            break;
        case BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT:
        case BEFOREINTRINSICLOCKACQUISITION:
            final LockEvent leba = (LockEvent) e;
            getState(leba).beforeLock(leba);
            break;
        default:
            break;

        }

    }

    ThreadState getState(TracedEvent e) {
        return getState(e.getInThread());
    }

    ThreadState getState(long thread) {
        return activeThreads.get(thread);
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
    }

    private class ThreadState {

        private long lastEvent;

        public ThreadState(long id, String name) {
            this.id = id;
            threadName = name;
        }

        public ThreadInfo toThreadInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        LinkedList<LockId> locks;

        void beforeLock(LockEvent le) {
            lastEvent = le.getNanoTime();
            status = status.beforeAcquisition();
        }

        void acquireLock(LockEvent le) {
            blockedTime = le.getNanoTime() - lastEvent;
            locks.push(le.getLockId());
            status = status.afterAcquisition();
        }

        void releaseLock(LockEvent le) {
            locks.remove(le.getLockId());
        }

        void waitLock(LockEvent le) {
            blockedTime = le.getNanoTime() - lastEvent;
            lastEvent = le.getNanoTime();
            status = status.waitObject();
        }

        void unwaitLock(LockEvent le) {
            status = status.unwaitObject();
        }

        long id;
        String threadName;
        boolean suspended;
        boolean inNative;
        long blockedCount;
        long blockedTime;
        long waitedCount;
        long waitedTime;
        LockInfo lockInfo;
        Trace stackTrace;
        Status status;

        // TODO lockedMonitors[]
        // TODO lockedSynchronizers[]

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

    }

}
