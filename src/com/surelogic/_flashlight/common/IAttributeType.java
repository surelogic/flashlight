package com.surelogic._flashlight.common;

import java.util.Comparator;

public interface IAttributeType {
	int base();
	int ordinal();
	String label();
	
	Comparator<IAttributeType> comparator = new Comparator<IAttributeType>() {
		public int compare(IAttributeType t1, IAttributeType t2) {
			return (t1.base() - t2.base()) + (t1.ordinal() - t2.ordinal());			       
		}
	};
}
