package com.surelogic.flashlight.common.prep;

public class StaticFieldWrite extends StaticFieldAccess {

	public String getXMLElementName() {
		return "field-write";
	}

	@Override
	protected String getRW() {
		return "W";
	}
}
