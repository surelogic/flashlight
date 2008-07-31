package com.surelogic.flashlight.common.prep;

public final class FieldWrite extends FieldAccess {

	public FieldWrite(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
	}

	public String getXMLElementName() {
		return "field-write";
	}

	@Override
	protected String getRW() {
		return "W";
	}
}
