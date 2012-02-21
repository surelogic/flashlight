package com.surelogic.flashlight.eclipse.client.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.FileUtility;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;

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
                File file = new File(f_dir, f_runName + type.getSuffix());
                OutputStream out = OutputType.getOutputStreamFor(file);
                FileUtility.copyToStream(false, "Port: " + f_port, in,
                        file.getAbsolutePath(), out, true);
            } finally {
                socket.close();
            }
            return SLStatus.OK_STATUS;
        } catch (IOException e) {
            return SLStatus.createErrorStatus(e);
        }
    }
}
