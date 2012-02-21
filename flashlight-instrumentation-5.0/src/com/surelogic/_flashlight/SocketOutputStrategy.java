package com.surelogic._flashlight;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

public class SocketOutputStrategy extends EventVisitor {

    private final ServerSocket f_socket;
    private final EventVisitor f_out;

    public SocketOutputStrategy(final RunConf f_conf, final Factory factory,
            final OutputType outType) {
        try {
            f_socket = new ServerSocket(StoreConfiguration.getOutputPort());
            Socket socket = f_socket.accept();
            OutputStream stream = OutputType.getOutputStreamFor(
                    socket.getOutputStream(), outType);
            f_out = factory.create(f_conf, stream);
        } catch (IOException e) {
            f_conf.logAProblem(
                    "Could not connect to socket and create output stream.", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    void visit(final CheckpointEvent e) {
        f_out.visit(e);
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
    void visit(final SelectedPackage e) {
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
    void flush() {
        f_out.flush();
    }

    @Override
    void printStats() {
        f_out.printStats();
    }

}
