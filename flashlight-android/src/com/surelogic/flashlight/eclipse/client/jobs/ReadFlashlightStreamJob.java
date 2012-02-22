package com.surelogic.flashlight.eclipse.client.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.prep.AbstractDataScan;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class ReadFlashlightStreamJob implements SLJob {

    private final String f_runName;
    private final int f_port;
    private final File f_dir;

    public ReadFlashlightStreamJob(final String runName, final File infoDir,
            final int outputPort) {
        f_runName = runName;
        f_port = outputPort;
        f_dir = infoDir;
    }

    @Override
    public String getName() {
        return "Collecting data from " + f_runName + ".";
    }

    @Override
    public SLStatus run(final SLProgressMonitor monitor) {
        try {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress("localhost", f_port));
                OutputType type = InstrumentationConstants.FL_OUTPUT_TYPE_DEFAULT;
                InputStream in = OutputType.getInputStreamFor(
                        socket.getInputStream(), type);
                SAXParser parser = OutputType.getParser(type);
                CheckpointingEventHandler h = new CheckpointingEventHandler(
                        type);
                try {
                    parser.parse(in, h);
                    h.streamFinished();
                } catch (Exception e) {
                    h.streamBroken();
                    return SLStatus.createErrorStatus(e);
                }
                // FileUtility.copyToStream(false, "Port: " + f_port, in,
                // file.getAbsolutePath(), out, true);
            } finally {
                socket.close();
            }
            return SLStatus.OK_STATUS;
        } catch (Exception e) {
            return SLStatus.createErrorStatus(e);
        }
    }

    class CheckpointingEventHandler extends AbstractDataScan {
        private final OutputType f_type;
        private PrintWriter f_out;
        private int f_count;
        private StringBuilder buf;

        public CheckpointingEventHandler(final OutputType type)
                throws IOException {
            super(new NullSLProgressMonitor());
            f_type = type;
        }

        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes)
                throws SAXException {
            PrepEvent event = PrepEvent.getEvent(qName);
            buf.append('<');
            buf.append(event.getXmlName());
            int size = attributes.getLength();
            for (int i = 0; i < size; i++) {
                buf.append(' ');
                buf.append(attributes.getQName(i));
                buf.append("='");
                buf.append(attributes.getValue(i));
                buf.append('\'');
            }
            if (event == PrepEvent.FLASHLIGHT) {
                buf.append('>');
            } else {
                buf.append("/>");
            }
            f_out.println(buf);
            buf.setLength(0);
            if (event == PrepEvent.CHECKPOINT) {
                try {
                    nextStream();
                } catch (IOException e) {
                    throw new SAXException(e);
                }
            }

        }

        void nextStream() throws IOException {
            if (f_out != null) {
                f_out.println("</flashlight>");
                f_out.close();
            }
            f_out = new PrintWriter(OutputType.getOutputStreamFor(new File(
                    f_dir, f_runName + String.format(".%06d", f_count++)
                            + f_type.getSuffix())));
            f_out.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");

        }

        /**
         * Call this if we get an error during data processing.
         */
        void streamBroken() {

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
        }

    }
}
