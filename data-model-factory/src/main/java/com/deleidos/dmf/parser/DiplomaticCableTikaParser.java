package com.deleidos.dmf.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.detector.DiplomaticCableDetector;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Parser for Diplomatic Cables.
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class DiplomaticCableTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(DiplomaticCableTikaParser.class);
	private BufferedReader reader;
	private static final String MESSAGE_NUMBER = "MessageNumber";
	private static final String DATE_TIME = "DateTime";
	private static final String ID_1 = "Id1";
	private static final String EMBASSY = "Embassy";
	private static final String CLASSIFICATION = "Classification";
	private static final String ID_2 = "Id2";
	private static final String FROM_TO_BLOCK = "FromToBlock";
	private static final Character REPLACEMENT_CHAR = '\uFFFD';
	private static final String BODY = "Body";
	private static final Character QUOTE = '"';
	private static final String FILE_CONTENT = "FileContent";
	private SimpleDateFormat sdf;

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding. " + e);
			reader = new BufferedReader(new StringReader(""));
		}
	}

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(DiplomaticCableDetector.CONTENT_TYPE);
	}
	
	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		return super.flattenedJsonToDefaultProfilerRecord(this.parseSingleRecordAsJson(inputStream, handler, metadata, context), context.getCharsRead());
	}

	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		JSONObject json = new JSONObject();
		parseRecord().forEach((x,v)->json.put(x,v));
		if(json.keySet().size() == 0) {
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
		String record;

		try {
			while ((line = bReader.readLine()) != null) {
				fileContent.append(line);
			}

			record = fileContent.toString();
			
			if(record.isEmpty()) {
				return map;
			}

			record = swapInvalidUTF8Chars(record, QUOTE);
			if (record == null) {
				return null;
			}

			String[] tokens = record.split("(?<=\")\\/\\/");
			if (tokens.length == 1) {
				return null;
			}

			int index = 0;
			// Assuming the order is consistent here for all cables
			
			map.put(MESSAGE_NUMBER, removeEnclosingQuotes(tokens[index++]));
			map.put(DATE_TIME, removeEnclosingQuotes(tokens[index++]));
			map.put(ID_1, removeEnclosingQuotes(tokens[index++]));
			map.put(EMBASSY, removeEnclosingQuotes(tokens[index++]));
			map.put(CLASSIFICATION, removeEnclosingQuotes(tokens[index++]));
			map.put(ID_2, removeEnclosingQuotes(tokens[index++]));
			map.put(FROM_TO_BLOCK, removeEnclosingQuotes(tokens[index++]));
			map.put(BODY, removeEnclosingQuotes(tokens[index++]));
			map.put(FILE_CONTENT, record);
		} catch (IOException e) {
			logger.error(e);
		} 

		return map;
	}
	
	protected String parseDate(String date) throws ParseException {
		if (date == null)
			return null;

		sdf = new SimpleDateFormat();
		long time = sdf.parse(date).getTime();
		return String.valueOf(time);
	}

	private String swapInvalidUTF8Chars(String record, Character newChar) {
		return record.replace(REPLACEMENT_CHAR, newChar);
	}

	private String removeEnclosingQuotes(String value) {
		value = value.trim();
		if (value.startsWith(QUOTE.toString()) && value.endsWith(QUOTE.toString())) {
			return value.substring(1, value.length() - 1);
		} else {
			return value;
		}
	}
}
