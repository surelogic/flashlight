package com.surelogic.flashlight.common.prep;

public class StaticFieldRead extends StaticFieldAccess {

	public String getXMLElementName() {
		return "field-read";
	}

	@Override
	protected String getRW() {
		return "R";
	}
}
