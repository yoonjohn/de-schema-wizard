package com.deleidos.dmf.exception;

public class AnalyticsInvalidSchemaException extends Exception {

	public AnalyticsInvalidSchemaException() {
		super();
	}
	
	public AnalyticsInvalidSchemaException(String message) {
		super(message);
	}
	
	public AnalyticsInvalidSchemaException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
