-------------------------
-- Flashlight Ant Task --
-------------------------

This Ant task can instrument a JAR or WAR to produce Flashlight results
when the JAR or WAR is run. After the program is run, the output is a
directory that can be loaded into Eclipse to examine its results.

Requirements: Ant 1.7 (or higher) running on a Java 7 (or higher) JRE

NOTE in the examples below you have to change flashlight.ant.home to point
to your unzipped "flashlight-ant" directory (the directory this README.txt
is located within).

-- Reference --

The "flashlight-instrument-archive" task takes a JAR or WAR as input
and outputs an instrumented JAR or WAR.

Attributes added by flashlight-instrument-archive task are:

o "srcfile" (required) The path to the JAR or WAR to instrument.
   e.g., srcfile="server.jar"
         srcfile="C:\\Users\\Tim\\site.war"

o "destfile" (required) This sets the name to use for the instrumented
   output JAR or WAR.
   e.g., destfile="flserver.jar"
         destfile="C:\\Users\\Tim\\flsite.war"
   
o "runtime" (required) The path to the flashlight runtime. Normally,
  as demonstrated in the example below you use
  runtime="${flashlight.runtime}"
  where ${flashlight.runtime} is configured in the Flashlight Ant task
  setup portion of your file.

-- Example --

For the FlashlightTutorial_PlanetBaron project create a flashlight-server.xml at the
project root:

<?xml version="1.0" encoding="UTF-8"?>
<project name="FlashlightTutorial_PlanetBaron" default="server"
	basedir=".">

	<!-- (CHANGE) path to the unzipped the Flashlight Ant task -->
	<property name="flashlight.ant.home" location="C:\\Users\\Tim\\flashlight-ant" />

	<!-- (COPY) Flashlight Ant task setup stuff -->
	<property name="flashlight.runtime"
		location="${flashlight.ant.home}\\lib\\runtime\\flashlight-runtime.jar" />
	<path id="flashlight-ant.classpath">
		<fileset dir="${flashlight.ant.home}" includes="lib/**/*.jar" />
	</path>
	<taskdef name="flashlight-instrument-archive" classname="com.surelogic.flashlight.ant.InstrumentArchive">
		<classpath refid="flashlight-ant.classpath" />
	</taskdef>

	<target name="server">
		<!-- First create working jar of your program -->
		<jar destfile="server.jar" basedir="bin">
			<manifest>
				<attribute name="Main-Class" value="com.surelogic.planetbaron.server.Server" />
			</manifest>
		</jar>
		<!-- Next instrument this JAR to collect information during a run -->
		<flashlight-instrument-archive srcfile="server.jar"
			destfile="flserver.jar" runtime="${flashlight.runtime}" />

		<echo>Flashlight instrumentation complete...launching...</echo>
		<!-- Next run the program from Ant -->
		<!-- Note the flashlight runtime MUST be before the instrumented JAR -->
		<java classname="com.surelogic.planetbaron.server.Server" fork="yes">
			<classpath>
				<pathelement location="${flashlight.runtime}" />
				<pathelement location="flserver.jar" />
			</classpath>
			<!-- Name of the run (when loaded in the IDE) -->
			<jvmarg value="-DFL_RUN=Server" />
			<!-- Place the output in a 'data' subdir under the project -->
			<jvmarg value="-DFL_RUN_FOLDER=data" />
		</java>
	</target>
</project>

This Ant script actually launches an instrumented Server. If you try this you should
use a ChatTestClient (per the tutorial) to stop the server (after playing the game).
To load the results into the Eclipse IDE choose "Import Ant/Maven Run..." from the
Flashlight menu in Eclipse. The directory to load should be under
'FlashlightTutorial_PlanetBaron/data' in a directory named something
like 'Server-2015.10.16-at-14.50.01.644'. Each run you make will have a different
time and date. Once the run is loaded you will have to manually prepare
the run for querying. To do this select the loaded run in the Flashlight Runs view
and choose "Prepare" from the context menu (or double-click on the run).

The Flashlight runtime JAR must be before the instrumented JAR in your classpath
In the example Ant script and command line java command above the
flashlight-runtime.jar is placed ahead of flserver.jar in the program's classpath.
This is required, and the program will fail to execute if it is not done. Any
other JARS your project needs may come in the usual order. It is a good idea
to always put the flashlight-runtime.jar first.

#