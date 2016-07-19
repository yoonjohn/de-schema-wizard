package com.deleidos.dmf.workflows;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

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
import com.deleidos.dp.h2.H2DataAccessObject;

public class AddSampleNameExtensionTest extends DMFMockUpEnvironmentTest {
	private static final AbstractAnalyzerTestWorkflow aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new AddSampleNameExtensionWorkflow()); 

	@BeforeClass
	public static void runAnalyzerWorkflow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat.setOutput(false);
		aat.runAnalysis();
	}

	@Test
	public void testNameIncremented() throws DataAccessException {
		Map<String,String> existing = H2DataAccessObject.getInstance().getExistingSampleNames();
		assertTrue(existing.containsKey("TeamsHalf"));
		assertTrue(existing.containsKey("TeamsHalf(1)"));
	}

	private static class AddSampleNameExtensionWorkflow extends AbstractAnalyzerTestWorkflow {
		
		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/TeamsHalf.csv");
			addResourceTestFile("/TeamsHalf.csv");
		}
		@Override
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {
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
