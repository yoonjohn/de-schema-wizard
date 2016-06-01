package com.deleidos.dp.accumulator;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;

public class BundleProfileAccumulator implements Accumulator<ArrayList<AbstractProfileAccumulator>> {
	// TODO remove domain field since it is important only at the profiler level
	private static final Logger logger = Logger.getLogger(BundleProfileAccumulator.class);
	public static final int NUMBER_METRICS_INDEX = MainType.NUMBER.getIndex();
	public static final int STRING_METRICS_INDEX = MainType.STRING.getIndex();
	public static final int BINARY_METRICS_INDEX = MainType.BINARY.getIndex();
	private static final int metricsCount = 3;
	private boolean accumulatePresence;
	private Tolerance toleranceLevel = Tolerance.STRICT;
	private String domainName;
	private float binaryPercentageCutoff = .3f;
	/**
	 * The array the keeps track of data type determinations.  This is used to make a final decision after the first pass.
	 */
	public int[] dataTypeTracker;
	private ArrayList<AbstractProfileAccumulator> metricsAccumulators;
	private Profile bestGuessProfile;
	private String fieldName;
	private boolean hasGeospatialData;
	
	public BundleProfileAccumulator(String fieldName, String domainName, Tolerance tolerance) {
		this.fieldName = fieldName;
		dataTypeTracker = new int[MainType.values().length];
		metricsAccumulators = new ArrayList<AbstractProfileAccumulator>(metricsCount);
		setDomain(domainName);
		setToleranceLevel(tolerance);
		hasGeospatialData = false;
		metricsAccumulators.add(STRING_METRICS_INDEX, null);
		metricsAccumulators.add(NUMBER_METRICS_INDEX, null);
		metricsAccumulators.add(BINARY_METRICS_INDEX, null);
	}

	public static int getMetricsCount() {
		return metricsCount;
	}

	public void nullifyMetricsAccumulator(int index) {
		metricsAccumulators.set(index, null);
	}

	public ArrayList<AbstractProfileAccumulator> getProfiles() {
		return metricsAccumulators;
	}

	public void setMetrics(ArrayList<AbstractProfileAccumulator> metrics) {
		this.metricsAccumulators = metrics;
	}
	
	private StringProfileAccumulator getStringProfileAccumulator() {
		return (StringProfileAccumulator) metricsAccumulators.get(STRING_METRICS_INDEX);
	}
	
	private NumberProfileAccumulator getNumberProfileAccumulator() {
		return (NumberProfileAccumulator) metricsAccumulators.get(NUMBER_METRICS_INDEX);
	}
	
	private BinaryProfileAccumulator getBinaryProfileAccumulator() {
		return (BinaryProfileAccumulator) metricsAccumulators.get(BINARY_METRICS_INDEX);
	}

	public static int getMetricscount() {
		return metricsCount;
	}

	public boolean hasGeospatialData() {
		return hasGeospatialData;
	}

	public void setHasGeospatialData(boolean hasGeospatialData) {
		this.hasGeospatialData = hasGeospatialData;
	}

	/**
	 * Use the instance's dataTypeTracker to determine the type of the field and return the appropriate metric.  
	 * Takes in an acceptable error level.  Once the data type is determined, incompatible (non-castable) 
	 * data types will be dropped during ingest.
	 * @return The appropriate metrics for the given field.
	 */
	public Profile determineBestGuessProfile() {
		MainType type = MetricsCalculationsFacade.getDataTypeFromDistribution(dataTypeTracker, toleranceLevel);
		if(type == null) {
			return null;
		}
		Profile profile;
		switch (type) {
		case STRING: {
			getStringProfileAccumulator().finish();
			profile = getStringProfileAccumulator().getState();
			hasGeospatialData = getStringProfileAccumulator().hasGeoSpatialData();
			nullifyMetricsAccumulator(NUMBER_METRICS_INDEX);
			nullifyMetricsAccumulator(BINARY_METRICS_INDEX);
			break;
		}
		case NUMBER: {
			getNumberProfileAccumulator().finish();
			profile = getNumberProfileAccumulator().getState();
			hasGeospatialData = getNumberProfileAccumulator().hasGeoSpatialData();
			profile.setMainType(MainType.NUMBER.toString());
			NumberDetail nd = Profile.getNumberDetail(profile);
			if(Double.isNaN(nd.getStdDev())) {
				logger.info(getNumberProfileAccumulator().getFieldName());
				logger.info(nd.getWalkingSquareSum());
				logger.info(nd.getWalkingSum());
				logger.info(nd.getAverage());
				logger.info(nd.getWalkingCount());
				logger.error("Standard Deviation not successfully calculated due to precision errors.  Treating as string value.");
				getStringProfileAccumulator().finish();			
				hasGeospatialData = getStringProfileAccumulator().hasGeoSpatialData();
				profile = getStringProfileAccumulator().getState();
				nullifyMetricsAccumulator(NUMBER_METRICS_INDEX);
				nullifyMetricsAccumulator(BINARY_METRICS_INDEX);
				break;
			}
			nullifyMetricsAccumulator(STRING_METRICS_INDEX);
			nullifyMetricsAccumulator(BINARY_METRICS_INDEX);
			break;
		}
		case BINARY: {
			getBinaryProfileAccumulator().finish();
			hasGeospatialData = getBinaryProfileAccumulator().hasGeoSpatialData();
			profile = getBinaryProfileAccumulator().getState();
			nullifyMetricsAccumulator(NUMBER_METRICS_INDEX);
			nullifyMetricsAccumulator(STRING_METRICS_INDEX);
			break;
		}
		default: {
			logger.error("Not determined to be number, string, or binary.");
			return null;
		}
		}
		return profile;
	}

	public Profile getBestGuessProfile() {
		finish();
		return bestGuessProfile;
	}

	/**
	 * Determine the possible data types and accumulate their metrics.  However, only one index in the dataTypeTracker
	 * will be incremented (intending to be the "most probable" type).  The sets of possible types can either be a 
	 * (number and string) or a (binary).  According to the logic mentioned in the class description, an object
	 * detected to be a number should be considered a number rather than a string.  Therefore, only the number index
	 * in the dataTypeTracker will be incremented if both a number and string are detected as possible.  This 
	 * incrementing is important in the final determination of type in MetricsBrain.determineProbableDataTypes(). 
	 */
	@Override
	public boolean accumulate(Object value) {
		if(value == null) {
			return false;
		}
		List<MainType> possibleTypes = MetricsCalculationsFacade.determineProbableDataTypes(value, binaryPercentageCutoff);
		try {
			//order is significant here
			if(possibleTypes.contains(MainType.NUMBER)) {
				dataTypeTracker[MainType.NUMBER.getIndex()]++;
			} else if(possibleTypes.contains(MainType.BINARY)) {
				dataTypeTracker[MainType.BINARY.getIndex()]++;
			} else {
				dataTypeTracker[MainType.STRING.getIndex()]++;
			}
			for(MainType possibleType : possibleTypes) {
				int index = possibleType.getIndex();
				AbstractProfileAccumulator a = metricsAccumulators.get(index);
				if(a != null) {
					a.accumulate(value, accumulatePresence);
				} else {
					if(index == NUMBER_METRICS_INDEX) {
						AbstractProfileAccumulator apa = new NumberProfileAccumulator(fieldName, value);
						metricsAccumulators.set(NUMBER_METRICS_INDEX, apa);
						break;
					}
					else if(index == STRING_METRICS_INDEX) {
						AbstractProfileAccumulator apa = new StringProfileAccumulator(fieldName, value);
						metricsAccumulators.set(STRING_METRICS_INDEX, apa);
						break;
					}
					else if(index == BINARY_METRICS_INDEX) {
						AbstractProfileAccumulator apa = new BinaryProfileAccumulator(fieldName, value);
						metricsAccumulators.set(BINARY_METRICS_INDEX, apa);
						break;
					}
				}
			}
		} catch(Exception e) {
			logger.error("Error accumulating " + value);
			logger.error(e);
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<AbstractProfileAccumulator> getState() {
		return metricsAccumulators;
	}

	@Override
	public void finish() {
		bestGuessProfile = determineBestGuessProfile();
	}

	public Tolerance getAcceptableErrorLevel() {
		return toleranceLevel;
	}

	public void setAcceptableErrorLevel(Tolerance acceptableErrorLevel) {
		this.toleranceLevel = acceptableErrorLevel;
	}

	public float getBinaryPercentageCutoff() {
		return binaryPercentageCutoff;
	}

	public void setBinaryPercentageCutoff(float binaryPercentageCutoff) {
		this.binaryPercentageCutoff = binaryPercentageCutoff;
	}

	public Tolerance getToleranceLevel() {
		return toleranceLevel;
	}

	public void setToleranceLevel(Tolerance toleranceLevel) {
		this.toleranceLevel = toleranceLevel;
	}

	public String getDomain() {
		return domainName;
	}

	public void setDomain(String domainName) {
		this.domainName = domainName;
	}

	public boolean isAccumulatePresence() {
		return accumulatePresence;
	}

	public void setAccumulatePresence(boolean accumulatePresence) {
		this.accumulatePresence = accumulatePresence;
	}

	@Override
	public boolean initFirstValue(Object value) {
		return false;
	}



}
