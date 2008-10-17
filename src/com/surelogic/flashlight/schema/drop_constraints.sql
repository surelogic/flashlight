ALTER TABLE "FLASHLIGHT"."SITE" DROP CONSTRAINT "SITE_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."SITE" DROP CONSTRAINT "SITE_UNIQUE_CN"
<<>>
ALTER TABLE "FLASHLIGHT"."SITE" DROP CONSTRAINT "SITE_INCLASS_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" DROP CONSTRAINT "FIELDLOCKSET_LOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" DROP CONSTRAINT "FIELDLOCKSET_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" DROP CONSTRAINT "FIELDLOCKSET_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" DROP CONSTRAINT "ACCESS_INCLASS_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" DROP CONSTRAINT "ACCESS_RECEIVER_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" DROP CONSTRAINT "ACCESS_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" DROP CONSTRAINT "ACCESS_INTHREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" DROP CONSTRAINT "ACCESS_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."OBJECT" DROP CONSTRAINT "OBJECT_TYPE_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."OBJECT" DROP CONSTRAINT "OBJECT_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" DROP CONSTRAINT "FIELDSTATICTHREAD_THREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" DROP CONSTRAINT "FIELDSTATICTHREAD_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" DROP CONSTRAINT "FIELDSTATICTHREAD_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLE" DROP CONSTRAINT "LOCKCYCLE_LOCKACQUIRED_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLE" DROP CONSTRAINT "LOCKCYCLE_LOCKHELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLE" DROP CONSTRAINT "LOCKCYCLE_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" DROP CONSTRAINT "FIELD_DECLARINGTYPE_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" DROP CONSTRAINT "FIELD_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" DROP CONSTRAINT "FIELDINSTANCETHREAD_THREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" DROP CONSTRAINT "FIELDINSTANCETHREAD_RECEIVER_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" DROP CONSTRAINT "FIELDINSTANCETHREAD_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" DROP CONSTRAINT "FIELDINSTANCETHREAD_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" DROP CONSTRAINT "LOCKDURATION_ACQUIREDEVENT_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" DROP CONSTRAINT "LOCKDURATION_BLOCKEDEVENT_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" DROP CONSTRAINT "LOCKDURATION_LOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" DROP CONSTRAINT "LOCKDURATION_INTHREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" DROP CONSTRAINT "LOCKDURATION_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."INTERESTINGFIELD" DROP CONSTRAINT "INTERESTINGFIELD_RECEIVER_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."INTERESTINGFIELD" DROP CONSTRAINT "INTERESTINGFIELD_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."INTERESTINGFIELD" DROP CONSTRAINT "INTERESTINFIELD_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKTHREADSTATS" DROP CONSTRAINT "LOCKTHREADSTATS_ACQUIREDEVENT_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKTHREADSTATS" DROP CONSTRAINT "LOCKTHREADSTATS_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."TRACE" DROP CONSTRAINT "TRACE_SITE_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."TRACE" DROP CONSTRAINT "TRACE_INTHREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."TRACE" DROP CONSTRAINT "TRACE_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" DROP CONSTRAINT "RWLOCK_WRITELOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" DROP CONSTRAINT "RWLOCK_READLOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" DROP CONSTRAINT "RWLOCK_ID_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" DROP CONSTRAINT "RWLOCK_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."BADPUBLISH" DROP CONSTRAINT "BADPUBLISH_RECEIVER_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."BADPUBLISH" DROP CONSTRAINT "BADPUBLISH_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."BADPUBLISH" DROP CONSTRAINT "BADPUBLISH_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" DROP CONSTRAINT "LOCKSHELD_ACQUIREDEVENT_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" DROP CONSTRAINT "LOCKSHELD_LOCK2_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" DROP CONSTRAINT "LOCKSHELD_LOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" DROP CONSTRAINT "LOCKSHELD_INTHREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" DROP CONSTRAINT "LOCKSHELD_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" DROP CONSTRAINT "LOCK_LOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" DROP CONSTRAINT "LOCK_INCLASS_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" DROP CONSTRAINT "LOCK_INTHREAD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" DROP CONSTRAINT "LOCK_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" DROP CONSTRAINT "FIELDINSTANCELOCKSET_RECEIVER_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" DROP CONSTRAINT "FIELDINSTANCELOCKSET_LOCK_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" DROP CONSTRAINT "FIELDINSTANCELOCKSET_FIELD_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" DROP CONSTRAINT "FIELDINSTANCELOCKSET_RUN_FK"
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."SITE" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."OBJECT" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."TRACE" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" DROP PRIMARY KEY
<<>>
ALTER TABLE "FLASHLIGHT"."RUN" DROP PRIMARY KEY
<<>>
DROP INDEX "FLASHLIGHT"."ACCESS_TS_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."LOCKDURATION_START_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."LOCKDURATION_STOP_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."TRACE_START_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."TRACE_STOP_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."LOCKSHELD_LOCKHELD_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."LOCKSHELD_LOCKACQUIRED_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."FIELD_STATIC_INDEX"
<<>>
DROP INDEX "FLASHLIGHT"."LOCK_LINE_INDEX"
<<>>
