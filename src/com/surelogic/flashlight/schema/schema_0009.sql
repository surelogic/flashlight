
CREATE TABLE BADPUBLISH ( -- Fields that are improperly accessed during construction
  Run           INT            NOT NULL CONSTRAINT BADPUBLISH_Run_FK REFERENCES RUN (Run),
  Field         BIGINT         NOT NULL,
  Receiver      BIGINT         NOT NULL,
  CONSTRAINT BADPUBLISH_Field_FK FOREIGN KEY (Run,Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT BADPUBLISH_Receiver_FK FOREIGN KEY (Run,Receiver) REFERENCES OBJECT (Run, Id)
)
<<>>
CREATE TABLE BADLOCKSET ( -- Fields flagged by the lock set analysis
  Run           INT            NOT NULL CONSTRAINT BADLOCKSET_Run_FK REFERENCES RUN (Run),
  Field         BIGINT         NOT NULL,
  Receiver      BIGINT         NOT NULL,
  CONSTRAINT BADLOCKSET_Field_FK FOREIGN KEY (Run,Field) REFERENCES FIELD (Run, Id),
  CONSTRAINT BADLOCKSET_Receiver_FK FOREIGN KEY (Run,Receiver) REFERENCES OBJECT (Run, Id)
)
<<>>