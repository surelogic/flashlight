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
   * The name of the archive that contains the Flashlight HTML documentation
   * Zip.
   * <p>
   * Within this Zip is all the Flashlight on-line documents in separate
   * directories so that users can examine them in a browser (which is often
   * preferred to Eclipse on-line help)..
   */
  public static final String HTML_DOCS_ZIP = "flashlight-html-docs.zip";

  /**
   * Full path to the Flashlight HTML documentation Zip within this Eclipse
   * plugin.
   */
  public static final String HTML_DOCS_ZIP_PATHNAME = PATH + HTML_DOCS_ZIP;

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
