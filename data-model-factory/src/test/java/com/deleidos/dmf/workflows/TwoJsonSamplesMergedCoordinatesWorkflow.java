package com.deleidos.dmf.workflows;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public class TwoJsonSamplesMergedCoordinatesWorkflow extends AbstractAnalyzerTestWorkflow {
	private Logger logger = Logger.getLogger(TwoJsonSamplesMergedCoordinatesWorkflow.class);

	@Override
	public void addNecessaryTestFiles() {
		addResourceTestFile("/FlightJson.txt");
		addResourceTestFile("/FlightPositionJson.txt");
	}

	@Override
	public String[] performMockVerificationStep(String[] generatedSampleGuids) {
		return generatedSampleGuids;
	}

	@Override
	public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {
		JSONObject sample1 = retrieveSourceAnalysisResult.getJSONObject(1);
		simulateMerge(sample1, "lat", "waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat");
		simulateMerge(sample1, "lon", "waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon");
		retrieveSourceAnalysisResult.put(1, sample1);
		return retrieveSourceAnalysisResult;
	}

	@Override
	public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
		schemaAnalysis.put("sName", getClass().getName());
		JSONObject profile = schemaAnalysis.getJSONObject("sProfile");
		JSONObject newProfileEntry = new JSONObject();
		newProfileEntry.put("main-type", "number");
		newProfileEntry.put("presence", -1.0);
		JSONObject detail = new JSONObject();
		detail.put("detail-type", "integer");
		newProfileEntry.put("detail", detail);
		profile.put("new-field", newProfileEntry);
		schemaAnalysis.put("sProfile", profile);
		return schemaAnalysis;
	}


}
