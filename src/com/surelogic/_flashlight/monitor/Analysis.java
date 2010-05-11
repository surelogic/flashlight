/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic._flashlight.IdPhantomReference;
import com.surelogic._flashlight.Phantom;

/**
 * The Analysis thread periodically collects events from every program thread
 * and combines them to form a unified view of the current state of the program.
 * 
 * @author nathan
 * 
 */
final class Analysis extends Thread {
	private static final long PERIOD = 1000L;

	private final MasterLockSet master;
	private final FieldDefs fieldDefs;
	private final SharedFields shared;

	private volatile boolean f_done;

	private AlertSpec alerts;

	private Set<FieldDef> edtViolations;
	private Set<FieldDef> sharedFieldViolations;
	private Set<FieldDef> lockSetViolations;

	Analysis(final FieldDefs fields) {
		super("flashlight-analysis");
		this.fieldDefs = fields;
		shared = new SharedFields();
		master = new MasterLockSet(fieldDefs, shared);
		edtViolations = new HashSet<FieldDef>();
		sharedFieldViolations = new HashSet<FieldDef>();
		lockSetViolations = new HashSet<FieldDef>();
		alerts = new AlertSpec(fields);
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

	private final List<IdPhantomReference> references = new ArrayList<IdPhantomReference>();

	public synchronized void process() {
		Phantom.drainTo(references);
		final Set<Long> edtThreads = new HashSet<Long>();
		for (final ThreadLocks other : MonitorStore.f_lockSets) {
			if (other.isEDT()) {
				edtThreads.add(other.getThreadId());
			}
			master.drain(other);
		}
		// Take care of garbage collected objects
		for (final IdPhantomReference ref : references) {
			final long receiverId = ref.getId();
			// Compute final lock set results
			master.purge(receiverId);
			shared.remove(receiverId);
		}
		references.clear();

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

	public synchronized AlertInfo getAlerts() {
		final Set<FieldDef> edts = new HashSet<FieldDef>(edtViolations);
		final Set<FieldDef> shared = new HashSet<FieldDef>(
				sharedFieldViolations);
		final Set<FieldDef> lockSets = new HashSet<FieldDef>(lockSetViolations);
		return new AlertInfo(edts, shared, lockSets);
	}

	public synchronized DeadlockInfo getDeadlocks() {
		return new DeadlockInfo(master.getLockOrders(), master.getDeadlocks());
	}

	public synchronized LockSetInfo getLockSets() {
		return master.getLockSetInfo();
	}

	public synchronized SharedFieldInfo getShared() {
		return new SharedFieldInfo(fieldDefs, shared.calculateSharedFields(),
				shared.calculateUnsharedFields());
	}

	@Override
	public synchronized String toString() {
		final StringBuilder b = new StringBuilder();
		b.append(getAlerts().toString());
		b.append(master.getLockSetInfo().toString());
		b.append(getDeadlocks().toString());
		b.append(getShared().toString());
		return b.toString();
	}

	private static Analysis activeAnalysis;
	private static final Lock analysisLock = new ReentrantLock();

	synchronized void setAlerts(final AlertSpec spec) {
		if (alerts != null) {
			alerts = alerts.merge(spec);
		} else {
			this.alerts = spec;
		}
		edtViolations = new HashSet<FieldDef>();
		sharedFieldViolations = new HashSet<FieldDef>();
		lockSetViolations = new HashSet<FieldDef>();
	}

	static void reviseAlerts(final AlertSpec spec) {
		analysisLock.lock();
		try {
			activeAnalysis.setAlerts(spec);
		} finally {
			analysisLock.unlock();
		}
	}

	/**
	 * Change the monitor specification that we are looking at
	 * 
	 * @param spec
	 */
	static void reviseSpec(final MonitorSpec spec) {
		analysisLock.lock();
		try {
			if (activeAnalysis != null) {
				activeAnalysis.wrapUp();
				try {
					activeAnalysis.join();
				} catch (final InterruptedException e) {
					MonitorStore.logAProblem(
							"Exception while changing analyses", e);
				}
			}
			activeAnalysis = new Analysis(MonitorStore.getFieldDefinitions());
			MonitorStore.updateSpec(spec);
			activeAnalysis.start();
		} finally {
			analysisLock.unlock();
		}
	}

	/**
	 * Get the active analysis
	 * 
	 * @return
	 */
	static Analysis getAnalysis() {
		analysisLock.lock();
		try {
			return activeAnalysis;
		} finally {
			analysisLock.unlock();
		}
	}

}