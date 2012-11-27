package com.surelogic._flashlight;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import com.surelogic._flashlight.common.FileChannelOutputStream;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

/**
 * Visitor for an {@link Event}.
 */
public abstract class EventVisitor {
    void visit(final AfterIntrinsicLockAcquisition e) {
        // do nothing
    }

    void visit(final AfterIntrinsicLockRelease e) {
        // do nothing
    }

    void visit(final AfterIntrinsicLockWait e) {
        // do nothing
    }

    void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
        // do nothing
    }

    void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
        // do nothing
    }

    void visit(final BeforeIntrinsicLockAcquisition e) {
        // do nothing
    }

    void visit(final BeforeIntrinsicLockWait e) {
        // do nothing
    }

    void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
        // do nothing
    }

    void visit(final FieldAssignment e) {
        // do nothing
    }

    void visit(final FieldDefinition e) {
        // do nothing
    }

    void visit(final FieldReadInstance e) {
        // do nothing
    }

    void visit(final FieldReadStatic e) {
        // do nothing
    }

    void visit(final FieldWriteInstance e) {
        // do nothing
    }

    void visit(final FieldWriteStatic e) {
        // do nothing
    }

    void visit(final FinalEvent e) {
        // do nothing
    }

    void visit(final GarbageCollectedObject e) {
        // do nothing
    }

    void visit(final IndirectAccess indirectAccess) {
        // do nothing
    }

    void visit(final ObjectDefinition e) {
        // do nothing
    }

    void visit(final ObservedCallLocation e) {
        // do nothing
    }

    void visit(final ReadWriteLockDefinition e) {
        // do nothing
    }

    void visit(final SingleThreadedFieldInstance e) {
        // do nothing
    }

    void visit(final SingleThreadedFieldStatic e) {
        // do nothing
    }

    void visit(final StaticCallLocation e) {
        // do nothing
    }

    void visit(final Time e) {
        // do nothing
    }

    void visit(final CheckpointEvent e) {
        // do nothing
    }

    public void visit(final TraceNode e) {
        // do nothing
    }

    void visit(HappensBefore e) {
        // do nothing
    }

    void visit(HappensBeforeObject e) {
        // do nothing
    }

    void flush() {
        // do nothing
    }

    void printStats() {
        // do nothing
    }

    public static File createStreamFile(final String fileName,
            final OutputType type) {
        final String extension = ".fl";
        return new File(fileName + extension
                + (type.isCompressed() ? ".gz" : ""));
    }

    static OutputStream createStream(final String fileName,
            final OutputType type) throws IOException {
        final File dataFile = createStreamFile(fileName, type);
        OutputStream stream;
        stream = new FileChannelOutputStream(dataFile);
        if (type.isCompressed()) {
            stream = new GZIPOutputStream(stream, 32768);
        } else {
            stream = new BufferedOutputStream(stream, 32768);
        }
        return stream;
    }

    interface Factory {
        EventVisitor create(RunConf conf, OutputStream stream)
                throws IOException;
    }
}
