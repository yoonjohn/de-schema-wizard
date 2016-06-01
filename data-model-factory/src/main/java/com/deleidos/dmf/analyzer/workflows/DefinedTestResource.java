package com.deleidos.dmf.analyzer.workflows;

import java.io.InputStream;

/**
 * 'Defined resources' are testing files that have a desired outcome for all tests.  The intent of this class is to allow test classes to 
 * use and reuse files that have a desired detection type, parsing outcome, and metrics result. 
 * Create a defined resource by instantiating this class.
 * Instances of this class will be created and stored in TestLoader to be used in all possible tests.
 * @author leegc
 *
 */
public class DefinedTestResource {
	private boolean detectorTestReady;
	private boolean parserTestReady;
	private String filePath;
	private String expectedType;
	private String expectedBodyContentType;
	private InputStream stream;
	
	public DefinedTestResource(String filePath, String expectedType, String expectedBodyContentType, InputStream stream, boolean detectorTestReady, boolean parserTestReady) {
		this.detectorTestReady = detectorTestReady;
		this.parserTestReady = parserTestReady;
		this.filePath = filePath;
		this.expectedType = expectedType;
		this.expectedBodyContentType = expectedBodyContentType;
		this.stream = stream;
	}

	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getExpectedType() {
		return expectedType;
	}
	public void setExpectedType(String expectedType) {
		this.expectedType = expectedType;
	}
	public InputStream getStream() {
		return stream;
	}
	public void setStream(InputStream stream) {
		this.stream = stream;
	}

	public boolean isDetectorTestReady() {
		return detectorTestReady;
	}

	public void setDetectorTestReady(boolean detectorTestReady) {
		this.detectorTestReady = detectorTestReady;
	}

	public boolean isParserTestReady() {
		return parserTestReady;
	}

	public void setParserTestReady(boolean parserTestReady) {
		this.parserTestReady = parserTestReady;
	}

	public String getExpectedBodyContentType() {
		return expectedBodyContentType;
	}

	public void setExpectedBodyContentType(String expectedBodyContentType) {
		this.expectedBodyContentType = expectedBodyContentType;
	}
	
}
