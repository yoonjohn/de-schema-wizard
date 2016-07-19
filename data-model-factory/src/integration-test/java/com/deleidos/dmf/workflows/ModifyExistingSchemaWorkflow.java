package com.deleidos.dmf.workflows;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dp.beans.Schema;

public class ModifyExistingSchemaWorkflow {

	public static class ThreeSimpleCSVFilesMergedFieldsWorkflow extends AbstractAnalyzerTestWorkflow {
		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/sample1.csv");
			addResourceTestFile("/sample2.csv");
			addResourceTestFile("/sample3.csv");
		}

		@Override
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {
			return generatedSampleGuids;
		}

		@Override
		public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {
			JSONObject sample1 = retrieveSourceAnalysisResult.getJSONObject(0);
			Iterator<String> it = sample1.getJSONObject("dsProfile").keys();
			String key;
			while(it.hasNext()) {
				key = it.next();
				sample1.getJSONObject("dsProfile").getJSONObject(key).put("used-in-schema", true);
			}
			retrieveSourceAnalysisResult.put(0, sample1);
			for(int i = 1; i < retrieveSourceAnalysisResult.length(); i++) {
				JSONObject otherSample = retrieveSourceAnalysisResult.getJSONObject(i);
				Set<String> keys  = new HashSet<String>(otherSample.getJSONObject("dsProfile").keySet());
				it = keys.iterator();
				while(it.hasNext()) {
					key = it.next();
					otherSample.getJSONObject("dsProfile").getJSONObject(key).put("merged-into-schema", true);
				}
				retrieveSourceAnalysisResult.put(i, otherSample);
			}
			return retrieveSourceAnalysisResult;
		}

		@Override
		public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
			return schemaAnalysis;
		}

	}
	
	public static class OneSimpleCSVFileWorkflow extends AbstractAnalyzerTestWorkflow {
		
		public OneSimpleCSVFileWorkflow(String existingSchemaGuid) {
			this.setExistingSchemaGuid(existingSchemaGuid);
		}

		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/sample4.csv");
		}

		@Override
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {
			return generatedSampleGuids;
		}

		@Override
		public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {
			JSONObject sampleObject = retrieveSourceAnalysisResult.getJSONObject(0);
			String key;
			Iterator<String> it = existingSchema.getsProfile().keySet().iterator();
			while(it.hasNext()) {
				key = it.next();
				simulateMerge(sampleObject, key, key);
			}
			retrieveSourceAnalysisResult.put(0, sampleObject);
			return retrieveSourceAnalysisResult;
		}

		@Override
		public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
			return schemaAnalysis;
		}
		
	}
}