package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BinaryProfileAccumulator;
import com.deleidos.dp.beans.AliasNameDetails;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;
import com.deleidos.dp.reversegeocoding.CoordinateProfile;

public class SchemaProfiler implements Profiler {
	private GroupingBehavior groupingBehavior = GroupingBehavior.GROUP_ARRAY_VALUES;
	private Logger logger = Logger.getLogger(SchemaProfiler.class);
	private ProfilingProgressUpdateHandler progressUpdateListener;

	private final Map<String, AbstractProfileAccumulator> fieldMapping;
	private final Map<String, Profile> staticProfiles;
	private final Map<String, List<AliasNameDetails>> aliasMapping;
	private final Map<String, RegionData> regionDataMapping;
	private Map<String, MergeData> mergeDataMapping; 
	private final List<DataSampleMetaData> dataSampleMetaDataList;
	private final Schema existingSchema;
	private String currentMergeLookupId;
	private int recordsParsed;
	private long recordsDropped;

	public SchemaProfiler(Schema existingSchema, List<DataSample> samples) {
		this.existingSchema = existingSchema;
		fieldMapping = new HashMap<String, AbstractProfileAccumulator>();
		dataSampleMetaDataList = new ArrayList<DataSampleMetaData>();
		aliasMapping = new HashMap<String, List<AliasNameDetails>>();
		staticProfiles = new HashMap<String, Profile>();
		recordsParsed = 0;
		recordsDropped = 0;
		mergeDataMapping = new HashMap<String, MergeData>();
		regionDataMapping = new HashMap<String, RegionData>();
		init(existingSchema, samples);
	}

	public void init(Schema existingSchema, List<DataSample> samples) { 
		try {
			initExistingSchema(existingSchema);
			analyzeDataSamples(samples);
			for(String key : fieldMapping.keySet()) {
				AbstractProfileAccumulator apa = initializeProfile(key, mergeDataMapping);
				if(apa == null) {
					// make it static, don't handle it as a normal accumulator (manually created and field exclusively in existing schema)
					staticProfiles.put(key, existingSchema.getsProfile().get(key));
				} else {
					fieldMapping.put(key, apa);
				}
			}
			// remove static profiles
			staticProfiles.forEach((k,v)->{
				if(fieldMapping.containsKey(k)) fieldMapping.remove(k);
			});
		} catch(MainTypeException e) {
			logger.error(e);
			throw new MainTypeRuntimeException("Unexpected main type error while initilizing schema.");
		}
	}

	private AbstractProfileAccumulator initializeProfile(String key, Map<String, MergeData> mergeDataMapping) throws MainTypeException {
		Profile existingSchemaProfile = null;
		int existingSchemaRecordCount = -1;
		List<Profile> mergedProfiles = new ArrayList<Profile>();
		for(String mergedDataKey : mergeDataMapping.keySet()) {
			if(mergeDataMapping.get(mergedDataKey).getSample() != null) {
				DataSample sample = mergeDataMapping.get(mergedDataKey).getSample();
				if(sample != null && sample.getDsProfile().containsKey(key)) {
					if(sample.getDsProfile().get(key).isUsedInSchema()
							|| sample.getDsProfile().get(key).isMergedInto()) {
						mergedProfiles.add(sample.getDsProfile().get(key));
					}
				}
			} else {
				Schema schema = mergeDataMapping.get(mergedDataKey).getSchema();
				existingSchemaRecordCount = schema.getRecordsParsedCount();
				if(schema.getsProfile().containsKey(key)) {
					Profile profile = schema.getsProfile().get(key);
					existingSchemaProfile = schema.getsProfile().get(key);
					if(profile.getAliasNames() == null || profile.getAliasNames().isEmpty()) {
						// field is manually created, so use it as a static field
						return null;
					} 
				}
			}
		}
		AbstractProfileAccumulator accumulator = AbstractProfileAccumulator.generateProfileAccumulator(
				key, existingSchemaProfile, existingSchemaRecordCount, mergedProfiles);
		/*if(accumulator instanceof BinaryProfileAccumulator) {
			((BinaryProfileAccumulator)accumulator)
			.setMediaType(Profile.getBinaryDetail(mergedProfiles.get(0)).getMimeType());
		}*/
		return accumulator;
	}

	private List<String> getCurrentSkipKeyList() {
		return mergeDataMapping.get(currentMergeLookupId).getSkipKeyList();
	}

	private DataSample getCurrentDataSample() {
		return mergeDataMapping.get(currentMergeLookupId).getSample();
	}

	private Map<String, String> getCurrentMergedFieldsMapping() {
		return mergeDataMapping.get(currentMergeLookupId).getSampleMergedFieldsMapping();
	}

	private Schema getExistingSchema() {
		for(String key : mergeDataMapping.keySet()) {
			if(mergeDataMapping.get(key).getSchema() != null) {
				return mergeDataMapping.get(key).getSchema();
			}
		}
		return null;
	}

	private List<DataSample> getAllDataSamples() {
		List<DataSample> samples = new ArrayList<DataSample>();
		for(String key : mergeDataMapping.keySet()) {
			if(mergeDataMapping.get(key).sample != null) {
				samples.add(mergeDataMapping.get(key).getSample());
			}
		}
		return samples;
	}

	@Override
	public int load(ProfilerRecord record) {
		boolean isBinary = record instanceof BinaryProfilerRecord;
		Map<String, List<Object>> normalizedRecord = record.normalizeRecord(groupingBehavior);
		for(String key : normalizedRecord.keySet()) {
			List<Object> values = normalizedRecord.get(key);

			if(mergeDataMapping.get(currentMergeLookupId).getSkipKeyList().contains(key)) {
				// key should be skipped because it was neither merged nor used in schema
				continue;
			} else {

				String mergedAccumulatorKey = (getCurrentMergedFieldsMapping().containsKey(key)) 
						? getCurrentMergedFieldsMapping().get(key) : key;

						/*if(!getCurrentDataSample().getDsProfile().containsKey(mergedAccumulatorKey)) {
					getCurrentSkipKeyList().add(mergedAccumulatorKey);
					continue;
				}*/

						if(fieldMapping.containsKey(mergedAccumulatorKey)) {
							int i = 0;
							// only want to accumulate presence for the first non null value
							for(; i < values.size(); i++) {
								try {
									fieldMapping.get(mergedAccumulatorKey).accumulate(values.get(i), true);
									i++;
									break;
								} catch (MainTypeException e) {
									recordsDropped++;
								}
							}
							for(; i < normalizedRecord.get(key).size(); i++) {
								try {
									fieldMapping.get(mergedAccumulatorKey).accumulate(values.get(i), false);
								} catch (MainTypeException e) {
									recordsDropped++;
								}
							}
						} else {
							logger.warn("Key "+mergedAccumulatorKey+" not found in accumulator mapping.  Skipping.");
							getCurrentSkipKeyList().add(mergedAccumulatorKey);
						} 


			}
		}
		if(!isBinary) {
			recordsParsed++;
		}

		return recordsParsed;
	}

	public List<DataSampleMetaData> getDataSampleMetaDataList() {
		return dataSampleMetaDataList;
	}

	private void initExistingSchema(Schema existingSchema) throws MainTypeException {
		if(existingSchema != null) {
			logger.info("Existing schema detected.  Using existing fields as seed values.");
			for(String existingField : existingSchema.getsProfile().keySet()) {
				existingSchema.getsProfile().get(existingField).setUsedInSchema(true);
				aliasMapping.put(existingField, existingSchema.getsProfile().get(existingField).getAliasNames());
			}
			this.recordsParsed = existingSchema.getRecordsParsedCount();

			Map<String, String> schemaMergeMap = new HashMap<String, String>();

			for(String key : existingSchema.getsProfile().keySet()) {
				schemaMergeMap.put(key, key);
				String mergedAccumulatorKey = key;
				fieldMapping.put(mergedAccumulatorKey, null);
			}

			MergeData mergedData = new MergeData();
			mergedData.setSchema(existingSchema);
			mergedData.setSample(null);
			mergedData.setSampleMergedFieldsMapping(schemaMergeMap);
			mergedData.setSkipKeyList(Arrays.asList());
			currentMergeLookupId = existingSchema.getsGuid();
			mergeDataMapping.put(existingSchema.getsGuid(), mergedData);
			outputCurrentSampleMergedfieldsMapping(existingSchema.getsName(), getCurrentMergedFieldsMapping());

			if(existingSchema.getsDataSamples().isEmpty()) {
				logger.warn("Empty samples list in existing schema object.");
			} else {
				dataSampleMetaDataList.addAll(existingSchema.getsDataSamples());
				for(DataSampleMetaData dsmd : dataSampleMetaDataList) {
					logger.info("Existing schema contains sample: " + dsmd.getDsName() + ".");
				}
			}

			regionDataMapping.putAll(mergeCoordinateProfiles(regionDataMapping, mergedData.getSampleMergedFieldsMapping(),
					ReverseGeocodingLoader.getCoordinateProfiles(existingSchema.getsProfile())));
		}
	}

	private void analyzeDataSamples(List<DataSample> dataSamples) throws MainTypeException {
		for(DataSample dataSample : dataSamples) {
			try {
				mergeDataMapping.put(dataSample.getDsGuid(),
						initializeSampleMergedData(dataSample.getDsProfile(), dataSample));
			} catch (MainTypeException e) {
				logger.error(e);
			}
			DataSampleMetaData dsmd = new DataSampleMetaData();
			dsmd.setDataSampleId(dataSample.getDataSampleId());
			String dsId = dataSample.getDsGuid();
			dsmd.setDsGuid(dsId);
			dsmd.setDsDescription(dataSample.getDsDescription());
			dsmd.setDsFileName(dataSample.getDsFileName());
			dsmd.setDsFileType(dataSample.getDsFileType());
			dsmd.setDsName(dataSample.getDsName());
			dsmd.setDsVersion(dataSample.getDsVersion());
			dsmd.setDsLastUpdate(dsmd.getDsLastUpdate());
			boolean containsGuid = false;
			if(existingSchema != null) {
				for(DataSampleMetaData d : existingSchema.getsDataSamples()) {
					if(d.getDsGuid().equals(dsmd.getDsGuid())) {
						containsGuid = true;
						break;
					}
				}
			}
			if(!containsGuid) {
				dataSampleMetaDataList.add(dsmd);
			}

		}
	}

	public void setCurrentDataSampleGuid(String dsGuid) {
		if(mergeDataMapping.get(currentMergeLookupId) != null && mergeDataMapping.get(currentMergeLookupId).getSample() != null) {
			logger.info("Finished profiling sample with GUID: " + getCurrentDataSample().getDsGuid());
		}
		this.currentMergeLookupId = dsGuid;

		logger.info("Profiling sample with GUID " + getCurrentDataSample().getDsGuid());

		outputCurrentSampleMergedfieldsMapping(getCurrentDataSample().getDsName(), getCurrentMergedFieldsMapping());
	}

	private MergeData initializeSampleMergedData(Map<String, Profile> profileMap, DataSample dataSample) throws MainTypeException {
		Map<String, String> currentSampleMergedFieldsMapping = new HashMap<String, String>();
		List<String> currentSkipKeyList = new ArrayList<String>();
		for(String key : profileMap.keySet()) {
			String fieldNameInSample = String.copyValueOf(key.toCharArray());
			String mergedAccumulatorKey = null;
			Profile profile = profileMap.get(key);
			if(profile.isMergedInto()) {
				//in sProfile mapping, key is schema field name
				//in profile object, original name is name from sample
				mergedAccumulatorKey = key;
				fieldNameInSample = (profileMap.get(key).getOriginalName() != null) ? profileMap.get(key).getOriginalName() : key;
				if(currentSkipKeyList.contains(fieldNameInSample)) {
					currentSkipKeyList.remove(fieldNameInSample);
				}
			} else if(profile.isUsedInSchema()) {
				// if used in schema, use its name
				mergedAccumulatorKey = key;
			} else {
				// otherwise make a note to skip this key for the sample because it's neither merged nor used
				logger.info("Key \"" + key + "\" detected as neither merged nor used in schema - ignoring.");
				if(!currentSampleMergedFieldsMapping.containsKey(key)) {
					currentSkipKeyList.add(key);
				}
				continue;
			}

			fieldMapping.put(mergedAccumulatorKey, null);

			currentSampleMergedFieldsMapping.put(fieldNameInSample, mergedAccumulatorKey);

			AliasNameDetails aliasNameDetails = new AliasNameDetails();
			aliasNameDetails.setAliasName(fieldNameInSample);
			aliasNameDetails.setDsGuid(dataSample.getDsGuid());
			List<AliasNameDetails> aliasList;
			if(aliasMapping.containsKey(mergedAccumulatorKey)) {
				aliasList = aliasMapping.get(mergedAccumulatorKey);
			} else {
				aliasList = new ArrayList<AliasNameDetails>();
			}
			aliasList.add(aliasNameDetails);
			aliasMapping.put(mergedAccumulatorKey, aliasList);

		}
		MergeData mergeData = new MergeData();
		mergeData.setSchema(null);
		mergeData.setSample(dataSample);
		mergeData.setSampleMergedFieldsMapping(currentSampleMergedFieldsMapping);
		mergeData.setSkipKeyList(currentSkipKeyList);

		regionDataMapping.putAll(mergeCoordinateProfiles(regionDataMapping, mergeData.getSampleMergedFieldsMapping(), 
				ReverseGeocodingLoader.getCoordinateProfiles(dataSample.getDsProfile())));

		return mergeData;
	}

	private void outputCurrentSampleMergedfieldsMapping(String name, Map<String, String> currentMergeMapping) {
		logger.info(name+" merged fields mapping: ");
		for(String key : currentMergeMapping.keySet()) {
			if(key.equals(currentMergeMapping.get(key))) {
				logger.debug("Accumulating metrics for schema field \"" + currentMergeMapping.get(key) + "\".");
			} else {
				logger.debug("Accumulating metrics for schema field \"" + currentMergeMapping.get(key) 
				+ "\" (original name - \"" + key + "\").");
			}
		}
	}
	
	private boolean containsInterpretation(List<Interpretation> interpretations, Interpretation candidate) {
		for(Interpretation i : interpretations) {
			if(i.getiName().equals(candidate.getiName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Schema asBean() {
		Schema schema = (existingSchema == null) ? new Schema() : existingSchema;
		Map<String, Profile> sProfile = new HashMap<String, Profile>();
		for(String key: fieldMapping.keySet()) {
			AbstractProfileAccumulator accumulator = fieldMapping.get(key);
			try {
				accumulator.finish();
				Profile profile = accumulator.getState();
				profile.setPresence(((float)accumulator.getPresenceCount())/((float)recordsParsed));
				if(aliasMapping.containsKey(key)) {
					profile.setAliasNames(aliasMapping.get(key));
				}
				if(existingSchema != null && existingSchema.getsProfile().containsKey(key)) {
					Integer existingNumDistinct = MetricsCalculationsFacade.stripNumDistinctValuesChars(
							existingSchema.getsProfile().get(key).getDetail().getNumDistinctValues());
					Integer newNumDistinct = MetricsCalculationsFacade.stripNumDistinctValuesChars(
							profile.getDetail().getNumDistinctValues());
					double m = Math.max(existingNumDistinct, newNumDistinct);
					String numDistinctValues = String.valueOf(m); 
					profile.getDetail().setNumDistinctValues(">=" + numDistinctValues);
				}

				for(String mergeDataKey : mergeDataMapping.keySet()) {
					MergeData mergeData = mergeDataMapping.get(mergeDataKey);
					if(mergeData.getSample() != null && mergeData.getSample().getDsProfile().containsKey(key)) {
						DataSample sample = mergeData.getSample();
						Interpretation i = sample.getDsProfile().get(key)
								.getInterpretation();
						if(!containsInterpretation(profile.getInterpretations(), i)) {
							profile.getInterpretations().add(i);
						}
					} else if(schema != null && schema.getsProfile().containsKey(key)) {
						Interpretation i = schema.getsProfile().get(key)
								.getInterpretation();
						if(!containsInterpretation(profile.getInterpretations(), i)) {
							profile.getInterpretations().add(i);
						}
					}
				}

				Interpretation interpretation = profile.getInterpretation();
				if(!Interpretation.isUnknown(interpretation)) {
					logger.info("Interpretting field: " + key + " as " + interpretation.getiName());
				}

				sProfile.put(key, profile);
			} catch (MainTypeException e) {
				logger.error("Main type error with field " + key +".", e);
				throw new MainTypeRuntimeException("Main type error with field " + key +".", e);
			}

		}
		if(staticProfiles != null) {
			sProfile.putAll(staticProfiles);
		}
		sProfile = DisplayNameHelper.determineDisplayNames(sProfile);
		for(String key : regionDataMapping.keySet()) {
			if(sProfile.containsKey(key)) {
				sProfile.get(key).getDetail().setRegionDataIfApplicable(regionDataMapping.get(key));
			} else {
				logger.error("Error adding region data for key " + key + ".");
			}
		}
		schema.setsProfile(sProfile);
		schema.setRecordsParsedCount(recordsParsed);
		schema.setsDataSamples(dataSampleMetaDataList);
		return schema;
	}

	public int getRecordsParsed() {
		return recordsParsed;
	}

	public void setRecordsParsed(int recordsParsed) {
		this.recordsParsed = recordsParsed;
	}

	public ProfilingProgressUpdateHandler getProgressUpdateListener() {
		return progressUpdateListener;
	}

	public void setProgressUpdateListener(ProfilingProgressUpdateHandler progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}

	public static Map<String, RegionData> mergeCoordinateProfiles(Map<String, RegionData> existingRegionData, Map<String, String> mergeMap, List<CoordinateProfile> coordinateProfiles) {
		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			String mergedLatitudeKey = mergeMap.get(coordinateProfile.getLatitude());
			String mergedLongitudeKey = mergeMap.get(coordinateProfile.getLongitude());

			coordinateProfile.setLatitude(mergedLatitudeKey);
			coordinateProfile.setLongitude(mergedLongitudeKey);

			RegionData regionData = ReverseGeocodingLoader.regionDataFromCoordinateProfile(coordinateProfile);

			if(existingRegionData.containsKey(mergedLatitudeKey)) {
				regionData = RegionData.add(existingRegionData.get(mergedLatitudeKey), regionData);
			}

			existingRegionData.put(mergedLatitudeKey, regionData);
			existingRegionData.put(mergedLongitudeKey, regionData);

		}
		return existingRegionData;
	}

	private static class MergeData {
		private Map<String, String> sampleMergedFieldsMapping;
		private List<String> skipKeyList;
		private DataSample sample;
		private Schema schema;

		public Map<String, String> getSampleMergedFieldsMapping() {
			return sampleMergedFieldsMapping;
		}

		public void setSampleMergedFieldsMapping(Map<String, String> sampleMergedFieldsMapping) {
			this.sampleMergedFieldsMapping = sampleMergedFieldsMapping;
		}

		public List<String> getSkipKeyList() {
			return skipKeyList;
		}

		public void setSkipKeyList(List<String> skipKeyList) {
			this.skipKeyList = skipKeyList;
		}

		public DataSample getSample() {
			return sample;
		}

		public void setSample(DataSample sample) {
			this.sample = sample;
		}

		public Schema getSchema() {
			return schema;
		}

		public void setSchema(Schema schema) {
			this.schema = schema;
		}

	}

	public static Schema generateSchema(List<DataSample> dataSamples, Map<String, List<ProfilerRecord>> sampleToRecordsMapping) {
		SchemaProfiler schemaProfiler = new SchemaProfiler(null, dataSamples);
		for(DataSample sample : dataSamples) {
			List<ProfilerRecord> records = sampleToRecordsMapping.get(sample.getDsGuid());
			schemaProfiler.setCurrentDataSampleGuid(sample.getDsGuid());
			records.forEach(record->schemaProfiler.load(record));
		}
		Schema schema = schemaProfiler.asBean();
		schema.setsName(UUID.randomUUID().toString());
		schema.setsGuid(schema.getsName());
		return schema;
	}

	public Map<String, AbstractProfileAccumulator> getFieldMapping() {
		return fieldMapping;
	}
}
