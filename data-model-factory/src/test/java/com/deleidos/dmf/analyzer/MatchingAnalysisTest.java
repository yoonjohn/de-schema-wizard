package com.deleidos.dmf.analyzer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;

public class MatchingAnalysisTest {
	private Logger logger = Logger.getLogger(MatchingAnalysisTest.class);
	private String arrayResult;
	
	@Before
	public void getString() throws IOException {
		InputStream is = getClass().getResourceAsStream("/retrieveSampleAnalysisResult.json");
		StringBuilder sb = new StringBuilder();
		int n = 0;
		while((n = is.read()) > 0) {
			sb.append((char)n);
		}
		arrayResult = sb.toString();
	}
	
	@Test
	public void testSampleNameIncrement() throws SQLException, DataAccessException {
		Set<String> existing = new HashSet<String>();
		existing.add("sample");
		existing.add("sample(1)");
		existing.add("sample(2)");
		String s = DataSampleMetaData.generateNewSampleName("sample", existing);
		assertTrue("sample(3)".equals(s));
	}
	
	@Test
	public void testGiveMultipleSamples() throws Exception {
		JSONArray array = new JSONArray(arrayResult);
		List<DataSample> samples = new ArrayList<DataSample>();
		for(int i = 0 ; i < array.length(); i++) {
			DataSample sample = SerializationUtility.deserialize(array.get(i).toString(), DataSample.class);
			samples.add(sample);
		}
		JSONArray analysisArray = new JSONArray(SerializationUtility.serialize(MetricsCalculationsFacade.matchFieldsAcrossSamplesAndSchema(null, samples, null)));

		logger.debug("Retrieve source analysis from multiple source guids: " + analysisArray);
		
		List<DataSample> dsList = new ArrayList<DataSample>();
		List<String> allKeys = new ArrayList<String>();
		for(int i =0 ; i < analysisArray.length(); i++) {
			dsList.add(SerializationUtility.deserialize(analysisArray.getJSONObject(i).toString(), DataSample.class));
			allKeys.addAll(dsList.get(i).getDsProfile().keySet());
		}
		
		for(String key : allKeys) {
			int usedInSchemaFlagCount = 0;
			for(DataSample sample : dsList) {
				if(sample.getDsProfile().containsKey(key)) {
					if(sample.getDsProfile().get(key).isUsedInSchema()) {
						usedInSchemaFlagCount++;
					}
				}
			}
			if(usedInSchemaFlagCount == 0) {
				logger.error("Used in schema flag not set for " + key + " field.");
				assertTrue(false);
			} else if(usedInSchemaFlagCount > 1) {
				logger.error("Multiple used in schema flags set for " + key +" field.");
				assertTrue(false);
			}
		}
		logger.info("Used in schema flags successfully set for each field.");
	}

	
}
