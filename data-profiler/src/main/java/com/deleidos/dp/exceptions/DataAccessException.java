package com.deleidos.dp.exceptions;

public class DataAccessException extends Exception {
	
	public DataAccessException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	public DataAccessException(String message) {
		super(message);
	}
	
}
