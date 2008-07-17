
DROP TABLE TRACE
<<>>
create table TRACE ( -- stack trace durations
  Run         INT           NOT NULL CONSTRAINT TRACE_Run_FK REFERENCES RUN (Run),
  Id          BIGINT        NOT NULL,
  InThread    BIGINT        NOT NULL,
  InClass     BIGINT        NOT NULL,
  InFile      VARCHAR(255)  NOT NULL,
  AtLine      INT           NOT NULL,
  Location    VARCHAR(255)  NOT NULL,
  Start       TIMESTAMP     NOT NULL,
  Stop        TIMESTAMP     NOT NULL,
  PRIMARY KEY (Run, Id),
  CONSTRAINT TRACE_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT TRACE_InClass_FK  FOREIGN KEY (Run, InClass) REFERENCES OBJECT (Run, Id)
)
<<>>
DELETE FROM ILOCK
<<>>
ALTER TABLE ILOCK DROP COLUMN InFile
<<>> 
ALTER TABLE ILOCK ADD COLUMN InClass BIGINT
<<>>
ALTER TABLE ILOCK ADD CONSTRAINT ILOCK_InClass_NULL_CN CHECK (InClass IS NOT NULL)
<<>>
ALTER TABLE ILOCK ADD CONSTRAINT ILOCK_InClass_FK FOREIGN KEY (Run,InClass) REFERENCES OBJECT (Run, Id)
<<>>
DROP TABLE ACCESS
<<>>
create table ACCESS ( -- field access (read/write) events
  Run               INT          NOT NULL CONSTRAINT ACCESS_Run_FK REFERENCES RUN (Run),
  TS                TIMESTAMP    NOT NULL,
  InThread          BIGINT       NOT NULL,
  InClass           BIGINT       NOT NULL,
  AtLine            INT          NOT NULL,
  Field             BIGINT       NOT NULL,
  RW                CHAR(1)      NOT NULL CONSTRAINT ACCESS_RW_CN CHECK (RW IN ('R', 'W')),
  Receiver          BIGINT,
  UnderConstruction CHAR(1) NOT NULL CONSTRAINT ACCESS_UnderConstruction_CN CHECK (UnderConstruction IN ('Y','N')),
  CONSTRAINT ACCESS_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ACCESS_Field_FK FOREIGN KEY (Run, Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT ACCESS_Receiver_FK FOREIGN KEY (Run, Receiver) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ACCESS_InClass_FK FOREIGN KEY (Run, Receiver) REFERENCES OBJECT (Run, Id)
)
<<>>
