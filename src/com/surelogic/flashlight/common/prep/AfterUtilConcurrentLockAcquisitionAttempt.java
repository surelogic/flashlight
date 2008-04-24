package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;

public class AfterUtilConcurrentLockAcquisitionAttempt extends
		UtilConcurrentLock {

	public AfterUtilConcurrentLockAcquisitionAttempt(BeforeTrace before) {
		super(before);
	}

	@Override
	protected String getType() {
		return "A";
	}

	@Override
	protected Boolean parseSuccess(Attributes attr) {
		return "yes".equals(attr.getValue("got-the-lock"));
	}

	public String getXMLElementName() {
		return "after-util-concurrent-lock-acquisition-attempt";
	}
}
