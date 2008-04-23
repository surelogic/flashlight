DROP TABLE ILOCKCYCLE
<<>>

create table ILOCKCYCLE ( -- derived from ILOCKSHELD to find locks involved in cycles
  Run          INT       NOT NULL CONSTRAINT ILOCKCYCLE_Run_FK REFERENCES RUN (Run),  
  LockHeld     BIGINT    NOT NULL, 
  LockAcquired BIGINT    NOT NULL, 
  Count        BIGINT    NOT NULL, 
  FirstTime    TIMESTAMP NOT NULL,
  LastTime     TIMESTAMP NOT NULL,
  CONSTRAINT ILOCKCYCLE_LockHeld_FK FOREIGN KEY (Run, LockHeld) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ILOCKCYCLE_LockAcquired_FK FOREIGN KEY (Run, LockAcquired) REFERENCES OBJECT (Run, Id)  
)
<<>>


