package com.surelogic.flashlight.common.prep;

public final class ThreadDefinition extends ReferenceDefinition {

	public String getXMLElementName() {
		return "thread-definition";
	}

	@Override
	protected String getFlag() {
		return "T";
	}

	@Override
	protected boolean filterUnused() {
		return true;
	}

}
