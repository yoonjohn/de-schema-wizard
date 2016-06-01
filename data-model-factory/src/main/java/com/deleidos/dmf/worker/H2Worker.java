package com.deleidos.dmf.worker;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.MatchingField;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.h2.H2DataAccessObject;

public class H2Worker {
	private static Logger logger = Logger.getLogger(H2Worker.class);
	
	public static String persistDataSample(DataSample dataSample) {
		return H2DataAccessObject.getInstance().addSample(dataSample);
	}
	
	public static String persistSchema(Schema schema) {
		return H2DataAccessObject.getInstance().addSchema(schema);
	}

	public static JSONArray performAnalysisOnSamples(List<DataSample> samples) {
		JSONArray analysisJson = null;
		try {
			samples = MetricsCalculationsFacade.matchFieldsAcrossSamples(samples);
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

	public static JSONArray analyzeMultipleSamples(String[] guids) {
		JSONArray analysisJson = null;

		List<DataSample> samples = H2DataAccessObject.getInstance().getSamplesByGuids(guids); // lat\long not set here!!1
		analysisJson = performAnalysisOnSamples(samples);

		return analysisJson;
	}

	public static JSONObject getSampleJSON(String guid) {
		JSONObject dsProfileJson;
		DataSample dataSample;
		try {
			dataSample = H2DataAccessObject.getInstance().getH2Samples().getSampleByGuid(guid);
			dsProfileJson = new JSONObject(SerializationUtility.serialize(dataSample));
			return dsProfileJson;
		} catch (JSONException e) {
			logger.error(e);
			logger.error("Error converting to JSON");
		} 
		return null;
	}

	public static String giveSchema(JSONObject schemaJson) {

		Schema schema = SerializationUtility.deserialize(schemaJson.toString(), Schema.class);
		H2DataAccessObject.getInstance().addSchema(schema);
		return schema.getsGuid();

	}

}
