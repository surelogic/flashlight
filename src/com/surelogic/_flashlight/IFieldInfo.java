package com.surelogic._flashlight;

import java.util.*;

public interface IFieldInfo {	
	void setLastThread(long key, IdPhantomReference thread);

	void getSingleThreadedFields(Collection<SingleThreadedField> fields);

	void clear();
}
