package com.surelogic._flashlight;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

public class SocketOutputStrategy extends EventVisitor {

    private final ServerSocket f_serverSocket;
    private EventVisitor f_out;
    private final OutputType f_outType;
    private final RunConf f_conf;
    private final Factory f_fact;

    public SocketOutputStrategy(final RunConf conf, final Factory factory,
            final OutputType outType) {
        f_conf = conf;
        f_fact = factory;
        f_outType = outType;
        try {
            f_conf.log("Depository will connect to output port "
                    + StoreConfiguration.getOutputPort() + ".");
            f_serverSocket = new ServerSocket(
                    StoreConfiguration.getOutputPort());
        } catch (IOException e) {
            f_conf.logAProblem(
                    "Could not connect to socket and create output stream.", e);
            throw new IllegalStateException(e);
        }
    }

    private void checkConnection() {
        if (f_out == null) {
            try {
                Socket socket = f_serverSocket.accept();
                OutputStream stream = OutputType.getOutputStreamFor(
                        socket.getOutputStream(), f_outType);
                f_out = f_fact.create(f_conf, stream);
            } catch (IOException e) {
                f_conf.logAProblem(
                        "Could not connect to socket and create output stream.",
                        e);
                e.printStackTrace();
            }
        }
    }

    @Override
    void visit(final CheckpointEvent e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockAcquisition e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockRelease e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final AfterIntrinsicLockWait e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final BeforeIntrinsicLockAcquisition e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final BeforeIntrinsicLockWait e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FieldAssignment e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FieldDefinition e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FieldReadInstance e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FieldReadStatic e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FieldWriteInstance e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FieldWriteStatic e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final FinalEvent e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final GarbageCollectedObject e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final IndirectAccess indirectAccess) {
        checkConnection();
        f_out.visit(indirectAccess);
    }

    @Override
    void visit(final ObjectDefinition e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final ObservedCallLocation e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final ReadWriteLockDefinition e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final SelectedPackage e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final SingleThreadedFieldInstance e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final SingleThreadedFieldStatic e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final StaticCallLocation e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void visit(final Time e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    public void visit(final TraceNode e) {
        checkConnection();
        f_out.visit(e);
    }

    @Override
    void flush() {
        checkConnection();
        f_out.flush();
    }

    @Override
    void printStats() {
        checkConnection();
        f_out.printStats();
    }

}
