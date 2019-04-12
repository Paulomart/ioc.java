package de.paulomart.ioc.factory;

import java.lang.ref.WeakReference;

public final class SingletonFactory<T> implements InstanceFactory<T> {

	private final InstanceFactory<T> provider;
	private WeakReference<T> instance;

	public SingletonFactory(InstanceFactory<T> provider) {
		this.provider = provider;
	}

	public synchronized T get() throws InstanceFactoryException {
		if (instance == null || instance.get() == null) {
			instance = new WeakReference<T>(provider.get());
		}
		return instance.get();
	}
}
