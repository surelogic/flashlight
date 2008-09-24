/*
 * Created on May 6, 2008
 */
package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;

import static com.surelogic._flashlight.common.AttributeType.*;

public class GarbageCollectedObject extends Event {
	GarbageCollectedObject(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public String getXMLElementName() {
		return "garbage-collected-object";
	}

	public void parse(final int runId, final Attributes attributes) {
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				if (ID.matches(aName)) {
					final String aValue = attributes.getValue(i);
					f_rowInserter.gcObject(Long.parseLong(aValue));
				}
			}
		}
	}
}
