package de.paulomart.ioc.examples;

import de.paulomart.ioc.Container;

public class TestClass {

	public static void main(String[] args) throws Exception {
		Container container = new Container();
	
		container.autoRegister(ClassLoader.getSystemClassLoader(), "de.paulomart.ioc.examples");
		// or register all class manuelly
		// container.registerTransient(A.class);
		// container.registerSingeltion(B.class);
		// container.registerSingeltion(C.class);

		InterfaceC r =  container.resolve(InterfaceC.class);
		System.out.println(r);
	}
}
