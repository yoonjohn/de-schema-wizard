package com.deleidos.dmf.detector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

@SuppressWarnings("serial")
public class SPOTDetector extends AbstractMarkSupportedAnalyticsDetector {
	public static final Logger logger = Logger.getLogger(SPOTDetector.class);
	public static final MediaType CONTENT_TYPE = MediaType.application("spot");
	protected InputStreamReader isReader;

	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		if (inputStream.available() == 0) {
			return null;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

		if (detect(br)) {
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
	private boolean detect(BufferedReader br) {
		String line;
		boolean exitAllowed = false;

		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();

				if (!line.isEmpty() && line.startsWith("SPOT_")) {
					exitAllowed = true;
				}

				if (line.startsWith("SPOT REPORT") && exitAllowed) {
					return true;
				}
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return false;
	}
}
