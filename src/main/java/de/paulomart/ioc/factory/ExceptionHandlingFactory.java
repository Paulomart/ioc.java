package de.paulomart.ioc.factory;

public abstract class ExceptionHandlingFactory<T> implements InstanceFactory<T> {

	private final InstanceFactory<T> proxied;

	public ExceptionHandlingFactory(InstanceFactory<T> proxied) {
		this.proxied = proxied;
	}

	@Override
	public final T get() {
		try {
			return proxied.get();
		} catch (InstanceFactoryException e) {
			return handleException(e);
		}
	}

	protected abstract T handleException(InstanceFactoryException e);
}
