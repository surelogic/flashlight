
CREATE TABLE LOCK ( -- All locking events
  Run         INT          NOT NULL CONSTRAINT LOCK_Run_FK REFERENCES RUN (Run),
  Id          BIGINT       NOT NULL,
  TS          TIMESTAMP    NOT NULL,
  InThread    BIGINT       NOT NULL,
  InClass     BIGINT       NOT NULL,
  AtLine      INT          NOT NULL,
  Lock        BIGINT       NOT NULL,
  Type        CHAR(1)      NOT NULL CONSTRAINT LOCK_TYPE_CN CHECK (Type IN ('U','I')), -- U(til.concurrent), I(ntrinsic)  
  State       VARCHAR(20)  NOT NULL CONSTRAINT LOCK_State_CN CHECK
    (State IN ('BEFORE ACQUISITION', 'AFTER ACQUISITION', 'BEFORE WAIT', 'AFTER WAIT', 'AFTER RELEASE')),
  Success     CHAR(1)      CONSTRAINT LOCK_SUCCESS CHECK (Success IS NULL OR Success IN ('Y','N')), -- util.concurrent lock property
  LockIsThis  CHAR(1)      CONSTRAINT LOCK_LockIsThis_CN CHECK (LockIsThis IS NULL OR LockIsThis IN ('Y', 'N')), -- Intrinsic lock property
  LockIsClass CHAR(1)      CONSTRAINT LOCK_LockIsClass_CN CHECK (LockIsClass IS NULL OR LockIsClass IN ('Y', 'N')), -- Intrinsic lock property
  PRIMARY KEY (Run, Id),
  CONSTRAINT LOCK_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT LOCK_InClass_FK FOREIGN KEY (Run, InClass) REFERENCES OBJECT (Run, Id),
  CONSTRAINT LOCK_Lock_FK FOREIGN KEY (Run, Lock) REFERENCES OBJECT (Run, Id)
)
<<>>
