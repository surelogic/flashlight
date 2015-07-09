package com.surelogic.dynamic.test;

import com.surelogic.dynamic.test.Deadlock.IntGenerator;
import com.surelogic.dynamic.test.Deadlock.IntGeneratorFactory;

public class TestDurations {
	public static void main(String[] args) {
		// Generator designed to cause lock contention
		Deadlock.run(20, new IntGeneratorFactory() {
			public IntGenerator create() {
				return new IntGenerator() {
					boolean min = true;
					public int nextInt(int max) {
						int result;
						if (min) {
							result = 0;
						} else if (max > 50) {
							result = 50;
						} else {
							result = max-1;
						}
						min = !min;
						return result;
					}
				};
			}    	  
		});	
	}
}
