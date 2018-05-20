package de.paulomart.ioc;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.paulomart.ioc.lifecycle.Destroyable;
import de.paulomart.ioc.lifecycle.Initializable;
import de.paulomart.ioc.lifecycle.LifeCycleHook;

public class Container {

	private final Map<Class<?>, Object> instances = new HashMap<>();
	private final Map<Class<?>, Class<?>> classes = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final LifeCycleHook lifeCycleHook;
	private final ExceptionHandler exceptionHandler;

	public Container(LifeCycleHook lifeCycleHook, ExceptionHandler exceptionHandler) {
		if (lifeCycleHook == null) {
			throw new IllegalArgumentException("lifeCycleHook == null");
		}
		if (exceptionHandler == null) {
			throw new IllegalArgumentException("exceptionHandler == null");
		}
		this.lifeCycleHook = lifeCycleHook;
		this.exceptionHandler = exceptionHandler;
	}

	public Container() {
		this(LifeCycleHook.NOOP, ExceptionHandler.THROWING);
	}

	public void registerClass(Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.isArray() || clazz.isAnnotation() || clazz.isEnum()) {
			throw new IllegalArgumentException(clazz.toString());
		}
		registerClass(clazz, clazz);
	}

	private void registerClass(Class<?> searchClass, Class<?> impl) {
		for (Class<?> iface : searchClass.getInterfaces()) {
			if (iface == Destroyable.class || iface == Initializable.class) {
				continue;
			}
			lock.writeLock().lock();
			try {
				classes.put(iface, impl);
			} finally {
				lock.writeLock().unlock();
			}
			registerClass(iface, impl);
		}
		if (!searchClass.isInterface() && searchClass.getSuperclass() != null && searchClass.getSuperclass() != Object.class) {
			registerClass(searchClass.getSuperclass(), impl);
		}
	}

	public void registerClasses(ClassLoader classLoader, String prefix) throws Exception {
		ClassDiscoverHelper searcher = new ClassDiscoverHelper(classLoader, (x) -> x.startsWith(prefix));
		searcher.search();
		for (Class<?> clazz : searcher.getResult()) {
			registerClass(clazz);
		}
	}

	public void registerInstance(Object instance) {
		registerInstance(instance, instance.getClass());
	}

	public void registerInstance(Object instance, Class<?> asClass) {
		lock.writeLock().lock();
		try {
			instances.put(asClass, instance);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public void destoryInstances() throws Exception {
		Collection<Entry<Class<?>, Object>> instancesToDestroy;
		lock.writeLock().lock();
		try {
			instancesToDestroy = instances.entrySet();
			instances.clear();
		} finally {
			lock.writeLock().unlock();
		}
		for (Entry<Class<?>, Object> entry : instancesToDestroy) {
			Object instance = entry.getValue();
			Class<Object> forClass = (Class<Object>) entry.getKey();
			try {
				lifeCycleHook.onInstanceDestroyed(instance, forClass);
			} catch (Exception ex) {
				exceptionHandler.handleExceptionOnDestruction(instance, forClass, ex);
			}
		}
	}

	public <T> T resolve(Class<T> ifaceClass) throws Exception {
		return resolveInterfaceOrClass(ifaceClass);
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveInterfaceOrClass(Class<T> clazz) throws Exception {
		Class<?> targetClass;
		boolean createNewInstance;
		lock.readLock().lock();
		try {
			if (instances.containsKey(clazz)) {
				return (T) instances.get(clazz);
			}
			targetClass = classes.get(clazz);
			createNewInstance = !instances.containsKey(targetClass);
		} finally {
			lock.readLock().unlock();
		}
		if (targetClass == null) {
			if (clazz.isInterface()) {
				throw new IllegalArgumentException("No implementation found for " + clazz);
			}
			targetClass = clazz;
		}
		if (createNewInstance == false) {
			lock.readLock().lock();
			try {
				return (T) instances.get(targetClass);
			} finally {
				lock.readLock().unlock();
			}
		}
		T instance;
		lock.writeLock().lock();
		try {
			// check for lost race
			if (instances.containsKey(targetClass)) {
				return (T) instances.get(targetClass);
			}
			try {
				instance = (T) createInstance(targetClass);
			} catch (Exception e) {
				instance = exceptionHandler.handleExceptionOnInstantiation(clazz, e);
			}
			instances.put(targetClass, instance);
		} finally {
			lock.writeLock().unlock();
		}
		try {
			lifeCycleHook.onInstanceCreated(instance, clazz);
		} catch (Throwable e) {
			Logger.getGlobal().log(Level.WARNING, "Error calling life cycle hook " + lifeCycleHook, e);
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private <T> T createInstance(Class<T> clazz) throws Exception {
		Constructor<?>[] constructorArray = clazz.getConstructors();
		List<Constructor<?>> constructors = Stream.of(constructorArray).filter(x -> {
			return !x.isVarArgs();
		}).sorted((a, b) -> {
			boolean aIsIOC = a.isAnnotationPresent(AutoDiscover.class);
			boolean bIsIOC = b.isAnnotationPresent(AutoDiscover.class);
			return Boolean.compare(aIsIOC, bIsIOC);
		}).collect(Collectors.toList());
		try {
			constructors.add(clazz.getConstructor());
		} catch (Throwable e) {
		}
		if (constructors.isEmpty()) {
			throw new Exception("No constructors found for " + clazz);
		}
		Throwable lastException = null;
		for (Constructor<?> constructor : constructors) {
			try {
				return callConstructor((Constructor<T>) constructor);
			} catch (Exception e) {
				lastException = e;
			}
		}
		throw new Exception("Error constructing " + clazz, lastException);
	}

	private <T> T callConstructor(Constructor<T> constructor) throws Exception {
		Object[] parameters = new Object[constructor.getParameterTypes().length];
		int index = 0;
		for (Class<?> clazz : constructor.getParameterTypes()) {
			parameters[index] = resolveInterfaceOrClass(clazz);
			index++;
		}
		return constructor.newInstance(parameters);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(getClass().getName());
		s.append("(classes={");
		classes.forEach((key, value) -> s.append("\n").append(key).append("=").append(value));
		s.append("\n}, instances={");
		instances.forEach((key, value) -> s.append("\n").append(key).append("=").append(value));
		s.append("\n})");
		return s.toString();
	}
}
