package com.surelogic.flashlight.common.model;

import java.io.File;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.NullDBTransaction;
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
	}

	private final Set<IRunManagerObserver> f_observers = new CopyOnWriteArraySet<IRunManagerObserver>();

	public void addObserver(final IRunManagerObserver o) {
		if (o == null) {
			return;
		}
		f_observers.add(o);
	}

	public void removeObserver(final IRunManagerObserver o) {
		f_observers.remove(o);
	}

	/**
	 * Do not call this method while holding a lock!
	 */
	private void notifyObservers() {
		for (final IRunManagerObserver o : f_observers) {
			o.notify(this);
		}
	}

	private final AtomicReference<File> f_dataDir =
		new AtomicReference<File>();
	
	public void setDataDirectory(File dir) {
		if (dir != null && dir.exists() && dir.isDirectory()) {
			f_dataDir.set(dir);
			refresh();
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
		final File dataDir = f_dataDir.get();
		if (dataDir == null) {
			return; // Nothing to do
		}		
		boolean isChanged = false; // assume nothing changed
		final Map<RunDescription, PrepRunDescription> descToPrep = new HashMap<RunDescription, PrepRunDescription>();
		final Set<RunDescription> rawDescriptions = RawFileUtility
				.getRunDescriptions(dataDir);
		/*
		 * Put in all the raw descriptions with a null. This indicates that
		 * there is no prepared description associated with them.
		 */
		for (final RunDescription key : rawDescriptions) {
			// Assume the run is not prepped until we find out otherwise
			descToPrep.put(key, null);
			final File dbDir = key.getRunDirectory().getDatabaseDirectory();
			if (dbDir.exists()) {
				// Check to see if there is a prepped run
				final DBConnection conn = FlashlightDBConnection
						.getInstance(dbDir);
				conn.withReadOnly(new NullDBTransaction() {
					@Override
					public void doPerform(final Connection conn)
							throws Exception {
						descToPrep.put(key, RunDAO.find(dataDir, conn));
					}
				});
			}
		}

		/*
		 * Check if anything changed...if so, update the map and notify
		 * observers.
		 */
		final Map<RunDescription, PrepRunDescription> descToPrepOld = f_descToPrep
				.get();
		if (!descToPrepOld.equals(descToPrep)) {
			isChanged = true;
		}
		if (isChanged) {
			f_descToPrep.set(Collections.unmodifiableMap(descToPrep));
			notifyObservers();
		}
	}

	/**
	 * The currently selected run, may be {@code null} which indicates that no
	 * run is selected.
	 */
	private volatile RunDescription selectedRun;

	/**
	 * Gets the currently selected run.
	 * 
	 * @return the currently selected run, or {@code null} if no run is
	 *         selected.
	 */
	public RunDescription getSelectedRun() {
		return selectedRun;
	}

	/**
	 * Sets the currently selected run.
	 * 
	 * @param runDescription
	 *            the run that is now selected, or {@code null} if no run is now
	 *            selected.
	 */
	public void setSelectedRun(final RunDescription runDescription) {
		selectedRun = runDescription;
	}
}
