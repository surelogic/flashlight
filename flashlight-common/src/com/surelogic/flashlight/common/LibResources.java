package com.surelogic.flashlight.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;

public final class LibResources {

    /**
     * The name of the archive that contains the Flashlight Ant tasks.
     */
    public static final String ANT_TASK_ZIP = "flashlight-ant.zip";

    public static final String PATH = "/lib/";
    public static final String ANT_TASK_ZIP_PATHNAME = PATH + ANT_TASK_ZIP;

    public static InputStream getAntTaskZip() throws IOException {
        final URL url = LibResources.class.getResource(ANT_TASK_ZIP_PATHNAME);
        final InputStream is = url.openStream();
        return is;
    }

    @Nullable
    public static URL getDefaultQueryUrl() {
        return Thread.currentThread().getContextClassLoader()
                .getResource("/lib/adhoc/default-flashlight-queries.xml");
    }

    @Nullable
    public static URL getQuerydocImageURL(String imageName) {
        final String path = "/lib/adhoc/docimages/" + imageName;
        final URL url = Thread.currentThread().getContextClassLoader()
                .getResource(path);
        if (url != null) {
            return url;
        } else {
            return CommonImages.getImageURL(imageName);
        }
    }
}
