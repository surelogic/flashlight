package com.surelogic._flashlight;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

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

    public static void outputHeader(String run, final PrintWriter out) {
        assert out != null;
        out.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
        StringBuilder b = new StringBuilder();
        b.append("<flashlight");
        Entities.addAttribute(AttributeType.VERSION.label(), version, b);
        Entities.addAttribute(AttributeType.RUN.label(), run, b);
        b.append(">"); // don't end this element
        out.println(b.toString());
    }

    static final Factory factory = new Factory() {
        @Override
        public EventVisitor create(final RunConf conf, final OutputStream stream)
                throws IOException {
            return new OutputStrategyXML(conf, stream);
        }
    };

    OutputStrategyXML(final RunConf conf, final OutputStream stream)
            throws IOException {
        assert stream != null;
        final OutputStreamWriter osw = new OutputStreamWriter(stream, "UTF-8");
        f_out = new PrintWriter(osw);
        outputHeader(conf.getRun(), f_out);
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
        f_out.close();
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
    void visit(HappensBeforeThread e) {
        o(e.toString());
    }

    @Override
    void visit(HappensBeforeObject e) {
        o(e.toString());
    }

    @Override
    void visit(HappensBeforeCollection e) {
        o(e.toString());
    }

    @Override
    void visit(HappensBeforeExecutor e) {
        o(e.toString());
    }

    @Override
    void visit(final Environment e) {
        o(e.toString());
    }

    @Override
    void flush() {
        f_out.flush();
    }
}
