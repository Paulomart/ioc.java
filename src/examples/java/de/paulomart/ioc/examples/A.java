package de.paulomart.ioc.examples;

import de.paulomart.ioc.AutoRegister;
import de.paulomart.ioc.AutoRegister.InstanceType;
import de.paulomart.ioc.lifecycle.Initializable;

@AutoRegister(InstanceType.TRANSIENT)
public class A implements InterfaceA, Initializable {

	public A() {
		System.out.println("A.A() -> " + this);
	}

	@Override
	public void initialize() {
		System.out.println("A.initialize()");
	}
}
