package com.surelogic.flashlight.client.eclipse.model;

public interface IRunManagerObserver {

	/**
	 * Indicates that something about the set of runs being managed has changed.
	 * This could include a new raw file being create, a raw file being
	 * prepared, or data being deleted.
	 * <p>
	 * Implementors should note that this method is called from an unknown
	 * thread.
	 * 
	 * @param manager
	 *            the run manager.
	 */
	void notify(RunManager manager);
}
