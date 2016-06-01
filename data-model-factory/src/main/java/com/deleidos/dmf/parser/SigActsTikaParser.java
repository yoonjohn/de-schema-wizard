package com.deleidos.dmf.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.detector.SigActsDetector;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Parser for SigActs files.
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class SigActsTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(SigActsTikaParser.class);
	private BufferedReader reader;
	private static final String NAME_VALUE_DELIMITER = ":";
	private static final String FILE_CONTENT = "FILE_CONTENT";

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported encoding. " + e);
			reader = new BufferedReader(new StringReader(""));
		}
	}

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(SigActsDetector.CONTENT_TYPE);
	}
	
	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		return super.flattenedJsonToDefaultProfilerRecord(this.parseSingleRecordAsJson(inputStream, handler, metadata, context), context.getCharsRead());
	}

	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		JSONObject json = new JSONObject();
		parseRecord().forEach((x, v) -> json.put(x, v));
		if (json.keySet().size() == 0) {
			return null;
		} else {
			return json;
		}
	}

	// Private Methods
	private HashMap<String, String> parseRecord() {
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader bReader = new BufferedReader(reader);
		StringBuffer fileContent = new StringBuffer();
		String line;

		try {
			while ((line = bReader.readLine()) != null) {
				fileContent.append(line);
				String[] nvPair = line.split(NAME_VALUE_DELIMITER, 2);
				if (nvPair != null && nvPair.length == 2) {
					map.put(nvPair[0].trim(), nvPair[1].trim());
				}
			}
			if (map.isEmpty()) {
				return map;
			}
			map.put(FILE_CONTENT, fileContent.toString());
		} catch (IOException e) {
			logger.error(e);
		}

		return map;
	}
}
