package com.deleidos.dp.accumulator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.profiler.BinaryProfilerRecord;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class BundleProfileAccumulator implements Accumulator<List<AbstractProfileAccumulator>> {
	private static final Logger logger = Logger.getLogger(BundleProfileAccumulator.class);
	public static final int NUMBER_METRICS_INDEX = MainType.NUMBER.getIndex();
	public static final int STRING_METRICS_INDEX = MainType.STRING.getIndex();
	public static final int BINARY_METRICS_INDEX = MainType.BINARY.getIndex();
	private static final int METRICS_TYPES_COUNT = 3;
	private Tolerance toleranceLevel = Tolerance.STRICT;
	private float binaryPercentageCutoff = .3f;
	/**
	 * The array the keeps track of data type determinations.  This is used to make a final decision after the first pass.
	 */
	private int[] dataTypeTracker;
	private List<AbstractProfileAccumulator> metricsAccumulators;
	private AbstractProfileAccumulator bestGuessAccumulator;
	private String fieldName;
	private boolean hasGeospatialData;

	public BundleProfileAccumulator(String fieldName, Tolerance tolerance) {
		this.fieldName = fieldName;
		dataTypeTracker = new int[MainType.values().length];
		metricsAccumulators = new ArrayList<AbstractProfileAccumulator>(METRICS_TYPES_COUNT);
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

	public void setMetrics(ArrayList<AbstractProfileAccumulator> metrics) {
		this.metricsAccumulators = metrics;
	}

	public static Optional<StringProfileAccumulator> getStringProfileAccumulator(List<AbstractProfileAccumulator> metricsAccumulators) {
		return Optional.ofNullable((StringProfileAccumulator) metricsAccumulators.get(STRING_METRICS_INDEX));
	}

	public static Optional<NumberProfileAccumulator> getNumberProfileAccumulator(List<AbstractProfileAccumulator> metricsAccumulators) {
		return Optional.ofNullable((NumberProfileAccumulator) metricsAccumulators.get(NUMBER_METRICS_INDEX));
	}

	public static Optional<BinaryProfileAccumulator> getBinaryProfileAccumulator(List<AbstractProfileAccumulator> metricsAccumulators) {
		return Optional.ofNullable((BinaryProfileAccumulator) metricsAccumulators.get(BINARY_METRICS_INDEX));
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

	/*public AbstractProfileAccumulator getBestGuessAccumulator() {
		if(bestGuessAccumulator == null) {
			bestGuessAccumulator = determineBestGuessAccumulator(metricsAccumulators, dataTypeTracker, toleranceLevel);
		}
		return bestGuessAccumulator;
	}*/

	private static AbstractProfileAccumulator determineBestGuessAccumulator(
			List<AbstractProfileAccumulator> accumulators, int[] dataTypeTracker, Tolerance toleranceLevel) 
					throws MainTypeException {
		MainType type = MetricsCalculationsFacade.getDataTypeFromDistribution(dataTypeTracker, toleranceLevel);
		if(type == null) {
			throw new MainTypeException("Could not determine data type from distribution.");
		}
		switch (type) {
		case STRING: 
			return getStringProfileAccumulator(accumulators)
					.orElseThrow(()->new MainTypeException("Type detected as string but string accumulator is null."));
		case NUMBER: 
			NumberProfileAccumulator npa = getNumberProfileAccumulator(accumulators)
			.orElseThrow(()->new MainTypeException("Type detected as number but number accumulator is null."));
			boolean precisionErrors = hasPrecisionErrors(npa);
			return (precisionErrors) ? getStringProfileAccumulator(accumulators)
					.orElseThrow(()->new MainTypeException("Tried to roll back to string but string accumulator is null."))
					: npa;
		case BINARY: 
			return getBinaryProfileAccumulator(accumulators)
					.orElseThrow(()->new MainTypeException("Type detected as binary but binary accumulator is null."));
		default: 
			logger.error("Not determined to be number, string, or binary.");
			throw new MainTypeException("Not determined to be number, string, or binary.");
		}
	}

	/**
	 * Use the instance's dataTypeTracker to determine the type of the field and return the appropriate metric.  
	 * Takes in an acceptable error level.  Once the data type is determined, incompatible (non-castable) 
	 * data types will be dropped during ingest.
	 * @return The appropriate metrics for the given field.
	 */
	public static Profile getBestGuessProfile(BundleProfileAccumulator bundleAccumulator, int numRecords) throws MainTypeException {
		AbstractProfileAccumulator bestAccumulator = 
				determineBestGuessAccumulator(bundleAccumulator.finish()
						, bundleAccumulator.dataTypeTracker, bundleAccumulator.toleranceLevel);
		bestAccumulator.getState().setPresence((float)bestAccumulator.getPresenceCount()/(float)numRecords);
		bundleAccumulator.hasGeospatialData = bestAccumulator.hasGeoSpatialData();
		return bestAccumulator.getState();
	}

	public Optional<Profile> getBestGuessProfile(int numRecords) {
		try {
			return Optional.of(BundleProfileAccumulator.getBestGuessProfile(this, numRecords));
		} catch (MainTypeException e) {
			logger.warn(e);
			return Optional.empty();
		}
	}

	private static boolean hasPrecisionErrors(NumberProfileAccumulator npa) {
		NumberDetail nd = Profile.getNumberDetail(npa.getState());
		if(Double.isNaN(nd.getStdDev())) {
			logger.error("Standard Deviation not successfully calculated due to precision errors.  Treating as string value.");
			return true;
		}
		return false;
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
	public List<AbstractProfileAccumulator> accumulate(Object value, boolean accumulatePresence) {
		if(value == null) {
			return getState();
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
				int index = possibleType.getIndex();
				AbstractProfileAccumulator a = metricsAccumulators.get(index);
				if(a != null) {
					a.accumulate(value, accumulatePresence);
				} else {
					a = AbstractProfileAccumulator.generateProfileAccumulator(fieldName, possibleType);
					a.accumulate(value, true);
					int profilerIndex = AbstractProfileAccumulator.associatedMainType(a).getIndex();
					metricsAccumulators.set(profilerIndex, a);
				}
			} catch (MainTypeException e) {
				logger.warn(e);
			}
		}
		return getState();
	}

	@Override
	public List<AbstractProfileAccumulator> getState() {
		return metricsAccumulators;
	}

	@Override
	public List<AbstractProfileAccumulator> finish() throws MainTypeException  {
		for(AbstractProfileAccumulator apa : getState()) {
			if(apa != null) {
				apa.finish();
			}
		}
		return getState();
	}

	public Tolerance getTolerance() {
		return toleranceLevel;
	}

	public void setTolerance(Tolerance acceptableErrorLevel) {
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

	public static Profile generateProfile(String fieldName, List<Object> exampleValues) throws MainTypeException {
		Profile first = generateFirstPassProfile(fieldName, exampleValues);
		List<Object> profileExampleValues = first.getExampleValues();
		Profile second = generateSecondPassProfile(fieldName, exampleValues, first);
		second.setExampleValues(profileExampleValues);
		return second;
	}
	
	public static Profile generateBinaryProfile(String fieldName, DetailType detailType, List<ByteBuffer> buffers) throws MainTypeException, DataAccessException{
		List<ProfilerRecord> binRecords = new ArrayList<ProfilerRecord>();
		buffers.forEach(buffer->binRecords.add(new BinaryProfilerRecord(fieldName, detailType, buffer)));
		DataSample sample = SampleProfiler.generateDataSampleFromProfilerRecords(null, Tolerance.STRICT, binRecords);
		return sample.getDsProfile().get(fieldName);
	}

	public static Profile generateFirstPassProfile(String fieldName, List<Object> exampleValues) throws MainTypeException {	
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator(fieldName, Tolerance.STRICT);
		exampleValues.forEach(value->bundleAccumulator.accumulate(value));
		return BundleProfileAccumulator.getBestGuessProfile(bundleAccumulator, exampleValues.size());
	}

	public static Profile generateSecondPassProfile(String fieldName, List<Object> exampleValues, Profile firstPassProfile) throws MainTypeException {
		AbstractProfileAccumulator apa = AbstractProfileAccumulator.generateProfileAccumulator(fieldName, firstPassProfile);
		for(Object value : exampleValues) {
			try {
				apa.accumulate(value, true);
			} catch (MainTypeException e) {
				logger.warn(e);
			}
		}
		apa.finish();
		return apa.getState();
	}

}
