package com.surelogic.flashlight.jobs.prep;

public final class FieldRead extends FieldAccess {

	public String getXMLElementName() {
		return "field-read";
	}

	@Override
	protected String getRW() {
		return "R";
	}
}
