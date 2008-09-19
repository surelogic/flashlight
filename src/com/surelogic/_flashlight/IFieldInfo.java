package com.surelogic._flashlight;

import java.util.*;

public interface IFieldInfo {
	IdPhantomReference SHARED_BY_THREADS = Phantom.ofClass(Object.class);
	
	void setLastThread(long key, IdPhantomReference thread);

	void getSingleThreadedFields(Collection<SingleThreadedField> fields);

	void clear();
}
