package com.surelogic._flashlight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.old.TraceNode;

public class CheckpointingOutputStreamStrategy extends EventVisitor {

    private final RunConf f_conf;
    private final OutputType f_outputType;
    private final EventVisitor.Factory f_factory;

    private int f_count;
    private EventVisitor f_out;

    CheckpointingOutputStreamStrategy(final RunConf conf,
            final EventVisitor.Factory factory, final OutputType type) {
        f_factory = factory;
        f_conf = conf;
        f_outputType = type;
        try {
            f_out = f_factory.create(f_conf, nextStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private OutputStream nextStream() throws IOException {
        return EventVisitor
                .createStream(
                        new File(
                                StoreConfiguration.getDirectory(),
                                String.format(
                                        "%s.%06d",
                                        InstrumentationConstants.FL_CHECKPOINT_PREFIX,
                                        f_count)), f_outputType);
    }

    private void checkpointStream(long nanos) throws IOException {
        f_out = null;
        FileWriter w = new FileWriter(new File(
                StoreConfiguration.getDirectory(), String.format("%s.%06d%s",
                        InstrumentationConstants.FL_CHECKPOINT_PREFIX,
                        f_count++, OutputType.COMPLETE.getSuffix())));
        w.write(nanos - f_conf.getStartNanoTime() + " ns\n");
        w.close();
    }

    @Override
    void visit(final CheckpointEvent e) {
        f_out.visit(e);
        f_out.visit(FinalEvent.FINAL_EVENT);
        try {
            checkpointStream(e.getNanoTime());
            f_out = f_factory.create(f_conf, nextStream());
        } catch (IOException exc) {
            throw new IllegalStateException(exc);
        }
    }

    @Override
    void visit(final AfterIntrinsicLockAcquisition e) {
        f_out.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockRelease e) {
        f_out.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockWait e) {
        f_out.visit(e);
    }

    @Override
    void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
        f_out.visit(e);
    }

    @Override
    void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
        f_out.visit(e);
    }

    @Override
    void visit(final BeforeIntrinsicLockAcquisition e) {
        f_out.visit(e);
    }

    @Override
    void visit(final BeforeIntrinsicLockWait e) {
        f_out.visit(e);
    }

    @Override
    void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FieldAssignment e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FieldDefinition e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FieldReadInstance e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FieldReadStatic e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FieldWriteInstance e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FieldWriteStatic e) {
        f_out.visit(e);
    }

    @Override
    void visit(final FinalEvent e) {
        f_out.visit(e);
        try {
            checkpointStream(System.nanoTime());
        } catch (IOException exc) {
            throw new IllegalStateException(exc);
        }
    }

    @Override
    void visit(final GarbageCollectedObject e) {
        f_out.visit(e);
    }

    @Override
    void visit(final IndirectAccess indirectAccess) {
        f_out.visit(indirectAccess);
    }

    @Override
    void visit(final ObjectDefinition e) {
        f_out.visit(e);
    }

    @Override
    void visit(final ObservedCallLocation e) {
        f_out.visit(e);
    }

    @Override
    void visit(final ReadWriteLockDefinition e) {
        f_out.visit(e);
    }

    @Override
    void visit(final SingleThreadedFieldInstance e) {
        f_out.visit(e);
    }

    @Override
    void visit(final SingleThreadedFieldStatic e) {
        f_out.visit(e);
    }

    @Override
    void visit(final StaticCallLocation e) {
        f_out.visit(e);
    }

    @Override
    void visit(final Time e) {
        f_out.visit(e);
    }

    @Override
    public void visit(final TraceNode e) {
        f_out.visit(e);
    }

    @Override
    void visit(HappensBeforeThread e) {
        f_out.visit(e);
    }

    @Override
    void visit(HappensBeforeObject e) {
        f_out.visit(e);
    }

    @Override
    void visit(HappensBeforeCollection e) {
        f_out.visit(e);
    }

    @Override
    public void visit(Environment e) {
        f_out.visit(e);
    }

    @Override
    void flush() {
        if (f_out != null) {
            f_out.flush();
        }
    }

    @Override
    void printStats() {
        f_out.printStats();
    }

}
