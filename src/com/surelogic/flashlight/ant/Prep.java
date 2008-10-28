package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLSeverity;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.console.ConsoleJob;
import com.surelogic.common.jobs.console.PrintWriterSLProgressMonitor;
import com.surelogic.flashlight.common.jobs.PrepSLJob;

/* Update bulid.xml to build this properly.  We need common and flashlight-common jar files too. */

public final class Prep extends Task {
  private String srcfile = null;
  private String destdir = null;
  
  public void setSrcfile(final String value) {
    srcfile = value;
  }
  
  public void setDestdir(final String value) {
    destdir = value;
  }

  
  
  private void checkParameters() throws BuildException {
    if (srcfile == null) {
      throw new BuildException("Input Flashlight data file not specified");
    }
    if (destdir == null) {
      throw new BuildException("Output Flashlight database not specified");
    }
  }

  /**
   * Work our magic.
   */
  @Override
  public void execute() throws BuildException {
    checkParameters();

    final ConsoleJob consoleJob = new ConsoleJob(new AntSLProgressMonitor.Factory(this));

    final DerbyConnection dbConnection = new FlashlightDBConnection(destdir);
    try {
      dbConnection.bootAndCheckSchema();
    } catch (final Exception e) {
      throw new BuildException("Failed to initialized database connection", e);
    }
    
    final SLJob prepJob = new PrepSLJob(new File(srcfile), dbConnection);
    final SLStatus status = consoleJob.submitJob(prepJob);
    
    if (status.getSeverity() != SLSeverity.OK || status.getCode() != SLStatus.OK) {
      throw new BuildException("Flashlight data preparation failed: " + status.getMessage(), status.getException());
    }
  }  

  public static void main(String[] args) throws Exception {
    final PrintWriter pw = new PrintWriter(System.out);
    final ConsoleJob consoleJob = new ConsoleJob(PrintWriterSLProgressMonitor.getFactory(pw));
    
    final DerbyConnection dbConnection = new FlashlightDBConnection(args[1]);
    dbConnection.bootAndCheckSchema();
    
    final SLJob prepJob = new PrepSLJob(new File(args[0]), dbConnection);
    final SLStatus status = consoleJob.submitJob(prepJob);
        
    if (status.getSeverity() != SLSeverity.OK || status.getCode() != SLStatus.OK) {
      System.out.println("Flashlight data preparation failed: " + status.getMessage());
      System.out.println(SLStatus.getStackTrace(status.getException()));
    }
  }
}
