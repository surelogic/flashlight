package com.surelogic._flashlight.instrument;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.surelogic._flashlight.rewriter.runtime.Log;

final class PrintWriterLog implements Log {
  private final PrintWriter pwLog;
  
  public PrintWriterLog(final String logFileName) throws IOException {
    PrintWriter logWriter = null;
    if (logFileName != null) {
      final FileWriter fw = new FileWriter(logFileName); // Might throw IOException
      final BufferedWriter bw = new BufferedWriter(fw);
      logWriter = new PrintWriter(bw);
    }
    pwLog = logWriter;
  }

  
  
  public synchronized void log(final String message) {
    if (pwLog != null) {
      pwLog.println(message);
    }
  }
  
  public void log(final Throwable throwable) {
    if (pwLog != null) {
      throwable.printStackTrace(pwLog);
    }
  }
  
  public void shutdown() {
    if (pwLog != null) {
      pwLog.close();
    }
  }
}
