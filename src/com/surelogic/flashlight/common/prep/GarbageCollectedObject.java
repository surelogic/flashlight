/*
 * Created on May 6, 2008
 */
package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;

public class GarbageCollectedObject extends Event {
	public String getXMLElementName() {
		return "garbage-collected-object";
	}

	public void parse(int runId, Attributes attributes) {
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				if ("id".equals(aName)) {
					final String aValue = attributes.getValue(i);
					f_rowInserter.gcObject(Long.parseLong(aValue));
				}
			}
		}
	}
}
