package de.paulomart.ioc;

public interface ExceptionHandler {

	public static final ExceptionHandler RETURNING_NULL = new ExceptionHandler() {

		@Override
		public <T> T handleExceptionOnInstantiation(Class<T> clazz, Exception exception) throws Exception {
			return null;
		}
	};
	public static final ExceptionHandler THROWING = new ExceptionHandler() {

		@Override
		public <T> T handleExceptionOnInstantiation(Class<T> clazz, Exception exception) throws Exception {
			throw exception;
		}
	};

	<T> T handleExceptionOnInstantiation(Class<T> clazz, Exception exception) throws Exception;
}
