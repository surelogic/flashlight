package com.surelogic.flashlight.common.prep;

public class ObjectDefinition extends ReferenceDefinition {

	public String getXMLElementName() {
		return "object-definition";
	}

	@Override
	protected String getFlag() {
		return "O";
	}
}
