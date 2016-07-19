package com.deleidos.dmf.exception;

/**
 * Checked exception indicating the type could not be detected from the source.
 * @author leegc
 *
 */
public class AnalyticsUndetectableTypeException extends AnalyzerException {
	
	public AnalyticsUndetectableTypeException(String message) {
		super(message);
	}
	
	public AnalyticsUndetectableTypeException(String message, Exception e) {
		super(message, e);
	}
}
