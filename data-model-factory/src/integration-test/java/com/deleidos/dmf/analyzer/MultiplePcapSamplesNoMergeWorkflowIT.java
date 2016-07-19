package com.deleidos.dmf.analyzer;

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
import com.deleidos.dmf.integration.DataModelFactoryIntegrationEnvironment;
import com.deleidos.dmf.parser.JNetPcapTikaParser;
import com.deleidos.dmf.workflows.TwoJsonSamplesMergedCoordinatesWorkflow;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;

public class MultiplePcapSamplesNoMergeWorkflowIT extends DataModelFactoryIntegrationEnvironment {
	private static Logger logger = Logger.getLogger(MultiplePcapSamplesNoMergeWorkflowIT.class);
	private static AbstractAnalyzerTestWorkflow aat;
	
	@BeforeClass
	public static void setup() throws AnalyticsUndetectableTypeException, AnalyticsUnsupportedParserException, IOException, AnalyzerException, DataAccessException {
		aat = AbstractAnalyzerTestWorkflow.addOrGetStaticWorkflow(new TwoPcapSamplesNoMerge());
		aat.setOutput(true);
		aat.runAnalysis();
	}
	
	@Test
	public void test() {
		DataSample ds = SerializationUtility.deserialize(aat.getSingleSourceAnalysis().get(0), DataSample.class);
		assertTrue(ds.getDsProfile().containsKey("wlan.fc.type"));
	}
	
	public static class TwoPcapSamplesNoMerge extends AbstractAnalyzerTestWorkflow {

		@Override
		public void addNecessaryTestFiles() {
			addResourceTestFile("/Network_Join_Nokia_Mobile.pcap");
			addResourceTestFile("/wpa-Induction.pcap");
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
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
