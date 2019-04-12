package de.paulomart.ioc.factory;

public interface InstanceFactory<T> {

	T get() throws InstanceFactoryException;
}
