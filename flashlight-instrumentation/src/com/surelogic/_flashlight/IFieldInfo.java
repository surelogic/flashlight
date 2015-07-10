package com.surelogic._flashlight;

public interface IFieldInfo {
  IdPhantomReference SHARED_BY_THREADS = Phantom.ofClass(IFieldInfo.class);

  void setLastThread(long key, IdPhantomReference thread);

  boolean getSingleThreadedFields(SingleThreadedRefs refs);

  void clear();
}
