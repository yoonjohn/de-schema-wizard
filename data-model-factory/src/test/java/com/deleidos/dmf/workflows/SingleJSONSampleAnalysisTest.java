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
import com.deleidos.dp.interpretation.builtin.BuiltinLatitudeInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinLongitudeInterpretation;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public class SingleJSONSampleAnalysisTest extends DMFMockUpEnvironmentTest {
	private static Logger logger = Logger.getLogger(SingleJSONSampleAnalysisTest.class);
	private static AbstractAnalyzerTestWorkflow aat;

	@BeforeClass
	public static void runAnalysisWorkflMockUpEnvironmentTestow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new SingleJSONSampleAnalysisWorkflow());
		aat.setOutput(true);
		aat.runAnalysis();
	}

	@Test
	public void testRegionDataInSamples() {
		JSONArray jArr = aat.getRetrieveSourceAnalysisResult();
		//logger.info(jArr);
		for(int i = 0 ; i < jArr.length(); i++) {
			assertTrue(!jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("lat").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
			assertTrue(!jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("lon").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
			assertTrue(!jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
			assertTrue(!jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
			assertTrue(jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
			assertTrue(jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("lon").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
			assertTrue(jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
			assertTrue(jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
		}
		logger.info("Region data is in sample objects.");
	}

	@Test
	public void testRegionDataInSchema() {
		JSONObject jObj = aat.getSchemaAnalysis();
		assertTrue(!jObj.getJSONObject("sProfile").getJSONObject("lat").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
		assertTrue(!jObj.getJSONObject("sProfile").getJSONObject("lon").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
		assertTrue(jObj.getJSONObject("sProfile").getJSONObject("lon").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
		assertTrue(jObj.getJSONObject("sProfile").getJSONObject("lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
		logger.info("Region data is in schema object.");
	}

	@Test
	public void testHasNumber() {
		JSONObject sample1 = aat.getSingleSourceAnalysis().get(0);
		assertTrue(sample1.getJSONObject("dsProfile").getJSONObject("alt").getString("main-type").equals("number"));
	}

	@Test
	public void testHasString() {
		JSONObject sample1 = aat.getSingleSourceAnalysis().get(0);
		assertTrue(sample1.getJSONObject("dsProfile").getJSONObject("ident").getString("main-type").equals("string"));
	}

	@Test
	public void verifyInterpretationsInSourceAnalysis() throws Exception {
		String shouldBeLat = aat.getRetrieveSourceAnalysisResult().getJSONObject(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("interpretation").getString("interpretation");
		String shouldBeLon = aat.getRetrieveSourceAnalysisResult().getJSONObject(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("interpretation").getString("interpretation");
		logger.info("Got " + shouldBeLat + " and " + shouldBeLon + ".");
		assertTrue(shouldBeLat.equals(new BuiltinLatitudeInterpretation().getInterpretationName()));
		assertTrue(shouldBeLon.equals(new BuiltinLongitudeInterpretation().getInterpretationName()));
		logger.info("Interpretations present in source analysis.");
	}

	private static class SingleJSONSampleAnalysisWorkflow extends AbstractAnalyzerTestWorkflow {
		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/FlightJson.txt");
		}

		@Override
		public String[] performMockVerificationStep(String[] generatedSampleGuids) {
			return generatedSampleGuids;
		}

		@Override
		public JSONArray performMockMergeSamplesStep(Schema existingSchema, JSONArray retrieveSourceAnalysisResult) {
			return retrieveSourceAnalysisResult;
		}

		@Override
		public JSONObject performMockSchemaInlineEdittingStep(JSONObject schemaAnalysis) {
			return schemaAnalysis;
		}

	}
}
