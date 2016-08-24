package com.deleidos.dp.profiler;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;

/**
 * Profiler class for sample data sets.  Takes in objects and loads them into a BundleAccumulator.  Every object key
 * has an associated BundleAccumulator.  This accumulator is a group of all three metrics types 
 * (number, string, binary) that will push the object's values into them as long as they are able to be parsed into that
 * form.
 * 
 * @author leegc
 *
 */
public class SampleProfiler implements Profiler {
	private ProfilingProgressUpdateHandler progressUpdateListener;
	private static Logger logger = Logger.getLogger(SampleProfiler.class);
	//private String domainName;
	private Tolerance tolerance;
	//private int numGeoSpatialQueries = 0;
	private int recordsParsed;
	protected Map<String, BundleProfileAccumulator> fieldMapping;

	public SampleProfiler(Tolerance tolerance) {
		setTolerance(tolerance);
		fieldMapping = new LinkedHashMap<String, BundleProfileAccumulator>();
		recordsParsed = 0;
	}

	@Override
	public int load(ProfilerRecord record) {
		boolean isBinary = record instanceof BinaryProfilerRecord;
		Map<String, List<Object>> normalizedMapping = record.normalizeRecord();
		for(String key : normalizedMapping.keySet()) {
			if(normalizedMapping.get(key) == null) {
				continue;
			}
			String accumulatorKey = key;
			BundleProfileAccumulator bundleAccumulator;
			List<Object> values = normalizedMapping.get(key);

			if(fieldMapping.containsKey(accumulatorKey)) { 
				bundleAccumulator = fieldMapping.get(accumulatorKey);
			} else {
				bundleAccumulator = new BundleProfileAccumulator(accumulatorKey, tolerance);
				fieldMapping.put(accumulatorKey, bundleAccumulator);
			}
			if(!values.isEmpty()) {
				bundleAccumulator.accumulate(values.get(0), !isBinary);
				for(int i = 1; i < values.size(); i++) {
					bundleAccumulator.accumulate(values.get(i), false);
				}
			}
			
		}
		if(!isBinary) {
			// do not increment records parsed count for binary objects
			recordsParsed++;
		} else {
			// if the record binary, use get the detail type from the profiler record
			// this is a special case because we need Tika (in dmf) to determine the detail type
			// for binary
			BinaryProfilerRecord binaryRecord = (BinaryProfilerRecord) record;
			if(fieldMapping.containsKey(binaryRecord.getBinaryName())) {
				BundleProfileAccumulator bundleAccumulator = fieldMapping.get(binaryRecord.getBinaryName());
				BundleProfileAccumulator.getBinaryProfileAccumulator(bundleAccumulator.getState()).ifPresent(
					binAccumulator->{
						binAccumulator.getDetailTypeTracker()
						[binaryRecord.getDetailType().getIndex()]++;
					});
			}
		}

		return recordsParsed;
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public BundleProfileAccumulator getMetricsBundle(String key) {
		return fieldMapping.get(key);
	}

	public Set<String> keySet() {
		return fieldMapping.keySet();
	}

	@Override
	public DataSample asBean() {
		DataSample dataSample = new DataSample();
		dataSample.setRecordsParsedCount(recordsParsed);
		final Map<String, Profile> dsProfile = new LinkedHashMap<String, Profile>();

		// put any recognizable profiles in the dsProfile map
		fieldMapping.forEach((k,v)-> 
			v.getBestGuessProfile(getRecordsParsed()).ifPresent(profile->dsProfile.put(k, profile)));

		/*if(!hasCalledInterpretationEngine && domainName != null) {
			try {
				dsProfile.putAll(InterpretationEngineFacade.getInstance().interpret(domainName, dsProfile));
			} catch (DataAccessException e) {
				logger.error("Could not interpret data sample.");
				logger.error(e);
			}
			hasCalledInterpretationEngine = true;
		}

		int numLats = 0;
		int numLngs = 0;

		for(String key : dsProfile.keySet()) {
			Profile profile = dsProfile.get(key);
			if(Interpretation.isLatitude(profile.getInterpretation())) {
				numLats += profile.getDetail().getWalkingCount().intValue();
			} else if(Interpretation.isLongitude(profile.getInterpretation())) {
				numLngs += profile.getDetail().getWalkingCount().intValue();
			}
		}

		numGeoSpatialQueries = Math.min(numLats, numLngs);*/

		dsProfile.putAll(DisplayNameHelper.determineDisplayNames(dsProfile));
		dataSample.setDsProfile(dsProfile);

		return dataSample;
	}

	/*public String getDomainGuid() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}*/

	public Tolerance getTolerance() {
		return tolerance;
	}

	public void setTolerance(Tolerance tolerance) {
		this.tolerance = tolerance;
	}

	public int getRecordsParsed() {
		return recordsParsed;
	}

	/*public int getNumGeoSpatialQueries() {
		return numGeoSpatialQueries;
	}

	public void setNumGeoSpatialQueries(int numGeoSpatialQueries) {
		this.numGeoSpatialQueries = numGeoSpatialQueries;
	}*/

	public ProfilingProgressUpdateHandler getProgressUpdateListener() {
		return progressUpdateListener;
	}

	public void setProgressUpdateListener(ProfilingProgressUpdateHandler progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}

	/**
	 * Convenience method for generating a data sample based on profiler records.
	 * @param records
	 * @return
	 * @throws DataAccessException 
	 * @throws MainTypeException 
	 */
	public static DataSample generateDataSampleFromProfilerRecords(String domain, Tolerance tolerance, List<ProfilerRecord> records) throws DataAccessException {
		SampleProfiler sampleProfiler = new SampleProfiler(tolerance);
		records.forEach(record->sampleProfiler.load(record));
		DataSample bean = sampleProfiler.asBean();
		InterpretationEngineFacade.interpretInline(bean, domain, null);
		SampleSecondPassProfiler secondPassProfiler = new SampleSecondPassProfiler(bean);
		records.forEach(record->secondPassProfiler.load(record));
		DataSample sample = secondPassProfiler.asBean();
		sample.setDsName(UUID.randomUUID().toString());
		sample.setDsGuid(sample.getDsName());
		sample.setDsLastUpdate(Timestamp.from(Instant.now()));
		return sample;
	}

}
