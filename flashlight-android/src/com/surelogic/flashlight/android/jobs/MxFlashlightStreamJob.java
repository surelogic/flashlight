package com.surelogic.flashlight.android.jobs;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;

import org.eclipse.ui.progress.UIJob;
import org.xml.sax.SAXParseException;

import com.android.ddmlib.IDevice;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.prep.events.FLManagementFactory;

public class MxFlashlightStreamJob extends AbstractSLJob {

    private static final int RETRIES = 500;
    private static final int TIMEOUT_MS = 1000;
    private static final int RETRY_DELAY_MS = 100;
    private final int f_port;
    private final File f_dir;
    private final IDevice f_device;
    private final int f_retries;
    private final String f_runId;

    private final StringBuilder f_pastAttemptsLog;

    public MxFlashlightStreamJob(String runId, final File infoDir,
            final int outputPort, final IDevice id) {
        this(runId, infoDir, outputPort, id, RETRIES, null);
    }

    private MxFlashlightStreamJob(final String runId, final File infoDir,
            final int outputPort, final IDevice id, final int retries,
            final StringBuilder pastAttemptsLog) {
        super("Collecting data in " + infoDir + ".");
        f_runId = runId;
        f_port = outputPort;
        f_dir = infoDir;
        f_device = id;
        f_retries = retries;
        f_pastAttemptsLog = pastAttemptsLog == null ? new StringBuilder()
                : pastAttemptsLog;
    }

    private final String getTStamp() {
        return "[" + new Date() + "] ";
    }

    @Override
    public SLStatus run(final SLProgressMonitor monitor) {
        Exception e = null;
        try {
            Socket socket = new Socket();
            try {
                OutputType socketType = InstrumentationConstants.FL_SOCKET_OUTPUT_TYPE;
                InputStream in;

                // Try to connect to the device
                try {
                    f_pastAttemptsLog
                            .append(getTStamp()
                                    + " Flashlight Android attempted socket.connect(localhost:"
                                    + f_port + ") with timeout of "
                                    + TIMEOUT_MS + " ms\n");
                    socket.connect(new InetSocketAddress("localhost", f_port),
                            TIMEOUT_MS);
                    in = OutputType.getInputStreamFor(socket.getInputStream(),
                            socketType);
                    f_pastAttemptsLog
                            .append(getTStamp()
                                    + " Flashlight Android connected OK to socket.connect(localhost:"
                                    + f_port + ")\n");
                } catch (Exception exc) {
                    if ((exc instanceof SocketTimeoutException || exc instanceof IOException)
                            && f_retries > 0) {
                        // Maybe something isn't set up yet, we'll try again in
                        // just a little bit.
                        f_pastAttemptsLog
                                .append(getTStamp()
                                        + " Flashlight Android FAILURE of socket.connect(localhost:"
                                        + f_port + ")...retrying in "
                                        + RETRY_DELAY_MS + " ms \n");
                        EclipseUtility.toEclipseJob(
                                new MxFlashlightStreamJob(f_runId, f_dir,
                                        f_port, f_device, f_retries - 1,
                                        f_pastAttemptsLog), f_dir.toString())
                                .schedule(RETRY_DELAY_MS);
                        return SLStatus.OK_STATUS;
                    } else {
                        // We are done trying to connect, so it is time to bail
                        // and to remove our forward port.
                        try {
                            f_device.removeForward(f_port, f_port);
                        } catch (Exception exc1) {
                            // That's okay, we were just doing our best. Maybe
                            // someone unplugged the device?
                        }
                        return SLStatus.createErrorStatus(
                                I18N.err(245, f_dir.toString(),
                                        f_pastAttemptsLog.toString()), exc);
                    }
                }
                // We managed to connect to the device. Time to start reading
                // data.
                SAXParser parser = OutputType.getParser(socketType);
                FLManagementFactory fact = FLManagementFactory.create();

                try {
                    fact.start(parser, in);
                } catch (Exception exc) {
                    if (!fact.isStarted()) {
                        // Let's just try this from the top, shall we?
                        f_pastAttemptsLog
                                .append(getTStamp()
                                        + " Flashlight Android FAILURE of socket read (localhost:"
                                        + f_port + ")...retrying in "
                                        + RETRY_DELAY_MS + " ms \n");
                        EclipseUtility.toEclipseJob(
                                new MxFlashlightStreamJob(f_runId, f_dir,
                                        f_port, f_device, f_retries - 1,
                                        f_pastAttemptsLog), f_dir.toString())
                                .schedule(RETRY_DELAY_MS);
                        return SLStatus.OK_STATUS;
                    } else {
                        if (exc instanceof SAXParseException
                                || exc instanceof EOFException) {
                            // This is going to fail a lot, so we aren't going
                            // to
                            // report anything here.
                            SLLogger.getLoggerFor(ReadFlashlightStreamJob.class)
                                    .log(Level.FINE, exc.getMessage(), e);
                        } else {
                            e = exc;
                        }
                    }
                }
            } finally {
                socket.close();
            }
        } catch (Exception exc) {
            e = exc;
        }
        // Time to remove the port forward.
        try {
            f_device.removeForward(f_port, f_port);
        } catch (Exception exc) {
            // That's okay, we were just doing our best. Maybe someone unplugged
            // the device?
        }
        if (e != null) {
            return SLStatus.createErrorStatus(f_pastAttemptsLog.toString(), e);
        } else {
            final UIJob job = new SwitchToFlashlightPerspectiveJob();
            job.schedule();
        }
        return SLStatus.OK_STATUS;
    }

}
