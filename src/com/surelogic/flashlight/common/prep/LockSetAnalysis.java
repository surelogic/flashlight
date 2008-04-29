package com.surelogic.flashlight.common.prep;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.common.jdbc.DBQueryEmpty;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.RowHandler;

/**
 * The lock set analysis looks for bad field publishes during construction of an
 * object and for field access that hold an inconsistent set of locks.
 * 
 * @author nathan
 * 
 */
public class LockSetAnalysis extends DBQueryEmpty {

	private final long runId;

	public LockSetAnalysis(long runId) {
		this.runId = runId;
	}

	public void doPerform(Query q) {
		final Queryable<Boolean> badAccess = q.prepared(
				"LockSet.fieldAccesses", new LocksSetHandler());
		final Queryable<Void> insertBadField = q
				.prepared("LockSet.insertSuspiciousField");
		final Queryable<Void> badPublish = q
				.prepared("LockSet.insertBadPublish");

		for (final Field id : q.prepared("LockSet.badPublishes",
				new FieldHandler()).call(runId)) {
			badPublish.call(runId, id.fieldId, id.receiverId);
		}
		for (final Field id : q.prepared("LockSet.interestingFields",
				new FieldHandler()).call(runId)) {
			if (badAccess.call(runId, id.fieldId, id.receiverId)) {
				insertBadField.call(runId, id.fieldId, id.receiverId);
			}
		}
	}

	/**
	 * Does the lock set analysis. Returns <code>true</code> if we have found
	 * a bad lock set.
	 * 
	 * @author nathan
	 * 
	 */
	private static class LocksSetHandler implements ResultHandler<Boolean> {

		public Boolean handle(Result r) {
			// A.TS,A.InThread,D.Lock,RW.Id
			Date ts = null;
			long thread = -1L;
			final Set<Long> lockSet = new HashSet<Long>();
			final Set<Long> locks = new HashSet<Long>();
			boolean first = true;
			/*
			 * We initialize our lock set w/ the contents of the first access.
			 * If our lock set ever drops to empty, we abort.
			 */
			for (final Row row : r) {
				final Date rts = row.nextDate();
				final long rThread = row.nextLong();
				final long rLock = row.nextLong();
				if (ts == null) {
					// handle the first pass
					ts = rts;
					thread = rThread;
				}
				if (!ts.equals(rts) || !(thread == rThread)) {
					if (first) {
						lockSet.addAll(locks);
					} else {
						lockSet.retainAll(locks);
						if (lockSet.isEmpty()) {
							// The set of locks shared by all threads is
							// apparently empty
							return true;
						}
					}
					ts = rts;
					thread = rThread;
					locks.clear();
					first = false;
				}
				locks.add(rLock);
			}
			// Handle the last row
			if (first) {
				lockSet.addAll(locks);
			} else {
				lockSet.retainAll(locks);
			}
			return lockSet.isEmpty();
		}

	}

	private static class FieldHandler implements RowHandler<Field> {

		public Field handle(Row r) {
			return new Field(r.nextLong(), r.nextLong());
		}

	}

	private static class Field {
		final long fieldId;
		final long receiverId;

		Field(long fieldId, long receiverId) {
			this.fieldId = fieldId;
			this.receiverId = receiverId;
		}
	}

}
