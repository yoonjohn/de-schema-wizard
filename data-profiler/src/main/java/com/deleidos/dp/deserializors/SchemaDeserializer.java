package com.deleidos.dp.deserializors;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class SchemaDeserializer extends JsonDeserializer<Schema> {

	@Override
	public Schema deserialize(JsonParser arg0, DeserializationContext arg1)
			throws IOException, JsonProcessingException {
		Schema schema = new Schema();

		JsonNode rootNode = arg0.readValueAsTree();
		JsonNode profileMappingNode = rootNode.path("sProfile");
		Iterator<String> profilefieldsIterator = profileMappingNode.fieldNames();
		Map<String, Profile> newProfiles = new HashMap<String, Profile>();
		while(profilefieldsIterator.hasNext()) {
			String nextKey = profilefieldsIterator.next();
			JsonNode profileNode = profileMappingNode.path(nextKey);
			Profile profile = SerializationUtility.deserialize(profileNode, Profile.class);
			newProfiles.put(nextKey, profile);
		}
		schema.setsProfile(newProfiles);

		schema.setSchemaModelId(rootNode.path("schema_model_id").asInt(0));
		schema.setsDescription(rootNode.path("sDescription").asText(null));
		schema.setsName(rootNode.path("sName").asText(null));
		schema.setsGuid(rootNode.path("sId").asText(null));
		schema.setsLastUpdate(SerializationUtility.deserialize(rootNode.path("sLastUpdate"), Timestamp.class));
		schema.setsName(rootNode.path("sName").asText(null));
		schema.setsVersion(rootNode.path("sVersion").asText(null));
		schema.setRecordsParsedCount(rootNode.path("recordsParseCount").asInt(0));

		List<DataSampleMetaData> dsms = new ArrayList<DataSampleMetaData>();
		JsonNode sDataSamples = rootNode.path("sDataSamples");

		for(int i = 0; i < sDataSamples.size(); i++) {
			dsms.add(SerializationUtility.deserialize(sDataSamples.get(i), DataSampleMetaData.class));
		}

		schema.setsDataSamples(dsms);

		return schema;
	}

}
