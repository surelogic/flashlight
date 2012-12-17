package com.surelogic.flashlight.client.eclipse.model;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.ReferenceObject;
import com.surelogic.ThreadSafe;
import com.surelogic.Vouch;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLJobTracker;
import com.surelogic.flashlight.common.model.FlashlightFileUtility;

@ThreadSafe
@ReferenceObject
public final class LaunchedRun {

  /**
   * Constructs a new instance in the
   * {@link RunState#INSTRUMENTATION_AND_LAUNCH} state.
   * 
   * @param runIdString
   *          a run identity string.
   */
  public LaunchedRun(@NonNull final String runIdString) {
    if (runIdString == null)
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    f_runIdString = runIdString;
    f_runLabel = FlashlightFileUtility.getSimpleRunName(FlashlightFileUtility.getRunName(runIdString));
    f_isAndroid = FlashlightFileUtility.isAndroid(runIdString);
    f_startTime = new Date();
  }

  @NonNull
  private final String f_runIdString;

  @NonNull
  public String getRunIdString() {
    return f_runIdString;
  }

  @NonNull
  private final String f_runLabel;

  @NonNull
  public String getRunLabel() {
    return f_runLabel;
  }

  private final boolean f_isAndroid;

  public boolean isAndroid() {
    return f_isAndroid;
  }

  @NonNull
  @Vouch("ThreadSafe")
  private final Date f_startTime;

  @NonNull
  public Date getStartTime() {
    return f_startTime;
  }

  @NonNull
  private final AtomicReference<RunState> f_state = new AtomicReference<RunState>(RunState.INSTRUMENTATION_AND_LAUNCH);

  @NonNull
  public RunState getState() {
    return f_state.get();
  }

  public boolean isFinishedCollectingData() {
    return RunState.IS_FINISHED_COLLECTING_DATA.contains(f_state.get());
  }

  public boolean isReady() {
    return RunState.READY.equals(f_state.get());
  }

  /**
   * Sets the state of this launched run.
   * 
   * @param value
   *          a state. Ignored if {@code null}.
   * @return {@code true} if the state changed, {@code false} if the state was
   *         not changed.
   */
  boolean setState(@Nullable final RunState value) {
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));

    final RunState oldValue = f_state.getAndSet(value);
    return value != oldValue;
  }

  private final AtomicReference<SLJobTracker> f_prepTracker = new AtomicReference<SLJobTracker>();

  @Nullable
  public SLJobTracker getPrepareJobTracker() {
    return f_prepTracker.get();
  }

  public void setPrepareJobTracker(@Nullable final SLJobTracker tracker) {
    f_prepTracker.set(tracker);
  }

  private final AtomicBoolean f_displayToUser = new AtomicBoolean(true);

  /**
   * Gets the state of if the user wants to see this launched run.
   * 
   * @return {@code true} if the user wants to see this launched run,
   *         {@code false} if the user does not want to see this launched run.
   */
  public boolean getDisplayToUser() {
    return f_displayToUser.get();
  }

  /**
   * Sets the state of if the user wants to see this launched run.
   * 
   * @param value
   *          {@code true} if the user wants to see this launched run,
   *          {@code false} if the user does not want to see this launched run.
   * @return {@code true} if the value changed, {@code false} if the value was
   *         not changed.
   */
  boolean setDisplayToUser(boolean value) {
    final boolean oldValue = f_displayToUser.getAndSet(value);
    return value != oldValue;
  }
}
