package com.surelogic.flashlight.common.prep;

public final class FieldRead extends FieldAccess {

	public FieldRead(BeforeTrace before) {
		super(before);
	}

	public String getXMLElementName() {
		return "field-read";
	}

	@Override
	protected String getRW() {
		return "R";
	}
}
