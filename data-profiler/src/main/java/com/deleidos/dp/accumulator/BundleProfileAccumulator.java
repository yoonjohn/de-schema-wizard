package com.deleidos.dp.accumulator;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.MainTypeException;

public class BundleProfileAccumulator implements Accumulator<List<AbstractProfileAccumulator>> {
	private static final Logger logger = Logger.getLogger(BundleProfileAccumulator.class);
	public static final int NUMBER_METRICS_INDEX = MainType.NUMBER.getIndex();
	public static final int STRING_METRICS_INDEX = MainType.STRING.getIndex();
	public static final int BINARY_METRICS_INDEX = MainType.BINARY.getIndex();
	private static final int METRICS_TYPES_COUNT = 3;
	private Tolerance toleranceLevel = Tolerance.STRICT;
	private String domainName;
	private float binaryPercentageCutoff = .3f;
	/**
	 * The array the keeps track of data type determinations.  This is used to make a final decision after the first pass.
	 */
	private int[] dataTypeTracker;
	private List<AbstractProfileAccumulator> metricsAccumulators;
	private Profile bestGuessProfile;
	private String fieldName;
	private boolean hasGeospatialData;

	public BundleProfileAccumulator(String fieldName, String domainName, Tolerance tolerance) {
		this.fieldName = fieldName;
		dataTypeTracker = new int[MainType.values().length];
		metricsAccumulators = new ArrayList<AbstractProfileAccumulator>(METRICS_TYPES_COUNT);
		setDomain(domainName);
		setToleranceLevel(tolerance);
		hasGeospatialData = false;
		metricsAccumulators.add(STRING_METRICS_INDEX, null);
		metricsAccumulators.add(NUMBER_METRICS_INDEX, null);
		metricsAccumulators.add(BINARY_METRICS_INDEX, null);
	}

	public static int getMetricsCount() {
		return METRICS_TYPES_COUNT;
	}

	public void nullifyMetricsAccumulator(int index) {
		metricsAccumulators.set(index, null);
	}

	public List<AbstractProfileAccumulator> getProfiles() {
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
		return METRICS_TYPES_COUNT;
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
	private Profile determineBestGuessProfile() {
		MainType type = MetricsCalculationsFacade.getDataTypeFromDistribution(dataTypeTracker, toleranceLevel);
		if(type == null) {
			return null;
		}
		Profile profile;
		AbstractProfileAccumulator accumulator;
		switch (type) {
		case STRING: 
			accumulator = getStringProfileAccumulator();
			nullifyMetricsAccumulator(NUMBER_METRICS_INDEX);
			nullifyMetricsAccumulator(BINARY_METRICS_INDEX);
			break;
		case NUMBER: 
			boolean precisionErrors = hasPrecisionErrors(getNumberProfileAccumulator());
			accumulator = (precisionErrors) ? getStringProfileAccumulator() : getNumberProfileAccumulator();
			nullifyMetricsAccumulator((precisionErrors) ? NUMBER_METRICS_INDEX : STRING_METRICS_INDEX);
			nullifyMetricsAccumulator(BINARY_METRICS_INDEX);
			break;
		case BINARY: 
			accumulator = getBinaryProfileAccumulator();
			nullifyMetricsAccumulator(NUMBER_METRICS_INDEX);
			nullifyMetricsAccumulator(STRING_METRICS_INDEX);
			break;
		default: 
			logger.error("Not determined to be number, string, or binary.");
			return null;
		}
		profile = accumulator.getState();
		hasGeospatialData = accumulator.hasGeoSpatialData();
		return profile;
	}

	private boolean hasPrecisionErrors(NumberProfileAccumulator npa) {
		NumberDetail nd = Profile.getNumberDetail(npa.getState());
		if(Double.isNaN(nd.getStdDev())) {
			logger.error("Standard Deviation not successfully calculated due to precision errors.  Treating as string value.");
			return true;
		}
		return false;
	}

	public Profile getBestGuessProfile() {
		if(bestGuessProfile != null) {
			return bestGuessProfile;
		} else {
			finish();
			return bestGuessProfile;
		}
	}

	public void accumulate(Object value) {
		accumulate(value, true);
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
	public void accumulate(Object value, boolean accumulatePresence) {
		if(value == null) {
			return;
		}
		List<MainType> possibleTypes = MetricsCalculationsFacade.determineProbableDataTypes(value, binaryPercentageCutoff);

		//order is significant here
		if(possibleTypes.contains(MainType.NUMBER)) {
			dataTypeTracker[MainType.NUMBER.getIndex()]++;
		} else if(possibleTypes.contains(MainType.BINARY)) {
			dataTypeTracker[MainType.BINARY.getIndex()]++;
		} else {
			dataTypeTracker[MainType.STRING.getIndex()]++;
		}
		for(MainType possibleType : possibleTypes) {
			try {
				Object typeSensitiveValue = possibleType.createAppropriateObject(value);
				int index = possibleType.getIndex();
				AbstractProfileAccumulator a = metricsAccumulators.get(index);
				if(a != null) {
					a.accumulate(typeSensitiveValue, accumulatePresence);
				} else if(index == NUMBER_METRICS_INDEX) {
					a = new NumberProfileAccumulator(fieldName, typeSensitiveValue);
					metricsAccumulators.set(NUMBER_METRICS_INDEX, a);
				} else if(index == STRING_METRICS_INDEX) {
					a = new StringProfileAccumulator(fieldName, typeSensitiveValue);
					metricsAccumulators.set(STRING_METRICS_INDEX, a);
				} else if(index == BINARY_METRICS_INDEX) {
					a = new BinaryProfileAccumulator(fieldName, typeSensitiveValue);
					metricsAccumulators.set(BINARY_METRICS_INDEX, a);
				}
				continue;
			} catch (MainTypeException e) {
				logger.warn(e);
			}
		}
	}

	@Override
	public List<AbstractProfileAccumulator> getState() {
		return metricsAccumulators;
	}

	@Override
	public void finish() {
		for(AbstractProfileAccumulator apa : getState()) {
			if(apa != null) {
				apa.finish();
			}
		}
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

	@Override
	public boolean initFirstValue(Object value) {
		return false;
	}

	public static Profile generateProfile(String fieldName, List<Object> exampleValues) throws MainTypeException {
		//copy them over so the array supports removal
		List<Object> copy = new ArrayList<Object>(exampleValues);
		copy.removeIf(p -> p == null || p.toString().isEmpty());
		if(copy.isEmpty()) {
			throw new MainTypeException("No values provided.  Main type cannot be determined.");
		}
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator(fieldName, null, Tolerance.STRICT);
		for(Object value : copy) {
			bundleAccumulator.accumulate(value);
		}
		return bundleAccumulator.getBestGuessProfile();
	}

}
