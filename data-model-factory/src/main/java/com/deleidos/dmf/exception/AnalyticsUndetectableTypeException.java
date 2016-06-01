package com.deleidos.dmf.exception;

import org.apache.tika.exception.TikaException;

/**
 * Checked exception indicating the type could not be detected from the source.
 * @author leegc
 *
 */
public class AnalyticsUndetectableTypeException extends TikaException {
	
	public AnalyticsUndetectableTypeException(String message) {
		super(message);
	}
	
	public AnalyticsUndetectableTypeException(String message, Exception e) {
		super(message, e);
	}
}
