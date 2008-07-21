package com.surelogic.flashlight.common.model;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.surelogic.common.ILifecycle;

public final class RunManager implements ILifecycle {

	private static final RunManager INSTANCE = new RunManager();

	public static RunManager getInstance() {
		return INSTANCE;
	}

	private RunManager() {
		// singleton
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

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init() {
		// TODO Auto-generated method stub
	}
}
