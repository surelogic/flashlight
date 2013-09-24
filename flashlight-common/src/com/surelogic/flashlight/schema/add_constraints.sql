-- ----------------------------------------------
-- DDL Statements for indexes
-- ----------------------------------------------
CREATE INDEX "FLASHLIGHT"."LOCKSHELD_LOCKHELD_INDEX" ON "FLASHLIGHT"."LOCKSHELD" ("LOCKHELD")
<<>>
CREATE INDEX "FLASHLIGHT"."LOCKSHELD_LOCKACQUIRED_INDEX" ON "FLASHLIGHT"."LOCKSHELD" ("LOCKACQUIRED")
<<>>
CREATE INDEX "FLASHLIGHT"."LOCKDURATION_TIME_INDEX" ON "FLASHLIGHT"."LOCKDURATION" ("INTHREAD","START","STOP")
<<>>
CREATE INDEX "FLASHLIGHT"."LOCKDURATION_STATE_INDEX" ON "FLASHLIGHT"."LOCKDURATION" ("STATE")
<<>>
CREATE INDEX "FLASHLIGHT"."LOCKDURATION_START_INDEX" ON "FLASHLIGHT"."LOCKDURATION" ("INTHREAD", "START")
<<>>
CREATE INDEX "FLASHLIGHT"."LOCKDURATION_STOP_INDEX" ON "FLASHLIGHT"."LOCKDURATION" ("INTHREAD", "STOP")
<<>>
CREATE INDEX "FLASHLIGHT"."FIELD_STATIC_INDEX" ON "FLASHLIGHT"."FIELD" ("STATIC")
<<>>
CREATE INDEX "FLASHLIGHT"."FIELD_VOLATILE_INDEX" ON "FLASHLIGHT"."FIELD" ("VOLATILE")
<<>>
CREATE INDEX "FLASHLIGHT"."OBJECT_FLAG_INDEX" ON "FLASHLIGHT"."OBJECT" ("FLAG")
<<>>
--Needed for 'Where are fields accessed during this particular lock acquisition?
CREATE INDEX "FLASHLIGHT"."ACCESS_TS_INDEX" ON "FLASHLIGHT"."ACCESS" ("TS")
<<>>
--Needed for Flashlight.Region.lockIsField
CREATE INDEX "FLASHLIGHT"."FIELDASSIGNMENT_VALUE_INDEX" ON "FLASHLIGHT"."FIELDASSIGNMENT" ("VALUE")
<<>>
--Needed for Accesses.happensBefore
CREATE INDEX "FLASHLIGHT"."HAPPENSBEFORE_TS_INDEX" ON "FLASHLIGHT"."HAPPENSBEFORE" ("TS")
<<>>
CREATE INDEX "FLASHLIGHT"."HAPPENSBEFORECOLLSOURCE_TS_INDEX" ON "FLASHLIGHT"."HAPPENSBEFORECOLLSOURCE" ("INTHREAD", "TS")
<<>>
CREATE INDEX "FLASHLIGHT"."HAPPENSBEFORECOLLTARGET_TS_INDEX" ON "FLASHLIGHT"."HAPPENSBEFORECOLLTARGET" ("INTHREAD", "TS")
<<>>
CREATE INDEX "FLASHLIGHT"."HAPPENSBEFORESOURCE_TS_INDEX" ON "FLASHLIGHT"."HAPPENSBEFORESOURCE" ("INTHREAD", "TS")
<<>>
CREATE INDEX "FLASHLIGHT"."HAPPENSBEFORETARGET_TS_INDEX" ON "FLASHLIGHT"."HAPPENSBEFORETARGET" ("INTHREAD", "TS")
<<>>
-- ----------------------------------------------
-- DDL Statements for keys
-- ----------------------------------------------
-- primary/unique
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" ADD CONSTRAINT "SQL081022155658030" PRIMARY KEY ("FIELD", "RECEIVER", "LOCK")
<<>>
ALTER TABLE "FLASHLIGHT"."RUN" ADD CONSTRAINT "SQL081022155658100" PRIMARY KEY ("NAME", "RAWDATAVERSION", "USERNAME", "JAVAVERSION", "JAVAVENDOR", "OSNAME", "OSARCH", "OSVERSION", "MAXMEMORYMB", "PROCESSORS", "STARTED")
<<>>
ALTER TABLE "FLASHLIGHT"."TRACE" ADD CONSTRAINT "SQL081022155702640" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "SQL081022155658130" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "SQL081022155702690" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" ADD CONSTRAINT "SQL081022155704730" PRIMARY KEY ("FIELD", "RECEIVER", "THREAD")
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "SQL081022155704830" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" ADD CONSTRAINT "SQL081022155704740" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" ADD CONSTRAINT "SQL081022155704760" PRIMARY KEY ("FIELD", "THREAD")
<<>>
ALTER TABLE "FLASHLIGHT"."OBJECT" ADD CONSTRAINT "SQL081022155704770" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."SITE" ADD CONSTRAINT "SQL081022155709820" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" ADD CONSTRAINT "SQL081022155709770" PRIMARY KEY ("FIELD", "LOCK")
<<>>
ALTER TABLE "FLASHLIGHT"."INDIRECTACCESS" ADD CONSTRAINT "INDIRECTACCESS_PK" PRIMARY KEY ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKTRACE" ADD CONSTRAINT "LOCKTRACE_PK" PRIMARY KEY ("ID")
<<>>
-- foreign
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" ADD CONSTRAINT "FIELDINSTANCELOCKSET_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" ADD CONSTRAINT "FIELDINSTANCELOCKSET_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" ADD CONSTRAINT "FIELDINSTANCELOCKSET_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."TRACE" ADD CONSTRAINT "TRACE_PARENT_FK" FOREIGN KEY ("PARENT") REFERENCES "FLASHLIGHT"."TRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKTRACE" ADD CONSTRAINT "LOCKTRACE_PARENT_FK" FOREIGN KEY ("PARENT") REFERENCES "FLASHLIGHT"."LOCKTRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKTRACE" ADD CONSTRAINT "LOCKTRACE_TRACE_FK" FOREIGN KEY ("TRACE") REFERENCES "FLASHLIGHT"."TRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
--ALTER TABLE "FLASHLIGHT"."TRACE" ADD CONSTRAINT "TRACE_SITE_FK" FOREIGN KEY ("SITE") REFERENCES "FLASHLIGHT"."SITE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
--<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_TRACE_FK" FOREIGN KEY ("TRACE") REFERENCES "FLASHLIGHT"."TRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_LOCKTRACE_FK" FOREIGN KEY ("LOCKTRACE") REFERENCES "FLASHLIGHT"."LOCKTRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" ADD CONSTRAINT "LOCKSHELD_INTHREAD_FK" FOREIGN KEY ("INTHREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" ADD CONSTRAINT "LOCKSHELD_LOCK_FK" FOREIGN KEY ("LOCKHELD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" ADD CONSTRAINT "LOCKSHELD_LOCK2_FK" FOREIGN KEY ("LOCKACQUIRED") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKSHELD" ADD CONSTRAINT "LOCKSHELD_ACQUIREDEVENT_FK" FOREIGN KEY ("LOCKEVENT") REFERENCES "FLASHLIGHT"."LOCK" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."BADPUBLISH" ADD CONSTRAINT "BADPUBLISH_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."BADPUBLISH" ADD CONSTRAINT "BADPUBLISH_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" ADD CONSTRAINT "RWLOCK_ID_FK" FOREIGN KEY ("ID") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" ADD CONSTRAINT "RWLOCK_READLOCK_FK" FOREIGN KEY ("READLOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."RWLOCK" ADD CONSTRAINT "RWLOCK_WRITELOCK_FK" FOREIGN KEY ("WRITELOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."INTERESTINGFIELD" ADD CONSTRAINT "INTERESTINGFIELD_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."INTERESTINGFIELD" ADD CONSTRAINT "INTERESTINGFIELD_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_INTHREAD_FK" FOREIGN KEY ("INTHREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_STARTEVENT_FK" FOREIGN KEY ("STARTEVENT") REFERENCES "FLASHLIGHT"."LOCK" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_STOPEVENT_FK" FOREIGN KEY ("STOPEVENT") REFERENCES "FLASHLIGHT"."LOCK" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_STARTTRACE_FK" FOREIGN KEY ("STARTTRACE") REFERENCES "FLASHLIGHT"."LOCKTRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_STOPTRACE_FK" FOREIGN KEY ("STOPTRACE") REFERENCES "FLASHLIGHT"."LOCKTRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" ADD CONSTRAINT "FIELDINSTANCETHREAD_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" ADD CONSTRAINT "FIELDINSTANCETHREAD_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" ADD CONSTRAINT "FIELDINSTANCETHREAD_THREAD_FK" FOREIGN KEY ("THREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "ACCESS_INTHREAD_FK" FOREIGN KEY ("INTHREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "ACCESS_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "ACCESS_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "ACCESS_TRACE_FK" FOREIGN KEY ("TRACE") REFERENCES "FLASHLIGHT"."TRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" ADD CONSTRAINT "FIELD_DECLARINGTYPE_FK" FOREIGN KEY ("DECLARINGTYPE") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLE" ADD CONSTRAINT "LOCKCYCLE_LOCKHELD_FK" FOREIGN KEY ("LOCKHELD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLE" ADD CONSTRAINT "LOCKCYCLE_LOCKACQUIRED_FK" FOREIGN KEY ("LOCKACQUIRED") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLETHREAD" ADD CONSTRAINT "LOCKCYCLETHREAD_LOCKHELD_FK" FOREIGN KEY ("LOCKHELD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLETHREAD" ADD CONSTRAINT "LOCKCYCLETHREAD_LOCKACQUIRED_FK" FOREIGN KEY ("LOCKACQUIRED") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCYCLETHREAD" ADD CONSTRAINT "LOCKCYCLETHREAD_THREAD_FK" FOREIGN KEY ("THREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" ADD CONSTRAINT "FIELDSTATICTHREAD_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" ADD CONSTRAINT "FIELDSTATICTHREAD_THREAD_FK" FOREIGN KEY ("THREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."OBJECT" ADD CONSTRAINT "OBJECT_TYPE_FK" FOREIGN KEY ("TYPE") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."SITE" ADD CONSTRAINT "SITE_INCLASS_FK" FOREIGN KEY ("INCLASS") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" ADD CONSTRAINT "FIELDLOCKSET_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDLOCKSET" ADD CONSTRAINT "FIELDLOCKSET_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."INDIRECTACCESS" ADD CONSTRAINT "INDIRECTACCESS_INTHREAD_FK" FOREIGN KEY ("INTHREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."INDIRECTACCESS" ADD CONSTRAINT "INDIRECTACCESS_TRACE_FK" FOREIGN KEY ("TRACE") REFERENCES "FLASHLIGHT"."TRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."INDIRECTACCESS" ADD CONSTRAINT "INDIRECTACCESS_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDASSIGNMENT" ADD CONSTRAINT "FA_FIELD_FK" FOREIGN KEY ("FIELD") REFERENCES "FLASHLIGHT"."FIELD" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."FIELDASSIGNMENT" ADD CONSTRAINT "FA_RECEIVER_FK" FOREIGN KEY ("RECEIVER") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."HAPPENSBEFORE" ADD CONSTRAINT "HB_TARGET_FK" FOREIGN KEY ("TARGET") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."HAPPENSBEFORE" ADD CONSTRAINT "HB_TRACE_FK" FOREIGN KEY ("TRACE") REFERENCES "FLASHLIGHT"."TRACE" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."HAPPENSBEFORE" ADD CONSTRAINT "HB_THREAD_FK" FOREIGN KEY ("INTHREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."HAPPENSBEFORE" ADD CONSTRAINT "HB_SOURCE_FK" FOREIGN KEY ("SOURCE") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKCOMPONENT" ADD CONSTRAINT "LOCKCOMPONENTS_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID") ON DELETE NO ACTION ON UPDATE NO ACTION
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKPACKAGE" ADD CONSTRAINT "LOCKPACKAGE_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKINFO" ADD CONSTRAINT "LOCKINFO_LOCK_FK" FOREIGN KEY ("LOCK") REFERENCES "FLASHLIGHT"."OBJECT" ("ID")
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKINFO" ADD CONSTRAINT "LOCKINFO_THREAD_FK" FOREIGN KEY ("INTHREAD") REFERENCES "FLASHLIGHT"."OBJECT" ("ID")
<<>>
