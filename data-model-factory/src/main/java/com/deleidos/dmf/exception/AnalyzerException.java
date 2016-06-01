package com.deleidos.dmf.exception;

/**
 * Exception thrown if there is an error during an analysis. 
 * @author leegc
 *
 */
public abstract class AnalyzerException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2472727042827887584L;

	public AnalyzerException(String message) {
		super(message);
	}

	public AnalyzerException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public AnalyzerException(Throwable cause) {
		super(cause);
	}

}
