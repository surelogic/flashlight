package com.surelogic.dynamic.test;

import java.util.Random;

/*
 * Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

public class Deadlock {
    static class Friend {
        private final String name;
        private final int delay;
        public Friend(String name, int delay) {
            this.name = name;
            this.delay = delay;
        }
        public String getName() {
            return this.name;
        }
        
		private void delay() {
			if (delay > 0) {
            	try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
		}
		
        public synchronized void bow(Friend bower) {
            System.out.format("%s: %s has bowed to me!%n", 
                    this.name, bower.getName());
            bower.bowBack(this);
        }

        public synchronized void bowBack(Friend bower) {
            delay();
        	
            System.out.format("%s: %s has bowed back to me!%n",
                    this.name, bower.getName());
        }
    }

    public static void main(String[] args) {
      run(0, new IntGeneratorFactory() {
		public IntGenerator create() {
			final Random r = new Random();
			return new IntGenerator() {
				public int nextInt(int max) {
					return r.nextInt(max);
				}
			};
		}    	  
      });	
    }
    
    public static void run(int delay, final IntGeneratorFactory igf) {
        final Friend[] friends = { 
        		new Friend("Alphonse", delay),
        		new Friend("Bob", delay),
        		new Friend("Carol", delay),
        		new Friend("Deirdre", delay),
        		new Friend("Ed", delay),
        		new Friend("Francois", delay),
        		new Friend("Gaston", delay),
        };
        for(final Friend f : friends) {
        	final Runnable r = new Runnable() {
        		final IntGenerator r = igf.create();
        		public void run() { 
        			for(int i=0; i<10; i++) {
        				try {
        					Thread.sleep(r.nextInt(1000));
        				} catch (InterruptedException e) {
        					e.printStackTrace();
        				}
        				Friend f2 = friends[r.nextInt(friends.length)];
        				if (f != f2) {
        				  f.bow(f2);
        				}
        			}
        		}
        	};
        	new Thread(r).start();
        }
    }
    
    static interface IntGenerator {
        int nextInt(int max);	
    }
    
    static interface IntGeneratorFactory {
    	IntGenerator create();
    }
}

