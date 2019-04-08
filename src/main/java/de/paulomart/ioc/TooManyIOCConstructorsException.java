package de.paulomart.ioc;

public final class TooManyIOCConstructorsException extends Exception {

	private static final long serialVersionUID = -9216897864986382608L;

	TooManyIOCConstructorsException(Class<?> clazz) {
		super("The " + clazz + " has at least two constructors that are annotated with the " + IOCConstructor.class + " annotation.");
	}
}
