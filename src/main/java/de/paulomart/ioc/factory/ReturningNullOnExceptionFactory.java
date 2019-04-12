package de.paulomart.ioc.factory;

import java.util.logging.Logger;

public final class ReturningNullOnExceptionFactory<T> extends ExceptionHandlingFactory<T> {

	public ReturningNullOnExceptionFactory(InstanceFactory<T> proxied, Logger log) {
		super(proxied);
	}

	@Override
	protected T handleException(InstanceFactoryException e) {
		return null;
	}
}
