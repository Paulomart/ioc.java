package de.paulomart.ioc.examples;

import de.paulomart.ioc.AutoDiscover;
import de.paulomart.ioc.lifecycle.Initializable;

@AutoDiscover
public class B implements InterfaceB, Initializable {

	public B(InterfaceA a) {
		System.out.println("B.B(" + a + ")");
	}

	public B(A a) {
		System.out.println("B.B(" + a + ")");
	}

	@Override
	public void initialize() {
		System.out.println("B.initialize()");
	}
}
