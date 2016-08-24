package com.deleidos.dp.profiler;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.profiler.api.Profiler;

public class ModifyExistingSchemaSimluationIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(ModifyExistingSchemaSimluationIT.class);
	Schema firstSchema;
	Schema secondSchema;
	Profile profileAfterSecondSchema;
	NumberDetail numberDetailAAfterSecondSchema;
	static int numAs = 0;
	
	@Before
	public void runThroughTwoConsecutiveWorkflows() throws MainTypeException {
		List<Integer> rands = new ArrayList<Integer>(Arrays.asList(1,2,3));
		List<Integer> randsSeeds = new ArrayList<Integer>(Arrays.asList(100,100,100));
		List<DataSample> samples = new ArrayList<DataSample>();
		int numInFirstPass = 3;
		for(int i = 0; i < numInFirstPass; i++) {
			DataSample sample = samplePass(rands.get(i), randsSeeds.get(i));
			sample.setDsName("sample"+i);
			samples.add(sample);
		}
		firstSchema = schemaPass(null, samples, rands, randsSeeds);
		firstSchema.setsName("schema1");
		firstSchema.setsGuid("test-schema-guid-1");
		
		List<DataSample> secondSamples = new ArrayList<DataSample>();
		secondSamples.add(samplePass(4, 100));
		secondSchema = schemaPass(firstSchema, secondSamples, Arrays.asList(4), Arrays.asList(100));
		profileAfterSecondSchema = secondSchema.getsProfile().get("a");
		numberDetailAAfterSecondSchema = Profile.getNumberDetail(secondSchema.getsProfile().get("a"));
	}
	
	@Test
	public void testMergedValues() {
		NumberDetail d2 = new NumberDetail();
		d2.setWalkingCount(BigDecimal.valueOf(400));
		d2.setAverage(BigDecimal.valueOf(2.5));
		d2.setStdDev(1.118);
		boolean a1 = (d2.getWalkingCount().equals(numberDetailAAfterSecondSchema.getWalkingCount()));
		boolean a2 = (d2.getAverage().equals(numberDetailAAfterSecondSchema.getAverage()));
		boolean a3 = (Math.abs(d2.getStdDev() - numberDetailAAfterSecondSchema.getStdDev()) < .1);
		boolean a4 = profileAfterSecondSchema.getPresence() <= 1.0;
		logger.info("Walking count: " + a1);
		logger.info(d2.getWalkingCount() + " -> " + numberDetailAAfterSecondSchema.getWalkingCount());
		logger.info("Average: " + a2);
		logger.info(d2.getAverage() + " -> " + numberDetailAAfterSecondSchema.getAverage());
		logger.info("Std dev: " + a3);
		logger.info(d2.getStdDev() + " -> " + numberDetailAAfterSecondSchema.getStdDev());
		logger.info("Presence: " + profileAfterSecondSchema.getPresence());
		logger.info("Num A's " + numAs);
		assertTrue(a1 && a2 && a3 && a4 && numAs == 1200);
	}

	private Schema schemaPass(Schema existingSchema, List<DataSample> samples, List<Integer> numRecords, List<Integer> randomSeeds) throws MainTypeException {
		int i = 0;
		for(DataSample dataSample : samples) {
			for(String key : dataSample.getDsProfile().keySet()) {
				if(i== 0 && existingSchema == null) {
					dataSample.getDsProfile().get(key).setUsedInSchema(true);
				} else {
					dataSample.getDsProfile().get(key).setMergedInto(true);
				}
			}
		}
		SchemaProfiler schemaProfiler = new SchemaProfiler(existingSchema, samples);
		for(DataSample dataSample : samples) {
			schemaProfiler.setCurrentDataSampleGuid(dataSample.getDsGuid());
			loadSomeRecords(schemaProfiler, numRecords.get(i), randomSeeds.get(i));
			i++;
		}
		Schema schema = schemaProfiler.asBean();
		schema.setsGuid(UUID.randomUUID().toString());
		return schema;
	}

	private DataSample samplePass(int numRecords, int randomSeed) throws MainTypeException {
		SampleProfiler sampleProfiler = new SampleProfiler(Tolerance.STRICT);
		loadSomeRecords(sampleProfiler, numRecords, randomSeed);
		DataSample sample = sampleProfiler.asBean();
		sample.setDsGuid(UUID.randomUUID().toString());

		SampleSecondPassProfiler srgProfiler = new SampleSecondPassProfiler(sample);
		srgProfiler.setMinimumBatchSize(500);
		loadSomeRecords(srgProfiler, numRecords, randomSeed);
		return srgProfiler.asBean();
	}

	private void loadSomeRecords(Profiler profiler, int recordValue, int numRecords) {
		for(int i = 0; i < numRecords; i++) {
			DefaultProfilerRecord profilerRecord = new DefaultProfilerRecord();
			profilerRecord.put("a", recordValue);
			numAs++;
			profilerRecord.put("b", recordValue);

			profiler.load(profilerRecord);
		}
	}

}
