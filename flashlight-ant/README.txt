                              Flashlight Ant Tasks
--------------------------------------------------------------------------------

Flashlight-related ANT tasks are declared in the JAR file flashlight-ant.jar,
located in the lib/ folder.  The JAR file flashlight-ant.jar contains an
antlib.xml file located at com/surelogic/flashlight/ant/antlib.xml.  The best
way to load the Flashlight ANT tasks into ANT and into their own namespace is to
use the ANT <taskdef> tag:

<?xml version="1.0" encoding="ISO-8859-1"?>
<project ... xmlns:flashlight="antlib:com.surelogic.flashlight.ant">
    <taskdef uri="antlib:com.surelogic.flashlight.ant"
    		resource="com/surelogic/flashlight/ant/antlib.xml"
    		classpath="path/to/flashlight-ant.jar"/>
    ...

Flashlight currently ships with three tasks -- instrument, instrument-archive,
and record.  See the Help documentation available through the Flashlight Eclipse
Client for details on how to use these tasks.
