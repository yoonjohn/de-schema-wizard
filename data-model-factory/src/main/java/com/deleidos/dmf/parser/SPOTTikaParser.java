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

import com.deleidos.dmf.detector.SPOTDetector;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Parser for SPOT files.
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class SPOTTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(SPOTTikaParser.class);
	private BufferedReader br;

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		try {
			br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding. " + e);
			br = new BufferedReader(new StringReader(""));
		}
	}

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(SPOTDetector.CONTENT_TYPE);
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
		HashMap<String, String> map;
		String line;

		try {
			map = new HashMap<String, String>();

			while ((line = br.readLine()) != null) {
				int colonBreak = line.indexOf(':');

				if (colonBreak > 0) {
					map.put(line.substring(0, colonBreak), line.substring(colonBreak + 2));
				}
			}

			// map.forEach((k, z) -> System.out.println(k + " : " + z));

			return map;
		} catch (IOException e) {
			logger.error("Error reading file.");
			logger.error(e);
		}

		return new HashMap<String, String>();
	}

}
