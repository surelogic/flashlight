
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
  METHODCALL  VARCHAR(32672)        
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
