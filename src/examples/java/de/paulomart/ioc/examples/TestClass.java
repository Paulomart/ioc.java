package de.paulomart.ioc.examples;

import de.paulomart.ioc.Container;

public class TestClass {

	public static void main(String[] args) throws Exception {
		Container container = new Container();
		container.registerClass(A.class);
		
		container.resolve(InterfaceA.class);
		
		container.registerClasses(ClassLoader.getSystemClassLoader(), "de.paulomart.ioc.examples");
		InterfaceC c = container.resolve(InterfaceC.class);
		System.out.println(c);
	}
}
