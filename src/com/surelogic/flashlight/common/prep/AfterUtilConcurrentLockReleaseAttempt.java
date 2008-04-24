package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;

public class AfterUtilConcurrentLockReleaseAttempt extends UtilConcurrentLock {

	public AfterUtilConcurrentLockReleaseAttempt(BeforeTrace before) {
		super(before);
	}

	@Override
	protected String getType() {
		return "R";
	}

	@Override
	protected Boolean parseSuccess(Attributes attr) {
		return Boolean.parseBoolean(attr.getValue("released-the-lock"));
	}

	public String getXMLElementName() {
		return "after-util-concurrent-lock-release-attempt";
	}

}
