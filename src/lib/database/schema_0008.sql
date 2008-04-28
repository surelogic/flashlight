create table LOCKDURATION ( -- derived from LOCK
  Run          INT       NOT NULL CONSTRAINT LOCKDURATION_Run_FK REFERENCES RUN (Run),
  InThread     BIGINT    NOT NULL,
  Lock         BIGINT    NOT NULL,
  Start        TIMESTAMP NOT NULL,
  StartEvent   BIGINT    NOT NULL,
  Stop         TIMESTAMP NOT NULL,
  StopEvent    BIGINT    NOT NULL,
  Duration     BIGINT    NOT NULL, -- redundant w/ Start and Stop
  State        VARCHAR(10) NOT NULL CONSTRAINT LOCKDURATION_State_CN CHECK
    (State IN ('BLOCKING', 'HOLDING', 'WAITING')),
  CONSTRAINT LOCKDURATION_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT LOCKDURATION_Lock_FK FOREIGN KEY (Run, Lock) REFERENCES OBJECT (Run, Id),
  CONSTRAINT LOCKDURATION_BlockedEvent_FK FOREIGN KEY (Run, StartEvent) REFERENCES LOCK (Run, Id),
  CONSTRAINT LOCKDURATION_AcquiredEvent_FK FOREIGN KEY (Run, StopEvent) REFERENCES LOCK (Run, Id)
)
<<>>
create table LOCKSHELD ( -- derived from LOCK to track locks held when trying to acquire another
  Run          INT       NOT NULL CONSTRAINT LOCKSHELD_Run_FK REFERENCES RUN (Run),
  LockEvent    BIGINT    NOT NULL,
  LockHeld     BIGINT    NOT NULL,
  LockAcquired BIGINT    NOT NULL, -- redundant w/ event
  InThread     BIGINT    NOT NULL, -- redundant w/ event
  CONSTRAINT LOCKSHELD_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT LOCKSHELD_Lock_FK FOREIGN KEY (Run, LockHeld) REFERENCES OBJECT (Run, Id),  
  CONSTRAINT LOCKSHELD_Lock2_FK FOREIGN KEY (Run, LockAcquired) REFERENCES OBJECT (Run, Id),  
  CONSTRAINT LOCKSHELD_AcquiredEvent_FK FOREIGN KEY (Run, LockEvent) REFERENCES LOCK (Run, Id)
)
<<>>

create table LOCKCYCLE ( -- derived from LOCKSHELD to find locks involved in cycles
  Run          INT       NOT NULL CONSTRAINT LOCKCYCLE_Run_FK REFERENCES RUN (Run),  
  Component    INT       NOT NULL,
  LockHeld     BIGINT    NOT NULL, 
  LockAcquired BIGINT    NOT NULL, 
  Count        BIGINT    NOT NULL, 
  FirstTime    TIMESTAMP NOT NULL,
  LastTime     TIMESTAMP NOT NULL,
  CONSTRAINT LOCKCYCLE_LockHeld_FK FOREIGN KEY (Run, LockHeld) REFERENCES OBJECT (Run, Id),
  CONSTRAINT LOCKCYCLE_LockAcquired_FK FOREIGN KEY (Run, LockAcquired) REFERENCES OBJECT (Run, Id)  
)
<<>>

create table LOCKTHREADSTATS ( -- derived from LOCK to track # of threads over time
  Run          INT       NOT NULL CONSTRAINT LOCKTHREADSTATS_Run_FK REFERENCES RUN (Run),
  LockEvent    BIGINT    NOT NULL,
  Time         TIMESTAMP NOT NULL,
  Blocking     INT       NOT NULL,
  Holding      INT       NOT NULL,
  Waiting      INT       NOT NULL,
  CONSTRAINT LOCKTHREADSTATS_AcquiredEvent_FK FOREIGN KEY (Run, LockEvent) REFERENCES LOCK (Run, Id)
)
<<>>


DROP TABLE ILOCKCYCLE
<<>>
DROP TABLE ILOCKSHELD
<<>>
DROP TABLE ILOCKTHREADSTATS
<<>>
DROP TABLE ILOCKDURATION
<<>>
DROP TABLE ILOCK
<<>>