package de.paulomart.ioc;

public final class ClassHasNoConstructorException extends Exception {

	private static final long serialVersionUID = -3671631740370248556L;

	ClassHasNoConstructorException(Class<?> clazz) {
		super("The " + clazz + " has no accessible constructors.");
	}
}
