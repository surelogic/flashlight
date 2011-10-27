/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic._flashlight.IdPhantomReference;
import com.surelogic._flashlight.RunConf;
import com.surelogic._flashlight.common.FieldDef;

/**
 * The Analysis thread periodically collects events from every program thread
 * and combines them to form a unified view of the current state of the program.
 * 
 * @author nathan
 * 
 */
final class Analysis extends Thread {
    private static final long PERIOD = 1000L;

    private final MonitorStore store;
    private final RunConf conf;
    private final MasterLockSet master;
    private final SharedFields shared;

    private volatile boolean f_done;

    private AlertSpec alerts;

    private Set<FieldDef> edtViolations;
    private Set<FieldDef> sharedFieldViolations;
    private Set<FieldDef> lockSetViolations;

    Analysis(final MonitorStore store, final RunConf conf) {
        super("flashlight-analysis");
        this.conf = conf;
        this.store = store;
        shared = new SharedFields();
        master = new MasterLockSet(conf.getFieldDefs(), shared);
        edtViolations = new HashSet<FieldDef>();
        sharedFieldViolations = new HashSet<FieldDef>();
        lockSetViolations = new HashSet<FieldDef>();
        alerts = new AlertSpec(conf.getFieldDefs());
    }

    @Override
    public void run() {
        while (!f_done) {
            final long time = System.currentTimeMillis();
            process();
            final long elapsed = System.currentTimeMillis() - time;
            if (elapsed < PERIOD) {
                try {
                    Thread.sleep(PERIOD - elapsed);
                } catch (final InterruptedException e) {
                    // Do nothing if we are interrupted
                }
            }
        }
        // Wrap up the last bit of analysis results
        process();
    }

    void wrapUp() {
        f_done = true;
    }

    public synchronized void gcReferences(
            final List<? extends IdPhantomReference> references) {
        // Take care of garbage collected objects
        for (final IdPhantomReference ref : references) {
            final long receiverId = ref.getId();
            // Compute final lock set results
            master.purge(receiverId);
            shared.remove(receiverId);
        }
    }

    public synchronized void process() {
        final Set<Long> edtThreads = new HashSet<Long>();
        for (final ThreadLocks other : store.f_lockSets) {
            if (other.isEDT()) {
                edtThreads.add(other.getThreadId());
            }
            master.drain(other);
        }
        for (final FieldDef field : alerts.getEDTFields()) {
            if (!shared.isConfinedTo(field, edtThreads)) {
                edtViolations.add(field);
            }
        }
        for (final FieldDef field : alerts.getSharedFields()) {
            if (shared.isShared(field)) {
                sharedFieldViolations.add(field);
            }
        }
        final List<FieldDef> lockSetFields = alerts.getLockSetFields();
        if (!lockSetFields.isEmpty()) {
            final LockSetInfo lockSets = getLockSets();
            for (final FieldDef field : alerts.getLockSetFields()) {
                if (!lockSets.hasLockSet(field)) {
                    lockSetViolations.add(field);
                }
            }
        }
    }

    public synchronized AlertSpec getAlertSpec() {
        return alerts;
    }

    public synchronized AlertInfo getAlerts() {
        final Set<FieldDef> edts = new HashSet<FieldDef>(edtViolations);
        final Set<FieldDef> shared = new HashSet<FieldDef>(
                sharedFieldViolations);
        final Set<FieldDef> lockSets = new HashSet<FieldDef>(lockSetViolations);
        return new AlertInfo(edts, shared, lockSets);
    }

    public synchronized DeadlockInfo getDeadlocks() {
        return new DeadlockInfo(master.getLockOrders(), master.getDeadlocks(),
                new HashMap<Long, String>(store.getLockNames()));
    }

    public synchronized LockSetInfo getLockSets() {
        return master.getLockSetInfo();
    }

    public synchronized SharedFieldInfo getShared() {
        return new SharedFieldInfo(conf.getFieldDefs(),
                shared.calculateSharedFields(),
                shared.calculateUnsharedFields());
    }

    @Override
    public synchronized String toString() {
        return new FullInfo(getAlerts(), master.getLockSetInfo(), getShared(),
                getDeadlocks()).toString();
    }

    synchronized void setAlertSpec(final AlertSpec spec) {
        if (alerts == null) {
            alerts = spec;
        } else {
            alerts = alerts.merge(spec);
        }
        edtViolations = new HashSet<FieldDef>();
        sharedFieldViolations = new HashSet<FieldDef>();
        lockSetViolations = new HashSet<FieldDef>();
    }

}