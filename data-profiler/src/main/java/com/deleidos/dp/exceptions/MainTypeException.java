package com.deleidos.dp.exceptions;

/**
 * Exception that means the value could not be accumulated as the determined main type.  Should be handled with a warning.
 * @author leegc
 *
 */
public class MainTypeException extends Exception {

	public MainTypeException() {
		
	}
	
	public MainTypeException(String message) {
		super(message);
	}
	
}
