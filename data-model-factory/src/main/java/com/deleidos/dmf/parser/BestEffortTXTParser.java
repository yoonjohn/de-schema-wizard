package com.deleidos.dmf.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class BestEffortTXTParser extends AbstractAnalyticsParser {
	private final String COLON = ":";

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(MediaType.TEXT_PLAIN);
	}

	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

		JSONObject json = new JSONObject();

		String key = null;
		String line = null;

		while ((line = br.readLine()) != null) {
			if (line.contains(COLON)) {
				String[] keyValue = line.split(COLON, 2);
				if (keyValue.length == 2) {
					key = keyValue[0].trim();
					json.put(key, keyValue[1].trim());
				} else {
					if (key != null) {
						json.put(key, json.get(key) + System.lineSeparator() + line);
					}
				}
			} else {
				if (key != null) {
					json.put(key, json.get(key) + System.lineSeparator() + line);
				}
			}
		}

		return json;
	}

	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			return super.flattenedJsonToDefaultProfilerRecord(this.parseSingleRecordAsJson(inputStream, handler, metadata, context), context.getCharsRead());
		} catch (IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}


}
