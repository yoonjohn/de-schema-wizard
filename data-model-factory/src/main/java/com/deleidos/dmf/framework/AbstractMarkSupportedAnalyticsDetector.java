package com.deleidos.dmf.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

/**
 * Abstract class to add extensions to the Tika Detectors.  <b> You must add the fully qualified class name to this 
 * project's src/main/resources/META-INF/services/org.apache.tika.detect.Detector file. </b>
 * @author leegc
 *
 */
public abstract class AbstractMarkSupportedAnalyticsDetector extends AnalyticsDetectorWrapper {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7041364422040404117L;
	private static final Logger logger = Logger.getLogger(AbstractMarkSupportedAnalyticsDetector.class);
	protected int validStartMark = 0;
	protected int validEndMark = 0;
	
	public AbstractMarkSupportedAnalyticsDetector() {
		setHasBodyPlainTextContent(false);
	}

	@Override
	public MediaType detect(InputStream input, Metadata metadata)
			throws IOException {
		TemporaryResources tmp = new TemporaryResources();
		TikaInputStream tikaInputStream = TikaInputStream.get(input, tmp);
		MediaType type = null;
		try {
			if(closeOnBinaryDetection(tikaInputStream)) return null;
			tikaInputStream.mark(Integer.MAX_VALUE);
			type = analyticsDetect(tikaInputStream, metadata);
			if(type != null && Float.floatToRawIntBits(confidenceInterval) == 0) {
				confidenceInterval = .99f; //as of 1/19, default confidence is 99%
			}
			tikaInputStream.reset();
		} finally {
			tmp.close();
		}
		return type;
	}

	/**
	 * Get the marks that represent a range of data in the file that matches the given detector type.
	 * As of 1/19, waiting for possible future implementation
	 * @return int[0] start mark, int[1] end mark
	 */
	public int[] getValidBoundaries() {
		return new int[] {validStartMark, validEndMark};
	}

	/**
	 * Implement this method to set the mark that starts valid formatting within the file.
	 */
	public void setValidStartMark(int startMark) {
		validStartMark = startMark;
	}

	/**
	 * Implement this method to set the mark that ends valid formatting within this file.
	 */
	public void setValidEndMark(int endMark) {
		validEndMark = endMark;
	}

	/**
	 * Main override method for any new detectors.  Call getTikaInputStream() within this method to obtain the working input stream.
	 * This is a protective layer to simplify the Apache Tika framework.
	 * @param metadata Any metadata you want to include or update with the class
	 * @return The media type that you have detected, or null if it is not that type.
	 * @throws IOException Throws an IOException if the stream is unreadable.
	 */
	public abstract MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException;

	/**
	 * A method to quickly close the stream if it is detected as binary.  Because some binary files can contain a certain percentage
	 * of printable characters, each subclass must define the appropriate number of characters to read and percentage of printable 
	 * characters.  To do this, call the testIsBinary() method.
	 * 
	 * This is an example implementation of the method that will test 2000 characters.  If less than 30% of them are
	 * printable, the file will be considered binary.  The input type will then be returned as null, which is appropriate
	 * behavior.<br><br>
	 * Example implementation: <br>
	 * <pre>
public boolean closeOnBinaryDetection() throws IOException {
	boolean binary = testIsBinary(getTikaInputStream(), 2000, .30f);
	if(binary) return true;
	return false;
}</pre>
	 *
	 * @return True if the file being detected is binary and should be closed immediately, 
	 * false if the stream should continue to be read and the analyticsDetect(InputStream inputStream, Metadata metadata) method should be called.
	 * @throws IOException If the stream cannot be read
	 * 
	 * 
	 */
	public abstract boolean closeOnBinaryDetection(InputStream inputStream) throws IOException;

	/**
	 * Test if the file is binary.  This method will mark the stream and test the file based on the parameters.  
	 * Printable characters are characters within the ASCII range (32, 127).
	 * @param inputStream The stream to be tested
	 * @param charactersToTest The number of characters to test
	 * @param acceptablePrintableCharacterPercentage
	 * @return
	 * @throws IOException
	 */
	protected static boolean testIsBinary(InputStream inputStream, int charactersToTest,
			float acceptablePrintableCharacterPercentage) throws IOException {
		if(inputStream.available() > charactersToTest) {
			inputStream.mark(charactersToTest);
		} else {
			charactersToTest = inputStream.available();
			inputStream.mark(charactersToTest);
		}
		int charCount = 0;
		int printableCharCount = 0;
		int nextInt = 0;

		for(int i = 0; i < charactersToTest; i++) {
			nextInt = inputStream.read();
			charCount++;
			if(isPrintableCharacter(nextInt)) {
				printableCharCount++;
			}
		}
		inputStream.reset();
		float printablePercentage = ((float)printableCharCount/(float)charCount);
		if(printablePercentage < acceptablePrintableCharacterPercentage) return true;
		else return false;
	}


	public static boolean isPrintableCharacter(int c) {
		if(c > 32 && c < 127) return true;
		return false;
	}

	public static boolean isPrintableCharacter(char c) {
		return isPrintableCharacter((int)c);
	}
	
	public boolean isWhiteSpace(int character) {
		if(character >= 9 && character <= 13) return true;
		else if(character == 32) return true;
		else return false;
	}
}
