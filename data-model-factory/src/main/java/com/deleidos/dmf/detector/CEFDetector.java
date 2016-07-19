package com.deleidos.dmf.detector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.TextDetector;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.deleidos.dmf.framework.AbstractMarkSupportedAnalyticsDetector;

public class CEFDetector extends AbstractMarkSupportedAnalyticsDetector {
	public static final MediaType CONTENT_TYPE = MediaType.application("cef");
	
	@Override
	public Set<MediaType> getDetectableTypes() {
		return Collections.singleton(CONTENT_TYPE);
	}

	@Override
	public MediaType analyticsDetect(InputStream inputStream, Metadata metadata) throws IOException {
		MediaType type = null;
		//TemporaryResources tmp = new TemporaryResources();;

		//input.mark(Integer.MAX_VALUE);
		StringBuilder firstSevenPipes = new StringBuilder();
		StringBuilder rawKeyValuePairs = new StringBuilder();
		String[] headerSplit = new String[2];
		int pipeCount = 0;
		char lastChar = ' ';
		int n = 0;
		while((n = inputStream.read()) > -1) {
			char c = (char)n;
			if(lastChar != '\\' && c=='|') pipeCount++;
			else if(n == 10 || c == '\n' || c == '\r') break;
			firstSevenPipes.append(c);
			if(pipeCount == 7) {
				rawKeyValuePairs.append(c);
				break;
			}
			lastChar = c;
		}
		headerSplit[0] = firstSevenPipes.toString();
		while((n = inputStream.read()) > -1) {
			char c = (char)n;
			if(n == 10 || c == '\n' || c == '\r') break;
			rawKeyValuePairs.append(c);
			lastChar = c;
		}
		headerSplit[1] = rawKeyValuePairs.toString();

		if(pipeCount == 7) {
			return CONTENT_TYPE;
		} else {
			return null;
		}
	}

	@Override
	public boolean closeOnBinaryDetection(InputStream inputStream) throws IOException {
		boolean binary = testIsBinary(inputStream, 2000, .30f);
		if(binary) return true;
		return false;
	}
}
