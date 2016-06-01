package com.deleidos.dmf.detector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

/**
 * Detector for Diplomatic Cables.
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class DiplomaticCableDetector extends AbstractMarkSupportedAnalyticsDetector {
	private static final Logger logger = Logger.getLogger(DiplomaticCableDetector.class);
	protected InputStreamReader isReader;
	public static final MediaType CONTENT_TYPE = MediaType.text("diplomaticcable");
	// https://en.wikipedia.org/wiki/Specials_(Unicode_block)#Replacement_character
	private static final Character REPLACEMENT_CHAR = '\uFFFD';
	private static final Character QUOTE = '"';

	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		if (inputStream.available() == 0) {
			return null;
		}

		isReader = new InputStreamReader(inputStream, "UTF-8");

		if (detect() != null) {
			return CONTENT_TYPE;
		} else {
			return null;
		}
	}

	@Override
	public boolean closeOnBinaryDetection(InputStream inputStream) throws IOException {
		boolean binary = testIsBinary(inputStream, 2000, .30f);
		if (binary) {
			return true;
		} else {
			return false;
		}
	}

	// Private Methods
	private String detect() {
		BufferedReader bReader = new BufferedReader(isReader);
		StringBuffer fileContent = new StringBuffer();
		String line;
		String record;

		try {
			// Read all lines into fileContent
			while ((line = bReader.readLine()) != null) {
				fileContent.append(line);
			}
			record = fileContent.toString();

			// Removes invalid quotes and replaces them with standard UTF-8 quotes
			record = swapInvalidUTF8Chars(record, QUOTE);
			// If the record is null, return null
			if (record == null) {
				return null;
			}

			String[] tokens = record.split("(?<=\")\\/\\/");
			if (tokens == null) {
				return null;
			}

			// If the tokens array is not the correct length, return null
			if (tokens.length != 8) {
				return null;
			}

			// Checks to see if the termination line exists (####)
			String properTerminationCheck = record;
			properTerminationCheck = properTerminationCheck.substring(properTerminationCheck.length() - 4,
					properTerminationCheck.length());

			if (!(properTerminationCheck.equals("####"))) {
				return null;
			}

			return "diplomaticcable";

		} catch (IOException e) {
			logger.error(e);
			return null;
		}
	}

	private String swapInvalidUTF8Chars(String record, Character newChar) {
		return record.replace(REPLACEMENT_CHAR, newChar);
	}
}
