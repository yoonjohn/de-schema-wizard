package com.deleidos.dp.h2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;

public class AccumulateHistogramIT extends DataProfilerIntegrationEnvironment {
	private static Logger logger = Logger.getLogger(AccumulateHistogramIT.class);

	@Test
	public void testHistogramSuccess() throws Exception {
		Random rand = new Random(10);
		List<Object> someLowPrecisionNumbers = new ArrayList<Object>();
		List<Object> someHighPrecisionNumbers = new ArrayList<Object>();
		for(int i = 0; i < 100; i++) {
			double num = (((int)(rand.nextFloat()*100)));
			double dec= (double)(((int)(rand.nextFloat()*10)))/(double)10;
			num += dec;
			someLowPrecisionNumbers.add(num);
		}
		logger.debug(someLowPrecisionNumbers);
		for(int i = 0; i < 100; i++) {
			double num = (((int)(rand.nextFloat()*10000)));
			double dec= (double)(((int)(rand.nextFloat()*100000)))/(double)100000;
			num += dec;
			someHighPrecisionNumbers.add(num);
		}
		logger.debug(someHighPrecisionNumbers);
		someLowPrecisionNumbers.addAll(someHighPrecisionNumbers);
		Profile profile = BundleProfileAccumulator.generateProfile("nums", someLowPrecisionNumbers);
		Map<String, Profile> profileMap = new HashMap<String, Profile>();
		profileMap.put("num", profile);
		DataSample dummySample = new DataSample();
		String guid = UUID.randomUUID().toString();
		dummySample.setDsGuid(guid);
		dummySample.setDsProfile(profileMap);
		dummySample.setDsFileName("/test");
		logger.debug(SerializationUtility.serialize(dummySample.getDsProfile().get("num").getDetail().getHistogramOptional().get()));
		H2DataAccessObject.getInstance().addSample(dummySample);
		DataSample sample = H2DataAccessObject.getInstance().getSampleByGuid(guid);
		logger.debug(SerializationUtility.serialize(sample.getDsProfile().get("num").getDetail().getHistogramOptional().get()));
		Profile schemaProfile = BundleProfileAccumulator.generateSecondPassProfile("num", someLowPrecisionNumbers, sample.getDsProfile().get("num"));
		logger.debug(SerializationUtility.serialize(schemaProfile.getDetail().getHistogramOptional().get()));
		Schema schema = new Schema();
		String schemaGuid = UUID.randomUUID().toString();
		schema.setsGuid(schemaGuid);
	}
}
