package com.deleidos.dmf.accessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.MatchingField;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.SchemaMetaData;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.h2.H2DataAccessObject;

/**
 * Service layer for the Schema Wizard. All calls from the Schema Wizard are
 * router through this class which makes calls to the database using Data Access
 * Objects.
 * 
 * @author yoonj1
 *
 */
public class ServiceLayerAccessor {
	public static final Logger logger = Logger.getLogger(ServiceLayerAccessor.class);
	H2DataAccessObject h2Dao = H2DataAccessObject.getInstance();

	/**
	 * Gets the catalog from the H2 Database.
	 * 
	 * @return A JSON Object conforming with the BNF file
	 */
	public JSONObject getCatalog() {
		JSONObject json = new JSONObject();

		try {
			List<SchemaMetaData> schemaList = h2Dao.getAllSchemaMetaData();
			List<DataSampleMetaData> sampleList = h2Dao.getAllSampleMetaData();

			json.put("schemaCatalog", new JSONArray(SerializationUtility.serialize(schemaList)));
			json.put("dataSamplesCatalog", new JSONArray(SerializationUtility.serialize(sampleList)));

			return json;
		} catch (JSONException e) {
			logger.error("Catalog was unable to be built into JSON.");
			logger.error(e);
		}
		return null;
	}

	/**
	 * Saves a given Schema to the database.
	 * 
	 * @param guid
	 */
	public void saveSchema(String guid) {
		h2Dao.deleteSchemaFromDeletionQueue(guid);
	}

	/**
	 * Gets Schema bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Schema bean as a JSON Object
	 */
	public JSONObject getSchemaByGuid(String guid, boolean showHistogram) {
		JSONObject json;
		Schema schema;

		schema = h2Dao.getSchemaByGuid(guid, showHistogram);

		String jsonString = SerializationUtility.serialize(schema);
		json = new JSONObject(jsonString);

		return json;

	}

	/**
	 * Gets SchemaMetaData bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return SchemaMetaData bean as a JSON Object
	 */
	public JSONObject getSchemaMetaDataByGuid(String guid) {
		JSONObject json;
		SchemaMetaData schemaMetaData;

		schemaMetaData = h2Dao.getSchemaMetaDataByGuid(guid);
		String jsonString = SerializationUtility.serialize(schemaMetaData);
		json = new JSONObject(jsonString);

		return json;
	}

	/**
	 * Gets Data Sample bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Data Sample bean as a JSON object
	 */
	public JSONObject getSampleByGuid(String guid) {
		JSONObject json;
		DataSample sample;

		sample = h2Dao.getSampleByGuid(guid);
		String jsonString = SerializationUtility.serialize(sample);
		json = new JSONObject(jsonString);

		return json;
	}

	/**
	 * Gets Data Sample Meta Data bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Data Sample Meta Data bean as a JSON object
	 */
	public JSONObject getSampleMetaDataByGuid(String guid) {
		JSONObject json;
		DataSampleMetaData sampleMetaData;

		sampleMetaData = h2Dao.getSampleMetaDataByGuid(guid);
		String jsonString = SerializationUtility.serialize(sampleMetaData);
		json = new JSONObject(jsonString);

		return json;
	}

	/**
	 * Gets the field of a given Schema.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public JSONObject getSchemaFieldByGuid(String guid) {
		JSONObject json;
		Map<String, Profile> map;

		map = h2Dao.getSchemaFieldByGuid(guid, true);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		return json;
	}

	/**
	 * Gets the field meta data of a given Schema.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public JSONObject getSchemaMetaDataFieldByGuid(String guid) {
		JSONObject json;
		Map<String, Profile> map;

		map = h2Dao.getSchemaFieldByGuid(guid, false);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		return json;
	}

	/**
	 * Gets the field of a given Sample.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public JSONObject getSampleFieldByGuid(String guid) {
		JSONObject json;
		Map<String, Profile> map;

		map = h2Dao.getSampleFieldByGuid(guid, true);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);
		
		return json;
	}

	/**
	 * Gets the field meta data of a given Sample.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public JSONObject getSampleMetaDataFieldByGuid(String guid) {
		JSONObject json;
		Map<String, Profile> map;

		map = h2Dao.getSampleFieldByGuid(guid, false);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		return json;
	}

	/**
	 * Deletes a Schema by its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Schema
	 */
	public void deleteSchemaByGuid(String guid) {
		h2Dao.deleteSchemaByGuid(guid);
	}

	/**
	 * Deletes a Data Sample by its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Data Sample
	 */
	public void deleteSampleByGuid(String guid) {
		h2Dao.deleteSampleByGuid(guid);
	}

	/**
	 * Takes an ambiguous GUID and determinatively deletes it from the database.
	 * 
	 * @param guid
	 *            A GUID from either a Schema or Data Sample
	 */
	public void deleteByGuid(String guid) {
		h2Dao.deleteByGuid(guid);
	}
	
	public JSONObject test() {
		JSONObject json = new JSONObject();
		json.put("test", "key");
		
		return json;
	}
	
	// methods that could be migrated over from H2Worker
	private String persistDataSample(DataSample dataSample) {
		return h2Dao.addSample(dataSample);
	}
	
	private String persistSchema(Schema schema) {
		return h2Dao.addSchema(schema);
	}

	private JSONArray performAnalysisOnSamples(List<DataSample> samples) {
		JSONArray analysisJson = null;
		try {
			List<String> usedFieldNames = new ArrayList<String>();
			for(DataSample sample : samples) {
				for(String key : sample.getDsProfile().keySet()) {
					if(!usedFieldNames.contains(key)) {
						sample.getDsProfile().get(key).setUsedInSchema(true);
						usedFieldNames.add(key);
					} else {
						continue; // seed value is already defined, skip analysis of this key
					}
					for(DataSample otherSample : samples) {
						if(sample.equals(otherSample)) {
							continue; // skip same sample
						} else {
							for(String otherKey : otherSample.getDsProfile().keySet()) {
								String p1Name = key;
								Profile p1 = sample.getDsProfile().get(p1Name);
								String p2Name = otherKey;
								Profile p2 = otherSample.getDsProfile().get(otherKey);
								
								double similarity = MetricsCalculationsFacade.similarityAlgorithm2(p1Name, p1, p2Name, p2);
								if(similarity > .8) {
									logger.debug("Match detected between " + p1Name + " in " + sample.getDsFileName() + " and " + p2Name + " in " + otherSample.getDsFileName() + " with " + similarity + " confidence.");
									MatchingField altName = new MatchingField();
									List<MatchingField> altNames = p2.getMatchingFields();
									altName.setMatchingField(p1Name);
									altName.setConfidence((int)(similarity*100));
									altNames.add(altName);
									altNames.sort((MatchingField a1, MatchingField a2)->a2.getConfidence()-a1.getConfidence());
									p2.setMatchingFields(altNames);
								}

							}
						}
					}
				}
			}
			analysisJson = new JSONArray();
			for(DataSample sample : samples) {
				analysisJson.put(new JSONObject(SerializationUtility.serialize(sample)));
			}
		} catch(JSONException e) {
			logger.error(e);
			logger.error("Error serializing sample object.");
		} 
		return analysisJson;
	}

	private JSONArray analyzeMultipleSamples(String[] guids) {
		JSONArray analysisJson = null;

		List<DataSample> samples = h2Dao.getSamplesByGuids(guids); // lat\long not set here!!1
		analysisJson = performAnalysisOnSamples(samples);

		return analysisJson;
	}

	private String giveSchema(JSONObject schemaJson) {

		Schema schema = SerializationUtility.deserialize(schemaJson.toString(), Schema.class);
		h2Dao.addSchema(schema);
		return schema.getsGuid();

	}
}
