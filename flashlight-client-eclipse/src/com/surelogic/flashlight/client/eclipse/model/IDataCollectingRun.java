package com.surelogic.flashlight.client.eclipse.model;

import com.surelogic.ReferenceObject;

/**
 * Interface to represent a launched Flashlight-instrumented application and
 * control its collection.
 */
@ReferenceObject
public interface IDataCollectingRun {
  /**
   * Requests that this terminate collection as soon as possible. <b>Only to be
   * invoked by the <tt>RunManager</tt>&mdash;client/UI code should never invoke
   * this method, rather call into <tt>RunManager</tt> instead.</b>
   */
  void stopDataCollectionAsSoonAsPossible();
}
