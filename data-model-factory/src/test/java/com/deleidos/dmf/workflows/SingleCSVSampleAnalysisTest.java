package com.deleidos.dmf.workflows;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.framework.DMFMockUpEnvironmentTest;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.exceptions.DataAccessException;

public class SingleCSVSampleAnalysisTest extends DMFMockUpEnvironmentTest {
	private static Logger logger = Logger.getLogger(SingleCSVSampleAnalysisTest.class);
	private static AbstractAnalyzerTestWorkflow aat;
	
	@BeforeClass
	public static void runAnalysisWorkflow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new SingleCSVSampleAnalysisWorkflow());
		aat.setOutput(true);
		aat.runAnalysis();
	}

	@Test
	public void testHasNumber() {
		JSONObject sample1 = aat.getSingleSourceAnalysis().get(0);
		assertTrue(sample1.getJSONObject("dsProfile").getJSONObject("Half").getString("main-type").equals("number"));
	}
	
	@Test
	public void testHasString() {
		JSONObject sample1 = aat.getSingleSourceAnalysis().get(0);
		assertTrue(sample1.getJSONObject("dsProfile").getJSONObject("teamID").getString("main-type").equals("string"));
	}
	
	@Test
	public void a() {
		
	}
	
	private static class SingleCSVSampleAnalysisWorkflow extends AbstractAnalyzerTestWorkflow {
		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/TeamsHalf.csv");
		}

		@Override
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {
			JSONObject sample1 = super.getSingleSourceAnalysis().get(0);
			assertTrue(sample1.getJSONObject("dsProfile").getJSONObject("Half").getString("main-type").equals("number"));
			return null;
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
}
