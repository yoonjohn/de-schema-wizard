package com.deleidos.dp.reversegeo;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SampleSecondPassProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class ReverseGeocodingCallsIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(ReverseGeocodingCallsIT.class);
	private static boolean geoDataInSamples;
	private static boolean geoDataInSchema;
	private static boolean geoDataMerged;
	private static boolean presenceMetricCorrect;

	@BeforeClass
	public static void makeSomeReverseGeoCallsTest() throws DataAccessException {
		int numRecords = 1001; // should be 2002 geocoding calls
		int numRecords2 = 550; // should be 1100 geocoding calls
		final int seed1 = 10; // DO NOT CHANGE
		final int seed2 = 40; // DO NOT CHANGE because some values do not successfully get interpretted as coordinates
		DataSample sampleWithGeo = samplePass(numRecords, seed1);

		int latCount = Profile.getNumberDetail(sampleWithGeo.getDsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().size();
		int waypointsLatCount = Profile.getNumberDetail(sampleWithGeo.getDsProfile().get("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat")).getHistogramOptional().get().getRegionData().getRows().size();
		boolean sampleGeoAssertion = latCount > 0 && waypointsLatCount > 0;

		DataSample sampleWithGeo2 = samplePass(numRecords2, seed2);
		
		int latCount2 = Profile.getNumberDetail(sampleWithGeo2.getDsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().size();
		int waypointsLatCount2 = Profile.getNumberDetail(sampleWithGeo2.getDsProfile().get("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat")).getHistogramOptional().get().getRegionData().getRows().size();
		boolean sampleGeoAssertion2 = latCount2 > 0 && waypointsLatCount2 > 0;
		
		geoDataInSamples = sampleGeoAssertion && sampleGeoAssertion2;

		List<DataSample> samplesWithMatches = MetricsCalculationsFacade.matchFieldsAcrossSamplesAndSchema(null, Arrays.asList(sampleWithGeo, sampleWithGeo2));

		Schema schemaBean = schemaPass(samplesWithMatches, Arrays.asList(numRecords, numRecords2),
				Arrays.asList(seed1, seed2));

		geoDataInSchema = Profile.getNumberDetail(schemaBean.getsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().size() > 0;
		
		geoDataMerged = isRegionDataCountInSamplesEqualToRegionDataCountInSchema(Arrays.asList(sampleWithGeo, sampleWithGeo2), schemaBean);
		
		presenceMetricCorrect = schemaBean.getsProfile().get("lat").getPresence() == 1.0f;
		logger.info("Presence: " + schemaBean.getsProfile().get("lat").getPresence());
	}
	
	@Test
	public void geoDataInSamples() {
		if(geoDataInSamples) {
			logger.info("Geo-data is in sample beans.");
		} else {
			logger.error("Geo-data is not in sample beans.");
			assertTrue(false);
		}
	}
	
	@Test
	public void geoDataInSchema() {
		if(geoDataInSchema) {
			logger.info("Geo-data is in schema bean.");
		} else {
			logger.error("Geo-data is not in schema bean.");
			assertTrue(false);
		}
	}
	
	@Test
	public void geoDataMerged() {
		if(geoDataMerged) {
			logger.info("Geo-data merged successfully.");
		} else {
			logger.error("Geo-data did not merge successfully.");
			assertTrue(false);
		}
	}
	
	@Test
	public void presenceCorrect() {
		if(presenceMetricCorrect) {
			logger.info("Presence indicator was correct.");
		} else {
			logger.error("Presence indicator was incorrect.");
			assertTrue(false);
		}
	}

	private static boolean isRegionDataCountInSamplesEqualToRegionDataCountInSchema(List<DataSample> samples, Schema schema) {
		// all geos should be merged together for this method
		Map<String, Integer> geoFields = new HashMap<String, Integer>();
		geoFields.put("lat", 0);
		geoFields.put("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat", 0);

		int unitedStatesCount = 0;

		for(String field : geoFields.keySet()) {
			for(int i = 0; i < samples.size(); i++) {
				RegionData regionData = samples.get(i).getDsProfile().get(field).getDetail().getHistogramOptional().get().getRegionData(); //SerializationUtility.deserialize(jArr.getJSONObject(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data"), RegionData.class);
				for(int j = 0 ; j < regionData.getRows().size(); j++) {
					if("United States".equals(regionData.getRows().get(j).getKey())) {
						unitedStatesCount = regionData.getRows().get(j).getValue();
						logger.info("United States count for " + field + " is " + unitedStatesCount + ".");
					}
				}
				geoFields.put(field, geoFields.get(field) + unitedStatesCount);
			}
		}

		boolean allTrue = true;
		for(String field : geoFields.keySet()) {
			RegionData schemaRegionData = schema.getsProfile().get(field).getDetail().getHistogramOptional().get().getRegionData();
			for(int j = 0 ; j < schemaRegionData.getRows().size(); j++) {
				if("United States".equals(schemaRegionData.getRows().get(j).getKey())) {
					logger.info("For field "+field+", expected: " + geoFields.get(field).intValue() + ", got: " + schemaRegionData.getRows().get(j).getValue());
					if(allTrue) {
						allTrue = geoFields.get(field).intValue() == schemaRegionData.getRows().get(j).getValue().intValue();
						if(!allTrue) {
							logger.error(geoFields.get(field).intValue() + " != " + schemaRegionData.getRows().get(j).getValue().intValue());
						}
					}
					logger.info("United States merged coordinates successfully.");
				}
			}
		}
		return allTrue;
	}

	private static Schema schemaPass(List<DataSample> samples, List<Integer> numRecords, List<Integer> randomSeeds) {
		SchemaProfiler schemaProfiler = new SchemaProfiler();
		int i = 0;
		for(DataSample dataSample : samples) {
			for(String key : dataSample.getDsProfile().keySet()) {
				if(i== 0) {
					dataSample.getDsProfile().get(key).setUsedInSchema(true);
				} else {
					dataSample.getDsProfile().get(key).setMergedInto(true);
				}
			}
			schemaProfiler.setCurrentDataSample(dataSample);
			loadSomeRecords(schemaProfiler, numRecords.get(i), randomSeeds.get(i));
			i++;
		}
		return schemaProfiler.asBean();
	}

	private static DataSample samplePass(int numRecords, int randomSeed) {
		SampleProfiler sampleProfiler = new SampleProfiler("Transportation", Tolerance.STRICT);
		loadSomeRecords(sampleProfiler, numRecords, randomSeed);
		DataSample sample = sampleProfiler.asBean();

		SampleSecondPassProfiler srgProfiler = new SampleSecondPassProfiler();
		srgProfiler.setMinimumBatchSize(500);
		srgProfiler.initializeWithSampleBean(sample);
		loadSomeRecords(srgProfiler, numRecords, randomSeed);
		return srgProfiler.asBean();
	}

	private static void loadSomeRecords(Profiler profiler, int numRecords, int randomSeed) {
		Random rand = new Random(randomSeed);
		for(int i = 0; i < numRecords; i++) {
			DefaultProfilerRecord profilerRecord = new DefaultProfilerRecord();
			double randomLat = (rand.nextDouble() * 180) - 90;
			double randomLng = (rand.nextDouble() * 360) - 180;
			profilerRecord.put("lat", randomLat);
			profilerRecord.put("lng", randomLng);

			randomLat = (rand.nextDouble() * 180) - 90;
			randomLng = (rand.nextDouble() * 360) - 180;
			profilerRecord.put("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat", randomLat);
			profilerRecord.put("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lng", randomLng);
			profiler.load(profilerRecord);
		}
	}


}
