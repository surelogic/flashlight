package com.surelogic.flashlight.common.prep;

public final class ClassDefinition extends ReferenceDefinition {

	public String getXMLElementName() {
		return "class-definition";
	}

	@Override
	protected String getFlag() {
		return "C";
	}
}
