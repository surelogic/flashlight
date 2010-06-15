package com.surelogic.flashlight.schema;

import java.io.InputStream;
import java.net.URL;

import com.surelogic.common.license.SLLicenseProduct;

public class FlashlightSchemaData extends AbstractSchemaData {
	public FlashlightSchemaData() {
		super("com.surelogic.flashlight.schema",
			  Thread.currentThread().getContextClassLoader(),
			  SLLicenseProduct.FLASHLIGHT);
	}

	protected Object newInstance(String qname) 
	throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return loader.loadClass(qname).newInstance();
	}
	
	public URL getSchemaResource(final String name) {
		return loader.getResource(getSchemaResourcePath(name));
	}

	protected InputStream getResourceAsStream(String path) {
		return loader.getResourceAsStream(path);
	}
}