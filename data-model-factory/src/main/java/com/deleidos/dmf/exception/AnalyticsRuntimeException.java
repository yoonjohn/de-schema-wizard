package com.deleidos.dmf.exception;

/**
 * Runtime exceptions specific to the analytics framework.
 * @author leegc
 *
 */
public class AnalyticsRuntimeException extends RuntimeException {
	/**
	 * 
	 */
	public static final long serialVersionUID = -3780042816724901334L;
	
	
	public AnalyticsRuntimeException(String string) {
		super(string);
	}

	public AnalyticsRuntimeException(String string, Throwable e) {
		super(string, e);
	}

	public AnalyticsRuntimeException(Throwable e) {
		super(e);
	}
}
