package de.paulomart.ioc;

public final class TooManyConstructorsException extends Exception {

	private static final long serialVersionUID = -3939009676127106569L;

	TooManyConstructorsException(Class<?> clazz) {
		super("The " + clazz + " has too many possible constructors.");
	}
}
