---------------------------------------------------------------------
-- The database schema definition for Flashlight
--
-- This script is automatically processed, ensure that nothing can
-- generate a result set (i.e., no queries).
---------------------------------------------------------------------

create table RUN (
  Run            INT          NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  Name           VARCHAR(200) NOT NULL,
  RawDataVersion VARCHAR(100) NOT NULL,
  UserName       VARCHAR(100) NOT NULL,
  JavaVersion    VARCHAR(100) NOT NULL,
  JavaVendor     VARCHAR(100) NOT NULL,
  OsName         VARCHAR(100) NOT NULL,
  OsArch         VARCHAR(100) NOT NULL,
  OsVersion      VARCHAR(100) NOT NULL,
  MaxMemoryMB    INT          NOT NULL,
  Processors     INT          NOT NULL,
  Started        TIMESTAMP    NOT NULL
)
<<>>

create table OBJECT ( -- class, object, and thread definitions
  Run         INT            NOT NULL CONSTRAINT OBJECT_Run_FK REFERENCES RUN (Run),
  Id          BIGINT         NOT NULL,
  Type        BIGINT         NOT NULL,
  ThreadName  VARCHAR(32672), -- null if the object is not an instance of java.lang.Thread
  PackageName VARCHAR(32672), -- null unless this is a class
  ClassName   VARCHAR(32672), -- null unless this is a class
  PRIMARY KEY (Run, Id),
  CONSTRAINT OBJECT_Type_FK FOREIGN KEY (Run, Type) REFERENCES OBJECT (Run, Id)
)
<<>>

create table FIELD ( -- field definitions
  Run           INT            NOT NULL CONSTRAINT FIELD_Run_FK REFERENCES RUN (Run),
  Id            BIGINT         NOT NULL,
  FieldName     VARCHAR(32672) NOT NULL,
  DeclaringType BIGINT         NOT NULL,
  Static        CHAR(1)        NOT NULL CONSTRAINT FIELD_Static_CN CHECK (Static IN ('Y', 'N')),
  Final         CHAR(1)        NOT NULL CONSTRAINT FIELD_Final_CN CHECK (Final IN ('Y', 'N')),
  Volatile      CHAR(1)        NOT NULL CONSTRAINT FIELD_Volatile_CN CHECK (Volatile IN ('Y', 'N')),
  PRIMARY KEY (Run, Id),
  CONSTRAINT FIELD_DeclaringType_FK FOREIGN KEY (Run, DeclaringType) REFERENCES OBJECT (Run, Id)
)
<<>>

create table ACCESS ( -- field access (read/write) events
  Run      INT          NOT NULL CONSTRAINT ACCESS_Run_FK REFERENCES RUN (Run),
  TS       TIMESTAMP    NOT NULL,
  InThread BIGINT       NOT NULL,
  InFile   VARCHAR(200) NOT NULL,
  AtLine   INT          NOT NULL,
  Field    BIGINT       NOT NULL,
  RW       CHAR(1)      NOT NULL CONSTRAINT ACCESS_RW_CN CHECK (RW IN ('R', 'W')),
  Receiver BIGINT,
  CONSTRAINT ACCESS_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ACCESS_Field_FK FOREIGN KEY (Run, Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT ACCESS_Receiver_FK FOREIGN KEY (Run, Receiver) REFERENCES OBJECT (Run, Id)
)
<<>>

create table ILOCK ( -- intrinsic lock events
  Run         INT          NOT NULL CONSTRAINT ILOCK_Run_FK REFERENCES RUN (Run),
  Id          BIGINT       NOT NULL,
  TS          TIMESTAMP    NOT NULL,
  InThread    BIGINT       NOT NULL,
  InFile      VARCHAR(200) NOT NULL,
  AtLine      INT          NOT NULL,
  Lock        BIGINT       NOT NULL,
  State       VARCHAR(20)  NOT NULL CONSTRAINT ILOCK_State_CN CHECK
    (State IN ('BEFORE ACQUISITION', 'AFTER ACQUISITION', 'BEFORE WAIT', 'AFTER WAIT', 'AFTER RELEASE')),
  LockIsThis  CHAR(1)      CONSTRAINT ILOCK_LockIsThis_CN CHECK (LockIsThis IN ('Y', 'N')),
  LockIsClass CHAR(1)      CONSTRAINT ILOCK_LockIsClass_CN CHECK (LockIsClass IN ('Y', 'N')),
  PRIMARY KEY (Run, Id),
  CONSTRAINT ILOCK_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ILOCK_Lock_FK FOREIGN KEY (Run, Lock) REFERENCES OBJECT (Run, Id)
)
<<>>

create table ILOCKDURATION ( -- derived from ILOCK
  Run          INT       NOT NULL CONSTRAINT ILOCKDURATION_Run_FK REFERENCES RUN (Run),
  InThread     BIGINT    NOT NULL,
  Lock         BIGINT    NOT NULL,
  Start        TIMESTAMP NOT NULL,
  StartEvent   BIGINT    NOT NULL,
  Stop         TIMESTAMP NOT NULL,
  StopEvent    BIGINT    NOT NULL,
  Duration     BIGINT    NOT NULL, -- redundant w/ Start and Stop
  State        VARCHAR(10) NOT NULL CONSTRAINT ILOCKDURATION_State_CN CHECK
    (State IN ('BLOCKING', 'HOLDING', 'WAITING')),
  CONSTRAINT ILOCKDURATION_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ILOCKDURATION_Lock_FK FOREIGN KEY (Run, Lock) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ILOCKDURATION_BlockedEvent_FK FOREIGN KEY (Run, StartEvent) REFERENCES ILOCK (Run, Id),
  CONSTRAINT ILOCKDURATION_AcquiredEvent_FK FOREIGN KEY (Run, StopEvent) REFERENCES ILOCK (Run, Id)
)
<<>>

create table ILOCKSHELD ( -- derived from ILOCK to track locks held when trying to acquire another
  Run          INT       NOT NULL CONSTRAINT ILOCKSHELD_Run_FK REFERENCES RUN (Run),
  LockEvent    BIGINT    NOT NULL,
  LockHeld     BIGINT    NOT NULL,
  LockAcquired BIGINT    NOT NULL, -- redundant w/ event
  InThread     BIGINT    NOT NULL, -- redundant w/ event
  CONSTRAINT ILOCKSHELD_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ILOCKSHELD_Lock_FK FOREIGN KEY (Run, LockHeld) REFERENCES OBJECT (Run, Id),  
  CONSTRAINT ILOCKSHELD_Lock2_FK FOREIGN KEY (Run, LockAcquired) REFERENCES OBJECT (Run, Id),  
  CONSTRAINT ILOCKSHELD_AcquiredEvent_FK FOREIGN KEY (Run, LockEvent) REFERENCES ILOCK (Run, Id)
)
<<>>

create table ILOCKCYCLE ( -- derived from ILOCKSHELD to find locks involved in cycles
  Run          INT       NOT NULL CONSTRAINT ILOCKCYCLE_Run_FK REFERENCES RUN (Run),  
  Lock         BIGINT    NOT NULL, 
  CONSTRAINT ILOCKCYCLE_Lock_FK FOREIGN KEY (Run, Lock) REFERENCES OBJECT (Run, Id)
)
<<>>

create table ILOCKTHREADSTATS ( -- derived from ILOCK to track # of threads over time
  Run          INT       NOT NULL CONSTRAINT ILOCKTHREADSTATS_Run_FK REFERENCES RUN (Run),
  LockEvent    BIGINT    NOT NULL,
  Time         TIMESTAMP NOT NULL,
  Blocking     INT       NOT NULL,
  Holding      INT       NOT NULL,
  Waiting      INT       NOT NULL,
  CONSTRAINT ILOCKTHREADSTATS_AcquiredEvent_FK FOREIGN KEY (Run, LockEvent) REFERENCES ILOCK (Run, Id)
)
<<>>
