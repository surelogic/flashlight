CREATE FUNCTION STACKTRACE 
       (TRACE_ID BIGINT)
RETURNS TABLE 
(
  CLASSNAME   VARCHAR(32672),
  PACKAGENAME VARCHAR(32672),
  INFILE      VARCHAR(255),
  LOCATION    VARCHAR(255),
  ATLINE      INTEGER,
  METHODCLASS VARCHAR(32672),
  METHODCALL  VARCHAR(32672),
  CLASSCODE   VARCHAR(11)
)
PARAMETER STYLE DERBY_JDBC_RESULT_SET
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.stackTrace'
<<>>
CREATE FUNCTION OBJID 
       (OBJECT_ID BIGINT)
RETURNS VARCHAR(100)
PARAMETER STYLE JAVA
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.objId'
<<>>
CREATE FUNCTION ACCESSSUMMARY
       (FIELD_ID BIGINT,
        RECEIVER_ID BIGINT)
RETURNS TABLE
(
  THREADID   BIGINT,
  THREADNAME VARCHAR(32672),
  STARTTIME  TIMESTAMP,
  ENDTIME    TIMESTAMP,
  READS      INT,
  WRITES     INT,
  HAPPENSBEFORE VARCHAR(3),
  LASTWRITETIME TIMESTAMP,
  LASTWRITETHREAD BIGINT,
  READSUC    INT,
  WRITESUC   INT
)
PARAMETER STYLE DERBY_JDBC_RESULT_SET
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.accessSummary'
<<>>
CREATE FUNCTION STATICACCESSSUMMARY
       (FIELD_ID BIGINT)
RETURNS TABLE
(
  THREADID   BIGINT,
  THREADNAME VARCHAR(32672),
  STARTTIME  TIMESTAMP,
  ENDTIME    TIMESTAMP,
  READS      INT,
  WRITES     INT,
  HAPPENSBEFORE VARCHAR(3),
  LASTWRITETIME TIMESTAMP,
  LASTWRITETHREAD BIGINT
)
PARAMETER STYLE DERBY_JDBC_RESULT_SET
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.staticAccessSummary'
<<>>
CREATE FUNCTION HAPPENSBEFOREEDGES
       (WRITETHREAD BIGINT,
        WRITETS     TIMESTAMP,
        READTHREAD  BIGINT,
        READTS      TIMESTAMP)
RETURNS TABLE
(
  SOURCEID     BIGINT,
  SOURCETS     TIMESTAMP,
  TRACEID      BIGINT,
  TRACETS      TIMESTAMP,
  TYPE         VARCHAR (32672),
  SOURCEMETHOD VARCHAR (32672),
  TRACEMETHOD  VARCHAR (32672),
  ID           VARCHAR (32672)
)
PARAMETER STYLE DERBY_JDBC_RESULT_SET
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.happensBeforeEdges'
<<>>
CREATE FUNCTION BLOCKSUMMARY
       (RECEIVER BIGINT,
        START TIMESTAMP,
        STOP TIMESTAMP)
RETURNS TABLE
(
  FIELDID BIGINT,
  FIELDNAME VARCHAR (32672),
  CODE VARCHAR (11),
  THREADID BIGINT,
  THREADNAME VARCHAR (32672),
  START TIMESTAMP,
  STOP TIMESTAMP,
  READS INT,
  WRITES INT,
  READSUC INT,
  WRITESUC INT
)
PARAMETER STYLE DERBY_JDBC_RESULT_SET
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.blockSummary'
<<>>
CREATE FUNCTION FIELDSBLOCKSUMMARY
       (RECEIVER BIGINT,
        START TIMESTAMP,
        STOP TIMESTAMP,
        FIELD BIGINT,
        SECONDFIELD BIGINT)
RETURNS TABLE
(
  FIELDID BIGINT,
  FIELDNAME VARCHAR (32672),
  CODE VARCHAR (11),
  THREADID BIGINT,
  THREADNAME VARCHAR (32672),
  START TIMESTAMP,
  STOP TIMESTAMP,
  READS INT,
  WRITES INT,
  READSUC INT,
  WRITESUC INT
)
PARAMETER STYLE DERBY_JDBC_RESULT_SET
READS SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'com.surelogic.common.derby.sqlfunctions.Functions.fieldsBlockSummary'
<<>>
