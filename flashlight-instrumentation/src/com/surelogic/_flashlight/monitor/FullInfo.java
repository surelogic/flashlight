package com.surelogic._flashlight.monitor;

public class FullInfo {
  private final AlertInfo alerts;
  private final LockSetInfo lockSets;
  private final SharedFieldInfo sharedFields;
  private final DeadlockInfo deadlocks;

  public FullInfo(final AlertInfo alerts, final LockSetInfo lockSets, final SharedFieldInfo sharedFields,
      final DeadlockInfo deadlocks) {
    super();
    this.alerts = alerts;
    this.lockSets = lockSets;
    this.sharedFields = sharedFields;
    this.deadlocks = deadlocks;
  }

  public AlertInfo getAlerts() {
    return alerts;
  }

  public LockSetInfo getLockSets() {
    return lockSets;
  }

  public SharedFieldInfo getSharedFields() {
    return sharedFields;
  }

  public DeadlockInfo getDeadlocks() {
    return deadlocks;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append(getAlerts().toString());
    b.append(getLockSets().toString());
    b.append(getDeadlocks().toString());
    b.append(getSharedFields().toString());
    return b.toString();
  }
}
