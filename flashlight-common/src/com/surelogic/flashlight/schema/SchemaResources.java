package com.surelogic.flashlight.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;

public final class SchemaResources {

  public static final String PLUGIN_ID = "com.surelogic.flashlight.common";

  public static final String PATH = "/lib/";
  public static final String ADHOC_PATH = "/lib/adhoc/";

  public static final String FLASHLIGHT_QUERIES = "default-flashlight-queries.xml";
  public static final String FLASHLIGHT_QUERIES_PATH = ADHOC_PATH + FLASHLIGHT_QUERIES;

  public static final String QUERYDOC_IMAGE_PATH = ADHOC_PATH + "docimages/";

  public static InputStream getStreamFor(String pathname) throws IOException {
    final URL url = SchemaResources.class.getResource(pathname);
    if (url == null)
      throw new IOException(I18N.err(323, pathname, PLUGIN_ID));
    final InputStream is = url.openStream();
    return is;
  }

  @Nullable
  public static URL getDefaultQueryUrl() {
    return Thread.currentThread().getContextClassLoader().getResource(FLASHLIGHT_QUERIES_PATH);
  }

  @Nullable
  public static URL getQuerydocImageURL(String imageName) {
    final String path = QUERYDOC_IMAGE_PATH + imageName;
    final URL url = Thread.currentThread().getContextClassLoader().getResource(path);
    if (url != null) {
      return url;
    } else {
      return CommonImages.getImageURL(imageName);
    }
  }
}
