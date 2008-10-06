package com.surelogic._flashlight;

public abstract class AbstractRefinery extends Thread {
	AbstractRefinery(String name) {
		super(name);
	}
	
	@Override
	public abstract void run();

	public void requestShutdown() {
		// Does nothing here
	}
}
