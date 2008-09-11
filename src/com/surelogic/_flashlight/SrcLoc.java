package com.surelogic._flashlight;

final class SrcLoc {	
  private static final ClassPhantomReference unknownErrorPhantom =
    Phantom.ofClass(UnknownError.class);
  
	/**
	 * Cache for SrcLoc objects
	 */
	/*
	private static final ConcurrentMap<Class, ConcurrentHashMap<Integer, SrcLoc>> f_cache = 
		new ConcurrentReferenceHashMap<Class, ConcurrentHashMap<Integer, SrcLoc>>(ReferenceType.WEAK, 
				                                                                  ReferenceType.STRONG, true);
	*/
	
	static final SrcLoc UNKNOWN = new SrcLoc(unknownErrorPhantom, 0);

	private final int f_line;

	/*
	static SrcLoc getLocation(final Class<?> withinClass, final int line) {
		// Check if it's in the cache first
		ConcurrentHashMap<Integer, SrcLoc> lineMap = f_cache.get(withinClass);
		if (lineMap == null) {
			ConcurrentHashMap<Integer, SrcLoc> temp = new ConcurrentHashMap<Integer, SrcLoc>();
			lineMap = f_cache.putIfAbsent(withinClass, temp);
			if (lineMap == null) {
				lineMap = temp;
			}
		}
		Integer key = Integer.valueOf(line);
		SrcLoc loc  = lineMap.get(key);
		if (loc != null) {
			return loc;
		}
		SrcLoc temp = new SrcLoc(withinClass, line);
		loc = lineMap.putIfAbsent(key, temp);
		if (loc == null) {
			loc = temp;
		}
		return loc;		
		//return new SrcLoc(withinClass, line);
	}
	*/
	
	int getLine() {
		return f_line;
	}

	private final ClassPhantomReference f_withinClass;

	long getWithinClassId() {
		return f_withinClass.getId();
	}

  SrcLoc(ClassPhantomReference withinClass, final int line) {
     if (withinClass == null) {
       withinClass = unknownErrorPhantom;
     }
     f_withinClass = withinClass;
     f_line = line;
   }

	@Override
	public String toString() {
		return f_withinClass + ":" + f_line;
	}
	
	@Override 
	public int hashCode() {
		return (int) (getWithinClassId() + f_line);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof SrcLoc) {
			SrcLoc loc2 = (SrcLoc) o;
			return f_line == loc2.f_line && f_withinClass == loc2.f_withinClass;
		}
		return false;
	}
} 
