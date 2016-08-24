package com.deleidos.dp.beans;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Test;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.MainTypeException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class MetricsBeansTest {
	Logger logger = Logger.getLogger(MetricsBeansTest.class);
	
	@Test
	public void numberDetailToJsonTest() throws JsonProcessingException, MainTypeException {
		NumberProfileAccumulator nma = AbstractProfileAccumulator.generateNumberProfileAccumulator("test-field");
		nma.accumulate(10, true);
		nma.accumulate(11,true);
		nma.accumulate(12,true);
		nma.finish();
		NumberDetail nd = Profile.getNumberDetail(nma.getState());
		String result = SerializationUtility.serialize(nd);
		logger.debug(result);
		assertTrue(result != null);
		logger.info("Number detail serialized.");
	}
	
	@Test
	public void jsonToNumberDetailTest() throws JsonParseException, JsonMappingException, IOException {
		JSONObject numberDetailJson = new JSONObject("{\r\n" + 
				"				\"average\": 28.977050951865873,\r\n" + 
				"				\"freq-histogram\": {\r\n" + 
				"					\"@class\": \"com.deleidos.dp.histogram.NumberBucketList\",\r\n" + 
				"					\"data\": [21, 20, 97, 107, 63, 23, 21, 35, 18, 10, 9, 6, 18, 8, 8, 5, 5, 0, 3, 1, 1, 2, 2, 4, 1, 5, 1, 0, 1, 26, 30, 20, 13, 49, 36, 75, 326, 519, 593, 596, 400, 142, 77, 108, 70, 45, 41, 9, 15, 5, 5, 3, 0],\r\n" + 
				"					\"series\": \"Values\",\r\n" + 
				"					\"type\": \"bar\",\r\n" + 
				"					\"yaxis\": \"Frequency\",\r\n" + 
				"					\"labels\": [\"[-41.8831741808,-39.8446960664)\", \"[-39.8446960664,-37.8062179520)\", \"[-37.8062179520,-35.7677398376)\", \"[-35.7677398376,-33.7292617232)\", \"[-33.7292617232,-31.6907836088)\", \"[-31.6907836088,-29.6523054944)\", \"[-29.6523054944,-27.6138273800)\", \"[-27.6138273800,-25.5753492656)\", \"[-25.5753492656,-23.5368711512)\", \"[-23.5368711512,-21.4983930368)\", \"[-21.4983930368,-19.4599149224)\", \"[-19.4599149224,-17.4214368080)\", \"[-17.4214368080,-15.3829586936)\", \"[-15.3829586936,-13.3444805792)\", \"[-13.3444805792,-11.3060024648)\", \"[-11.3060024648,-9.2675243504)\", \"[-9.2675243504,-7.2290462360)\", \"[-7.2290462360,-5.1905681216)\", \"[-5.1905681216,-3.1520900072)\", \"[-3.1520900072,-1.1136118928)\", \"[-1.1136118928,0.9248662216)\", \"[0.9248662216,2.9633443360)\", \"[2.9633443360,5.0018224504)\", \"[5.0018224504,7.0403005648)\", \"[7.0403005648,9.0787786792)\", \"[9.0787786792,11.1172567936)\", \"[11.1172567936,13.1557349080)\", \"[13.1557349080,15.1942130224)\", \"[15.1942130224,17.2326911368)\", \"[17.2326911368,19.2711692512)\", \"[19.2711692512,21.3096473656)\", \"[21.3096473656,23.3481254800)\", \"[23.3481254800,25.3866035944)\", \"[25.3866035944,27.4250817088)\", \"[27.4250817088,29.4635598232)\", \"[29.4635598232,31.5020379376)\", \"[31.5020379376,33.5405160520)\", \"[33.5405160520,35.5789941664)\", \"[35.5789941664,37.6174722808)\", \"[37.6174722808,39.6559503952)\", \"[39.6559503952,41.6944285096)\", \"[41.6944285096,43.7329066240)\", \"[43.7329066240,45.7713847384)\", \"[45.7713847384,47.8098628528)\", \"[47.8098628528,49.8483409672)\", \"[49.8483409672,51.8868190816)\", \"[51.8868190816,53.9252971960)\", \"[53.9252971960,55.9637753104)\", \"[55.9637753104,58.0022534248)\", \"[58.0022534248,60.0407315392)\", \"[60.0407315392,62.0792096536)\", \"[62.0792096536,64.1176877680)\", \"[64.1176877680,66.1561658824)\"]\r\n" + 
				"				},\r\n" + 
				"				\"@class\": \"com.deleidos.dp.beans.NumberDetail\",\r\n" + 
				"				\"min\": -41.39906,\r\n" + 
				"				\"max\": 63.55,\r\n" + 
				"				\"num-distinct-values\": 1307,\r\n" + 
				"				\"detail-type\": \"decimal\",\r\n" + 
				"				\"std-dev\": 24.003336043672036\r\n" + 
				"			}");
		NumberDetail result = SerializationUtility.deserialize(numberDetailJson, NumberDetail.class);
		assertTrue(result != null);
		logger.info("Number metrics deserialized.");
	}
}
