package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BinaryProfileAccumulator;
import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.accumulator.StringProfileAccumulator;
import com.deleidos.dp.beans.AliasNameDetails;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.domain.JavaDomain;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.interpretation.AbstractJavaInterpretation;
import com.deleidos.dp.interpretation.JavaUnknownInterpretation;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateListener;

public class SchemaProfiler implements Profiler {
	private GroupingBehavior groupingBehavior = GroupingBehavior.GROUP_ARRAY_VALUES;
	private Logger logger = Logger.getLogger(SchemaProfiler.class);
	protected SchemaReverseGeocodingProfiler geocodingProfiler;
	private ProfilingProgressUpdateListener progressUpdateListener;
	
	protected Map<String, AbstractProfileAccumulator> fieldMapping;
	private Map<String, List<AliasNameDetails>> aliasMapping;
	private List<DataSampleMetaData> dataSampleMetaDataList;
	
	private Map<String, String> currentSampleMergedFieldsMapping;
	private List<String> currentSkipKeyList;
	private DataSample currentDataSample;

	private int recordsParsed;

	public SchemaProfiler() {
		geocodingProfiler = new SchemaReverseGeocodingProfiler();
		fieldMapping = new HashMap<String, AbstractProfileAccumulator>();
		dataSampleMetaDataList = new ArrayList<DataSampleMetaData>();
		aliasMapping = new HashMap<String, List<AliasNameDetails>>();
		recordsParsed = 0;
	}

	@Override
	public int load(ProfilerRecord record) {
		Map<String, List<Object>> normalizedRecord = record.normalizeRecord(groupingBehavior);
		for(String key : normalizedRecord.keySet()) {
			if(normalizedRecord.get(key) == null) {
				continue;
			}
			String accumulatorKey = key;

			if(currentSkipKeyList.contains(accumulatorKey)) {
				// key should be skipped because it was neither merged nor used in schema
				continue;
			} else {
				String mergedAccumulatorKey;
				if(currentSampleMergedFieldsMapping.containsKey(accumulatorKey)) {
					mergedAccumulatorKey = currentSampleMergedFieldsMapping.get(accumulatorKey);
				} else {
					mergedAccumulatorKey = accumulatorKey;
				}

				AbstractProfileAccumulator accumulator;
				List<Object> values = normalizedRecord.get(key);

				if(!fieldMapping.containsKey(mergedAccumulatorKey)) {
					Object nonNullValue = null;
					int nonNullIndex = 0;
					while(nonNullIndex < values.size()) {
						if(values.get(nonNullIndex) != null) {
							break;
						}
						nonNullIndex++;
					}

					if(nonNullIndex >= values.size()) {
						// this particular record only had null values, skip it without instantiating its accumulator
						continue;
					} else {
						nonNullValue = values.get(nonNullIndex);
					}

					Profile currentProfile = currentDataSample.getDsProfile().get(mergedAccumulatorKey);

					if(currentProfile.getMainType().equals("number")) {
						accumulator = new NumberProfileAccumulator(mergedAccumulatorKey);
					} else if(currentProfile.getMainType().equals("string")) {
						accumulator = new StringProfileAccumulator(mergedAccumulatorKey);
					} else if(currentProfile.getMainType().equals("binary")) {
						accumulator = new BinaryProfileAccumulator(mergedAccumulatorKey);
					} else {
						logger.error("Not detected as number, string, or binary in Schema Profiler!  Warning: the instantiation of accumulators"
								+ " in Schema Profiler use direct string equivalence to determine the appropriate accumulator.  Main type is " + currentProfile.getMainType());
						return -1;
					}

					accumulator.initFirstValue(nonNullValue);
					if(accumulator instanceof BinaryProfileAccumulator) {
						((BinaryProfileAccumulator)accumulator)
							.setMediaType(Profile.getBinaryDetail(currentProfile).getMimeType());
					}

					//accumulator.setDomain("transportation");
					//AbstractInterpretation interpretation = AbstractInterpretation.getInterpretationByName(currentProfile.getInterpretation().getInterpretationName());
					Interpretation interpretation = currentProfile.getInterpretation();
					if(!Interpretation.isUnknown(interpretation)) {
						logger.info("Interpretting field: " + mergedAccumulatorKey + " as " + interpretation.getInterpretation());
					}
					accumulator.getState().setInterpretation(interpretation);
					
					for(int i = 0; i < values.size(); i++) {
						if(i == nonNullIndex) {
							// skip the non null value that was used to initialized the list, and add all others (even nulls)
							continue;
						}
						accumulator.accumulate(values.get(i), false);
					}

					fieldMapping.put(mergedAccumulatorKey, accumulator);
				} else {
					accumulator = fieldMapping.get(mergedAccumulatorKey);
					if(!values.isEmpty()) {
						if(accumulator.getDetail() != null) {
							accumulator.accumulate(values.get(0), true);
							for(int i = 1; i < values.size(); i++) {
								accumulator.accumulate(values.get(i), false);
							}
						} else {
							accumulator.initFirstValue(values.get(0));
							for(int i = 1; i < values.size(); i++) {
								accumulator.accumulate(values.get(i), false);
							}
						}
					} 
				}
			}
		}
		recordsParsed++;
		if(progressUpdateListener != null) {
			progressUpdateListener.handleProgressUpdate(record.recordProgressWeight());
		}
		return recordsParsed;
	}

	public List<DataSampleMetaData> getDataSampleMetaDataList() {
		return dataSampleMetaDataList;
	}

	public void setCurrentDataSample(DataSample dataSample) {
		if(currentDataSample != null) {
			logger.info("Finished profiling sample with GUID: " + currentDataSample.getDsGuid());
		}
		currentDataSample = dataSample;
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
		dataSampleMetaDataList.add(dsmd);

		logger.info("Profiling sample with GUID " + currentDataSample.getDsGuid());

		currentSampleMergedFieldsMapping = new HashMap<String, String>();
		currentSkipKeyList = new ArrayList<String>();
		for(String key : dataSample.getDsProfile().keySet()) {
			String fieldNameInSample = String.copyValueOf(key.toCharArray());
			String mergedAccumulatorKey = null;
			Profile profile = dataSample.getDsProfile().get(key);
			if(profile.isMergedInto()) {
				//in sProfile mapping, key is schema field name
				//in profile object, original name is name from sample
				mergedAccumulatorKey = key;
				fieldNameInSample = (dataSample.getDsProfile().get(key).getOriginalName() != null) ? dataSample.getDsProfile().get(key).getOriginalName() : key;
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

			currentSampleMergedFieldsMapping.put(fieldNameInSample, mergedAccumulatorKey);

			AliasNameDetails aliasNameDetails = new AliasNameDetails();
			aliasNameDetails.setAliasName(fieldNameInSample);
			aliasNameDetails.setDsGuid(currentDataSample.getDsGuid());
			List<AliasNameDetails> aliasList;
			if(aliasMapping.containsKey(mergedAccumulatorKey)) {
				aliasList = aliasMapping.get(mergedAccumulatorKey);
			} else {
				aliasList = new ArrayList<AliasNameDetails>();
			}
			aliasList.add(aliasNameDetails);
			aliasMapping.put(mergedAccumulatorKey, aliasList);

		}
		outputCurrentSampleMergedfieldsMapping();

		Map<String, Profile> adjustedProfileWithMergedKeys = copyProfileWithMergedKeyValues(dataSample.getDsProfile()); //region data set here
		geocodingProfiler.initializeWithMap(adjustedProfileWithMergedKeys);
	}

	private Map<String, Profile> copyProfileWithMergedKeyValues(Map<String, Profile> dsProfile) {
		Map<String, Profile> copyOfProfile = new HashMap<String, Profile>(dsProfile);
		for(String nonMerged : currentSampleMergedFieldsMapping.keySet()) {
			String merged = currentSampleMergedFieldsMapping.get(nonMerged);
			if(nonMerged.equals(currentSampleMergedFieldsMapping.get(nonMerged))) {
				continue; // same key
			}
			if(copyOfProfile.containsKey(merged)) {
				String oldKey = dsProfile.get(merged).getOriginalName();
				if(oldKey == null) {
					if(dsProfile.get(merged).isUsedInSchema()) {
						oldKey = merged;
					} else {
						logger.warn("Original name detected as null and not used-in-schema.");
						continue;
					}
				}
				Profile profile = copyOfProfile.get(merged);
				if(profile.getDetail().getBucketListIfApplicable() != null) {
					RegionData regionData = profile.getDetail().getBucketListIfApplicable().getRegionData();
					if(regionData == null) {
						logger.warn("Region data detected as null when expected to be present.");
						continue;
					}
					String mergedPartnerKey;

					if(oldKey.equals(regionData.getLatitudeKey())) {
						regionData.setLatitudeKey(merged);
						mergedPartnerKey = currentSampleMergedFieldsMapping.get(regionData.getLongitudeKey());
						regionData.setLongitudeKey(mergedPartnerKey);
						logger.debug("Set latitude to " + merged + " and its partner to " + mergedPartnerKey);
					} else if(oldKey.equals(regionData.getLongitudeKey())) {
						regionData.setLongitudeKey(merged);
						mergedPartnerKey = currentSampleMergedFieldsMapping.get(regionData.getLatitudeKey());
						regionData.setLatitudeKey(mergedPartnerKey);
						logger.debug("Set longitude to " + merged + " and its partner to " + mergedPartnerKey);
					} else {
						logger.debug(merged + " already merged by partner field.");
						continue;
					}

					Profile partnerProfile = copyOfProfile.get(mergedPartnerKey);
					if(partnerProfile == null) {
						//TODO define logic for invalid coordinate merges
						logger.error("Partial coordinate merge detected.  Rolling back merge.");
						profile.getDetail().setRegionDataIfApplicable(null);
						profile.setInterpretation(Interpretation.UNKNOWN);
						copyOfProfile.put(merged, profile);
						continue;
					}

					profile.getDetail().setRegionDataIfApplicable(regionData);
					copyOfProfile.put(merged, profile);

					partnerProfile.getDetail().setRegionDataIfApplicable(regionData);
					copyOfProfile.put(mergedPartnerKey, partnerProfile);
				}
			}
		}
		return copyOfProfile;
	}

	private void outputCurrentSampleMergedfieldsMapping() {
		logger.info("Sample "+currentDataSample.getDsGuid()+" merged fields mapping: ");
		for(String key : currentSampleMergedFieldsMapping.keySet()) {
			if(key.equals(currentSampleMergedFieldsMapping.get(key))) {
				logger.info("Accumulating metrics for schema field \"" + currentSampleMergedFieldsMapping.get(key) + "\".");
			} else {
				logger.info("Accumulating metrics for schema field \"" + currentSampleMergedFieldsMapping.get(key) 
				+ "\" (original name - \"" + key + "\").");
			}
		}
	}

	@Override
	public Schema asBean() {
		Schema schema = new Schema();
		Map<String, Profile> sProfile = new HashMap<String, Profile>();
		fieldMapping.forEach((key, accumulator) -> {
			accumulator.finish();
			Profile profile = accumulator.getState();
			profile.setPresence(((float)accumulator.getPresenceCount())/((float)recordsParsed));
			if(aliasMapping.containsKey(key)) {
				profile.setAliasNames(aliasMapping.get(key));
			}
			sProfile.put(key, profile);
		});
		schema.setsProfile(sProfile);
		geocodingProfiler.setBean(schema);
		Schema schemaWithGeoData = geocodingProfiler.asBean();
		return schemaWithGeoData;
	}

	public int getRecordsParsed() {
		return recordsParsed;
	}

	public void setRecordsParsed(int recordsParsed) {
		this.recordsParsed = recordsParsed;
	}

	public ProfilingProgressUpdateListener getProgressUpdateListener() {
		return progressUpdateListener;
	}

	public void setProgressUpdateListener(ProfilingProgressUpdateListener progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}


}
