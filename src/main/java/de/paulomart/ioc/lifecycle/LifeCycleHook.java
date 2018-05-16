package de.paulomart.ioc.lifecycle;

import java.util.logging.Level;
import java.util.logging.Logger;

public interface LifeCycleHook {

	public static final LifeCycleHook NOOP = new LifeCycleHook() {

		@Override
		public <T> void onInstanceCreated(T instance, Class<T> forClass) {
		}

		@Override
		public <T> void onInstanceDestroyed(T instance, Class<T> forClass) {
		}
	};
	public static final LifeCycleHook DEFAULT = new LifeCycleHook() {

		@Override
		public <T> void onInstanceCreated(T instance, Class<T> forClass) {
			if (instance instanceof Initializable) {
				try {
					((Initializable) instance).initialize();
				} catch (Throwable e) {
					Logger.getGlobal().log(Level.WARNING, "Error calling initialize for " + instance, e);
				}
			}
		}

		@Override
		public <T> void onInstanceDestroyed(T instance, Class<T> forClass) {
			if (instance instanceof Destroyable) {
				try {
					((Destroyable) instance).destroy();
				} catch (Throwable e) {
					Logger.getGlobal().log(Level.WARNING, "Error calling destroy for " + instance, e);
				}
			}
		}
	};

	<T> void onInstanceCreated(T instance, Class<T> forClass);

	<T> void onInstanceDestroyed(T instance, Class<T> forClass);
}
