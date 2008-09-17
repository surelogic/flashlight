package com.surelogic._flashlight;

public interface IFieldInfo {
	IdPhantomReference SHARED_BY_THREADS = Phantom.ofClass(Object.class);
	
	void setLastThread(ObservedField key, IdPhantomReference thread);
}
