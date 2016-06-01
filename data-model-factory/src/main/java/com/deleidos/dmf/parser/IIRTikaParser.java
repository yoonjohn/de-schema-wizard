package com.deleidos.dmf.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.detector.IIRDetector;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class IIRTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(IIRTikaParser.class);
	private static final String COLON = ":";

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(IIRDetector.CONTENT_TYPE);
	}
	
	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			return super.flattenedJsonToDefaultProfilerRecord(this.parseSingleRecordAsJson(inputStream, handler, metadata, context), context.getCharsRead());
		} catch(IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}

	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		
		JSONObject json = new JSONObject();

		String key = null;
		String line = null;
		String lastCleanBreak = null;
		
		while ((line = br.readLine()) != null) {
			int colonIndex = line.indexOf(COLON);
			if (colonIndex > 0 && colonIndex < 15) {
				String[] keyValue = line.split(COLON, 2);
				if (keyValue.length == 2) {
					key = keyValue[0].trim();
					json.put(key, keyValue[1].trim());
				} else {
					if (key != null && lastCleanBreak != null) {
						json.put(key, json.get(key) + System.lineSeparator() + line);
					}
				}
			} else {
				if (key != null) {
					json.put(key, json.get(key) + System.lineSeparator() + line);
				}
			}
		}
		
		if(json.keySet().size() == 0) {
			return null;
		} else {
			return json;
		}
	}
}


