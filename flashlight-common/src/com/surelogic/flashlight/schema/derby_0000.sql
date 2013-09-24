CREATE TABLE "FLASHLIGHT"."INTERESTINGFIELD" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."ACCESS" ("ID" BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL, "FIELD" BIGINT NOT NULL, "RW" CHAR(1) NOT NULL, "RECEIVER" BIGINT, "UNDERCONSTRUCTION" CHAR(1) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."ACCESSLOCKSHELD" ("ACCESS" BIGINT NOT NULL, "LOCKSHELD" INT NOT NULL, "LASTACQUISITION" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDINSTANCELOCKSET" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKDURATION" ("ID" BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "INTHREAD" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL, "START" TIMESTAMP NOT NULL, "STARTEVENT" BIGINT NOT NULL, "STARTTRACE" BIGINT, "STOP" TIMESTAMP NOT NULL, "STOPEVENT" BIGINT NOT NULL, "STOPTRACE" BIGINT , "DURATION" BIGINT NOT NULL, "STATE" VARCHAR(10) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."RUN" ("NAME" VARCHAR(200) NOT NULL, "RAWDATAVERSION" VARCHAR(100) NOT NULL, "HOSTNAME" VARCHAR(255), "USERNAME" VARCHAR(100) NOT NULL, "JAVAVERSION" VARCHAR(100) NOT NULL, "JAVAVENDOR" VARCHAR(100) NOT NULL, "OSNAME" VARCHAR(100) NOT NULL, "OSARCH" VARCHAR(100) NOT NULL, "OSVERSION" VARCHAR(100) NOT NULL, "MAXMEMORYMB" INTEGER NOT NULL, "PROCESSORS" INTEGER NOT NULL, "STARTED" TIMESTAMP NOT NULL, "DURATION" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKSHELD" ("LOCKEVENT" BIGINT NOT NULL, "LOCKHELDEVENT" BIGINT NOT NULL, "LOCKHELD" BIGINT NOT NULL, "LOCKACQUIRED" BIGINT NOT NULL, "INTHREAD" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDINSTANCETHREAD" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL, "THREAD" BIGINT NOT NULL, "READCOUNT" BIGINT NOT NULL, "WRITECOUNT" BIGINT NOT NULL, "READUCCOUNT" BIGINT NOT NULL DEFAULT 0, "WRITEUCCOUNT" BIGINT NOT NULL DEFAULT 0)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKCYCLE" ("COMPONENT" INTEGER NOT NULL, "LOCKHELD" BIGINT NOT NULL, "LOCKACQUIRED" BIGINT NOT NULL, "COUNT" BIGINT NOT NULL, "FIRSTTIME" TIMESTAMP NOT NULL, "LASTTIME" TIMESTAMP NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKCYCLETHREAD" ("COMPONENT" INTEGER NOT NULL, "LOCKHELD" BIGINT NOT NULL, "LOCKACQUIRED" BIGINT NOT NULL, "THREAD" BIGINT NOT NULL, "COUNT" BIGINT NOT NULL, "FIRSTTIME" TIMESTAMP NOT NULL, "LASTTIME" TIMESTAMP NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELD" ("ID" BIGINT NOT NULL, "FIELDNAME" VARCHAR(32672) NOT NULL, "DECLARINGTYPE" BIGINT NOT NULL, "VISIBILITY" INTEGER NOT NULL, "STATIC" CHAR(1) NOT NULL, "FINAL" CHAR(1) NOT NULL, "VOLATILE" CHAR(1) NOT NULL, "CODE" VARCHAR(11) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."BADPUBLISH" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."SITE" ("ID" BIGINT NOT NULL, "ATLINE" INTEGER NOT NULL, "INCLASS" BIGINT NOT NULL, "INFILE" VARCHAR(255) NOT NULL, "LOCATION" VARCHAR(255) NOT NULL, "LOCATIONCODE" VARCHAR (11),"METHODCLASS" VARCHAR(32672), "METHODCALL" VARCHAR(255), "METHODSPEC" VARCHAR(32672), "METHODCODE" VARCHAR(11))
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDSTATICTHREAD" ("FIELD" BIGINT NOT NULL, "THREAD" BIGINT NOT NULL, "READCOUNT" BIGINT NOT NULL, "WRITECOUNT" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."RWLOCK" ("ID" BIGINT NOT NULL, "READLOCK" BIGINT NOT NULL, "WRITELOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."TRACE" ("ID" BIGINT NOT NULL, "SITE" BIGINT NOT NULL, "PARENT" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCK" ("ID" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL, "OBJECT" BIGINT NOT NULL, "TYPE" CHAR(1) NOT NULL, "STATE" VARCHAR(20) NOT NULL, "SUCCESS" CHAR(1), "LOCKISTHIS" CHAR(1), "LOCKISCLASS" CHAR(1), "TRACE" BIGINT NOT NULL, "LOCKTRACE" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."OBJECT" ("ID" BIGINT NOT NULL, "TYPE" BIGINT NOT NULL, "FLAG" CHAR(1) NOT NULL, "THREADNAME" VARCHAR(32672), "PACKAGENAME" VARCHAR(32672), "CLASSNAME" VARCHAR(32672), "CLASSCODE" VARCHAR(11))
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDLOCKSET" ("FIELD" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."INDIRECTACCESS" ("ID" BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."INDIRECTACCESSLOCKSHELD" ("ACCESS" BIGINT NOT NULL, "LOCKSHELD" INT NOT NULL, "LASTACQUISITION" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDASSIGNMENT" ("FIELD" BIGINT NOT NULL, "VALUE" BIGINT NOT NULL, "RECEIVER" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDINSTANCESHARED" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFORE" ("ID" VARCHAR (32672), "SOURCE" BIGINT NOT NULL, "TARGET" BIGINT NOT NULL,  "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFORECOLLSOURCE" ("ID" VARCHAR (32672), "COLL" BIGINT NOT NULL, "OBJ" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFORECOLLTARGET" ("ID" VARCHAR (32672), "COLL" BIGINT NOT NULL, "OBJ" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFORESOURCE" ("ID" VARCHAR (32672), "OBJ" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFORETARGET" ("ID" VARCHAR (32672), "OBJ" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."BADHAPPENSBEFORE" ("FIELD" BIGINT NOT NULL, "RECEIVER" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFOREVOLATILEREAD" ("FIELD" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."HAPPENSBEFOREVOLATILEWRITE" ("FIELD" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKCOMPONENT" ("COMPONENT" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."BLOCKSTATS" ("RECEIVER" BIGINT NOT NULL, "FIELD" BIGINT NOT NULL, "INTHREAD" BIGINT NOT NULL, "START" TIMESTAMP NOT NULL, "STOP" TIMESTAMP NOT NULL, "READS" BIGINT NOT NULL, "WRITES" BIGINT NOT NULL, "QUOTIENT" DECIMAL (4,1) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."FIELDBLOCKSTATS" ("RECEIVER" BIGINT NOT NULL, "FIELD" BIGINT NOT NULL, RFIELD BIGINT NOT NULL, "INTHREAD" BIGINT NOT NULL, "START" TIMESTAMP NOT NULL, "STOP" TIMESTAMP NOT NULL, "READS" BIGINT NOT NULL, "WRITES" BIGINT NOT NULL, "QUOTIENT" DECIMAL (3,0) NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."CLASSINIT" ("OBJ" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."CLASSACCESS" ("OBJ" BIGINT NOT NULL, "TS" TIMESTAMP NOT NULL, "INTHREAD" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKTRACE" ("ID" BIGINT NOT NULL, "LOCK" BIGINT NOT NULL, "TRACE" BIGINT NOT NULL, "PARENT" BIGINT)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKPACKAGE" ("LOCK" BIGINT NOT NULL, "PACKAGES" BIGINT NOT NULL)
<<>>
CREATE TABLE "FLASHLIGHT"."LOCKINFO" ("LOCK" BIGINT NOT NULL, "INTHREAD" BIGINT NOT NULL, "COUNT" BIGINT NOT NULL, "DURATION" BIGINT NOT NULL, "AVERAGE" BIGINT NOT NULL, "INTRINSIC" CHAR(1), "JUC" CHAR(1), "MAXTYPES" BIGINT , "MAXPACKAGES" BIGINT)
<<>>

