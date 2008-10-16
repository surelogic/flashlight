/*
 * Created on May 6, 2008
 */
package com.surelogic.flashlight.common.prep;

import com.surelogic._flashlight.common.*;

import static com.surelogic._flashlight.common.AttributeType.*;

public class GarbageCollectedObject extends Event {
	GarbageCollectedObject(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public String getXMLElementName() {
		return "garbage-collected-object";
	}

	public void parse(final int runId, final PreppedAttributes attributes) {
		long id = attributes.getLong(ID);
		if (id != IdConstants.ILLEGAL_ID) {
			f_rowInserter.gcObject(id);
		}
	}
}
