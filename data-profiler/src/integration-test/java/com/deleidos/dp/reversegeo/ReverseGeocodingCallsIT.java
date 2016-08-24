package com.deleidos.dp.reversegeo;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.ReverseGeocodingLoader;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.SampleSecondPassProfiler;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.reversegeocoding.CoordinateProfile;

public class ReverseGeocodingCallsIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(ReverseGeocodingCallsIT.class);
	private static boolean geoDataInSamples;
	private static boolean geoDataInSchema;
	private static boolean geoDataMerged;
	private static boolean presenceMetricCorrect;
	private static boolean nonGeoWorked;

	@BeforeClass
	public static void makeSomeReverseGeoCallsTest() throws DataAccessException, MainTypeException {
		int numRecords = 1001; // should be 2002 geocoding calls
		int numRecords2 = 550; // should be 1100 geocoding calls
		final int seed1 = 10; // DO NOT CHANGE
		final int seed2 = 40; // DO NOT CHANGE because some values do not successfully get interpretted as coordinates
		DataSample sampleWithGeo = samplePass(numRecords, seed1);

		int latCount = Profile.getNumberDetail(sampleWithGeo.getDsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().size();
		int usIndex = ReverseGeocodingLoader.getCountryIndexByName(sampleWithGeo.getDsProfile().get("lat").getDetail().getHistogramOptional().get().getRegionData().getRows(), "United States");
		int usLatCount = Profile.getNumberDetail(sampleWithGeo.getDsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().get(usIndex).getValue();
		int waypointsLatCount = Profile.getNumberDetail(sampleWithGeo.getDsProfile().get("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat")).getHistogramOptional().get().getRegionData().getRows().size();
		boolean sampleGeoAssertion = latCount > 0 && waypointsLatCount > 0;

		DataSample sampleWithGeo2 = samplePass(numRecords2, seed2);
		
		int latCount2 = Profile.getNumberDetail(sampleWithGeo2.getDsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().size();
		int usIndex2 = ReverseGeocodingLoader.getCountryIndexByName(sampleWithGeo2.getDsProfile().get("lat").getDetail().getHistogramOptional().get().getRegionData().getRows(), "United States");
		int usLatCount2 = Profile.getNumberDetail(sampleWithGeo2.getDsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().get(usIndex2).getValue();
		int waypointsLatCount2 = Profile.getNumberDetail(sampleWithGeo2.getDsProfile().get("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat")).getHistogramOptional().get().getRegionData().getRows().size();
		boolean sampleGeoAssertion2 = latCount2 > 0 && waypointsLatCount2 > 0;
		
		String g1 = H2DataAccessObject.getInstance().addSample(sampleWithGeo);
		String g2 = H2DataAccessObject.getInstance().addSample(sampleWithGeo2);
		
		geoDataInSamples = sampleGeoAssertion && sampleGeoAssertion2;
		
		DataSample sample1 = H2DataAccessObject.getInstance().getSampleByGuid(g1);
		DataSample sample2 = H2DataAccessObject.getInstance().getSampleByGuid(g2);
		List<DataSample> samplesWithMatches = MetricsCalculationsFacade.matchFieldsAcrossSamplesAndSchema(null, Arrays.asList(sampleWithGeo, sampleWithGeo2), null);
		logger.info(SerializationUtility.serialize(samplesWithMatches));
		Schema schemaBean = schemaPass(null, samplesWithMatches, Arrays.asList(numRecords, numRecords2),
				Arrays.asList(seed1, seed2));
		//logger.info(SerializationUtility.serialize(schemaBean));
		geoDataInSchema = Profile.getNumberDetail(schemaBean.getsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().size() > 0;
		
		int usIndexSchema = ReverseGeocodingLoader.getCountryIndexByName(schemaBean.getsProfile().get("lat").getDetail().getHistogramOptional().get().getRegionData().getRows(), "United States");
		int usLatCountSchema = Profile.getNumberDetail(schemaBean.getsProfile().get("lat")).getHistogramOptional().get().getRegionData().getRows().get(usIndexSchema).getValue();
		geoDataMerged = isRegionMerged(usLatCount, usLatCount2, usLatCountSchema);
		//geoDataMerged = isRegionDataCountInSamplesEqualToRegionDataCountInSchema(Arrays.asList(sample1, sample2), schemaBean);
		
		presenceMetricCorrect = schemaBean.getsProfile().get("lat").getPresence() == 1.0f;
		logger.info("Presence: " + schemaBean.getsProfile().get("lat").getPresence());
		
		nonGeoWorked = assertNonGeoDoesntHaveRegionData(schemaBean.getsProfile().get("no-geo"));
	}
	
	private static boolean assertNonGeoDoesntHaveRegionData(Profile nonGeoProfile) {
		return nonGeoProfile.getDetail().getHistogramOptional().get().getRegionData() == null;
	}
	
	@Test
	public void nonGeoWorked() {
		if(nonGeoWorked) {
			logger.info("Geo-data stayed out of non geo fields.");
		} else {
			logger.error("Geo-data spread to the wrong fields.");
			assertTrue(false);
		}
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
	
	@Test
	public void testAddToExistingRegionData() {
		Map<String, RegionData> existingRegionData = new HashMap<String, RegionData>();
		Map<String, String> mergeMap = new HashMap<String, String>();
		mergeMap.put("old-lat", "new-lat");
		mergeMap.put("old-lon", "new-lon");
		List<CoordinateProfile> coordinateProfiles = new ArrayList<CoordinateProfile>();
		CoordinateProfile cp = new CoordinateProfile("old-lat", "old-lon");
		cp.getCountryFrequencyMapping().put("United States", 10);
		coordinateProfiles.add(cp);
		existingRegionData = SchemaProfiler.mergeCoordinateProfiles(existingRegionData, mergeMap, coordinateProfiles);
		existingRegionData.forEach((k,v)->System.out.println(k+":"+SerializationUtility.serialize(v)));
		assertTrue(existingRegionData.get("new-lon").getRows().get(0).getValue() == 10);
		
		mergeMap = new HashMap<String, String>();
		mergeMap.put("old-lat2", "new-lat");
		mergeMap.put("old-lon2", "new-lon");
		coordinateProfiles = new ArrayList<CoordinateProfile>();
		cp = new CoordinateProfile("old-lat2", "old-lon2");
		cp.getCountryFrequencyMapping().put("United States", 10);
		coordinateProfiles.add(cp);
		existingRegionData = SchemaProfiler.mergeCoordinateProfiles(existingRegionData, mergeMap, coordinateProfiles);
		existingRegionData.forEach((k,v)->System.out.println(k+":"+SerializationUtility.serialize(v)));
		assertTrue(existingRegionData.get("new-lon").getRows().get(0).getValue() == 20);
		
	}
	
	private static boolean isRegionMerged(int sampleUsCount, int sample2UsCount, int schemaUsCount) {
		boolean merged = sample2UsCount + sampleUsCount == schemaUsCount;
		if(merged) {
			logger.info(sample2UsCount + " + " + sampleUsCount + " = " + schemaUsCount);
		} else {
			logger.error(sample2UsCount + " + " + sampleUsCount + " != " + schemaUsCount);
		}
		return merged;
	}

	private static boolean isRegionDataCountInSamplesEqualToRegionDataCountInSchema(List<DataSample> samples, Schema schema) {
		// all geos should be merged together for this method
		Map<String, Integer> geoFields = new HashMap<String, Integer>();
		geoFields.put("lat", 0);
		geoFields.put("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat", 0);

		for(String field : geoFields.keySet()) {
			for(int i = 0; i < samples.size(); i++) {
				RegionData regionData = samples.get(i).getDsProfile().get(field).getDetail().getHistogramOptional().get().getRegionData(); //SerializationUtility.deserialize(jArr.getJSONObject(0).getJSONObject("dsProfile").getJSONObject("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat").getJSONObject("detail").getJSONObject("freq-histogram").getJSONObject("region-data"), RegionData.class);
				int index = ReverseGeocodingLoader.getCountryIndexByName(regionData.getRows(), "United States");
				int count = regionData.getRows().get(index).getValue();
				geoFields.put(field, geoFields.get(field) + count);
			}
		}
		geoFields.forEach((k,v)->logger.info("Expected United States count for " + k+" is " + v +"."));

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

	private static Schema schemaPass(Schema existingSchema, List<DataSample> samples, List<Integer> numRecords, List<Integer> randomSeeds) throws MainTypeException {
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

	private static DataSample samplePass(int numRecords, int randomSeed) throws MainTypeException, DataAccessException {
		SampleProfiler sampleProfiler = new SampleProfiler(Tolerance.STRICT);
		loadSomeRecords(sampleProfiler, numRecords, randomSeed);
		DataSample sample = sampleProfiler.asBean();
		InterpretationEngineFacade.interpretInline(sample, "Transportation", null);

		SampleSecondPassProfiler srgProfiler = new SampleSecondPassProfiler(sample);
		srgProfiler.setMinimumBatchSize(500);
		loadSomeRecords(srgProfiler, numRecords, randomSeed);
		DataSample ds = srgProfiler.asBean();
		ds.setDsFileName("/test-sample"+String.valueOf(numRecords)+"-"+String.valueOf(randomSeed));
		ds.setDsGuid(UUID.randomUUID().toString());
		return ds;
	}

	private static void loadSomeRecords(Profiler profiler, int numRecords, int randomSeed) {
		Random rand = new Random(randomSeed);
		for(int i = 0; i < numRecords; i++) {
			DefaultProfilerRecord profilerRecord = new DefaultProfilerRecord();
			double randomLat = (rand.nextDouble() * 180) - 90;
			double randomLng = (rand.nextDouble() * 360) - 180;
			int randomNotGeo = rand.nextInt(10000000);
			profilerRecord.put("lat", randomLat);
			profilerRecord.put("lng", randomLng);

			randomLat = (rand.nextDouble() * 180) - 90;
			randomLng = (rand.nextDouble() * 360) - 180;
			profilerRecord.put("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lat", randomLat);
			profilerRecord.put("waypoints"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER+"lng", randomLng);
			
			
			profilerRecord.put("no-geo", randomNotGeo);
			profiler.load(profilerRecord);
		}
	}


}
