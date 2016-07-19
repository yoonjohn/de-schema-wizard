package com.deleidos.dmf.exception;

import org.apache.tika.exception.TikaException;

/**
 * Checked exception indicating that the type was successfully determined, but there is not an associated 
 * parser to handle this particular source. 
 * @author leegc
 *
 */
public class AnalyticsUnsupportedParserException extends AnalyzerException {
	private String type;

	public AnalyticsUnsupportedParserException(String message) {
		super(message);
	}
	
	public AnalyticsUnsupportedParserException(String message, Exception e) {
		super(message, e);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
