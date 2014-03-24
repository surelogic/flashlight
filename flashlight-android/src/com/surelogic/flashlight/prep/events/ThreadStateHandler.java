package com.surelogic.flashlight.prep.events;

import java.lang.management.LockInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ThreadStateHandler implements EventHandler {
    private static final int GC_BUFFER_SIZE = 500;

    private final Map<Long, ThreadState> activeThreads;
    private final ArrayList<Long> toGc;

    ThreadStateHandler() {
        activeThreads = new HashMap<Long, ThreadStateHandler.ThreadState>();
        toGc = new ArrayList<Long>(500);
    }

    @Override
    public void handle(Event e) {
        switch (e.getEventType()) {
        case THREADDEFINITION:
            ThreadDefinition td = (ThreadDefinition) e;
            ThreadState state = new ThreadState(td.getId(), td.getName());
            activeThreads.put(state.getId(), state);
            break;
        case GARBAGECOLLECTEDOBJECT:
            GCObject gc = (GCObject) e;
            toGc.add(gc.getId());
            if (toGc.size() >= GC_BUFFER_SIZE) {
                for (Long id : toGc) {
                    activeThreads.remove(id);
                    for (ThreadState ts : activeThreads.values()) {
                        List<Long> locks = ts.locks;
                        boolean more = true;
                        while (more) {
                            more = locks.remove(id);
                        }
                    }
                }
                toGc.clear();
            }
            break;
        case FIELDREAD:
        case FIELDWRITE:
            break;
        case AFTERINTRINSICLOCKACQUISITION:
        case AFTERUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            final LockEvent lea = (LockEvent) e;
            if (lea.isSuccess()) {
                activeThreads.get(lea.getInThread()).acquireLock(lea.getLock());
            }
            break;
        case AFTERINTRINSICLOCKRELEASE:
        case AFTERUTILCONCURRENTLOCKRELEASEATTEMPT:
            final LockEvent ler = (LockEvent) e;
            if (ler.isSuccess()) {
                activeThreads.get(ler.getInThread()).releaseLock(ler.getLock());
            }
            break;
        case AFTERINTRINSICLOCKWAIT:
        case BEFOREINTRINSICLOCKWAIT:
        case BEFOREINTRINSICLOCKACQUISITION:
        case BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            // Do nothing
            break;
        case CHECKPOINT:
            break;
        case CLASSDEFINITION:
            break;
        case ENVIRONMENT:
            break;
        case FIELDASSIGNMENT:
            break;
        case FIELDDEFINITION:
            break;
        case FINAL:
            break;
        case FLASHLIGHT:
            break;
        case HAPPENSBEFORECOLLECTION:
            break;
        case HAPPENSBEFOREOBJECT:
            break;
        case HAPPENSBEFORETHREAD:
            break;
        case INDIRECTACCESS:
            break;
        case OBJECTDEFINITION:
            break;
        case READWRITELOCK:
            break;
        case SELECTEDPACKAGE:
            break;
        case SINGLETHREADEFIELD:
            break;
        case STATICCALLLOCATION:
            break;
        case TIME:
            break;
        case TRACENODE:
            break;
        default:
            break;

        }

    }

    static class Trace {
        String classname;
        String methodName;
        String fileName;
        int lineNumber;
        boolean nativeMethod;
    }

    class ThreadState {
        public ThreadState(long id, String name) {
            this.id = id;
            threadName = name;
        }

        LinkedList<Long> locks;

        void acquireLock(Long lockId) {
            locks.push(lockId);
        }

        void releaseLock(Long lockId) {
            locks.remove(lockId);
        }

        long id;
        String threadName;
        String threadState;
        boolean suspended;
        boolean inNative;
        long blockedCount;
        long blockedTime;
        long waitedCount;
        long waitedTime;
        LockInfo lockInfo;
        Trace stackTrace;

        // TODO lockedMonitors[]
        // TODO lockedSynchronizers[]

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getThreadName() {
            return threadName;
        }

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }

        public String getThreadState() {
            return threadState;
        }

        public void setThreadState(String threadState) {
            this.threadState = threadState;
        }

        public boolean isSuspended() {
            return suspended;
        }

        public void setSuspended(boolean suspended) {
            this.suspended = suspended;
        }

        public boolean isInNative() {
            return inNative;
        }

        public void setInNative(boolean inNative) {
            this.inNative = inNative;
        }

        public long getBlockedCount() {
            return blockedCount;
        }

        public void setBlockedCount(long blockedCount) {
            this.blockedCount = blockedCount;
        }

        public long getBlockedTime() {
            return blockedTime;
        }

        public void setBlockedTime(long blockedTime) {
            this.blockedTime = blockedTime;
        }

        public long getWaitedCount() {
            return waitedCount;
        }

        public void setWaitedCount(long waitedCount) {
            this.waitedCount = waitedCount;
        }

        public long getWaitedTime() {
            return waitedTime;
        }

        public void setWaitedTime(long waitedTime) {
            this.waitedTime = waitedTime;
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
