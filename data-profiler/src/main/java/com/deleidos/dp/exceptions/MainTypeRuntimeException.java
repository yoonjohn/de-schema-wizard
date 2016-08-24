package com.deleidos.dp.exceptions;

public class MainTypeRuntimeException extends RuntimeException {
	
	public MainTypeRuntimeException() {
		this("An expected attribute of a main type was not found.");
	}
	
	public MainTypeRuntimeException(String message) {
		super(message);
	}
	
	public MainTypeRuntimeException(String message, Throwable e) {
		super(message, e);
	}
}
