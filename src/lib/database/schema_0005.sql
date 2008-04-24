create table RWLOCK ( -- Read/Write Lock definitions
  Run       INT          NOT NULL CONSTRAINT RWLOCK_Run_FK REFERENCES RUN (Run),
  Id        BIGINT       NOT NULL,
  ReadLock  BIGINT       NOT NULL,
  WriteLock BIGINT       NOT NULL,
  CONSTRAINT RWLOCK_Id_FK FOREIGN KEY (Run, Id) REFERENCES OBJECT (Run, Id),
  CONSTRAINT RWLOCK_ReadLock_FK FOREIGN KEY (Run, ReadLock) REFERENCES OBJECT (Run, Id),
  CONSTRAINT RWLOCK_WriteLock_FK FOREIGN KEY (Run, WriteLock) REFERENCES OBJECT (Run, Id)
)
<<>>
create table UCLOCK ( -- java.util.concurrent lock events
  Run         INT          NOT NULL CONSTRAINT UCLOCK_Run_FK REFERENCES RUN (Run),
  Id          BIGINT       NOT NULL,
  TS          TIMESTAMP    NOT NULL,
  InThread    BIGINT       NOT NULL,
  InClass     BIGINT       NOT NULL,
  AtLine      INT          NOT NULL,
  Lock        BIGINT       NOT NULL,
  Type        CHAR(1)      NOT NULL CONSTRAINT UCLOCK_TYPE_CN CHECK (Type IN ('B','A','R')), -- B(efore acquisition attempt),A(fter acquisition attempt),R(elease attempt),
  Success     CHAR(1)      CONSTRAINT UCLOCK_SUCCESS CHECK (Success IS NULL OR Success IN ('Y','N')),
  PRIMARY KEY (Run, Id),
  CONSTRAINT UCLOCK_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT UCLOCK_InClass_FK FOREIGN KEY (Run, InClass) REFERENCES OBJECT (Run, Id),
  CONSTRAINT UCLOCK_Lock_FK FOREIGN KEY (Run, Lock) REFERENCES OBJECT (Run, Id)
)
<<>>
