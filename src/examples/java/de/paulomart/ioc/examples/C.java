package de.paulomart.ioc.examples;

import de.paulomart.ioc.AutoDiscover;
import de.paulomart.ioc.lifecycle.Initializable;

@AutoDiscover
public class C implements InterfaceC, Initializable {

	public C(InterfaceB b) {
		System.out.println("C.C(" + b + ")");
	}

	@Override
	public void initialize() {
		System.out.println("C.initialize()");
	}
}
