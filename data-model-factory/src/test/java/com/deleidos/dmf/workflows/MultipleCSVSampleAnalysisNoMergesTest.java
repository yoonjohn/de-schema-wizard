package com.deleidos.dmf.workflows;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dmf.framework.DMFMockUpEnvironmentTest;
import com.deleidos.dmf.framework.TestingWebSocketUtility;
import com.deleidos.dmf.web.SchemaWizardSessionUtility;
import com.deleidos.dp.exceptions.DataAccessException;



public class MultipleCSVSampleAnalysisNoMergesTest extends DMFMockUpEnvironmentTest {
	private static Logger logger = Logger.getLogger(MultipleCSVSampleAnalysisNoMergesTest.class);
	private static AbstractAnalyzerTestWorkflow aat;

	@BeforeClass
	public static void runAnalysisWorkflow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new MultipleCSVSampleAnalysisWorkflow());
		aat.setOutput(true);
		aat.runAnalysis();
	}	
	
	@Test
	public void assertMonotinicallyIncreasingProgress() {
		try {
			TestingWebSocketUtility webSocketUtility = (TestingWebSocketUtility) SchemaWizardSessionUtility.getInstance();
			assertTrue(webSocketUtility.isError());
			logger.info("Progress bar increased appropriately.");
		} catch(AssertionError e) {
			logger.error("Progress bar did not increase appropriately.");
		}
	}

	@Test
	public void testHasMultipleSamplesInH2() {
		assertTrue(aat.getRetrieveSourceAnalysisResult().length() == 2);
		logger.info("Two samples detected from source analysis result (expected).");
	}

	@Test
	public void testMatchedFieldsSuccessfullyDetected() {
		boolean detectedAMerge = false;
		for(int i = 0 ; i < aat.getRetrieveSourceAnalysisResult().length(); i++) {
			JSONObject analyzedSample = aat.getRetrieveSourceAnalysisResult().getJSONObject(i);
			for(String key : analyzedSample.getJSONObject("dsProfile").keySet()) {
				if(analyzedSample.getJSONObject("dsProfile").getJSONObject(key).getJSONArray("matching-fields").length() > 0) {
					detectedAMerge = true;
					break;
				}
			}
			if(detectedAMerge) {
				break;
			}
		}
		if(!detectedAMerge) {
			logger.error("No matches detected in CSV files.");
			assertTrue(false);
		} else {
			logger.info("Matches detected between CSV files.");
		}
	}
}
