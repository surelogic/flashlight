package com.surelogic._flashlight;

import static com.surelogic._flashlight.common.InstrumentationConstants.FL_ACCESS_SUFFIX;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_INDIRECT_SUFFIX;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_LOCK_SUFFIX;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OBJECT_SUFFIX;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OTHER_SUFFIX;

import java.io.IOException;

import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

final class OutputStreamsStrategy extends EventVisitor {
    /**
     * For lock events, and g-c-object
     */
    final EventVisitor lockStream;
    /**
     * For field read, write
     */
    final EventVisitor accessStream;
    /**
     * For class, thread, object defs
     */
    final EventVisitor objectStream;
    /**
     * For indirect accesses
     */
    final EventVisitor indirectStream;
    /**
     * For the rest of the events
     */
    final EventVisitor otherStream;

    OutputStreamsStrategy(final RunConf conf, final Factory factory)
            throws IOException {
        final OutputType type = StoreConfiguration.getOutputType();
        final String prefix = conf.getFilePrefix();
        lockStream = factory.create(conf,
                createStream(prefix + FL_LOCK_SUFFIX, type));
        accessStream = factory.create(conf,
                createStream(prefix + FL_ACCESS_SUFFIX, type));
        objectStream = factory.create(conf,
                createStream(prefix + FL_OBJECT_SUFFIX, type));
        indirectStream = factory.create(conf,
                createStream(prefix + FL_INDIRECT_SUFFIX, type));
        otherStream = factory.create(conf,
                createStream(prefix + FL_OTHER_SUFFIX, type));
    }

    @Override
    void visit(final AfterIntrinsicLockAcquisition e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockRelease e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockWait e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final BeforeIntrinsicLockAcquisition e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final BeforeIntrinsicLockWait e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final FieldDefinition e) {
        otherStream.visit(e);
    }

    @Override
    void visit(final FieldReadInstance e) {
        accessStream.visit(e);
    }

    @Override
    void visit(final FieldReadStatic e) {
        accessStream.visit(e);
    }

    @Override
    void visit(final FieldWriteInstance e) {
        accessStream.visit(e);
    }

    @Override
    void visit(final FieldWriteStatic e) {
        accessStream.visit(e);
    }

    @Override
    void visit(final FinalEvent e) {
        lockStream.visit(e);
        accessStream.visit(e);
        objectStream.visit(e);
        indirectStream.visit(e);
        otherStream.visit(e);
    }

    @Override
    void visit(final GarbageCollectedObject e) {
        lockStream.visit(e);
    }

    @Override
    void visit(final IndirectAccess e) {
        indirectStream.visit(e);
    }

    @Override
    void visit(final ObjectDefinition e) {
        objectStream.visit(e);
    }

    @Override
    void visit(final ObservedCallLocation e) {
        otherStream.visit(e);
    }

    @Override
    void visit(final ReadWriteLockDefinition e) {
        otherStream.visit(e);
    }

    @Override
    void visit(final SingleThreadedFieldInstance e) {
        otherStream.visit(e);
    }

    @Override
    void visit(final SingleThreadedFieldStatic e) {
        otherStream.visit(e);
    }

    @Override
    void visit(final StaticCallLocation e) {
        otherStream.visit(e);
    }

    @Override
    void visit(final Time e) {
        lockStream.visit(e);
        accessStream.visit(e);
        objectStream.visit(e);
        indirectStream.visit(e);
        otherStream.visit(e);
    }

    @Override
    public void visit(final TraceNode e) {
        otherStream.visit(e);
    }

    @Override
    void flush() {
        lockStream.flush();
        accessStream.flush();
        objectStream.flush();
        indirectStream.flush();
        otherStream.flush();
    }

    @Override
    void printStats() {
        lockStream.printStats();
        accessStream.printStats();
        objectStream.printStats();
        indirectStream.printStats();
        otherStream.printStats();
    }
}
