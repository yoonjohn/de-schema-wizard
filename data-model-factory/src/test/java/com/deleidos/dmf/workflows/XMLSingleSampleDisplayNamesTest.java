package com.deleidos.dmf.workflows;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;

public class XMLSingleSampleDisplayNamesTest extends DMFMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(XMLSingleSampleDisplayNamesTest.class);
	private static AbstractAnalyzerTestWorkflow aat;
	
	@BeforeClass
	public static void runAnalyzerWorkflow() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new XMLAsdiSampleWorkflow());
		aat.runAnalysis();
	}
	
	@Test
	public void assertUniqueDisplayNames() {
		DataSample sample = SerializationUtility.deserialize(aat.getSingleSourceAnalysis().get(0), DataSample.class);
		Map<String, List<String>> displayNames = new HashMap<String,List<String>>();
		for(String key : sample.getDsProfile().keySet()) {
			String displayName = sample.getDsProfile().get(key).getDisplayName();
			if(displayNames.containsKey(displayName)) {
				displayNames.get(displayName).add(key);
			} else {
				List<String> list = new ArrayList<String>();
				list.add(key);
				displayNames.put(displayName, list);
			}
		}
		boolean allGood = true;
		for(String s : displayNames.keySet()) {
			try {
				assertTrue(displayNames.get(s).size() == 1);
			} catch (AssertionError e) {
				logger.error("Duplicate display name: " + s + " with keys: ");
				displayNames.get(s).forEach(x->logger.error("\t" + x));
				allGood = false;
			}
		}
		assertTrue(allGood);
	}

	public static class XMLAsdiSampleWorkflow extends AbstractAnalyzerTestWorkflow {

		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/100_AsdiOutput.xml");
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
