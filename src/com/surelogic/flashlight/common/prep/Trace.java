package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;

public abstract class Trace extends Event {	
	public void parse(int runId, Attributes attributes) {
		parseAttrs(attributes);
		long time = Long.parseLong(getAttr(NANO_TIME));
		long inThread = Long.parseLong(getAttr(THREAD));
		String file = getAttr(FILE);
		int lineNumber = Integer.parseInt(getAttr(LINE));
		handleTrace(runId, inThread, time, file, lineNumber);
	}

	protected abstract void handleTrace(int runId, long inThread, long time, 
			                            String file,	int lineNumber);
}
