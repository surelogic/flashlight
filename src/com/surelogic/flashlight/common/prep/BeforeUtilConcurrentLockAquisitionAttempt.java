package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;

public class BeforeUtilConcurrentLockAquisitionAttempt extends
		UtilConcurrentLock {

	public BeforeUtilConcurrentLockAquisitionAttempt(BeforeTrace before) {
		super(before);
	}

	@Override
	protected String getType() {
		return "B";
	}

	public String getXMLElementName() {
		return "before-util-concurrent-lock-acquisition-attempt";
	}

	@Override
	protected Boolean parseSuccess(Attributes attr) {
		return null;
	}

}
