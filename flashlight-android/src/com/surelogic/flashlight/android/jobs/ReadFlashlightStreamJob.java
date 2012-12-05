package com.surelogic.flashlight.android.jobs;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
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
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entities;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.common.prep.AbstractDataScan;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class ReadFlashlightStreamJob implements SLJob {

    private static final int RETRIES = 50;
    private static final int TIMEOUT_MS = 1000;
    private static final int RETRY_DELAY_MS = 100;
    private final String f_runName;
    private final int f_port;
    private final File f_dir;
    private final IDevice f_device;
    private final int f_retries;

    private final StringBuilder f_pastAttemptsLog;

    public ReadFlashlightStreamJob(final String runName, final File infoDir,
            final int outputPort, final IDevice id) {
        this(runName, infoDir, outputPort, id, RETRIES, null);
    }

    private ReadFlashlightStreamJob(final String runName, final File infoDir,
            final int outputPort, final IDevice id, final int retries,
            final StringBuilder pastAttemptsLog) {
        f_runName = runName;
        f_port = outputPort;
        f_dir = infoDir;
        f_device = id;
        f_retries = retries;
        f_pastAttemptsLog = pastAttemptsLog == null ? new StringBuilder()
                : pastAttemptsLog;
    }

    @Override
    public String getName() {
        return "Collecting data from " + f_runName + ".";
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
                        EclipseJob.getInstance().schedule(
                                new ReadFlashlightStreamJob(f_runName, f_dir,
                                        f_port, f_device, f_retries - 1,
                                        f_pastAttemptsLog), RETRY_DELAY_MS);
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
                                I18N.err(245, f_runName,
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
        private final PrintWriter f_header;
        private int f_count;
        private final StringBuilder f_buf;
        boolean f_doHeader;

        public CheckpointingEventHandler(final OutputType type)
                throws IOException {
            super(new NullSLProgressMonitor());
            f_doHeader = true;
            f_type = type;
            // FIXME we set up the additional folders needed by eclipse here,
            // but we should probably remove the dependencies in eclipse or do
            // something more here
            new File(f_dir, "source").mkdir();
            new File(f_dir, "external").mkdir();
            new File(f_dir, "projects").mkdir();
            f_header = new PrintWriter(new File(f_dir, f_runName
                    + OutputType.FLH.getSuffix()));
            f_header.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
            f_buf = new StringBuilder();
            nextStream();
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
                f_buf.append('>');
            } else {
                f_buf.append("/>");
            }
            f_out.println(f_buf);
            if (f_doHeader) {
                if (event == PrepEvent.ENVIRONMENT
                        || event == PrepEvent.FLASHLIGHT) {
                    f_header.println(f_buf);
                } else if (event == PrepEvent.TIME) { // Last element
                    f_header.println(f_buf);
                    f_header.println("</flashlight>");
                    f_header.close();
                    f_doHeader = false;
                }
            }
            if (event == PrepEvent.CHECKPOINT) {
                try {
                    nextStream();
                } catch (IOException e) {
                    throw new SAXException(e);
                }
            }
            f_buf.setLength(0);
        }

        void nextStream() throws IOException {
            boolean firstFile = f_out == null;
            if (!firstFile) {
                f_out.println("</flashlight>");
                f_out.flush();
                f_out.close();
            }
            f_outFile = new File(f_dir, f_runName
                    + String.format(".%06d", f_count++) + f_type.getSuffix());
            f_out = new PrintWriter(OutputType.getOutputStreamFor(f_outFile));
            if (firstFile) {
                f_out.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
            } else {
                // Grab the header information and place it into this file.
                BufferedReader reader = new BufferedReader(
                        new FileReader(new File(f_dir, f_runName
                                + OutputType.FLH.getSuffix())));
                try {
                    String line;
                    while (!(line = reader.readLine())
                            .startsWith("</flashlight>")) {
                        f_out.println(line);
                    }
                } finally {
                    reader.close();
                }
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
            f_header.close();
            // Still write out a Run.Complete as long as we have some data.
            if (f_count > 1) {
                File complete = new File(f_dir,
                        InstrumentationConstants.FL_COMPLETE_RUN_LOC);
                try {
                    complete.createNewFile();
                } catch (IOException e) {
                    SLLogger.getLoggerFor(ReadFlashlightStreamJob.class).log(
                            Level.WARNING, "Could not create Run.Complete", e);
                }
                return true;
            }
            return false;
        }

        /**
         * This should be called at the end of stream processing ONLY if
         * everything has gone well.
         * 
         * @throws IOException
         */
        void streamFinished() throws IOException {
            if (f_out != null) {
                f_out.println("</flashlight>");
                f_out.close();
            }
            f_header.close();
            File complete = new File(f_dir,
                    InstrumentationConstants.FL_COMPLETE_RUN_LOC);
            try {
                complete.createNewFile();
            } catch (IOException e) {
                SLLogger.getLoggerFor(ReadFlashlightStreamJob.class).log(
                        Level.WARNING, "Could not create Run.Complete", e);
            }
        }

    }
}