package com.surelogic.flashlight.common.model;

import com.surelogic.ReferenceObject;

/**
 * Interface to represent a running Flashlight-instrumented application and
 * control its collection.
 * 
 * @see RunControlManager
 */
@ReferenceObject
public interface IDataCollectingRun {

  /**
   * Requests that this terminate collection as soon as possible.
   */
  void stopDataCollectionAsSoonAsPossible();

  /**
   * Gets if this represents an instrumented Android application.
   * 
   * @return {@code true} if this represents an instrumented Android
   *         application, {@code false} otherwise.
   */
  boolean isAndroid();

}
