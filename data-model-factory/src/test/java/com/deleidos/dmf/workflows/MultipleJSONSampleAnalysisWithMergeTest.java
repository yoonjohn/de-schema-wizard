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
import com.deleidos.dmf.workflows.TwoJsonSamplesMergedCoordinatesWorkflow;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.interpretation.builtin.BuiltinLatitudeInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinLongitudeInterpretation;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public class MultipleJSONSampleAnalysisWithMergeTest extends DMFMockUpEnvironmentTest {
	private static Logger logger = Logger.getLogger(MultipleJSONSampleAnalysisWithMergeTest.class);
	private static AbstractAnalyzerTestWorkflow aat;

	@BeforeClass
	public static void setup() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new TwoJsonSamplesMergedCoordinatesWorkflow());
		aat.setOutput(true);
		aat.runAnalysis();
	}

	@Test
	public void testPresenceInSample() {
		assertTrue(aat.getRetrieveSourceAnalysisResult().getJSONObject(0).getJSONObject("dsProfile").getJSONObject("lat").has("presence"));
		float presence = (float)aat.getRetrieveSourceAnalysisResult().getJSONObject(0).getJSONObject("dsProfile").getJSONObject("lat").getDouble("presence");
		logger.info("Presence field in sample: " + presence);
		assertTrue(presence > 0);
	}

	@Test
	public void testPresenceInSchema() {
		assertTrue(aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").has("presence"));
		float presence = (float)aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getDouble("presence");
		logger.info("Presence field in schema: " + presence);
		assertTrue(presence > 0);
	}

	@Test
	public void testRegionDataMergedSuccessfully() {
		JSONArray jArr = aat.getRetrieveSourceAnalysisResult();
		int expectedUnitedStatesTotal = 0;
		int unitedStatesCount = 0;
		RegionData regionData = SerializationUtility.deserialize(jArr.getJSONObject(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data"), RegionData.class);
		for(int j = 0 ; j < regionData.getRows().size(); j++) {
			if("United States".equals(regionData.getRows().get(j).getKey())) {
				unitedStatesCount = regionData.getRows().get(j).getValue();
			}
		}
		expectedUnitedStatesTotal += unitedStatesCount;

		unitedStatesCount = 0;
		regionData = SerializationUtility.deserialize(jArr.getJSONObject(1).getJSONObject("dsProfile").getJSONObject("lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data"), RegionData.class);
		for(int j = 0 ; j < regionData.getRows().size(); j++) {
			if("United States".equals(regionData.getRows().get(j).getKey())) {
				unitedStatesCount = regionData.getRows().get(j).getValue();
			}
		}
		expectedUnitedStatesTotal += unitedStatesCount;


		JSONObject jObj = aat.getSchemaAnalysis();
		regionData = SerializationUtility.deserialize(jObj.getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data"), RegionData.class);
		for(int j = 0 ; j < regionData.getRows().size(); j++) {
			if("United States".equals(regionData.getRows().get(j).getKey())) {
				logger.info("Expected: " + expectedUnitedStatesTotal + ", got: " + regionData.getRows().get(j).getValue());
				assertTrue(expectedUnitedStatesTotal == regionData.getRows().get(j).getValue().intValue());
				logger.info("United States merged coordinates successfully.");
			}
		}
	}

	@Test
	public void testRegionDataInSchema() {
		JSONObject jObj = aat.getSchemaAnalysis();
		assertTrue(!jObj.getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
		assertTrue(!jObj.getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
		assertTrue(jObj.getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
		assertTrue(jObj.getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data").getJSONArray("rows").length() > 0);
		logger.info("Region data is in schema object.");
	}

	@Test
	public void testRegionDataInSamples() {
		JSONArray jArr = aat.getRetrieveSourceAnalysisResult();
		for(int i = 0 ; i < jArr.length(); i++) {
			assertTrue(!jArr.getJSONObject(i).getJSONObject("dsProfile").getJSONObject("lat").getJSONObject("detail").getJSONObject("freq-histogram").isNull("region-data"));
		}
		logger.info("Region data is in sample objects.");
	}

	@Test
	public void verifyAliasNamesInSchema() {
		boolean assertion = !aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").isNull("alias-names")
				&& !aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").isNull("alias-names");
		if(!assertion) {
			logger.error("Alias Name objects not present in schema.");
			logger.debug(aat.getSchemaAnalysis());
		} else {
			logger.info("Alias Name objects present in schema.");
		}
	}
	
	@Test
	public void verifyCorrectMergedMinAndMax() {
		double min = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getDouble("min");
		double max = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getDouble("max");
		assertTrue(Math.abs(min - -58.51385) < .1);
		assertTrue(Math.abs(max - 87.78863) < .1);
	}
	
	@Test
	public void verifyCorrectNumDistinctValues() {
		String numDistinctString = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getString("num-distinct-values");
		Integer num = MetricsCalculationsFacade.stripNumDistinctValuesChars(numDistinctString);
		assertTrue(num == 4552);
	}

	@Test
	public void verifySumOfWalkingCountsIsCorrect() {
		long count1 = aat.getSingleSourceAnalysis().get(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getLong("walkingCount");
		long count2 = aat.getSingleSourceAnalysis().get(1).getJSONObject("dsProfile").getJSONObject("lat").getJSONObject("detail").getLong("walkingCount");
		long combinedCount = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getLong("walkingCount");
		try {
			assertTrue(count1 > 0 && count2 > 0 && Math.abs((count1 + count2) - combinedCount) < 100);
		} catch (AssertionError e) {
			logger.error(count1 + " + " + count2 + " - " + combinedCount + " >= 100");
			throw e;
		}
		logger.info("\"lat\" and \"waypoints.lat\" successfully merged.");
		count1 = aat.getSingleSourceAnalysis().get(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("detail").getLong("walkingCount");
		count2 = aat.getSingleSourceAnalysis().get(1).getJSONObject("dsProfile").getJSONObject("lon").getJSONObject("detail").getLong("walkingCount");
		combinedCount = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("detail").getLong("walkingCount");
		try {
			assertTrue(count1 > 0 && count2 > 0 && Math.abs((count1 + count2) - combinedCount) < 100);		
		} catch (AssertionError e) {
			logger.error(count1 + " + " + count2 + " - " + combinedCount + " >= 100");
			throw e;
		}
		logger.info("\"lon\" and \"waypoints.lon\" successfully merged.");
	}

	@Test
	public void verifyInterpretationsAreInSchema() {
		String shouldBeLat = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("interpretation").getString("interpretation");
		assertTrue(shouldBeLat.equals(new BuiltinLatitudeInterpretation().getInterpretationName()));
		String shouldBeLon = aat.getSchemaAnalysis().getJSONObject("sProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lon").getJSONObject("interpretation").getString("interpretation");
		assertTrue(shouldBeLon.equals(new BuiltinLongitudeInterpretation().getInterpretationName()));
		logger.info("Interpretations present in schema analysis.");
	}
}
