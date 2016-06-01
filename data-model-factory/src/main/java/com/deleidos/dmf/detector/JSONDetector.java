package com.deleidos.dmf.detector;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

/**
 * Detector for JSON Objects and JSON Arrays
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class JSONDetector extends AbstractMarkSupportedAnalyticsDetector {
	private static final Logger logger = Logger.getLogger(JSONDetector.class);
	public static final MediaType CONTENT_TYPE = MediaType.application("json");
	public static final String WRAPPED_IN_ARRAY = null;
	protected InputStreamReader isReader;

	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		if (inputStream.available() == 0) { return null; }

		isReader = new InputStreamReader(inputStream, "UTF-8");

		if (readRecord(metadata) != null) { return CONTENT_TYPE; } 
		else 					{ return null; }
	}

	@Override
	public boolean closeOnBinaryDetection(InputStream inputStream) throws IOException {
		boolean binary = testIsBinary(inputStream, 2000, .30f);
		if (binary) { return true; }
		else { return false; }
	}

	// Private methods
	private String readRecord(Metadata metadata) {
		try {
			int tempChar;
			int numLeftCurly = 0, numRightCurly = 0;
			int numLeftSquare = 0, numRightSquare = 0;
			boolean exit = false;
			
			do {
				tempChar = isReader.read(); // read first character
			} while(isWhiteSpace(tempChar)); // loop through if whitespace
			
			switch (tempChar) { // test if first character is a JSON Object or JSON Array
			case -1: return null; // EOF
			case 44: return null; // Comma
			case 91: {
				numLeftSquare++; // Left Square Bracket
				metadata.set(WRAPPED_IN_ARRAY, Boolean.TRUE.toString());
				break; 
			}
			case 93: {
				return null; // Right Square Bracket
			}
			case 123: {
				numLeftCurly++; // Left Curly Bracket
				break;
			}
			case 125: {
				return null; // Right Curly Bracket
			}
			default:
				return null;
			}


			while (!exit) {
				tempChar = isReader.read();

				switch (tempChar) {
				case -1: return null; // EOF
				case 44: break; // Comma
				case 91: numLeftSquare++; // Left Square Bracket
				break; 
				case 93: numRightSquare++; // Right Square Bracket
				break; 
				case 123: numLeftCurly++; // Left Curly Bracket
				break;
				case 125: numRightCurly++; // Right Curly Bracket
				break;
				}


				if (numLeftSquare > 0) {
					// exit if the array and objects are all closed
					if (numLeftSquare == numRightSquare && numLeftCurly != 0 && numLeftCurly == numRightCurly) {
						exit = true;
					} 
				}
				// exit if at least one '{' was hit, and all '{' are closed by '}'
				else if (numLeftCurly != 0 && numLeftCurly == numRightCurly) {
					exit = true; 
				}
			}

			return "Json";
		} catch (UnsupportedEncodingException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}

		return null;
	}
}