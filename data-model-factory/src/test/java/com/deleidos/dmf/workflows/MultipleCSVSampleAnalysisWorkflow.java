package com.deleidos.dmf.workflows;

import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dp.beans.Schema;

public class MultipleCSVSampleAnalysisWorkflow extends AbstractAnalyzerTestWorkflow {

	@Override
	public void addNecessaryTestFiles() {
		addResourceTestFile("/TeamsHalf.csv");
		addResourceTestFile("/ManagersHalf.csv");
	}

	@Override
	public String[] performMockVerificationStep(String[] generatedSampleGuids) {
		return generatedSampleGuids;
	}

	@Override
	public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {
		return null;
	}

	@Override
	public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
		return null;
	}
}