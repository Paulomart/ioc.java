package de.paulomart.ioc;

public class ClassNotRegisteredException extends Exception {

	private static final long serialVersionUID = 1218369557932294533L;

	ClassNotRegisteredException(Class<?> clazz) {
		super("Error resolving class that was not registered: " + clazz);
	}
}
