package com.surelogic.flashlight.jobs.prep;

public final class FieldWrite extends FieldAccess {

	public String getXMLElementName() {
		return "field-write";
	}

	@Override
	protected String getRW() {
		return "W";
	}
}
