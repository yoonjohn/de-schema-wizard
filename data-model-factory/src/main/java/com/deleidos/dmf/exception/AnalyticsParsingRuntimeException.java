package com.deleidos.dmf.exception;

import org.apache.tika.parser.Parser;

/**
 * Runtime exception for errors during analytics parsing.  Meant to indicate a programming error 
 * in a particular parser.
 * @author leegc
 *
 */
public class AnalyticsParsingRuntimeException extends AnalyticsRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4229316183698227813L;
	private Parser parser;
	private boolean shouldRetry;

	public AnalyticsParsingRuntimeException(String string, Parser parser) {
		super(string);
		this.setParser(parser);
	}

	public AnalyticsParsingRuntimeException(String message, Throwable cause, Parser parser) {
		super(message, cause);
		this.setParser(parser);
	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public boolean isShouldRetry() {
		return shouldRetry;
	}

	public void setShouldRetry(boolean shouldRetry) {
		this.shouldRetry = shouldRetry;
	}

}
