package com.surelogic.dynamic.test;

public class TraceStress {

	TraceStress() {
		System.out.println("TraceStress()");
		m1();
	}

	{
		System.out.println("TraceStress:instance initialization");
		m1();
	}

	static {
		System.out.println("TraceStress: static initialization");
	}

	void m1() {
		System.out.println("m1()");
		// nested class
		Foo f = new Foo();
		f.m1();
		// static nested class
		Bar b = new Bar();
		b.m1();
		// anonymous class
		Runnable r = new Runnable() {
			public void run() {
				System.out.println("r.run()");
			}
		};
		r.run();
		// local class
		class Local {
			Local() {
				System.out.println("Local()");
			}

			void m1() {
				System.out.println("m1()");
			}
		}
		Local l = new Local();
		l.m1();
	}

	class Foo {
		Foo() {
			System.out.println("Foo()");
		}

		void m1() {
			System.out.println("m1()");
		}
	}

	static class Bar {
		Bar() {
			System.out.println("Bar()");
		}

		void m1() {
			System.out.println("m1()");
		}
	}

	void m2() {
		System.out.println("m2()");
		m3();
	}

	void m3() {
		System.out.println("m3()");
		throw new IllegalArgumentException("bogas");
	}

	public static void main(String[] args) {
		System.out.println("main()");
		TraceStress ts = new TraceStress();
		ts.m1();
		try {
			ts.m2();
		} catch (Exception e) {
			System.out.println("caught");
		}
	}
}
