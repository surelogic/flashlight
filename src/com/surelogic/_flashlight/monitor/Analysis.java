/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic._flashlight.IdPhantomReference;
import com.surelogic._flashlight.Phantom;
import com.surelogic._flashlight.monitor.ThreadLocks.LockStack;

/**
 * The Analysis thread periodically collects events from every program thread
 * and combines them to form a unified view of the current state of the program.
 * 
 * @author nathan
 * 
 */
final class Analysis extends Thread {
	private static final long PERIOD = 1000L;

	private final List<IdPhantomReference> f_references = new ArrayList<IdPhantomReference>();
	private final MasterLockSet master;
	private final FieldDefs fieldDefs;
	private volatile boolean f_done;
	private final Map<Long, Set<String>> fields;
	private final SharedFields shared;

	private AlertSpec alerts;

	private final Set<Long> lockSetFields;
	private final Set<Long> staticLockSetFields;
	private final Set<Long> noLockSetFields;
	private final Set<Long> noStaticLockSetFields;

	private Set<Long> edtViolations;
	private Set<Long> sharedFieldViolations;
	private Set<Long> lockSetViolations;

	Analysis(final FieldDefs fields) {
		super("flashlight-analysis");
		this.fieldDefs = fields;
		this.fields = new HashMap<Long, Set<String>>();
		shared = new SharedFields();
		master = new MasterLockSet(shared);
		lockSetFields = new HashSet<Long>();
		staticLockSetFields = new HashSet<Long>();
		noLockSetFields = new HashSet<Long>();
		noStaticLockSetFields = new HashSet<Long>();
		edtViolations = new HashSet<Long>();
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

	public synchronized void process() {
		Phantom.drainTo(f_references);
		final Set<Long> edtThreads = new HashSet<Long>();
		for (final ThreadLocks other : MonitorStore.f_lockSets) {
			if (other.isEDT()) {
				edtThreads.add(other.getThreadId());
			}
			master.drain(other);
		}
		for (final IdPhantomReference ref : f_references) {
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
		f_references.clear();
		for (final Entry<Long, Set<Long>> e : master.getStaticLockSets()
				.entrySet()) {
			final long fieldId = e.getKey();
			if (shared.isShared(fieldId)) {
				if (e.getValue().isEmpty()) {
					noStaticLockSetFields.add(fieldId);
				} else {
					staticLockSetFields.add(fieldId);
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
				edtViolations.add(field.getId());
			}
		}
		for (final FieldDef field : alerts.getSharedFields()) {
			if (shared.isShared(field)) {
				sharedFieldViolations.add(field.getId());
			}
		}
		for (final FieldDef field : alerts.getLockSetFields()) {
			if (field.isStatic()) {
				if (noStaticLockSetFields.contains(field)) {
					lockSetViolations.add(field.getId());
				}
			} else {
				if (noLockSetFields.contains(field)) {
					lockSetViolations.add(field.getId());
				}
			}

		}
	}

	@Override
	public synchronized String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("EDT thread alerts:\n");
		appendFields(b, edtViolations);
		b.append("Fields that ALWAYS have a  Lock Set:\n");
		b.append("Instance:\n");
		final HashSet<Long> instanceSet = new HashSet<Long>(lockSetFields);
		instanceSet.removeAll(noLockSetFields);
		appendFields(b, instanceSet);
		b.append("Static:\n");
		final HashSet<Long> staticSet = new HashSet<Long>(staticLockSetFields);
		staticSet.removeAll(noStaticLockSetFields);
		appendFields(b, staticSet);

		b.append("Fields With Lock Sets:\n");
		b.append("Instance:\n");
		appendFields(b, lockSetFields);
		b.append("Static:\n");
		appendFields(b, staticLockSetFields);

		b.append("Fields With No Lock Set:\n");
		b.append("Instance:\n");
		appendFields(b, noLockSetFields);
		b.append("Static:\n");
		appendFields(b, noStaticLockSetFields);

		b.append("Lock Orderings:\n");
		appendLockOrders(b, master.getLockOrders());
		b.append("Potential Deadlocks:\n");
		final Set<Long> deadlocks = master.getDeadlocks();
		b.append(deadlocks);
		b.append("\n");
		b.append("Shared Fields:\n");
		appendFields(b, shared.calculateSharedFields());
		b.append("Unshared Fields:\n");
		appendFields(b, shared.calculateUnsharedFields());
		return b.toString();
	}

	private void appendLockOrders(final StringBuilder b,
			final Set<LockStack> lockOrders) {
		final List<String> strs = new ArrayList<String>();
		for (LockStack stack : lockOrders) {
			String s = "";
			for (; stack.getLockId() != LockStack.HEAD; stack = stack
					.getParentLock()) {
				s = "-> " + stack.getLockId() + s;
			}
			strs.add(s);
		}
		Collections.sort(strs);
		for (final String s : strs) {
			b.append(s);
			b.append('\n');
		}
	}

	void appendFields(final StringBuilder b, final Set<Long> fields) {
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
		this.alerts = spec;
		edtViolations = new HashSet<Long>();
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