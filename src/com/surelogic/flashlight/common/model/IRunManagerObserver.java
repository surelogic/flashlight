package com.surelogic.flashlight.common.model;

public interface IRunManagerObserver {

	/**
	 * Indicates that something about the set of runs being managed has changed.
	 * This could include a new raw file being create, a raw file being
	 * prepared, or data being deleted.
	 * 
	 * @param manager
	 *            the run manager.
	 */
	void notify(RunManager manager);
}
