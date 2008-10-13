package com.surelogic._flashlight.common;

import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.jobs.SLProgressMonitor;

public class AbstractDataScan extends DefaultHandler {
	protected final SLProgressMonitor f_monitor;

	public AbstractDataScan(final SLProgressMonitor monitor) {
		assert monitor != null;
		f_monitor = monitor;
	}
	
	protected static Map<String,Object> preprocessAttributes(Attributes a) {
		throw new UnsupportedOperationException();
	}
}
