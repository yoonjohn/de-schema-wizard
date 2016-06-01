package com.deleidos.dmf.exception;

/**
 * Runtime exception showing that the necessary components of a Schema Wizard analysis were not initialized.
 * Usually indicates changes in framework code. 
 * @author leegc
 *
 */
public class AnalyticsInitializationRuntimeException extends AnalyticsRuntimeException {

	public AnalyticsInitializationRuntimeException(String string) {
		super(string);
	}

	public AnalyticsInitializationRuntimeException(String string, Exception e) {
		super(string, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3780042816724901334L;
	
}
