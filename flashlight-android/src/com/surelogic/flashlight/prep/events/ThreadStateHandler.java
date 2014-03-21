package com.surelogic.flashlight.prep.events;

import java.util.Map;

public class ThreadStateHandler implements EventHandler {

    Map<Long, ThreadState> activeThreads;

    @Override
    public void handle(Event e) {
        switch (e.type()) {
        case THREADDEFINITION:
            ThreadDefinition td = (ThreadDefinition) e;
            ThreadState state = new ThreadState(td.getId(), td.getName());
            activeThreads.put(state.getId(), state);
            break;
        case GARBAGECOLLECTEDOBJECT:
            GCObject gc = (GCObject) e;
            activeThreads.remove(gc.getId());
            break;
        case FIELDREAD:
        case FIELDWRITE:

            break;
        }

    }

    static class LockState {

    }

    static class Trace {
        String classname;
        String methodName;
        String fileName;
        int lineNumber;
        boolean nativeMethod;
    }

    static class ThreadState {
        public ThreadState(long id, String name) {
            this.id = id;
            threadName = name;
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
        LockState lockInfo;
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

    }

}
