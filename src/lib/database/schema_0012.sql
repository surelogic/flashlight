
CREATE TABLE FIELDSTATICTHREAD ( --Read/Write counts of each static field  per thread
  Run         INT          NOT NULL CONSTRAINT FIELDSTATICTHREAD_Run_FK REFERENCES RUN (Run),
  Field       BIGINT       NOT NULL,
  Thread      BIGINT       NOT NULL,
  ReadCount   BIGINT       NOT NULL,
  WriteCount  BIGINT       NOT NULL,
  CONSTRAINT FIELDSTATICTHREAD_Field_FK FOREIGN KEY (Run, Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT FIELDSTATICTHREAD_Thread_FK FOREIGN KEY (Run, Thread) REFERENCES OBJECT (Run, Id),
  PRIMARY KEY (Run, Field, Thread)
)
<<>>
CREATE TABLE INTERESTINGFIELD (
  Run         INT          NOT NULL CONSTRAINT INTERESTINFIELD_Run_FK REFERENCES RUN (Run),
  Field       BIGINT       NOT NULL,
  Receiver    BIGINT,
  CONSTRAINT INTERESTINGFIELD_Field_FK FOREIGN KEY (Run, Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT INTERESTINGFIELD_Receiver_FK  FOREIGN KEY (Run, Receiver) REFERENCES OBJECT (Run, Id)
)
<<>>