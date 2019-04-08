package de.paulomart.ioc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.paulomart.ioc.factory.InstanceFactory;
import de.paulomart.ioc.factory.InstanceFactoryException;
import de.paulomart.ioc.factory.SingletonFactory;
import de.paulomart.ioc.lifecycle.Destroyable;
import de.paulomart.ioc.lifecycle.Initializable;
import de.paulomart.ioc.lifecycle.LifeCycleHook;

public final class Container {

	private final Object lock = new Object[0];
	private final Map<Class<?>, Registration<?>> registrations = new HashMap<>();
	private final Map<Class<?>, Class<?>> implemenations = new HashMap<>();
	private final LifeCycleService lifeCycle;
	private final Function<InstanceFactory<?>, InstanceFactory<?>> factoryWrapper;

	public Container() {
		this(LifeCycleHook.DEFAULT);
	}

	public Container(LifeCycleHook lifeCycleHook) {
		this.lifeCycle = new LifeCycleService(lifeCycleHook);
		this.factoryWrapper = Function.identity();
	}

	public Container(LifeCycleHook lifeCycleHook, Function<InstanceFactory<?>, InstanceFactory<?>> factoryWrapper) {
		this.lifeCycle = new LifeCycleService(lifeCycleHook);
		this.factoryWrapper = factoryWrapper;
	}

	@Deprecated
	public void registerClasses(ClassLoader classLoader, String prefix) throws Exception {
		ClassDiscoverHelper searcher = new ClassDiscoverHelper(classLoader, (x) -> x.startsWith(prefix));
		searcher.search();
		for (Class<?> clazz : searcher.getResult()) {
			registerClass(clazz);
		}
	}

	@Deprecated
	public <T> void registerClass(Class<T> clazz) {
		registerSingeltion(clazz);
	}

	private void validateType(Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.isArray() || clazz.isAnnotation() || clazz.isEnum()) {
			throw new IllegalArgumentException("Illegal type:" + clazz.toString());
		}
	}

	public void autoRegister(ClassLoader classLoader, String prefix) throws Exception {
		ClassDiscoverHelper searcher = new ClassDiscoverHelper(classLoader, (x) -> x.startsWith(prefix));
		searcher.search();
		for (Class<?> clazz : searcher.getResult()) {
			AutoRegister autoRegister = clazz.getAnnotation(AutoRegister.class);
			if (autoRegister == null) {
				// legacy class
				registerClass(clazz);
				continue;
			}
			AutoRegister.InstanceType type = autoRegister.value();
			switch (type) {
				case SINGLETON:
					registerSingeltion(clazz);
					break;
				case TRANSIENT:
					registerTransient(clazz);
					break;
				default:
					throw new IllegalArgumentException("Unknown type: " + type);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> void register(RegistrationType type, Class<T> clazz, InstanceFactory<T> factory) {
		InstanceFactory<T> wrapped = (InstanceFactory<T>) factoryWrapper.apply(factory);
		Registration<T> registration = new Registration<>(clazz, type, wrapped);
		synchronized (this.lock) {
			this.registrations.put(registration.registeredClass, registration);
			registerImplementions(clazz, clazz);
		}
	}

	public <T> void registerTransient(Class<T> clazz) {
		TransientFactory<T> factory = new TransientFactory<>(clazz);
		register(RegistrationType.TRANSIENT, clazz, factory);
	}

	// Note: Instances registered as singeltion will have no lifecycle.
	@SuppressWarnings("unchecked")
	public <T> void registerSingeltion(T instance) {
		registerSingeltion(instance, (Class<T>) instance.getClass());
	}

	public <T> void registerSingeltion(T instance, Class<T> as) {
		SingletonFactory<T> factory = new SingletonFactory<>(() -> instance);
		register(RegistrationType.SINGLETON, as, factory);
	}

	public <T> void registerSingeltion(Class<T> clazz) {
		TransientFactory<T> transientFactory = new TransientFactory<>(clazz);
		SingletonFactory<T> singletonFactory = new SingletonFactory<>(transientFactory);
		register(RegistrationType.SINGLETON, clazz, singletonFactory);
	}

	// Must hold lock
	private void registerImplementions(Class<?> impl, Class<?> searchClass) {
		for (Class<?> iface : searchClass.getInterfaces()) {
			if (iface == Destroyable.class || iface == Initializable.class) {
				continue;
			}
			this.implemenations.put(iface, impl);
			registerImplementions(impl, iface);
		}
		if (!searchClass.isInterface() && searchClass.getSuperclass() != null && searchClass.getSuperclass() != Object.class) {
			registerImplementions(impl, searchClass.getSuperclass());
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Registration<T> findImplemention(Class<?> iface) {
		synchronized (this.lock) {
			Class<T> target = (Class<T>) this.implemenations.get(iface);
			return (Registration<T>) this.registrations.get(target);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T resolve(Class<T> interfaceOrClass) throws InstanceFactoryException, NoImplementionFoundException, ClassNotRegisteredException {
		// Find to the target class.
		Class<?> resolvedForInterface;
		Registration<T> target;
		if (interfaceOrClass.isInterface()) {
			// If the class is an interface, we must lookup the target class
			resolvedForInterface = interfaceOrClass;
			target = (Registration<T>) findImplemention(resolvedForInterface);
			if (target == null) {
				throw new NoImplementionFoundException(resolvedForInterface);
			}
		} else {
			// If the class is a class, just continue with that
			resolvedForInterface = null;
			synchronized (this.lock) {
				target = (Registration<T>) this.registrations.get(interfaceOrClass);
			}
			if (target == null) {
				throw new ClassNotRegisteredException(interfaceOrClass);
			}
		}
		return target.factory.get();
	}

	@SuppressWarnings("unchecked")
	private <T> Constructor<T> getConstructorForClass(Class<T> clazz) throws ClassHasNoConstructorException, TooManyIOCConstructorsException, TooManyConstructorsException {
		Constructor<T>[] constructors = (Constructor<T>[]) clazz.getConstructors();
		if (constructors.length == 0) {
			throw new ClassHasNoConstructorException(clazz);
		} else if (constructors.length == 1) {
			// only one constructor was found for the class, use it
			return constructors[0];
		}
		// Try to find another constructor
		Constructor<T> targetConstructor = null;
		for (Constructor<T> constructor : constructors) {
			if (constructor.isAnnotationPresent(IOCConstructor.class)) {
				if (targetConstructor != null && targetConstructor.isAnnotationPresent(IOCConstructor.class)) {
					// there are 2 constructors with the @IOCConstructor!
					throw new TooManyIOCConstructorsException(clazz);
				}
				targetConstructor = constructor;
			}
		}
		if (targetConstructor != null && targetConstructor.isAnnotationPresent(IOCConstructor.class)) {
			return targetConstructor;
		}
		throw new TooManyConstructorsException(clazz);
	}

	@Override
	public String toString() {
		return "Container [registrations=" + registrations.entrySet().stream().map(Entry::toString).collect(Collectors.joining(",\n")) + ",\n implemenations=" + implemenations.entrySet().stream().map(Entry::toString).collect(Collectors.joining(",\n")) + ",\n lifeCycle=" + lifeCycle + ",\n factoryWrapper=" + factoryWrapper + "]";
	}

	private final static class Registration<T> {

		private final Class<T> registeredClass;
		private final RegistrationType type;
		private final InstanceFactory<T> factory;

		private Registration(Class<T> registeredClass, RegistrationType type, InstanceFactory<T> factory) {
			this.registeredClass = registeredClass;
			this.type = type;
			this.factory = factory;
		}

		@Override
		public String toString() {
			return "Registration [registeredClass=" + registeredClass + ", type=" + type + "]";
		}
	}

	/**
	 * Destroys all instances that were created by this container.
	 */
	public void destroyInstances() {
		lifeCycle.destroyAllInstances();
		// TODO: Instances are still references in singeltion factories.
	}

	private final class LifeCycleService {

		private final Set<Object> instances = new LinkedHashSet<>();
		private final LifeCycleHook hook;

		public LifeCycleService(LifeCycleHook lifeCycleHook) {
			this.hook = lifeCycleHook;
		}

		void destroyAllInstances() {
			synchronized (instances) {
				List<Object> asList = new ArrayList<>(instances);
				// iterate in reversed order
				for (int i = asList.size() - 1; i >= 0; i--) {
					Object instance = asList.get(i);
					onInstanceDestroyed(instance);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private <T> void onInstanceCreated(T instance) {
			synchronized (instances) {
				instances.add(instance);
			}
			hook.onInstanceCreated(instance, (Class<T>) instance.getClass());
		}

		@SuppressWarnings("unchecked")
		private <T> void onInstanceDestroyed(T instance) {
			synchronized (instances) {
				instances.remove(instance);
			}
			hook.onInstanceDestroyed(instance, (Class<T>) instance.getClass());
		}
	}

	public final class TransientFactory<T> implements InstanceFactory<T> {

		private final Class<T> target;

		private TransientFactory(Class<T> target) {
			validateType(target);
			if (target.isInterface()) {
				throw new IllegalArgumentException("Class is an interface: " + target);
			}
			if (Modifier.isAbstract(target.getModifiers())) {
				throw new IllegalArgumentException("Class is abstract: " + target);
			}
			this.target = target;
		}

		@Override
		public T get() throws InstanceFactoryException {
			try {
				// TODO: Cache constructor
				Constructor<T> constructor = getConstructorForClass(target);
				Class<?>[] dependencies = constructor.getParameterTypes();
				Object[] initargs = new Object[dependencies.length];
				for (int i = 0; i < initargs.length; i++) {
					initargs[i] = resolve(dependencies[i]);
				}
				T instance = constructor.newInstance(initargs);
				lifeCycle.onInstanceCreated(instance);
				return instance;
			} catch (Exception cause) {
				throw new InstanceFactoryException("Unable to create instance for " + target, cause);
			}
		}
	}
}
