package com.deleidos.dmf.exception;

import java.io.IOException;

import org.apache.tika.exception.TikaException;

import com.deleidos.dmf.exception.AnalyzerException;

/**
 * Profiling exception specific to Tika Analysis calls.
 * @author leegc
 *
 */
public class AnalyticsTikaProfilingException extends AnalyzerException {
	
	public AnalyticsTikaProfilingException(String message) {
		super(message);
	}

	public AnalyticsTikaProfilingException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public AnalyticsTikaProfilingException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

}
