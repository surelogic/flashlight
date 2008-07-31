package com.surelogic.flashlight.common.prep;

public final class FieldRead extends FieldAccess {

	public FieldRead(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
	}

	public String getXMLElementName() {
		return "field-read";
	}

	@Override
	protected String getRW() {
		return "R";
	}
}
