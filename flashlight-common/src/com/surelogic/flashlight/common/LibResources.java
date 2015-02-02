package com.surelogic.flashlight.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;

public final class LibResources {

  public static final String PLUGIN_ID = "com.surelogic.flashlight.common";

  public static final String PATH = "/lib/";

  /**
   * The name of the archive that contains the Flashlight Ant tasks.
   * <p>
   * Within this Zip should be a single directory of the form
   * <tt>flashlight-ant</tt>. The name of the Zip file is versioned when it is
   * saved to the disk, e.g., <tt>flashlight-ant-5.6.0.zip</tt>.
   */
  public static final String ANT_TASK_ZIP = "flashlight-ant.zip";

  /**
   * Full path to the Flashlight Ant tasks within this Eclipse plugin.
   */
  public static final String ANT_TASK_ZIP_PATHNAME = PATH + ANT_TASK_ZIP;

  /**
   * The name of the archive that contains the Flashlight Maven plugin.
   * <p>
   * Within this Zip should be a single directory of the form
   * <tt>flashlight-maven</tt>. The name of the Zip file is versioned when it is
   * saved to the disk, e.g., <tt>flashlight-maven-5.6.0.zip</tt>.
   */
  public static final String MAVEN_PLUGIN_ZIP = "flashlight-maven.zip";

  /**
   * Full path to the Flashlight Maven plugin within this Eclipse plugin.
   */
  public static final String MAVEN_PLUGIN_ZIP_PATHNAME = PATH + MAVEN_PLUGIN_ZIP;

  public static InputStream getStreamFor(String pathname) throws IOException {
    final URL url = LibResources.class.getResource(pathname);
    if (url == null)
      throw new IOException(I18N.err(323, pathname, PLUGIN_ID));
    final InputStream is = url.openStream();
    return is;
  }

  @Nullable
  public static URL getDefaultQueryUrl() {
    return Thread.currentThread().getContextClassLoader().getResource("/lib/adhoc/default-flashlight-queries.xml");
  }

  @Nullable
  public static URL getQuerydocImageURL(String imageName) {
    final String path = "/lib/adhoc/docimages/" + imageName;
    final URL url = Thread.currentThread().getContextClassLoader().getResource(path);
    if (url != null) {
      return url;
    } else {
      return CommonImages.getImageURL(imageName);
    }
  }
}
