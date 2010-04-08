/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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

	private final Set<Long> lockSetFields;
	private final Set<Long> staticLockSetFields;
	private final Set<Long> noLockSetFields;
	private final Set<Long> noStaticLockSetFields;

	private Set<FieldDef> edtViolations;
	private Set<FieldDef> sharedFieldViolations;
	private Set<FieldDef> lockSetViolations;

	Analysis(final FieldDefs fields) {
		super("flashlight-analysis");
		this.fieldDefs = fields;
		shared = new SharedFields();
		master = new MasterLockSet(shared);
		lockSetFields = new HashSet<Long>();
		staticLockSetFields = new HashSet<Long>();
		noLockSetFields = new HashSet<Long>();
		noStaticLockSetFields = new HashSet<Long>();
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
			final Map<Long, Set<Long>> fields = master.purge(receiverId);
			if (fields != null) {
				for (final Entry<Long, Set<Long>> e : fields.entrySet()) {
					final long fieldId = e.getKey();
					if (shared.isShared(receiverId, fieldId)) {
						if (e.getValue().isEmpty()) {
							noLockSetFields.add(fieldId);
						} else {
							lockSetFields.add(fieldId);
						}
					}
				}
			}
			shared.remove(receiverId);
		}
		references.clear();
		for (final Entry<Long, Set<Long>> e : master.getStaticLockSets()
				.entrySet()) {
			final long fieldId = e.getKey();
			if (shared.isShared(fieldId)) {
				if (!noStaticLockSetFields.contains(fieldId)) {
					if (e.getValue().isEmpty()) {
						noStaticLockSetFields.add(fieldId);
					} else {
						staticLockSetFields.add(fieldId);
					}
				}
			}
		}
		for (final Entry<Long, Map<Long, Set<Long>>> e : master.getLockSets()
				.entrySet()) {
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				if (e1.getValue().isEmpty()) {
					noLockSetFields.add(e1.getKey());
				} else {
					lockSetFields.add(e1.getKey());
				}
			}
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
		for (final FieldDef field : alerts.getLockSetFields()) {
			if (field.isStatic()) {
				if (noStaticLockSetFields.contains(field)) {
					lockSetViolations.add(field);
				}
			} else {
				if (noLockSetFields.contains(field)) {
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

	@Override
	public synchronized String toString() {
		final StringBuilder b = new StringBuilder();
		b.append(getAlerts().toString());
		b.append("Static:\n");
		final HashSet<Long> staticSet = new HashSet<Long>(staticLockSetFields);
		staticSet.removeAll(noStaticLockSetFields);
		appendFields(b, staticSet);

		b.append("Fields With Lock Sets:\n");
		b.append("Static Fields:\n");
		appendFields(b, staticLockSetFields);

		b.append("Instance Fields that ALWAYS have a  Lock Set:\n");
		final HashSet<Long> instanceSet = new HashSet<Long>(lockSetFields);
		instanceSet.removeAll(noLockSetFields);
		appendFields(b, instanceSet);

		b.append("Instance Fields that SOMETIMES have a Lock Set:\n");
		appendFields(b, lockSetFields);

		b.append("Fields With No Lock Set:\n");
		b.append("Instance:\n");
		appendFields(b, noLockSetFields);
		b.append("Static:\n");
		appendFields(b, noStaticLockSetFields);

		b.append(getDeadlocks().toString());
		b.append("Shared Fields:\n");
		appendFields(b, shared.calculateSharedFields());
		b.append("Unshared Fields:\n");
		appendFields(b, shared.calculateUnsharedFields());
		return b.toString();
	}

	private void appendFields(final StringBuilder b, final Set<Long> fields) {
		final List<String> list = new ArrayList<String>();
		for (final long f : fields) {
			list.add(String.format("\t%s - %d", fieldDefs.get(f), f));
		}
		Collections.sort(list);
		for (final String s : list) {
			b.append(s);
			b.append('\n');
		}
	}

	private static Analysis activeAnalysis;
	private static final ReentrantLock analysisLock = new ReentrantLock();

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