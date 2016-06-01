package com.deleidos.dmf.parser;

import java.io.IOException;
import java.io.InputStream;
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
import org.json.XML;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsTikaProfilingException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.TikaProfilerParameters;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * Parser for known XML files
 * 
 * @author yoonj1
 *
 */
@SuppressWarnings("serial")
public class XMLTikaParser extends AbstractAnalyticsParser {
	private static final Logger logger = Logger.getLogger(XMLTikaParser.class);
	InputStream inputStream;

	@Override
	public void preParse(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		this.inputStream = inputStream;
	}

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return Collections.singleton(MediaType.application("xml"));
	}
	
	@Override
	public ProfilerRecord getNextProfilerRecord(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) throws AnalyticsTikaProfilingException {
		return super.flattenedJsonToDefaultProfilerRecord(this.parseSingleRecordAsJson(inputStream, handler, metadata, context), context.getCharsRead());
		//return mapped();
	}

	@Override
	public JSONObject parseSingleRecordAsJson(InputStream inputStream, ContentHandler handler, Metadata metadata, TikaProfilerParameters context) {
		JSONObject json = null;
		json = xmlParse();

		return json;
	}

	// Private Methods
	private JSONObject xmlParse() {
		JSONObject json = null;
		StringBuilder builder = new StringBuilder();
		int tempChar = 0;

		try {
			while ((tempChar = inputStream.read()) != -1) {
				builder.append((char) tempChar);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		String xml = builder.toString();
		
		if (!xml.isEmpty()) {
			json = XML.toJSONObject(xml);
			
			try {
				json = flatten(json.toString());
			} catch (JsonProcessingException e) {
				logger.error("Error flattening JSON.");
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				logger.error("Error reading stream");
				e.printStackTrace();
				return null;
			}
		}

		return json;
	}

	private JSONObject flatten(String content) throws JsonProcessingException, IOException {
		JSONObject json = new JSONObject();
		JsonNode jsonNode;
		Map<String, String> map = new HashMap<String, String>();

		jsonNode = new ObjectMapper().readTree(content);
		addKeys("", jsonNode, map);
		map.forEach((k, v) -> json.put(k, v));

		return json;
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
	private DefaultProfilerRecord addKeyWithHierarchialObjects(JsonNode jsonNode) {
		Map<String,Object> map = SerializationUtility.getObjectMapper().convertValue(jsonNode, Map.class);
		DefaultProfilerRecord profilerRecord = new DefaultProfilerRecord();
		profilerRecord.putAll(map);
		return profilerRecord;
	}
	
	private DefaultProfilerRecord mapped() throws IOException {
		String content;
		String tempLine;
		int tempCharIntRepresentation;
		StringBuffer contentBuilder = new StringBuffer();
		JsonNode jsonNode;

		// Read the lines of the stream and append them to
		// a buffer.
		while ((tempCharIntRepresentation = inputStream.read()) != -1) {
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
				

				if (!jsonNode.isNull()) {
					
						//addKeys("", jsonNode, map);
					DefaultProfilerRecord profilerRecord = addKeyWithHierarchialObjects(jsonNode);
					return profilerRecord;
					

				} else {
					return null;
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
}
