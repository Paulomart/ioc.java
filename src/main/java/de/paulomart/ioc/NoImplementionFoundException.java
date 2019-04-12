package de.paulomart.ioc;

public final class NoImplementionFoundException extends Exception {

	private static final long serialVersionUID = 3864535307164472946L;

	NoImplementionFoundException(Class<?> iface) {
		super("No implemention found for " + iface + ".");
	}
}
