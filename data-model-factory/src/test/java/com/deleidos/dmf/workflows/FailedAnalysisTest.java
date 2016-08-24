package com.deleidos.dmf.workflows;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.framework.DMFMockUpEnvironmentTest;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.hd.h2.H2Database;

public class FailedAnalysisTest extends DMFMockUpEnvironmentTest {
	static AbstractAnalyzerTestWorkflow aat;
	static AbstractAnalyzerTestWorkflow aat2;
	static boolean wasUndetectableThrown = false;
	static boolean wasUnsupportedThrown = false;
	
	@BeforeClass
	public static void setup() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new UndetectableTypeWorkflow());
		try {
			aat.runAnalysis();
		} catch (AnalyticsUndetectableTypeException e) {
			wasUndetectableThrown = true;
		}
		aat2 = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new UnsupportedParserWorkflow());
		try {
			aat2.runAnalysis();
		} catch (AnalyticsUnsupportedParserException e) {
			wasUnsupportedThrown = true;
		}
	}
	
	@Test
	public void assertExceptionsThrown() {
		assertTrue(wasUndetectableThrown);
		assertTrue(wasUnsupportedThrown);
	}
	
	@Test
	public void doesH2ReturnExpectedErrorBeans() throws DataAccessException {
		DataSample sample = H2DataAccessObject.getInstance().getSampleByGuid(H2Database.UNDETECTABLE_SAMPLE_GUID);
		assertTrue(sample.getDsGuid().equals(H2Database.UNDETECTABLE_SAMPLE_GUID) && 
				sample.getDsDescription().equals(H2Database.getFailedAnalysisMapping().get(H2Database.UNDETECTABLE_SAMPLE_GUID)));
		DataSample sample2 = H2DataAccessObject.getInstance().getSampleByGuid(H2Database.UNSUPPORTED_PARSER_GUID);
		assertTrue(sample2.getDsGuid().equals(H2Database.UNSUPPORTED_PARSER_GUID) && 
				sample2.getDsDescription().equals(H2Database.getFailedAnalysisMapping().get(H2Database.UNSUPPORTED_PARSER_GUID)));
	}

	private static class UndetectableTypeWorkflow extends AbstractAnalyzerTestWorkflow {
		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/DS_Store");
		}
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {return null;}
		public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {return null;}
		public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {return null;}
	
	}
	private static class UnsupportedParserWorkflow extends AbstractAnalyzerTestWorkflow {
		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/parser-test-results.xlsx");
		}
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {return null;}
		public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {return null;}
		public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {return null;}
	
	}
}
