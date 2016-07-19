package com.deleidos.dp.profiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Domain;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.api.Profiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateListener;

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
	public static final String EMPTY_FIELD_NAME = "(Blank Field Name)";
	private boolean hasCalledInterpretationEngine;
	private GroupingBehavior groupingBehavior = GroupingBehavior.GROUP_ARRAY_VALUES;
	private ProfilingProgressUpdateListener progressUpdateListener;
	private static Logger logger = Logger.getLogger(SampleProfiler.class);
	private String domainName;
	private Tolerance tolerance;
	private int numGeoSpatialQueries = 0;
	private int recordsParsed;
	protected Map<String, BundleProfileAccumulator> fieldMapping;

	public SampleProfiler(String domainGuid, Tolerance tolerance) {
		hasCalledInterpretationEngine = false;
		setDomainName(domainGuid);
		setTolerance(tolerance);
		fieldMapping = new LinkedHashMap<String, BundleProfileAccumulator>();
		recordsParsed = 0;
	}

	@Override
	public void load(ProfilerRecord record) {
		boolean isBinary = record instanceof BinaryProfilerRecord;
		Map<String, List<Object>> normalizedMapping = record.normalizeRecord(groupingBehavior);
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
				bundleAccumulator = new BundleProfileAccumulator(accumulatorKey, domainName, tolerance);
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
		} 
		if(progressUpdateListener != null) {
			progressUpdateListener.handleProgressUpdate(record.recordProgressWeight());
		}
		return;
	}

	/**
	 * Get a copy of the appropriate metrics that the loader has created for the given key.
	 * @param key Any key in the data model object.
	 * @return The metrics for the given key.
	 */
	public Profile getProfile(String key) {
		return fieldMapping.get(key).getBestGuessProfile();
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
		Map<String, Profile> dsProfile = new LinkedHashMap<String, Profile>();

		Set<String> accKeys = fieldMapping.keySet();
		for(String accKey : accKeys) {
			for(AbstractProfileAccumulator bpa : fieldMapping.get(accKey).getState()) {
				if(bpa != null) {
					float presence = ((float)bpa.getPresenceCount())/((float)recordsParsed);
					bpa.getState().setPresence(presence);
				} 
			}
			Profile profile = fieldMapping.get(accKey).getBestGuessProfile();
			if(profile == null) {
				logger.warn("Field \'" + accKey + "\' did not have any values.  Dropping.");
				continue;
			}

			accKey = accKey.isEmpty() ? EMPTY_FIELD_NAME : accKey;
			dsProfile.put(accKey, profile);
		}

		if(!hasCalledInterpretationEngine) {
			try {
				dsProfile = InterpretationEngineFacade.getInstance().interpret(domainName, dsProfile);
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

		numGeoSpatialQueries = Math.min(numLats, numLngs);

		dsProfile = DisplayNameHelper.determineDisplayNames(dsProfile);
		dataSample.setDsProfile(dsProfile);

		return dataSample;
	}

	public String getDomainGuid() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public Tolerance getTolerance() {
		return tolerance;
	}

	public void setTolerance(Tolerance tolerance) {
		this.tolerance = tolerance;
	}

	public int getRecordsParsed() {
		return recordsParsed;
	}

	public int getNumGeoSpatialQueries() {
		return numGeoSpatialQueries;
	}

	public void setNumGeoSpatialQueries(int numGeoSpatialQueries) {
		this.numGeoSpatialQueries = numGeoSpatialQueries;
	}

	public ProfilingProgressUpdateListener getProgressUpdateListener() {
		return progressUpdateListener;
	}

	public void setProgressUpdateListener(ProfilingProgressUpdateListener progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}


}
