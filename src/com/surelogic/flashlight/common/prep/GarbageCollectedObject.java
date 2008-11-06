/*
 * Created on May 6, 2008
 */
package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.ID;

import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.PreppedAttributes;

public class GarbageCollectedObject extends Event {
	GarbageCollectedObject(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public String getXMLElementName() {
		return "garbage-collected-object";
	}

	public void parse(final PreppedAttributes attributes) {
		final long id = attributes.getLong(ID);
		if (id != IdConstants.ILLEGAL_ID) {
			f_rowInserter.gcObject(id);
		}
	}
}
