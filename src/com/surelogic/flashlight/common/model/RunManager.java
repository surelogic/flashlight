package com.surelogic.flashlight.common.model;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.common.jdbc.DBTransaction;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.RawFileUtility;

/**
 * A singleton that manages the set of run description aggregates. This class
 * will not be populated with data until its {@link #refresh()} method is
 * invoked.
 */
public final class RunManager {

	private static final RunManager INSTANCE = new RunManager();

	public static RunManager getInstance() {
		return INSTANCE;
	}

	private RunManager() {
		refresh();
	}

	private final Set<IRunManagerObserver> f_observers = new CopyOnWriteArraySet<IRunManagerObserver>();

	public void addObserver(final IRunManagerObserver o) {
		if (o == null)
			return;
		f_observers.add(o);
	}

	public void removeObserver(final IRunManagerObserver o) {
		f_observers.remove(o);
	}

	/**
	 * Do not call this method while holding a lock!
	 */
	private void notifyObservers() {
		for (IRunManagerObserver o : f_observers) {
			o.notify(this);
		}
	}

	/**
	 * Holds a mapping from all known run descriptions (in files or in the
	 * database) to prepared run descriptions. Not all run descriptions have an
	 * associated prepared run description, these entries will have a value of
	 * {@code null}.
	 */
	private final AtomicReference<Map<RunDescription, PrepRunDescription>> f_descToPrep = new AtomicReference<Map<RunDescription, PrepRunDescription>>(
			new HashMap<RunDescription, PrepRunDescription>());

	/**
	 * Gets the set of run descriptions known to this manager.
	 * 
	 * @return the non-null set of run descriptions known to this manager. This
	 *         is a copy of the set maintained by this manager so it can be
	 *         freely mutated by callers.
	 */
	public Set<RunDescription> getRunDescriptions() {
		return new HashSet<RunDescription>(f_descToPrep.get().keySet());
	}

	/**
	 * Gets the prepared run description corresponding to the given run
	 * description, or {@code null} if there is none.
	 * 
	 * @param description
	 *            the run description.
	 * @return the prepared run description corresponding to the given run
	 *         description, or {@code null} if there is none.
	 */
	PrepRunDescription getPrepRunDescriptionFor(final RunDescription description) {
		return f_descToPrep.get().get(description);
	}

	/**
	 * Refreshes the set of run descriptions managed by this class and notifies
	 * all observers if that set has changed.
	 */
	public void refresh() {
		final Map<RunDescription, PrepRunDescription> descToPrep = new HashMap<RunDescription, PrepRunDescription>();
		final Set<RunDescription> rawDescriptions = RawFileUtility
				.getRunDescriptions();
		/*
		 * Put in all the raw descriptions with a null. This indicates that
		 * there is no prepared description associated with them.
		 */
		for (RunDescription key : rawDescriptions) {
			descToPrep.put(key, null);
		}

		/*
		 * Map all prepared run descriptions currently in the database.
		 */
		final DBTransaction<Set<PrepRunDescription>> tran = new DBTransaction<Set<PrepRunDescription>>() {
			public Set<PrepRunDescription> perform(Connection conn)
					throws Exception {
				return RunDAO.getAll(conn);
			}
		};
		final Set<PrepRunDescription> preppedDescriptions = Data.getInstance()
				.withReadOnly(tran);
		for (PrepRunDescription value : preppedDescriptions) {
			final RunDescription key = value.getDescription();
			descToPrep.put(key, value);
		}

		/*
		 * Check if anything changed...if so, update the map and notify
		 * observers.
		 */
		final Map<RunDescription, PrepRunDescription> descToPrepOld = f_descToPrep
				.get();
		final boolean isChanged = !descToPrepOld.equals(descToPrep);
		if (isChanged) {
			f_descToPrep.set(Collections.unmodifiableMap(descToPrep));
			notifyObservers();
		}
	}
}
