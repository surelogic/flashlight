CREATE TABLE SITE (
  Run      INT           NOT NULL CONSTRAINT SITE_Run_FK REFERENCES RUN (Run),
  Id       BIGINT        NOT NULL,
  AtLine   INT           NOT NULL,
  InClass  BIGINT        NOT NULL,
  InFile   VARCHAR(255)  NOT NULL,
  Location VARCHAR(255)  NOT NULL,
  PRIMARY KEY (Run,Id),
  CONSTRAINT SITE_InClass_FK  FOREIGN KEY (Run, InClass) REFERENCES OBJECT (Run, Id),
  CONSTRAINT SITE_Unique_CN UNIQUE (Run,AtLine,InFile,Location)
)
<<>>
DROP TABLE TRACE
<<>>
create table TRACE ( -- stack trace durations
  Run         INT           NOT NULL CONSTRAINT TRACE_Run_FK REFERENCES RUN (Run),
  Id          BIGINT        NOT NULL,
  InThread    BIGINT        NOT NULL,
  Site        BIGINT        NOT NULL,
  Start       TIMESTAMP     NOT NULL,
  Stop        TIMESTAMP     NOT NULL,
  PRIMARY KEY (Run, Id),
  CONSTRAINT TRACE_InThread_FK FOREIGN KEY (Run, InThread) REFERENCES OBJECT (Run, Id),
  CONSTRAINT TRACE_Site_FK FOREIGN KEY (Run,Site) REFERENCES SITE (Run,Id)
)
<<>>
CREATE INDEX TRACE_STOP_INDEX ON TRACE (STOP)
<<>>
CREATE INDEX TRACE_START_INDEX ON TRACE (START)
<<>>
