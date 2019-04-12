package de.paulomart.ioc.examples;

import de.paulomart.ioc.AutoRegister;
import de.paulomart.ioc.AutoRegister.InstanceType;
import de.paulomart.ioc.lifecycle.Initializable;

@AutoRegister(InstanceType.SINGLETON)
public class B implements InterfaceB, Initializable {

	public B(InterfaceA a, InterfaceA a2) {
		System.out.println("B.B(" + a + ", " + a2 + ") -> " + this);
	}

	@Override
	public void initialize() {
		System.out.println("B.initialize()");
	}
}
