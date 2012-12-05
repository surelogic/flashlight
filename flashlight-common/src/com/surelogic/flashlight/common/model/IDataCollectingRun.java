package com.surelogic.flashlight.common.model;

import java.util.Date;

import com.surelogic.NonNull;
import com.surelogic.ReferenceObject;

/**
 * Interface to represent a launched Flashlight-instrumented application and
 * control its collection.
 * 
 * @see RunControlManager
 */
@ReferenceObject
public interface IDataCollectingRun {

  /**
   * Gets if this represents an instrumented Android application.
   * <p>
   * Implementers should ensure that this is not a high-overhead method because
   * it is invoked frequently from the user interface.
   * 
   * @return {@code true} if this represents an instrumented Android
   *         application, {@code false} otherwise.
   */
  boolean isAndroid();

  /**
   * Gets the simple name of the class launched for display in the user
   * interface. For example, <tt>com.surelogic.example.CounterRace</tt> would
   * simply return <tt>CounterRace</tt>.
   * <p>
   * The result of this method is <i>only</i> intended for user interface code
   * and cannot be relied upon for identification or being unique.
   * <p>
   * Implementers should ensure that this is not a high-overhead method because
   * it is invoked frequently from the user interface.
   * 
   * @return the simple name of the class launched for display in the user
   *         interface.
   */
  @NonNull
  String getRunSimpleNameforUI();

  /**
   * Gets the time that this Flashlight-instrumented application was launched.
   * <p>
   * Implementers should ensure that this is not a high-overhead method because
   * it is invoked frequently from the user interface.
   * 
   * @return the time that this Flashlight-instrumented application was
   *         launched.
   */
  @NonNull
  Date getLaunchTime();

  /**
   * Requests that this terminate collection as soon as possible. <b>Only to be
   * invoked by {@link RunControlManager}&mdash;client code should not invoke
   * this method.</b>
   * 
   * @see RunControlManager#stopDataCollectionAsSoonAsPossible(IDataCollectingRun)
   */
  void stopDataCollectionAsSoonAsPossible();
}
