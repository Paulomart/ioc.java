package de.paulomart.ioc;

public interface ExceptionHandler {

	public static final ExceptionHandler RETURNING_NULL = new ExceptionHandler() {

		@Override
		public <T> T handleExceptionOnInstantiation(Class<T> clazz, Exception exception) throws Exception {
			return null;
		}

		@Override
		public <T> void handleExceptionOnDestruction(T instance, Class<T> clazz, Exception exception) {
		}
	};
	public static final ExceptionHandler THROWING = new ExceptionHandler() {

		@Override
		public <T> T handleExceptionOnInstantiation(Class<T> clazz, Exception exception) throws Exception {
			throw exception;
		}

		@Override
		public <T> void handleExceptionOnDestruction(T instance, Class<T> clazz, Exception exception) {
		}
	};

	<T> T handleExceptionOnInstantiation(Class<T> clazz, Exception exception) throws Exception;

	<T> void handleExceptionOnDestruction(T instance, Class<T> clazz, Exception exception);
}
