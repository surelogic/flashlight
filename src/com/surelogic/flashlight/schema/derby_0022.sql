DROP TABLE TRACE
<<>>
create table TRACE ( -- stack trace durations
  Run         INT           NOT NULL CONSTRAINT TRACE_Run_FK REFERENCES RUN (Run),
  Id          BIGINT        NOT NULL,
  Site        BIGINT        NOT NULL,
  Parent      BIGINT        NOT NULL,
  PRIMARY KEY (Run, Id),
  CONSTRAINT TRACE_Parent_FK FOREIGN KEY (Run, Parent) REFERENCES TRACE (Run, Id),
  CONSTRAINT TRACE_Site_FK FOREIGN KEY (Run,Site) REFERENCES SITE (Run,Id)
)
<<>>
DROP TABLE ACCESS
<<>>
create table ACCESS ( -- field access (read/write) events
  ID                BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  Run               INT          NOT NULL CONSTRAINT ACCESS_Run_FK REFERENCES RUN (Run),
  TS                TIMESTAMP    NOT NULL,
  InThread          BIGINT       NOT NULL,
  Trace             BIGINT       NOT NULL,
  Field             BIGINT       NOT NULL,
  RW                CHAR(1)      NOT NULL CONSTRAINT ACCESS_RW_CN CHECK (RW IN ('R', 'W')),
  Receiver          BIGINT,
  UnderConstruction CHAR(1) NOT NULL CONSTRAINT ACCESS_UnderConstruction_CN CHECK (UnderConstruction IN ('Y','N')),
  CONSTRAINT ACCESS_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ACCESS_Field_FK FOREIGN KEY (Run, Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT ACCESS_Receiver_FK FOREIGN KEY (Run, Receiver) REFERENCES OBJECT (Run, Id),
  CONSTRAINT ACCESS_Trace_FK FOREIGN KEY (Run, Trace) REFERENCES Trace (Run, Id)
)
<<>>
CREATE INDEX ACCESS_TS_INDEX ON ACCESS (TS)
<<>>
ALTER TABLE LOCK DROP CONSTRAINT LOCK_Site_FK
<<>>
ALTER TABLE LOCK DROP COLUMN Site
<<>>
ALTER TABLE LOCK ADD COLUMN Trace BIGINT NOT NULL DEFAULT -1
<<>>
ALTER TABLE LOCK ADD CONSTRAINT LOCK_Trace_FK FOREIGN KEY (Run, Trace) REFERENCES Trace (Run, Id)
<<>>