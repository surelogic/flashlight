package com.surelogic._flashlight;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

/**
 * This output strategy listens to the output port for a client to connect. Once
 * a client has connected, it can begin transmitting data to it.
 * 
 * @author nathan
 * 
 */
public class SocketOutputStrategy extends EventVisitor {

    private static final int TIMEOUT = 5000;

    private EventVisitor f_out;
    private final OutputType f_outType;
    private final RunConf f_conf;
    private final Factory f_fact;
    private final Thread f_connectThread;
    private volatile boolean connecting;

    public SocketOutputStrategy(final RunConf conf, final Factory factory,
            final OutputType outType) {
        f_conf = conf;
        f_fact = factory;
        f_outType = outType;
        f_conf.log("Depository will connect to output port "
                + StoreConfiguration.getOutputPort() + " with output type "
                + outType + ".");
        connecting = true;
        f_connectThread = new ConnectToHost();
        f_connectThread.start();
    }

    class ConnectToHost extends Thread {
        private Socket f_socket;

        @Override
        public void run() {
            try {
                boolean success = false;
                try {
                    connectToHost();
                    // If it didn't work, we try again.
                    if (f_socket == null) {
                        connectToHost();
                    }
                    // If it worked, we are good to go. If it didn't, then it is
                    // time to shut down.
                    if (f_socket != null) {
                        OutputStream stream = OutputType.getOutputStreamFor(
                                f_socket.getOutputStream(), f_outType);
                        f_out = f_fact.create(f_conf, stream);
                        success = true;
                    } else {
                        f_conf.logAProblem("Could not connect to host computer, shutting down instrumentation now.");
                    }
                } catch (IOException e) {
                    f_conf.logAProblem(
                            "Could not connect to socket and create output stream, shutting down instrumentation now..",
                            e);
                }
                if (!success) {
                    Store.shutdown();
                }
            } finally {
                connecting = false;
            }
        }

        void connectToHost() {
            try {
                ServerSocket serverSocket = new ServerSocket(
                        StoreConfiguration.getOutputPort());
                serverSocket.setSoTimeout(TIMEOUT);
                try {
                    f_socket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                f_conf.logAProblem(
                        "Could not connect to socket and create output stream.",
                        e);
            }
        }

    }

    private boolean checkConnection() {
        while (connecting) {
            try {
                f_connectThread.join();
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        if (f_out == null) {
            return false;
        }
        return true;
    }

    @Override
    void visit(final CheckpointEvent e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final AfterIntrinsicLockAcquisition e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final AfterIntrinsicLockRelease e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final AfterIntrinsicLockWait e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final AfterUtilConcurrentLockAcquisitionAttempt e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final AfterUtilConcurrentLockReleaseAttempt e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final BeforeIntrinsicLockAcquisition e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final BeforeIntrinsicLockWait e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final BeforeUtilConcurrentLockAcquisitionAttempt e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FieldAssignment e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FieldDefinition e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FieldReadInstance e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FieldReadStatic e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FieldWriteInstance e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FieldWriteStatic e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final FinalEvent e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final GarbageCollectedObject e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final IndirectAccess indirectAccess) {
        if (checkConnection()) {
            f_out.visit(indirectAccess);
        }
    }

    @Override
    void visit(final ObjectDefinition e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final ObservedCallLocation e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final ReadWriteLockDefinition e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final SingleThreadedFieldInstance e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final SingleThreadedFieldStatic e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final StaticCallLocation e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void visit(final Time e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    public void visit(final TraceNode e) {
        if (checkConnection()) {
            f_out.visit(e);
        }
    }

    @Override
    void flush() {
        if (checkConnection()) {
            f_out.flush();
        }
    }

    @Override
    void printStats() {
        if (checkConnection()) {
            f_out.printStats();
        }
    }

}
