package com.surelogic.flashlight.common;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.adhoc.AbstractAdHocGlue;

public abstract class AbstractFlashlightAdhocGlue extends AbstractAdHocGlue {
	public final Connection getConnection() throws SQLException {
		return Data.getInstance().getConnection();
	}
	
    protected static void copyDefaultQueryFile(File qsf) throws IOException {
        InputStream is = null;
        try {
            Data.getInstance();
			URL url = Data.getDefaultQueryFileURL();
            is = url.openStream();
            is = new BufferedInputStream(is, 8192);
            OutputStream os = new FileOutputStream(qsf);
            
            byte[] buf = new byte[8192];
            int num;
            while ((num = is.read(buf)) >= 0) {
                os.write(buf, 0, num);
            }
        } finally {
            if (is != null) is.close();
        }
    }
}
