package com.surelogic.flashlight.android.jobs;

import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;

import org.eclipse.ui.progress.UIJob;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.android.ddmlib.IDevice;
import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entities;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.prep.AbstractDataScan;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class ReadFlashlightStreamJob extends AbstractSLJob {

    private static final int RETRIES = 50;
    private static final int TIMEOUT_MS = 1000;
    private static final int RETRY_DELAY_MS = 100;
    private final int f_port;
    private final File f_dir;
    private final IDevice f_device;
    private final int f_retries;

    private final StringBuilder f_pastAttemptsLog;

    public ReadFlashlightStreamJob(final File infoDir, final int outputPort,
            final IDevice id) {
        this(infoDir, outputPort, id, RETRIES, null);
    }

    private ReadFlashlightStreamJob(final File infoDir, final int outputPort,
            final IDevice id, final int retries,
            final StringBuilder pastAttemptsLog) {
        super("Collecting data in " + infoDir + ".");
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
                                new ReadFlashlightStreamJob(f_dir, f_port,
                                        f_device, f_retries - 1,
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
                CheckpointingEventHandler h = new CheckpointingEventHandler(
                        OutputType.FL_GZ);
                try {
                    parser.parse(in, h);
                    h.streamFinished();
                } catch (Exception exc) {
                    boolean success = h.streamBroken();
                    if (success
                            && (exc instanceof SAXParseException || exc instanceof EOFException)) {
                        // This is going to fail a lot, so we aren't going to
                        // report anything here.
                        SLLogger.getLoggerFor(ReadFlashlightStreamJob.class)
                                .log(Level.FINE, exc.getMessage(), e);
                    } else {
                        e = exc;
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

    class CheckpointingEventHandler extends AbstractDataScan {
        private final OutputType f_type;
        private PrintWriter f_out;
        private File f_outFile;
        private int f_count;
        private final StringBuilder f_buf;

        private String f_run;
        private boolean f_haveTime;
        private long f_time;
        private long f_lastTime;

        public CheckpointingEventHandler(final OutputType type)
                throws IOException {
            super(new NullSLProgressMonitor());
            f_type = type;
            // FIXME we set up the additional folders needed by eclipse here,
            // but we should probably remove the dependencies in eclipse or do
            // something more here
            new File(f_dir, InstrumentationConstants.FL_SOURCE_FOLDER_LOC)
                    .mkdirs();
            new File(f_dir, InstrumentationConstants.FL_EXTERNAL_FOLDER_LOC)
                    .mkdirs();
            new File(f_dir, InstrumentationConstants.FL_PROJECTS_FOLDER_LOC)
                    .mkdirs();
            f_buf = new StringBuilder();
            nextStream(true);
        }

        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes)
                throws SAXException {
            PrepEvent event = PrepEvent.getEvent(qName);
            Entities.start(event.getXmlName(), f_buf);
            int size = attributes.getLength();
            for (int i = 0; i < size; i++) {
                Entities.addAttribute(attributes.getQName(i),
                        attributes.getValue(i), f_buf);
            }

            if (event == PrepEvent.FLASHLIGHT) {
                f_run = attributes.getValue(AttributeType.RUN.label());
                RunManager.getInstance().notifyCollectingData(f_run);
                f_buf.append('>');
            } else {
                f_buf.append("/>");
            }
            f_out.println(f_buf);
            if (event == PrepEvent.CHECKPOINT) {
                try {
                    checkpointStream(Long.parseLong(attributes
                            .getValue(AttributeType.TIME.label())));
                    nextStream(false);
                } catch (IOException e) {
                    throw new SAXException(e);
                }
            } else if (event == PrepEvent.TIME) {
                if (!f_haveTime) {
                    f_time = Long.parseLong(attributes
                            .getValue(AttributeType.TIME.label()));
                    f_haveTime = true;
                } else {
                    f_lastTime = Long.parseLong(attributes
                            .getValue(AttributeType.TIME.label()));
                }
            }

            f_buf.setLength(0);
        }

        void checkpointStream(long nanos) throws IOException {
            f_out.println("</flashlight>");
            f_out.flush();
            f_out.close();
            // Build completion file
            FileWriter complete = new FileWriter(new File(f_dir,
                    InstrumentationConstants.FL_CHECKPOINT_PREFIX
                            + String.format(".%06d", f_count++)
                            + OutputType.COMPLETE.getSuffix()));
            try {
                complete.write(nanos - f_time + " ns\n");
            } finally {
                complete.close();
            }
        }

        void nextStream(boolean firstFile) throws IOException {
            f_outFile = new File(f_dir,
                    InstrumentationConstants.FL_CHECKPOINT_PREFIX
                            + String.format(".%06d", f_count)
                            + f_type.getSuffix());
            f_out = new PrintWriter(OutputType.getOutputStreamFor(f_outFile));
            f_out.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
            if (!firstFile) {
                f_out.println(String.format(
                        "<flashlight version='1.0' run='%s'>", f_run));
            }
        }

        /**
         * Call this if we get an error during data processing.
         * 
         * @return whether or not the run should be treated as valid
         */
        boolean streamBroken() {
            if (f_out != null) {
                f_out.close();
            }
            f_outFile.delete();
            new File(f_dir, InstrumentationConstants.FL_PORT_FILE_LOC).delete();
            return f_count >= 1;
        }

        /**
         * This should be called at the end of stream processing ONLY if
         * everything has gone well.
         * 
         * @throws IOException
         */
        void streamFinished() throws IOException {
            checkpointStream(f_lastTime);
            new File(f_dir, InstrumentationConstants.FL_PORT_FILE_LOC).delete();
        }

    }
}
