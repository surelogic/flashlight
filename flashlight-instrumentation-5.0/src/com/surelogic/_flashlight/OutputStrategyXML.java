package com.surelogic._flashlight;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.trace.TraceNode;

final class OutputStrategyXML extends EventVisitor {
  static final String version = "1.0";
  private final PrintWriter f_out;
  private String f_indent = "";

  private void o(final String s) {
    f_out.print(f_indent);
    f_out.println(s);
  }

  private static void addProperty(final String key, final AttributeType attr, final StringBuilder b) {
    String prop = System.getProperty(key);
    if (prop == null) {
      prop = "UNKNOWN";
    }
    Entities.addAttribute(attr.label(), prop, b);
  }

  public static void outputHeader(final RunConf conf, final PrintWriter out, final Time time, final String version) {
    assert out != null;
    out.println("<?xml version='1.0' encoding='" + conf.getEncoding() + "' standalone='yes'?>");
    StringBuilder b = new StringBuilder();
    b.append("<flashlight");
    Entities.addAttribute(AttributeType.VERSION.label(), version, b);
    Entities.addAttribute(AttributeType.RUN.label(), conf.getRun(), b);
    b.append(">"); // don't end this element
    out.println(b.toString());
    b = new StringBuilder();
    b.append("  <environment");
    try {
      Entities.addAttribute(AttributeType.HOSTNAME.label(), InetAddress.getLocalHost().getHostName(), b);
    } catch (UnknownHostException e) {
      Entities.addAttribute(AttributeType.HOSTNAME.label(), "unknown", b);
    }
    addProperty("user.name", AttributeType.USER_NAME, b);
    addProperty("java.version", AttributeType.JAVA_VERSION, b);
    addProperty("java.vendor", AttributeType.JAVA_VENDOR, b);
    addProperty("os.name", AttributeType.OS_NAME, b);
    addProperty("os.arch", AttributeType.OS_ARCH, b);
    addProperty("os.version", AttributeType.OS_VERSION, b);
    Entities.addAttribute(AttributeType.MEMORY_MB.label(), Runtime.getRuntime().maxMemory() / (1024L * 1024L), b);
    Entities.addAttribute(AttributeType.CPUS.label(), Runtime.getRuntime().availableProcessors(), b);
    b.append("/>");
    out.println(b.toString());
    if (time != null) {
      out.print("  ");
      out.println(time);
      out.println("</flashlight>");
    }
  }

  static final Factory factory = new Factory() {
    public EventVisitor create(final RunConf conf, final OutputStream stream) throws IOException {
      return new OutputStrategyXML(conf, stream);
    }
  };

  OutputStrategyXML(final RunConf conf, final OutputStream stream) throws IOException {
    assert stream != null;
    final OutputStreamWriter osw = new OutputStreamWriter(stream, conf.getEncoding());
    f_out = new PrintWriter(osw);
    outputHeader(conf, f_out, null, version);
  }

  @Override
  void visit(final AfterIntrinsicLockAcquisition e) {
    o(e.toString());
  }

  @Override
  void visit(final AfterIntrinsicLockRelease e) {
    o(e.toString());
  }

  @Override
  void visit(final AfterIntrinsicLockWait e) {
    o(e.toString());
  }

  @Override
  void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
    o(e.toString());
  }

  @Override
  void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
    o(e.toString());
  }

  @Override
  void visit(final BeforeIntrinsicLockAcquisition e) {
    o(e.toString());
  }

  @Override
  void visit(final BeforeIntrinsicLockWait e) {
    o(e.toString());
  }

  @Override
  void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
    o(e.toString());
  }

  @Override
  void visit(final CheckpointEvent e) {
    o(e.toString());
  }

  @Override
  public void visit(final FieldAssignment e) {
    o(e.toString());
  }

  @Override
  void visit(final FieldDefinition e) {
    o(e.toString());
  }

  @Override
  void visit(final FieldReadInstance e) {
    o(e.toString());
  }

  @Override
  void visit(final FieldReadStatic e) {
    o(e.toString());
  }

  @Override
  void visit(final FieldWriteInstance e) {
    o(e.toString());
  }

  @Override
  void visit(final FieldWriteStatic e) {
    o(e.toString());
  }

  @Override
  void visit(final FinalEvent e) {
    f_indent = "";
    o("</flashlight>");
    // System.out.println("Closed.");
    f_out.close();
    // new Throwable("Visiting FinalEvent").printStackTrace(System.out);
  }

  @Override
  void visit(final GarbageCollectedObject e) {
    o(e.toString());
  }

  @Override
  void visit(final IndirectAccess e) {
    o(e.toString());
  }

  @Override
  void visit(final ObjectDefinition e) {
    o(e.toString());
  }

  @Override
  void visit(final ReadWriteLockDefinition e) {
    o(e.toString());
  }

  @Override
  void visit(final SingleThreadedFieldInstance e) {
    o(e.toString());
  }

  @Override
  void visit(final SingleThreadedFieldStatic e) {
    o(e.toString());
  }

  @Override
  void visit(final StaticCallLocation e) {
    o(e.toString());
  }

  @Override
  void visit(final Time e) {
    o(e.toString());
  }

  @Override
  public void visit(final TraceNode e) {
    o(e.toString());
  }

  @Override
  void visit(HappensBefore e) {
    o(e.toString());
  }

  @Override
  void visit(HappensBeforeObject e) {
    o(e.toString());
  }

  @Override
  void flush() {
    f_out.flush();
  }
}
