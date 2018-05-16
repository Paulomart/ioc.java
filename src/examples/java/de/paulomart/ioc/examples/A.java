package de.paulomart.ioc.examples;

import de.paulomart.ioc.AutoDiscover;
import de.paulomart.ioc.lifecycle.Initializable;

@AutoDiscover
public class A implements InterfaceA, Initializable {

	public A() {
		System.out.println("A.A()");
	}

	@Override
	public void initialize() {
		System.out.println("A.initialize()");
	}
}
