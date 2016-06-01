package com.deleidos.dp.deserializors;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Profile;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DataSampleDeserializer extends JsonDeserializer<DataSample> {

	@Override
	public DataSample deserialize(JsonParser arg0, DeserializationContext arg1)
			throws IOException, JsonProcessingException {
		/*ObjectMapper objectMapper = new ObjectMapper();
		boolean failOnUnknown = false;
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown);
		DataSample sample = objectMapper.readValue(arg0, DataSample.class);
		for(String key : sample.getDsProfile().keySet()) {
			AbstractMetrics am = sample.getDsProfile().get(key).getMetrics();
			am.setFieldName(key);
			sample.getDsProfile().get(key).setMetrics(am);
		}*/
		DataSample sample = new DataSample();
		JsonNode rootNode = arg0.readValueAsTree();
		JsonNode profileMappingNode = rootNode.path("dsProfile");
		Iterator<String> profilefieldsIterator = profileMappingNode.fieldNames();
		Map<String, Profile> newProfiles = new HashMap<String, Profile>();
		while(profilefieldsIterator.hasNext()) {
			String nextKey = profilefieldsIterator.next();
			JsonNode profileNode = profileMappingNode.path(nextKey);
			Profile profile = SerializationUtility.deserialize(profileNode, Profile.class);
			newProfiles.put(nextKey, profile);
		}
		sample.setDsProfile(newProfiles);
		sample.setDataSampleId(rootNode.path("data-sample-id").asInt(0));
		sample.setDsDescription(rootNode.path("dsDescription").asText(null));
		sample.setDsFileName(rootNode.path("dsFileName").asText(null));
		sample.setDsFileType(rootNode.path("dsFileType").asText(null));
		sample.setDsGuid(rootNode.path("dsId").asText(null));
		sample.setDsLastUpdate(SerializationUtility.deserialize(rootNode.path("dsLastUpdate"), Timestamp.class));
		sample.setDsName(rootNode.path("dsName").asText(null));
		sample.setDsVersion(rootNode.path("dsVersion").asText(null));
		sample.setRecordsParsedCount(rootNode.path("recordsParseCount").asInt(0));
		sample.setDsExtractedContentDir(rootNode.path("dsExtractedContentDir").asText(null));
		return sample;
		
	}

}
