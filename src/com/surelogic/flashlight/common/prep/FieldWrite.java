package com.surelogic.flashlight.common.prep;

public final class FieldWrite extends FieldAccess {

	public FieldWrite(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public String getXMLElementName() {
		return "field-write";
	}

	@Override
	protected String getRW() {
		return "W";
	}
}
