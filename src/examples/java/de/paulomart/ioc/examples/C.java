package de.paulomart.ioc.examples;

import de.paulomart.ioc.AutoRegister;
import de.paulomart.ioc.AutoRegister.InstanceType;
import de.paulomart.ioc.lifecycle.Initializable;

@AutoRegister(InstanceType.SINGLETON)
public class C implements InterfaceC, Initializable {

	public C(InterfaceB b, InterfaceA a) {
		System.out.println("C.C(" + b + ", " + a + ") -> " + this);
	}

	@Override
	public void initialize() {
		System.out.println("C.initialize()");
	}
}
