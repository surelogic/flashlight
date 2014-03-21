package com.surelogic._flashlight;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLThreadMxBean implements ThreadMXBean {

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
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Returns the peak live thread count since the Java virtual machine started
     * or peak was reset.
     *
     * @return the peak live thread count.
     */
    @Override
    public int getPeakThreadCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Returns the total number of threads created and also started since the
     * Java virtual machine started.
     *
     * @return the total number of threads started.
     */
    @Override
    public long getTotalStartedThreadCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Returns the current number of live daemon threads.
     *
     * @return the current number of live daemon threads.
     */
    @Override
    public int getDaemonThreadCount() {
        // TODO Auto-generated method stub
        return 0;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isThreadContentionMonitoringSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isThreadContentionMonitoringEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setThreadContentionMonitoringEnabled(boolean enable) {
        // TODO Auto-generated method stub

    }

    @Override
    public long getCurrentThreadCpuTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getCurrentThreadUserTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getThreadCpuTime(long id) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getThreadUserTime(long id) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isThreadCpuTimeSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCurrentThreadCpuTimeSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isThreadCpuTimeEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setThreadCpuTimeEnabled(boolean enable) {
        // TODO Auto-generated method stub

    }

    @Override
    public long[] findMonitorDeadlockedThreads() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resetPeakThreadCount() {
        // TODO Auto-generated method stub

    }

    @Override
    public long[] findDeadlockedThreads() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isObjectMonitorUsageSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSynchronizerUsageSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
            boolean lockedSynchronizers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
            boolean lockedSynchronizers) {
        // TODO Auto-generated method stub
        return null;
    }

}
