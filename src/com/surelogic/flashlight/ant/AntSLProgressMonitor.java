package com.surelogic.flashlight.ant;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLProgressMonitorFactory;

/**
 * A progress monitor that prints status reports to a {@link PrintWriter}.
 */
final class AntSLProgressMonitor implements SLProgressMonitor {
  public static final class Factory implements SLProgressMonitorFactory {
    private final Task task;
    
    public Factory(final Task t) {
      task = t;
    }
    
    public SLProgressMonitor createSLProgressMonitor(final String taskName) {
      return new AntSLProgressMonitor(task, taskName);
    }
  }

  
  
  private static final String INDENT = "\t";
  
  
  
  private final Task task;
  private final String taskName;
  private int indentLevel = 0;
  private final AtomicBoolean isCanceled = new AtomicBoolean(false);

  
  
  public AntSLProgressMonitor(final Task t, final String name) {
    task = t;
    taskName = name;
  }
  
  
  
  private String indent() {
    final StringBuilder sb = new StringBuilder(); 
    for (int i = 0; i < indentLevel; i++) {
      sb.append(INDENT);
    }
    return sb.toString();
  }
  
  
  
  public synchronized void begin() {
    task.log(indent() + taskName, Project.MSG_VERBOSE);
  }

  public synchronized void begin(int totalWork) {
    task.log(indent() + taskName, Project.MSG_VERBOSE);
  }

  public synchronized void done() {
    // do nothing
  }

  public synchronized boolean isCanceled() {
    return isCanceled.get();
  }

  public synchronized void setCanceled(final boolean value) {
    isCanceled.set(value);
  }

  public synchronized void subTask(final String name) {
    indentLevel += 1;
    task.log(indent() + name, Project.MSG_VERBOSE);
  }

  public synchronized void subTaskDone() {
    indentLevel -= 1;
  }
  
  public synchronized void worked(final int work) {
    // do nothing
  }
}
