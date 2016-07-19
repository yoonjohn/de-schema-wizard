package com.deleidos.dmf.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.detector.JSONDetector;
import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * Parser for JSON Objects with a given InputStream. It is assumed that the
 * InputStream is giving JSON in a valid format. Returns a single JSON object.
 * 
 * @author yoonj1
 */
@SuppressWarnings("serial")
public class JSONTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(JSONTikaParser.class);
	private InputStreamReader reader;
	private boolean arrayWrapped;

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			if(metadata.get(JSONDetector.WRAPPED_IN_ARRAY) != null && metadata.get(JSONDetector.WRAPPED_IN_ARRAY).equals(Boolean.TRUE.toString())) {
				arrayWrapped = true;
			} else {
				arrayWrapped = false;
			}
			reader = new InputStreamReader(inputStream, "UTF-8");
			
			// bReader = new BufferedReader(reader);
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding. " + e);
		}
	}

	/**
	 * Returns the media type (application/json)
	 * 
	 * @param ParseContext
	 * @return
	 */
	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(MediaType.application("json"));
	}
	
	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		try {
			return mapped(context);
		} catch (IOException e) {
			throw new AnalyticsTikaProfilingException(e);
		}
	}
	
	private void readPassedNextComma(InputStreamReader isr) throws IOException {
		final int comma = ',';
		final int endArray = ']';
		int next = 0;
		do {
			next = isr.read();
			if(next == -1 || next == endArray) {
				break;
			}
		} while (next != comma);
	}

	/**
	 * Parses a single record from an InputStream by counting tags. Returns a
	 * single JSONObject.
	 * @param InputStream
	 * @param MetaData
	 * 
	 * @return
	 * @throws UnsupportedEncodingException
	
	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, ParseContext context) {
		JSONObject json = null;
		try {
			json = jsonFlattener();
		} catch (IOException e) {
			logger.error("Error reading stream.");
			e.printStackTrace();
		}

		return json;
	} */

	// Private methods
	/**
	 * Creates a JSON Object by reading from an input stream line by line and
	 * attempting to map it on each iteration. If the mapping fails, the method
	 * will loop until either an object is found or the end of the stream is
	 * reached.
	 * 
	 * @return Single flattened JSON record from an input stream
	 * @throws IOException
	 */
	private JSONObject jsonFlattener() throws IOException {
		JSONObject json = new JSONObject();
		Map<String, String> map = new HashMap<String, String>();
		String content;
		String tempLine;
		int tempCharIntRepresentation;
		StringBuffer contentBuilder = new StringBuffer();
		JsonNode jsonNode;

		// Read the lines of the stream and append them to
		// a buffer.
		while ((tempCharIntRepresentation = reader.read()) != -1) {
			tempLine = Character.toString((char) tempCharIntRepresentation);
			contentBuilder.append(tempLine);
			content = contentBuilder.toString();

			// Remove beginning array character
			// Jackson will not parse a partial array
			if (content.indexOf('[') == 0) {
				content = content.substring(1);
			}

			// Remove ending array character
			// Jackson will not parse a partial array
			if (content.indexOf(']') == content.length()) {
				content = content.substring(0, content.length() - 1);
			}

			// Try to perform an object mapping to the current String
			// that is in the buffer. If it fails, it will loop to create an
			// object.
			// If a JSON Object is not found by the end of stream, returns null
			try {
				if (tempCharIntRepresentation == 125) {
				jsonNode = new ObjectMapper().readTree(content);

				if (!jsonNode.toString().equals("null")) {
					
					addKeys("", jsonNode, map);
					
					if (!map.isEmpty()) {
						map.forEach((k, v) -> json.put(k, v));
						contentBuilder = new StringBuffer();
						return json;
					}
				}
			}
			} catch (JsonParseException e) {
				// do nothing
			} catch (JsonMappingException f) {
				// do nothing
			}
		}
		return null;
	}
	
	private DefaultProfilerRecord mapped(TikaProfilerParameters context) throws IOException {
		String content;
		String tempLine;
		int tempCharIntRepresentation;
		StringBuffer contentBuilder = new StringBuffer();
		JsonNode jsonNode;

		// Read the lines of the stream and append them to
		// a buffer.
		DefaultProfilerRecord profilerRecord = null;
		while ((tempCharIntRepresentation = reader.read()) != -1) {
			tempLine = Character.toString((char) tempCharIntRepresentation);
			contentBuilder.append(tempLine);
			content = contentBuilder.toString();

			// Remove beginning array character
			// Jackson will not parse a partial array
			if (content.indexOf('[') == 0) {
				content = content.substring(1);
			}

			// Remove ending array character
			// Jackson will not parse a partial array
			if (content.indexOf(']') == content.length()) {
				content = content.substring(0, content.length() - 1);
			}

			// Try to perform an object mapping to the current String
			// that is in the buffer. If it fails, it will loop to create an
			// object.
			// If a JSON Object is not found by the end of stream, returns null
			try {
				if (tempCharIntRepresentation == 125) {
					this.getParams().setCharsRead(getParams().getCharsRead()+content.length());
				jsonNode = new ObjectMapper().readTree(content);
				
				if (!jsonNode.isNull()) {
						//addKeys("", jsonNode, map);
					profilerRecord = addKeyWithHierarchicalObjects(jsonNode);
					profilerRecord.setRecordProgress(context.getCharsRead()+content.length());
					break;

				} else {
					break;
				}
			}
			} catch (JsonParseException e) {
				// do nothing
			} catch (JsonMappingException f) {
				// do nothing
			}
		}
		if(arrayWrapped) {
			readPassedNextComma(this.reader);
		}
		return profilerRecord;
	}

	/**
	 * Recursive function that flattens JSON Objects so that array indices are
	 * preserved.
	 * 
	 * Credited to: 'Harleen' http://stackoverflow.com/users/618562/harleen
	 * Answered at:
	 * http://stackoverflow.com/questions/20355261/how-to-deserialize-json-into-
	 * flat-map-like-structure
	 * 
	 * @param currentPath
	 * @param jsonNode
	 * @param map
	 */
	private void addKeys(String currentPath, JsonNode jsonNode, Map<String, String> map) {
		if (jsonNode.isObject()) {
			ObjectNode objectNode = (ObjectNode) jsonNode;
			Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
			String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
			}
		} else if (jsonNode.isArray()) {
			ArrayNode arrayNode = (ArrayNode) jsonNode;
			for (int i = 0; i < arrayNode.size(); i++) {
				addKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
			}
		} else if (jsonNode.isValueNode()) {
			ValueNode valueNode = (ValueNode) jsonNode;
			map.put(currentPath, valueNode.asText());
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	private DefaultProfilerRecord addKeyWithHierarchicalObjects(JsonNode jsonNode) {
		Map<String,Object> map = SerializationUtility.getObjectMapper().convertValue(jsonNode, Map.class);
		DefaultProfilerRecord profilerRecord = new DefaultProfilerRecord();
		profilerRecord.putAll(map);
		return profilerRecord;
	}
	
}
