package com.surelogic.flashlight.prep.events;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.surelogic.flashlight.prep.events.ThreadStateHandler.ThreadState;

public class FLThreadMxBean implements ThreadMXBean {

    private final ThreadStateHandler handler;

    public FLThreadMxBean(ThreadStateHandler handler) {
        super();
        this.handler = handler;
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName.getInstance("java.lang:type=Threading");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the current number of live threads including both daemon and
     * non-daemon threads.
     *
     * @return the current number of live threads.
     */
    @Override
    public int getThreadCount() {
        return handler.getThreadCount();
    }

    /**
     * Returns the peak live thread count since the Java virtual machine started
     * or peak was reset.
     *
     * @return the peak live thread count.
     */
    @Override
    public int getPeakThreadCount() {
        return handler.getPeakThreadCount();
    }

    /**
     * Returns the total number of threads created and also started since the
     * Java virtual machine started.
     *
     * @return the total number of threads started.
     */
    @Override
    public long getTotalStartedThreadCount() {
        return handler.getTotalStartedThreadCount();
    }

    /**
     * Returns the current number of live daemon threads.
     *
     * @return the current number of live daemon threads.
     */
    @Override
    public int getDaemonThreadCount() {
        return -1;
    }

    /**
     * Returns all live thread IDs. Some threads included in the returned array
     * may have been terminated when this method returns.
     *
     * @return an array of <tt>long</tt>, each is a thread ID.
     *
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     */
    @Override
    public long[] getAllThreadIds() {
        return handler.getAllThreadIds();
    }

    /**
     * Returns the thread info for a thread of the specified <tt>id</tt> with no
     * stack trace. This method is equivalent to calling: <blockquote>
     * {@link #getThreadInfo(long, int) getThreadInfo(id, 0);} </blockquote>
     *
     * <p>
     * This method returns a <tt>ThreadInfo</tt> object representing the thread
     * information for the thread of the specified ID. The stack trace, locked
     * monitors, and locked synchronizers in the returned <tt>ThreadInfo</tt>
     * object will be empty.
     *
     * If a thread of the given ID is not alive or does not exist, this method
     * will return <tt>null</tt>. A thread is alive if it has been started and
     * has not yet died.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>ThreadInfo</tt> is <tt>CompositeData</tt> with
     * attributes as specified in the {@link ThreadInfo#from ThreadInfo.from}
     * method.
     *
     * @param id
     *            the thread ID of the thread. Must be positive.
     *
     * @return a {@link ThreadInfo} object for the thread of the given ID with
     *         no stack trace, no locked monitor and no synchronizer info;
     *         <tt>null</tt> if the thread of the given ID is not alive or it
     *         does not exist.
     *
     * @throws IllegalArgumentException
     *             if <tt>id &lt= 0</tt>.
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     */
    @Override
    public ThreadInfo getThreadInfo(long id) {
        return toThreadInfo(handler.getThreadState(id));
    }

    /**
     * Returns the thread info for each thread whose ID is in the input array
     * <tt>ids</tt> with no stack trace. This method is equivalent to calling:
     * <blockquote>
     *
     * <pre>
     *   {@link #getThreadInfo(long[], int) getThreadInfo}(ids, 0);
     * </pre>
     *
     * </blockquote>
     *
     * <p>
     * This method returns an array of the <tt>ThreadInfo</tt> objects. The
     * stack trace, locked monitors, and locked synchronizers in each
     * <tt>ThreadInfo</tt> object will be empty.
     *
     * If a thread of a given ID is not alive or does not exist, the
     * corresponding element in the returned array will contain <tt>null</tt>. A
     * thread is alive if it has been started and has not yet died.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>ThreadInfo</tt> is <tt>CompositeData</tt> with
     * attributes as specified in the {@link ThreadInfo#from ThreadInfo.from}
     * method.
     *
     * @param ids
     *            an array of thread IDs.
     * @return an array of the {@link ThreadInfo} objects, each containing
     *         information about a thread whose ID is in the corresponding
     *         element of the input array of IDs with no stack trace, no locked
     *         monitor and no synchronizer info.
     *
     * @throws IllegalArgumentException
     *             if any element in the input array <tt>ids</tt> is
     *             <tt>&lt= 0</tt>.
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     */
    @Override
    public ThreadInfo[] getThreadInfo(long[] ids) {
        ThreadState[] threadState = handler.getThreadState(ids);
        ThreadInfo[] infos = new ThreadInfo[threadState.length];
        for (int i = 0; i < threadState.length; i++) {
            infos[i] = toThreadInfo(threadState[i]);
        }
        return infos;
    }

    /**
     * Returns a thread info for a thread of the specified <tt>id</tt>, with
     * stack trace of a specified number of stack trace elements. The
     * <tt>maxDepth</tt> parameter indicates the maximum number of
     * {@link StackTraceElement} to be retrieved from the stack trace. If
     * <tt>maxDepth == Integer.MAX_VALUE</tt>, the entire stack trace of the
     * thread will be dumped. If <tt>maxDepth == 0</tt>, no stack trace of the
     * thread will be dumped. This method does not obtain the locked monitors
     * and locked synchronizers of the thread.
     * <p>
     * When the Java virtual machine has no stack trace information about a
     * thread or <tt>maxDepth == 0</tt>, the stack trace in the
     * <tt>ThreadInfo</tt> object will be an empty array of
     * <tt>StackTraceElement</tt>.
     *
     * <p>
     * If a thread of the given ID is not alive or does not exist, this method
     * will return <tt>null</tt>. A thread is alive if it has been started and
     * has not yet died.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>ThreadInfo</tt> is <tt>CompositeData</tt> with
     * attributes as specified in the {@link ThreadInfo#from ThreadInfo.from}
     * method.
     *
     * @param id
     *            the thread ID of the thread. Must be positive.
     * @param maxDepth
     *            the maximum number of entries in the stack trace to be dumped.
     *            <tt>Integer.MAX_VALUE</tt> could be used to request the entire
     *            stack to be dumped.
     *
     * @return a {@link ThreadInfo} of the thread of the given ID with no locked
     *         monitor and synchronizer info. <tt>null</tt> if the thread of the
     *         given ID is not alive or it does not exist.
     *
     * @throws IllegalArgumentException
     *             if <tt>id &lt= 0</tt>.
     * @throws IllegalArgumentException
     *             if <tt>maxDepth is negative</tt>.
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     *
     */
    @Override
    public ThreadInfo getThreadInfo(long id, int maxDepth) {
        return toThreadInfo(handler.getThreadState(id));
    }

    /**
     * Returns the thread info for each thread whose ID is in the input array
     * <tt>ids</tt>, with stack trace of a specified number of stack trace
     * elements. The <tt>maxDepth</tt> parameter indicates the maximum number of
     * {@link StackTraceElement} to be retrieved from the stack trace. If
     * <tt>maxDepth == Integer.MAX_VALUE</tt>, the entire stack trace of the
     * thread will be dumped. If <tt>maxDepth == 0</tt>, no stack trace of the
     * thread will be dumped. This method does not obtain the locked monitors
     * and locked synchronizers of the threads.
     * <p>
     * When the Java virtual machine has no stack trace information about a
     * thread or <tt>maxDepth == 0</tt>, the stack trace in the
     * <tt>ThreadInfo</tt> object will be an empty array of
     * <tt>StackTraceElement</tt>.
     * <p>
     * This method returns an array of the <tt>ThreadInfo</tt> objects, each is
     * the thread information about the thread with the same index as in the
     * <tt>ids</tt> array. If a thread of the given ID is not alive or does not
     * exist, <tt>null</tt> will be set in the corresponding element in the
     * returned array. A thread is alive if it has been started and has not yet
     * died.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>ThreadInfo</tt> is <tt>CompositeData</tt> with
     * attributes as specified in the {@link ThreadInfo#from ThreadInfo.from}
     * method.
     *
     * @param ids
     *            an array of thread IDs
     * @param maxDepth
     *            the maximum number of entries in the stack trace to be dumped.
     *            <tt>Integer.MAX_VALUE</tt> could be used to request the entire
     *            stack to be dumped.
     *
     * @return an array of the {@link ThreadInfo} objects, each containing
     *         information about a thread whose ID is in the corresponding
     *         element of the input array of IDs with no locked monitor and
     *         synchronizer info.
     *
     * @throws IllegalArgumentException
     *             if <tt>maxDepth is negative</tt>.
     * @throws IllegalArgumentException
     *             if any element in the input array <tt>ids</tt> is
     *             <tt>&lt= 0</tt>.
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     *
     */
    @Override
    public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
        ThreadState[] threadState = handler.getThreadState(ids);
        ThreadInfo[] infos = new ThreadInfo[threadState.length];
        for (int i = 0; i < threadState.length; i++) {
            infos[i] = toThreadInfo(threadState[i]);
        }
        return infos;
    }

    /**
     * Tests if the Java virtual machine supports thread contention monitoring.
     *
     * @return <tt>true</tt> if the Java virtual machine supports thread
     *         contention monitoring; <tt>false</tt> otherwise.
     */
    @Override
    public boolean isThreadContentionMonitoringSupported() {
        return true;
    }

    /**
     * Tests if thread contention monitoring is enabled.
     *
     * @return <tt>true</tt> if thread contention monitoring is enabled;
     *         <tt>false</tt> otherwise.
     *
     * @throws java.lang.UnsupportedOperationException
     *             if the Java virtual machine does not support thread
     *             contention monitoring.
     *
     * @see #isThreadContentionMonitoringSupported
     */
    @Override
    public boolean isThreadContentionMonitoringEnabled() {
        return true;
    }

    /**
     * Enables or disables thread contention monitoring. Thread contention
     * monitoring is disabled by default.
     *
     * @param enable
     *            <tt>true</tt> to enable; <tt>false</tt> to disable.
     *
     * @throws java.lang.UnsupportedOperationException
     *             if the Java virtual machine does not support thread
     *             contention monitoring.
     *
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("control").
     *
     * @see #isThreadContentionMonitoringSupported
     */
    @Override
    public void setThreadContentionMonitoringEnabled(boolean enable) {
        // FIXME
    }

    @Override
    public long getCurrentThreadCpuTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCurrentThreadUserTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getThreadCpuTime(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getThreadUserTime(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isThreadCpuTimeSupported() {
        return false;
    }

    @Override
    public boolean isCurrentThreadCpuTimeSupported() {
        return false;
    }

    @Override
    public boolean isThreadCpuTimeEnabled() {
        return false;
    }

    @Override
    public void setThreadCpuTimeEnabled(boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] findMonitorDeadlockedThreads() {
        return handler.getDeadlockedThreads();
    }

    /**
     * Resets the peak thread count to the current number of live threads.
     *
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("control").
     *
     * @see #getPeakThreadCount
     * @see #getThreadCount
     */
    @Override
    public void resetPeakThreadCount() {
        handler.resetPeakThreadCount();
    }

    @Override
    public long[] findDeadlockedThreads() {
        return handler.getDeadlockedThreads();
    }

    /**
     * Tests if the Java virtual machine supports monitoring of object monitor
     * usage.
     *
     * @return <tt>true</tt> if the Java virtual machine supports monitoring of
     *         object monitor usage; <tt>false</tt> otherwise.
     *
     * @see #dumpAllThreads
     * @since 1.6
     */
    @Override
    public boolean isObjectMonitorUsageSupported() {
        return true;
    }

    /**
     * Tests if the Java virtual machine supports monitoring of <a
     * href="LockInfo.html#OwnableSynchronizer"> ownable synchronizer</a> usage.
     *
     * @return <tt>true</tt> if the Java virtual machine supports monitoring of
     *         ownable synchronizer usage; <tt>false</tt> otherwise.
     *
     * @see #dumpAllThreads
     * @since 1.6
     */
    @Override
    public boolean isSynchronizerUsageSupported() {
        return true;
    }

    /**
     * Returns the thread info for each thread whose ID is in the input array
     * <tt>ids</tt>, with stack trace and synchronization information.
     *
     * <p>
     * This method obtains a snapshot of the thread information for each thread
     * including:
     * <ul>
     * <li>the entire stack trace,</li>
     * <li>the object monitors currently locked by the thread if
     * <tt>lockedMonitors</tt> is <tt>true</tt>, and</li>
     * <li>the <a href="LockInfo.html#OwnableSynchronizer"> ownable
     * synchronizers</a> currently locked by the thread if
     * <tt>lockedSynchronizers</tt> is <tt>true</tt>.</li>
     * </ul>
     * <p>
     * This method returns an array of the <tt>ThreadInfo</tt> objects, each is
     * the thread information about the thread with the same index as in the
     * <tt>ids</tt> array. If a thread of the given ID is not alive or does not
     * exist, <tt>null</tt> will be set in the corresponding element in the
     * returned array. A thread is alive if it has been started and has not yet
     * died.
     * <p>
     * If a thread does not lock any object monitor or <tt>lockedMonitors</tt>
     * is <tt>false</tt>, the returned <tt>ThreadInfo</tt> object will have an
     * empty <tt>MonitorInfo</tt> array. Similarly, if a thread does not lock
     * any synchronizer or <tt>lockedSynchronizers</tt> is <tt>false</tt>, the
     * returned <tt>ThreadInfo</tt> object will have an empty <tt>LockInfo</tt>
     * array.
     *
     * <p>
     * When both <tt>lockedMonitors</tt> and <tt>lockedSynchronizers</tt>
     * parameters are <tt>false</tt>, it is equivalent to calling: <blockquote>
     *
     * <pre>
     *     {@link #getThreadInfo(long[], int)  getThreadInfo(ids, Integer.MAX_VALUE)}
     * </pre>
     *
     * </blockquote>
     *
     * <p>
     * This method is designed for troubleshooting use, but not for
     * synchronization control. It might be an expensive operation.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>ThreadInfo</tt> is <tt>CompositeData</tt> with
     * attributes as specified in the {@link ThreadInfo#from ThreadInfo.from}
     * method.
     *
     * @param ids
     *            an array of thread IDs.
     * @param lockedMonitors
     *            if <tt>true</tt>, retrieves all locked monitors.
     * @param lockedSynchronizers
     *            if <tt>true</tt>, retrieves all locked ownable synchronizers.
     *
     * @return an array of the {@link ThreadInfo} objects, each containing
     *         information about a thread whose ID is in the corresponding
     *         element of the input array of IDs.
     *
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     * @throws java.lang.UnsupportedOperationException
     *             <ul>
     *             <li>if <tt>lockedMonitors</tt> is <tt>true</tt> but the Java
     *             virtual machine does not support monitoring of
     *             {@linkplain #isObjectMonitorUsageSupported object monitor
     *             usage}; or</li> <li>if <tt>lockedSynchronizers</tt> is <tt>
     *             true</tt> but the Java virtual machine does not support
     *             monitoring of {@linkplain #isSynchronizerUsageSupported
     *             ownable synchronizer usage}.</li>
     *             </ul>
     *
     * @see #isObjectMonitorUsageSupported
     * @see #isSynchronizerUsageSupported
     *
     * @since 1.6
     */
    @Override
    public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
            boolean lockedSynchronizers) {
        return getThreadInfo(ids);
    }

    /**
     * Returns the thread info for all live threads with stack trace and
     * synchronization information. Some threads included in the returned array
     * may have been terminated when this method returns.
     *
     * <p>
     * This method returns an array of {@link ThreadInfo} objects as specified
     * in the {@link #getThreadInfo(long[], boolean, boolean)} method.
     *
     * @param lockedMonitors
     *            if <tt>true</tt>, dump all locked monitors.
     * @param lockedSynchronizers
     *            if <tt>true</tt>, dump all locked ownable synchronizers.
     *
     * @return an array of {@link ThreadInfo} for all live threads.
     *
     * @throws java.lang.SecurityException
     *             if a security manager exists and the caller does not have
     *             ManagementPermission("monitor").
     * @throws java.lang.UnsupportedOperationException
     *             <ul>
     *             <li>if <tt>lockedMonitors</tt> is <tt>true</tt> but the Java
     *             virtual machine does not support monitoring of
     *             {@linkplain #isObjectMonitorUsageSupported object monitor
     *             usage}; or</li> <li>if <tt>lockedSynchronizers</tt> is <tt>
     *             true</tt> but the Java virtual machine does not support
     *             monitoring of {@linkplain #isSynchronizerUsageSupported
     *             ownable synchronizer usage}.</li>
     *             </ul>
     *
     * @see #isObjectMonitorUsageSupported
     * @see #isSynchronizerUsageSupported
     *
     * @since 1.6
     */
    @Override
    public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
            boolean lockedSynchronizers) {
        return getThreadInfo(getAllThreadIds());
    }

    private ThreadInfo toThreadInfo(ThreadState threadState) {
        return ThreadInfo.from(toCompositeData(threadState));
    }

    interface DataType {
        String getName();

        OpenType<?> getType();
    }

    static CompositeType makeType(String name, String desc, DataType[] values) {
        OpenType<?>[] types = new OpenType[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = values[i].getType();
        }
        String[] names = new String[values.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = values[i].getName();
        }
        try {
            return new CompositeType(name, desc, names, names, types);
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    enum StackTraceElementDataTypes implements DataType {
        CLASSNAME("className", SimpleType.STRING), METHODNAME("methodName",
                SimpleType.STRING), FILENAME("fileName", SimpleType.STRING), LINENUMBER(
                "lineNumber", SimpleType.INTEGER), NATIVEMETHOD("nativeMethod",
                SimpleType.BOOLEAN);
        private final String name;
        private final OpenType<?> type;

        StackTraceElementDataTypes(String name, OpenType<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OpenType<?> getType() {
            return type;
        }

        static CompositeType type() {
            return makeType("java.lang.StackTraceElement",
                    "java.lang.StackTraceElement", values());
        }

        static OpenType<?> arrayType() {
            try {
                return ArrayType.getArrayType(type());
            } catch (OpenDataException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    enum MonitorInfoDataTypes implements DataType {
        STACKDEPTH("stackDepth", SimpleType.INTEGER), STACKFRAME("stackFrame",
                StackTraceElementDataTypes.type()), CLASSNAME("className",
                SimpleType.STRING), IDENTITYHASHCODE("identityHashCode",
                SimpleType.LONG);
        private final String name;
        private final OpenType<?> type;

        MonitorInfoDataTypes(String name, OpenType<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OpenType<?> getType() {
            return type;
        }

        static CompositeType type() {
            return makeType("java.lang.management.MonitorInfo",
                    "java.lang.management.MonitorInfo", values());
        }

        public static OpenType<?> arrayType() {
            try {
                return ArrayType.getArrayType(type());
            } catch (OpenDataException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    enum LockInfoDataTypes implements DataType {
        CLASSNAME("className", SimpleType.STRING), IDENTITYHASHCODE(
                "identityHashCode", SimpleType.LONG);
        private final String name;
        private final OpenType<?> type;

        LockInfoDataTypes(String name, OpenType<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OpenType<?> getType() {
            return type;
        }

        static CompositeType type() {
            return makeType("java.lang.management.LockInfo",
                    "java.lang.management.LockInfo", values());
        }

        public static OpenType<?> arrayType() {
            try {
                return ArrayType.getArrayType(type());
            } catch (OpenDataException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    enum ThreadInfoDataTypes implements DataType {
        ID("threadId", SimpleType.LONG), NAME("threadName", SimpleType.STRING), STATE(
                "threadState", SimpleType.STRING), SUSPENDED("suspended",
                SimpleType.BOOLEAN), NATIVE("inNative", SimpleType.BOOLEAN), BLOCKEDCOUNT(
                "blockedCount", SimpleType.LONG), BLOCKEDTIME("blockedTime",
                SimpleType.LONG), WAITEDCOUNT("waitedCount", SimpleType.LONG), WAITEDTIME(
                "waitedTime", SimpleType.LONG), LOCKINFO("lockInfo",
                LockInfoDataTypes.type()), LOCKNAME("lockName",
                SimpleType.STRING), LOCKOWNERID("lockOwnerId", SimpleType.LONG), LOCKOWNERNAME(
                "lockWonderName", SimpleType.STRING), STACKTRACE("stackTrace",
                StackTraceElementDataTypes.arrayType()), LOCKEDMONITORS(
                                                                                        "lockedMonitors", MonitorInfoDataTypes.arrayType()), LOCKEDSYNCHRONIZERS(
                "lockedSynchronizers", LockInfoDataTypes.arrayType());
        private final String name;
        private final OpenType<?> type;

        ThreadInfoDataTypes(String name, OpenType<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OpenType<?> getType() {
            return type;
        }

        static CompositeType type() {
            return makeType("java.lang.management.ThreadInfo",
                    "J2SE 5.0 java.lang.management.ThreadInfo", values());
        }

        static Object fillIn(ThreadInfoDataTypes dt, ThreadState state) {
            switch (dt) {
            case BLOCKEDCOUNT:
                return state.getBlockedCount();
            case BLOCKEDTIME:
                return state.getBlockedTime();
            case ID:
                return state.getId();
            case LOCKEDMONITORS:
                return null;
            case LOCKEDSYNCHRONIZERS:
                return null;
            case LOCKINFO:
                return state.getLockInfo();
            case LOCKNAME:
                return state.getLockInfo() == null ? null : state.getLockInfo()
                        .toString();
            case LOCKOWNERID:
                return state.getLockInfo() == null ? -1 : state.getId();
            case LOCKOWNERNAME:
                return state.getLockInfo() == null ? null : state.getName();
            case NAME:
                return state.getName();
            case NATIVE:
                return false;
            case STACKTRACE:
                return state.getTrace();
            case STATE:
                return state.getState();
            case SUSPENDED:
                return false;
            case WAITEDCOUNT:
                return state.getWaitedCount();
            case WAITEDTIME:
                return state.getWaitedTime();
            default:
                throw new IllegalStateException();
            }
        }
    }

    private static CompositeData toCompositeData(ThreadState threadState) {
        ThreadInfoDataTypes[] tidt = ThreadInfoDataTypes.values();
        String[] names = new String[tidt.length];
        Object[] values = new Object[tidt.length];
        for (ThreadInfoDataTypes dt : tidt) {
            names[dt.ordinal()] = dt.getName();
            values[dt.ordinal()] = ThreadInfoDataTypes.fillIn(dt, threadState);
        }
        try {
            return new CompositeDataSupport(ThreadInfoDataTypes.type(), names,
                    values);
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }
}
