package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BinaryProfileAccumulator;
import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.accumulator.StringProfileAccumulator;
import com.deleidos.dp.beans.AliasNameDetails;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateListener;

public class SchemaProfiler implements Profiler {
	private GroupingBehavior groupingBehavior = GroupingBehavior.GROUP_ARRAY_VALUES;
	private Logger logger = Logger.getLogger(SchemaProfiler.class);
	protected SchemaReverseGeocodingProfiler geocodingProfiler;
	private ProfilingProgressUpdateListener progressUpdateListener;

	protected Map<String, AbstractProfileAccumulator> fieldMapping;
	private Map<String, Profile> userCreatedFields;
	private Map<String, List<AliasNameDetails>> aliasMapping;
	private List<DataSampleMetaData> dataSampleMetaDataList;

	private Map<String, String> currentSampleMergedFieldsMapping;
	private List<String> currentSkipKeyList;
	private DataSample currentDataSample;
	private Schema existingSchema = null;
	
	private int recordsParsed;

	public SchemaProfiler() {
		geocodingProfiler = new SchemaReverseGeocodingProfiler();
		fieldMapping = new HashMap<String, AbstractProfileAccumulator>();
		dataSampleMetaDataList = new ArrayList<DataSampleMetaData>();
		aliasMapping = new HashMap<String, List<AliasNameDetails>>();
		recordsParsed = 0;
	}

	@Override
	public void load(ProfilerRecord record) {
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
				
				if(!currentDataSample.getDsProfile().containsKey(mergedAccumulatorKey)) {
					currentSkipKeyList.add(mergedAccumulatorKey);
					continue;
				}

				AbstractProfileAccumulator accumulator;
				List<Object> values = normalizedRecord.get(key);
				if(!fieldMapping.containsKey(mergedAccumulatorKey)) {

					Profile currentProfile = currentDataSample.getDsProfile().get(mergedAccumulatorKey);

					switch(currentProfile.getMainType()) {
					case "number": {
						accumulator = new NumberProfileAccumulator(mergedAccumulatorKey);
						break;
					} 
					case "string": {
						accumulator = new StringProfileAccumulator(mergedAccumulatorKey);
						break;
					}
					case "binary": {
						accumulator = new BinaryProfileAccumulator(mergedAccumulatorKey);
						break;
					}
					default: {
						logger.error("Not detected as number, string, or binary in Schema Profiler!  Warning: the instantiation of accumulators"
								+ " in Schema Profiler use direct string equivalence to determine the appropriate accumulator.  Main type is " + currentProfile.getMainType());
						return;
					}
					}
					Object nonNullValue = null;
					int nonNullIndex = 0;
					while(nonNullIndex < values.size()) {
						if(values.get(nonNullIndex) != null) {
							nonNullValue = values.get(nonNullIndex);
							try {
								nonNullValue = accumulator.getState().getMainTypeClass().createAppropriateObject(nonNullValue);
								accumulator.initFirstValue(nonNullValue);
								break;
							} catch (MainTypeException e) {
								logger.warn("Value "+nonNullValue.toString()+" could not be "
										+ "successful converted to " + accumulator.getState().getMainType() +".", e);
							}
						}
						nonNullIndex++;
					}

					if(nonNullValue == null) {
						// could not pull a useful value out of this record, skip it
						accumulator = null;
						continue;
					}

					if(accumulator instanceof BinaryProfileAccumulator) {
						((BinaryProfileAccumulator)accumulator)
							.setMediaType(Profile.getBinaryDetail(currentProfile).getMimeType());
					}

					Interpretation interpretation = currentProfile.getInterpretation();
					if(!Interpretation.isUnknown(interpretation)) {
						logger.info("Interpretting field: " + mergedAccumulatorKey + " as " + interpretation.getiName());
					}
					accumulator.getState().setInterpretation(interpretation);
					accumulator.getState().setInterpretations(Arrays.asList(interpretation));

					for(int i = 0; i < values.size(); i++) {
						try {
							if(i == nonNullIndex) {
								// skip the non null value that was used to initialized the list, and add all others (even nulls)
								continue;
							}
							Object value = accumulator.getState().getMainTypeClass().createAppropriateObject(values.get(i));
							accumulator.accumulate(value, false);
						} catch (MainTypeException e) {
							logger.warn(e.getMessage());
						}
					}

					fieldMapping.put(mergedAccumulatorKey, accumulator);
				} else {
					accumulator = fieldMapping.get(mergedAccumulatorKey);
					if(!values.isEmpty()) {
						boolean accumulatePresence = true;
						if(accumulator.getDetail() != null) {
							for(int i = 0; i < values.size(); i++) {
								try {
									Object value = accumulator.getState().getMainTypeClass().createAppropriateObject(values.get(i));
									accumulator.accumulate(value, accumulatePresence);
									accumulatePresence = false;
								} catch (MainTypeException e) {
									logger.warn(e.getMessage());
									accumulatePresence = true;
								}
							}
						} else {
							int i = 0;
							for(; i < values.size(); i++) {
								try {
									Object value = accumulator.getState().getMainTypeClass().createAppropriateObject(values.get(i));
									accumulator.initFirstValue(value);
									accumulatePresence = false;
									break;
								} catch (MainTypeException e) {
									logger.warn("Error initializing accumulator.  " + e.getMessage());
								}
							}
							for(; i < values.size(); i++) {
								try {
									Object value = accumulator.getState().getMainTypeClass().createAppropriateObject(values.get(i));
									accumulator.accumulate(value, accumulatePresence);
								} catch (MainTypeException e) {
									logger.warn(e.getMessage());
								}
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
		return;
	}

	public List<DataSampleMetaData> getDataSampleMetaDataList() {
		return dataSampleMetaDataList;
	}

	public void initExistingSchema(Schema existingSchema) {
		if(existingSchema != null) {
			logger.info("Existing schema detected.  Using existing fields as seed values.");
			for(String existingField : existingSchema.getsProfile().keySet()) {
				existingSchema.getsProfile().get(existingField).setUsedInSchema(true);
				aliasMapping.put(existingField, existingSchema.getsProfile().get(existingField).getAliasNames());
			}
			this.recordsParsed = existingSchema.getRecordsParsedCount();

			userCreatedFields = new HashMap<String, Profile>();
			initializeWithProfile(existingSchema.getsProfile(), true);

			outputCurrentSampleMergedfieldsMapping(existingSchema.getsName(), currentSampleMergedFieldsMapping);

			Map<String, Profile> adjustedProfileWithMergedKeys = copyProfileWithMergedKeyValues(existingSchema.getsProfile());
			geocodingProfiler.initializeWithMap(adjustedProfileWithMergedKeys);

			if(existingSchema.getsProfile().keySet().isEmpty()) {
				logger.warn("Empty existing schema pass.  Defaulting to no existing schema behavior.");
				this.existingSchema = null;
			} else {
				this.existingSchema = existingSchema;
			}
			if(existingSchema.getsDataSamples().isEmpty()) {
				logger.warn("Empty samples list in existing schema object.");
			} else {
				dataSampleMetaDataList = existingSchema.getsDataSamples();
				for(DataSampleMetaData dsmd : dataSampleMetaDataList) {
					logger.info("Existing schema contains sample: " + dsmd.getDsName() + ".");
				}
			}
		}
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

		logger.info("Profiling sample with GUID " + currentDataSample.getDsGuid());

		initializeWithProfile(currentDataSample.getDsProfile(), false);
		outputCurrentSampleMergedfieldsMapping(dataSample.getDsName(), currentSampleMergedFieldsMapping);

		Map<String, Profile> adjustedProfileWithMergedKeys = copyProfileWithMergedKeyValues(dataSample.getDsProfile()); //region data set here
		geocodingProfiler.initializeWithMap(adjustedProfileWithMergedKeys);
	}

	private void initializeWithProfile(Map<String, Profile> profileMap, boolean isExistingSchema) {
		currentSampleMergedFieldsMapping = new HashMap<String, String>();
		currentSkipKeyList = new ArrayList<String>();
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

			currentSampleMergedFieldsMapping.put(fieldNameInSample, mergedAccumulatorKey);

			if(!isExistingSchema) {
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
			} else {
				AbstractProfileAccumulator accumulator;
				if(profile.getMainType().equals("number")) {
					accumulator = new NumberProfileAccumulator(mergedAccumulatorKey);
				} else if(profile.getMainType().equals("string")) {
					accumulator = new StringProfileAccumulator(mergedAccumulatorKey);
				} else if(profile.getMainType().equals("binary")) {
					accumulator = new BinaryProfileAccumulator(mergedAccumulatorKey);
				} else {
					logger.error("Not detected as number, string, or binary in Schema Profiler!  Warning: the instantiation of accumulators"
							+ " in Schema Profiler use direct string equivalence to determine the appropriate accumulator.  Main type is " + profile.getMainType());
					continue;
				}
				if(profile.getAliasNames() == null || profile.getAliasNames().isEmpty()) {
					userCreatedFields.put(mergedAccumulatorKey, profile);
				} else {
					accumulator.initializeFromExistingProfile(profile);
					fieldMapping.put(mergedAccumulatorKey, accumulator);
				}
			}
		}
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
				Optional<Histogram> optionalHistogram = profile.getDetail().getHistogramOptional();
				if(optionalHistogram.isPresent()) {
					RegionData regionData = optionalHistogram.get().getRegionData();
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
						logger.error("Partial coordinate merge detected.  Rolling back merge.");
						profile.getDetail().getHistogramOptional().ifPresent(x->x.setRegionData(null));
						profile.setInterpretation(Interpretation.UNKNOWN);
						copyOfProfile.put(merged, profile);
						continue;
					}

					profile.getDetail().getHistogramOptional().ifPresent(x->x.setRegionData(regionData));
					copyOfProfile.put(merged, profile);

					partnerProfile.getDetail().getHistogramOptional().ifPresent(x->x.setRegionData(regionData));
					copyOfProfile.put(mergedPartnerKey, partnerProfile);
				}
			}
		}
		return copyOfProfile;
	}

	private void outputCurrentSampleMergedfieldsMapping(String name, Map<String, String> currentMergeMapping) {
		logger.info(name+" merged fields mapping: ");
		for(String key : currentMergeMapping.keySet()) {
			if(key.equals(currentMergeMapping.get(key))) {
				logger.info("Accumulating metrics for schema field \"" + currentMergeMapping.get(key) + "\".");
			} else {
				logger.info("Accumulating metrics for schema field \"" + currentMergeMapping.get(key) 
				+ "\" (original name - \"" + key + "\").");
			}
		}
	}

	@Override
	public Schema asBean() {
		Schema schema = (existingSchema == null) ? new Schema() : existingSchema;
		Map<String, Profile> sProfile = new HashMap<String, Profile>();
		for(String key: fieldMapping.keySet()) {
			AbstractProfileAccumulator accumulator = fieldMapping.get(key);
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
				int m = Math.max(existingNumDistinct, newNumDistinct);
				String numDistinctValues = String.valueOf(m); 
				profile.getDetail().setNumDistinctValues(">=" + numDistinctValues);
			}
			sProfile.put(key, profile);
		};
		if(userCreatedFields != null) {
			sProfile.putAll(userCreatedFields);
		}
		sProfile = DisplayNameHelper.determineDisplayNames(sProfile);
		schema.setsProfile(sProfile);
		schema.setRecordsParsedCount(recordsParsed);
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
