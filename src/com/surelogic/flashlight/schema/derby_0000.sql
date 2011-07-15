CREATE TABLE "FLASHLIGHT"."INTERESTINGFIELD" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."ACCESS" ("ID" BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL, "FIELD" BIGINT NOT NULL, "RW" CHAR(1) NOT NULL, "RECEIVER" BIGINT, "UNDERCONSTRUCTION" CHAR(1) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."ACCESSLOCKSHELD" ("ACCESS" BIGINT NOT NULL, "LOCKSHELD" INT NOT NULL, "LASTACQUISITION" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKDURATION" ("ID" BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "INTHREAD" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL, "START" TIMESTAMP NOT NULL, "STARTEVENT" BIGINT NOT NULL, "STOP" TIMESTAMP NOT NULL, "STOPEVENT" BIGINT NOT NULL, "DURATION" BIGINT NOT NULL, "STATE" VARCHAR(10) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."RUN" ("NAME" VARCHAR(200) NOT NULL, "RAWDATAVERSION" VARCHAR(100) NOT NULL, "HOSTNAME" VARCHAR(255), "USERNAME" VARCHAR(100) NOT NULL, "JAVAVERSION" VARCHAR(100) NOT NULL, "JAVAVENDOR" VARCHAR(100) NOT NULL, "OSNAME" VARCHAR(100) NOT NULL, "OSARCH" VARCHAR(100) NOT NULL, "OSVERSION" VARCHAR(100) NOT NULL, "MAXMEMORYMB" INTEGER NOT NULL, "PROCESSORS" INTEGER NOT NULL, "STARTED" TIMESTAMP NOT NULL, "DURATION" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKSHELD" ("LOCKEVENT" BIGINT NOT NULL, "LOCKHELDEVENT" BIGINT NOT NULL, "LOCKHELD" BIGINT NOT NULL, "LOCKACQUIRED" BIGINT NOT NULL, "INTHREAD" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL, "THREAD" BIGINT NOT NULL, "READCOUNT" BIGINT NOT NULL, "WRITECOUNT" BIGINT NOT NULL, "READUCCOUNT" BIGINT NOT NULL DEFAULT 0, "WRITEUCCOUNT" BIGINT NOT NULL DEFAULT 0)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKCYCLE" ("COMPONENT" INTEGER NOT NULL, "LOCKHELD" BIGINT NOT NULL, "LOCKACQUIRED" BIGINT NOT NULL, "COUNT" BIGINT NOT NULL, "FIRSTTIME" TIMESTAMP NOT NULL, "LASTTIME" TIMESTAMP NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELD" ("ID" BIGINT NOT NULL, "FIELDNAME" VARCHAR(32672) NOT NULL, "DECLARINGTYPE" BIGINT NOT NULL, "VISIBILITY" INTEGER NOT NULL, "STATIC" CHAR(1) NOT NULL, "FINAL" CHAR(1) NOT NULL, "VOLATILE" CHAR(1) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."BADPUBLISH" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."SITE" ("ID" BIGINT NOT NULL, "ATLINE" INTEGER NOT NULL, "INCLASS" BIGINT NOT NULL, "INFILE" VARCHAR(255) NOT NULL, "LOCATION" VARCHAR(255) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" ("FIELD" BIGINT NOT NULL, "THREAD" BIGINT NOT NULL, "READCOUNT" BIGINT NOT NULL, "WRITECOUNT" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."RWLOCK" ("ID" BIGINT NOT NULL, "READLOCK" BIGINT NOT NULL, "WRITELOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."TRACE" ("ID" BIGINT NOT NULL, "SITE" BIGINT NOT NULL, "PARENT" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCK" ("ID" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL, "OBJECT" BIGINT NOT NULL, "TYPE" CHAR(1) NOT NULL, "STATE" VARCHAR(20) NOT NULL, "SUCCESS" CHAR(1), "LOCKISTHIS" CHAR(1), "LOCKISCLASS" CHAR(1), "TRACE" BIGINT NOT NULL DEFAULT -1)
<<>>
CREATE TABLE "FLASHLIGHT"."OBJECT" ("ID" BIGINT NOT NULL, "TYPE" BIGINT NOT NULL, "THREADNAME" VARCHAR(32672), "PACKAGENAME" VARCHAR(32672), "CLASSNAME" VARCHAR(32672), "FLAG" CHAR(1))
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDLOCKSET" ("FIELD" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."INDIRECTACCESS" ("ID" BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."INDIRECTACCESSLOCKSHELD" ("ACCESS" BIGINT NOT NULL, "LOCKSHELD" INT NOT NULL, "LASTACQUISITION" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDASSIGNMENT" ("FIELD" BIGINT NOT NULL, "VALUE" BIGINT NOT NULL, "RECEIVER" BIGINT)
<<>>
-- ----------------------------------------------
-- DDL Statements for checks
-- ----------------------------------------------
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_LOCKISCLASS_CN" CHECK (LockIsClass IS NULL OR LockIsClass IN ('Y', 'N'))
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_LOCKISTHIS_CN" CHECK (LockIsThis IS NULL OR LockIsThis IN ('Y', 'N'))
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_SUCCESS" CHECK (Success IS NULL OR Success IN ('Y','N'))
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_STATE_CN" CHECK (State IN ('BEFORE ACQUISITION', 'AFTER ACQUISITION', 'BEFORE WAIT', 'AFTER WAIT', 'AFTER RELEASE'))
<<>>
ALTER TABLE "FLASHLIGHT"."LOCK" ADD CONSTRAINT "LOCK_TYPE_CN" CHECK (Type IN ('U','I'))
<<>>
ALTER TABLE "FLASHLIGHT"."LOCKDURATION" ADD CONSTRAINT "LOCKDURATION_STATE_CN" CHECK (State IN ('BLOCKING', 'HOLDING', 'WAITING'))
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "ACCESS_UNDERCONSTRUCTION_CN" CHECK (UnderConstruction IN ('Y','N'))
<<>>
ALTER TABLE "FLASHLIGHT"."ACCESS" ADD CONSTRAINT "ACCESS_RW_CN" CHECK (RW IN ('R', 'W'))
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" ADD CONSTRAINT "FIELD_VOLATILE_CN" CHECK (Volatile IN ('Y', 'N'))
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" ADD CONSTRAINT "FIELD_FINAL_CN" CHECK (Final IN ('Y', 'N'))
<<>>
ALTER TABLE "FLASHLIGHT"."FIELD" ADD CONSTRAINT "FIELD_STATIC_CN" CHECK (Static IN ('Y', 'N'))
<<>>
ALTER TABLE "FLASHLIGHT"."OBJECT" ADD CONSTRAINT "OBJECT_FLAG_CN" CHECK (FLAG IS NOT NULL AND FLAG IN ('C','O','T'))
<<>>

