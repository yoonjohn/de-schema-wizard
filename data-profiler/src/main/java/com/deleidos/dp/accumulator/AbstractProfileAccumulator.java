package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;

public abstract class AbstractProfileAccumulator implements Accumulator<Profile> {
	protected enum Stage {
		UNITIALIZED, SAMPLE_AWAITING_FIRST_VALUE, SAMPLE_FIRST_PASS, SAMPLE_SECOND_PASS, SCHEMA_AWAITING_FIRST_VALUE, SCHEMA_PASS, FINISHED
	}
	public static final MathContext DEFAULT_CONTEXT = MathContext.DECIMAL128;
	private static Logger logger = Logger.getLogger(AbstractProfileAccumulator.class);
	protected String fieldName;
	protected int[] detailTypeTracker;
	protected List<Object> distinctValues;
	protected boolean accumulateHashes = true;
	private boolean hasGeoSpatialData = false;
	protected int presenceCount;
	protected Profile profile;
	private Stage accumulationStage;
	
	// fields for extremely precise progress updating 
	// transientWalkingCount is not persisted
	private int transientWalkingCount;
	private Optional<ProfilingProgressUpdateHandler> optionalCallback = Optional.empty();

	@Override
	public Profile getState() {
		return profile;
	}

	public AbstractProfileAccumulator(String key, MainType mainType) {
		fieldName = key;
		accumulationStage = Stage.UNITIALIZED;
		profile = new Profile();
		profile.setInterpretation(Interpretation.UNKNOWN);
		profile.setMainType(mainType.toString());
		detailTypeTracker = new int[DetailType.values().length]; 
		presenceCount = 0;
		distinctValues = new ArrayList<Object>();
	}
	
	protected void initializeDetailFields(String knownDetailType) {
		initializeDetailFields(knownDetailType, Stage.SAMPLE_AWAITING_FIRST_VALUE);
	}
	
	public boolean isEmpty() {
		return getState().getPresence() < 0;
	}

	@Override
	public Profile accumulate(Object object, boolean accumulatePresence) throws MainTypeException {
		presenceCount += (accumulatePresence) ? 1 : 0; 
		Object value = associatedMainType(this).createAppropriateObject(object);
		if(value == null) {
			return getState();
		}
		transientWalkingCount++;
		
		switch(accumulationStage) {
		case UNITIALIZED: throw new MainTypeRuntimeException("Accumulator called but has not been initialized.");
		case SAMPLE_FIRST_PASS: 
			return accumulate(accumulationStage, value);
		case SAMPLE_SECOND_PASS: 
			handleProgressUpdate(transientWalkingCount);
			return accumulate(accumulationStage, value);
		case SCHEMA_PASS: 
			handleProgressUpdate(transientWalkingCount);
			return accumulate(accumulationStage, value);
		case SAMPLE_AWAITING_FIRST_VALUE: { 
			initializeFirstValue(accumulationStage, value);
			accumulationStage = Stage.SAMPLE_FIRST_PASS;
			return accumulate(accumulationStage, value);
		}
		case SCHEMA_AWAITING_FIRST_VALUE: {
			initializeFirstValue(accumulationStage, value);
			accumulationStage = Stage.SCHEMA_PASS;
			return accumulate(accumulationStage, value);
		}
		default: throw new MainTypeRuntimeException("Accumulator stuck in unknown stage.");
		}
		
	}

	public void accumulateNumDistinctValues(Object object) {
		if(!distinctValues.contains(object)) {
			distinctValues.add(object);
		}
	}
	
	public void accumulateWalkingCount() {
		profile.getDetail().setWalkingCount(profile.getDetail().getWalkingCount().add(BigDecimal.ONE));
	}

	public boolean hasGeoSpatialData() {
		return hasGeoSpatialData;
	}

	public int getPresenceCount() {
		return presenceCount;
	}

	public void setPresenceCount(int presenceCount) {
		this.presenceCount = presenceCount;
	}

	public void setHasGeoSpatialData(boolean hasGeoSpatialData) {
		if(hasGeoSpatialData) {
			getState().getDetail().getHistogramOptional().ifPresent(x->x.setType("map"));
		}
		this.hasGeoSpatialData = hasGeoSpatialData;
	}

	public List<Object> getDistinctValuesAsExampleValuesList() {
		final int maxExampleListSize = 50;
		List<Object> exampleValues = getState().getExampleValues();
		if(distinctValues != null) {
			exampleValues = new ArrayList<Object>();
			//purposely lose precision
			int exampleListSize = (distinctValues.size() > maxExampleListSize) ? maxExampleListSize : distinctValues.size();
			float valueChooserRange = (float)distinctValues.size()/(float)exampleListSize;
			for(int i = 0; i < exampleListSize; i++) {
				exampleValues.add(distinctValues.get((int)valueChooserRange*i));
			}
			return exampleValues;
		} 
		if(exampleValues == null) {
			logger.error("Null distinct values object while trying to generate example values.");
			return exampleValues;
		} else {
			return exampleValues;
		}
	}

	public int[] getDetailTypeTracker() {
		return detailTypeTracker;
	}

	public void setDetailTypeTracker(int[] detailTypeTracker) {
		this.detailTypeTracker = detailTypeTracker;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public List<Object> getDistinctValues() {
		return distinctValues;
	}

	public void setDistinctValues(List<Object> distinctHashes) {
		this.distinctValues = distinctHashes;
	}

	public static NumberProfileAccumulator generateNumberProfileAccumulator(String key) throws MainTypeException {
		return (NumberProfileAccumulator)generateProfileAccumulator(key, MainType.NUMBER);
	}

	public static StringProfileAccumulator generateStringProfileAccumulator(String key) throws MainTypeException {
		return (StringProfileAccumulator)generateProfileAccumulator(key, MainType.STRING);
	}

	public static BinaryProfileAccumulator generateBinaryProfileAccumulator(String key) throws MainTypeException {
		return (BinaryProfileAccumulator)generateProfileAccumulator(key, MainType.BINARY);
	}

	public static AbstractProfileAccumulator generateProfileAccumulator(String key, MainType type) 
			throws MainTypeException {
		AbstractProfileAccumulator abstractProfileAccumulator = null;
		switch(type) {
		case STRING: abstractProfileAccumulator = new StringProfileAccumulator(key); break;
		case NUMBER: abstractProfileAccumulator = new NumberProfileAccumulator(key); break;
		case BINARY: abstractProfileAccumulator = new BinaryProfileAccumulator(key); break;
		case NULL: return null;
		default: throw new MainTypeException("Not number, string, or binary.");
		}
		abstractProfileAccumulator.initializeDetailFields(null);
		abstractProfileAccumulator.setAccumulationStage(Stage.SAMPLE_AWAITING_FIRST_VALUE);
		return abstractProfileAccumulator;
	}

	public static AbstractProfileAccumulator generateProfileAccumulator(String key, Profile profile)
			throws MainTypeException{
		MainType mainType = profile.getMainTypeClass();
		AbstractProfileAccumulator abstractProfileAccumulator = null;
		switch(mainType) {
		case STRING: abstractProfileAccumulator = new StringProfileAccumulator(key); break;
		case NUMBER: abstractProfileAccumulator = new NumberProfileAccumulator(key); break;
		case BINARY: abstractProfileAccumulator = new BinaryProfileAccumulator(key); break;
		case NULL: return null;
		default: throw new MainTypeException("Not number, string, or binary.");
		}
		abstractProfileAccumulator.accumulationStage = Stage.SAMPLE_SECOND_PASS;
		abstractProfileAccumulator.profile = profile;
		//abstractProfileAccumulator.expectedWalkingCount = profile.getDetail().getWalkingCount().intValue();
		abstractProfileAccumulator.initializeForSecondPassAccumulation(profile);
		return abstractProfileAccumulator;
	}

	public static AbstractProfileAccumulator generateProfileAccumulator(String key, Profile schemaProfile, int recordsInSchema, List<Profile> sampleProfiles)
			throws MainTypeException {
		AbstractProfileAccumulator abstractProfileAccumulator = null;
		if(sampleProfiles.size() == 0) {
			return null;
		} else {
			MainType mainType = (schemaProfile != null) ? schemaProfile.getMainTypeClass() : sampleProfiles.get(0).getMainTypeClass();
			switch(mainType) {
			case STRING: {
				abstractProfileAccumulator = new StringProfileAccumulator(key); 
				break;
			}
			case NUMBER: {
				abstractProfileAccumulator = new NumberProfileAccumulator(key); 
				break;
			}
			case BINARY: {
				abstractProfileAccumulator = new BinaryProfileAccumulator(key);
				break;
			}
			case NULL: return null;
			default: throw new MainTypeException("Not number, string, or binary.");
			}
			if(schemaProfile == null && sampleProfiles.size() < 1) {
				throw new MainTypeRuntimeException("No data samples or schema given to accumulator.");
			}
			int expectedTotalCount = 0;
			if(schemaProfile != null) {
				abstractProfileAccumulator.accumulationStage = Stage.SCHEMA_PASS;
				abstractProfileAccumulator.profile = schemaProfile;
				expectedTotalCount = schemaProfile.getDetail().getWalkingCount().intValue();
				abstractProfileAccumulator.setPresenceCount(
						(int)(recordsInSchema * abstractProfileAccumulator.profile.getPresence()));
			} else {
				abstractProfileAccumulator.profile = sampleProfiles.get(0);
				abstractProfileAccumulator.accumulationStage = Stage.SCHEMA_AWAITING_FIRST_VALUE;
			}
			for(Profile sampleProfile : sampleProfiles) {
				expectedTotalCount += sampleProfile.getDetail().getWalkingCount().intValue();
			}
			//abstractProfileAccumulator.expectedWalkingCount = expectedTotalCount;
			abstractProfileAccumulator = abstractProfileAccumulator.initializeForSchemaAccumulation(schemaProfile, recordsInSchema, sampleProfiles);
			if(schemaProfile == null) {
				abstractProfileAccumulator.initializeDetailFields(sampleProfiles.get(0).getDetail().getDetailType(), Stage.SCHEMA_AWAITING_FIRST_VALUE);
			}
			return abstractProfileAccumulator;
		}
	}

	@Override
	public Profile finish() throws MainTypeException {
		profile = finish(accumulationStage);

		if(profile.getDetail().getDetailType() == null) {
			String detailTypeString = MetricsCalculationsFacade.getDetailTypeFromDistribution(getState().getMainType(), getDetailTypeTracker()).toString();
			profile.getDetail().setDetailType(detailTypeString);
		}

		if (profile.getExampleValues() == null) {
			getState().setExampleValues(getDistinctValuesAsExampleValuesList());
		}
		
		if(accumulationStage.equals(Stage.FINISHED)) {
			return profile;
		} else {
			accumulationStage = Stage.FINISHED;
		}
		return profile;
	}

	public Stage getAccumulationStage() {
		return accumulationStage;
	}

	public void setAccumulationStage(Stage accumulationStage) {
		this.accumulationStage = accumulationStage;
	}

	public static MainType associatedMainType(AbstractProfileAccumulator accumulator) {
		if(accumulator instanceof NumberProfileAccumulator) {
			return MainType.NUMBER;
		} else if(accumulator instanceof StringProfileAccumulator) {
			return MainType.STRING;
		} else if(accumulator instanceof BinaryProfileAccumulator) {
			return MainType.BINARY;
		} else {
			return null;
		}
	}
	
	
	public void handleProgressUpdate(final int progress) {
		optionalCallback.ifPresent(x->optionalCallback.get().handleProgressUpdate(progress));
	}

	public Optional<ProfilingProgressUpdateHandler> getOptionalCallback() {
		return optionalCallback;
	}

	/**
	 * 
	 * @param optionalCallback
	 * @throws NullPointerException if optionalCallback is null
	 */
	public void setCallback(ProfilingProgressUpdateHandler optionalCallback, boolean restartCount) {
		this.optionalCallback = Optional.of(optionalCallback);
		if(restartCount) {
			transientWalkingCount = 0;
		}
	}
	
	public void removeCallback() {
		this.optionalCallback = Optional.empty();
	}
	
	/**
	 * Abstract method requiring that a subclass initial any detail fields that do not require a first value.
	 * An example of this is setting a walking count to 0 because it can be initialized without an initial value.
	 * @param knownDetailType The detail type or null if unknown
	 * @param resultingStage the expected stage after this method is called
	 */
	protected abstract void initializeDetailFields(String knownDetailType, Stage resultingStage);
	
	/**
	 * Initialize fields of an accumulator based on an initial value.  An example of this is setting a minimum
	 * value because a minimum cannot be initialized without at least one value.
	 * @param stage The stage of accumulation
	 * @param value The value to be accumulated
	 * @return the accumulator with metrics reflecting the first value
	 * @throws MainTypeException thrown if the first value cannot be used to initialize the accumulator
	 */
	protected abstract AbstractProfileAccumulator initializeFirstValue(Stage stage, Object value) 
			throws MainTypeException;
	
	/**
	 * Initialize values necessary for second pass accumulation.  For example, defined number bucket ranges.
	 * @param profile The existing profile
	 * @return The initialized accumulator
	 * @throws MainTypeException thrown if the initialization cannot be completed
	 */
	protected abstract AbstractProfileAccumulator initializeForSecondPassAccumulation(Profile profile) 
			throws MainTypeException;
	
	/**
	 * Initialize the accumulator for schema analysis.  Subclasses should initialize based on existing schema profiles
	 * and any metrics in samples that are needed.  For example, determining the number histogram based on
	 * unique valued histograms in both schema and samples.
	 * @param schemaProfile the existing schema profile, or null if there is no existing schema
	 * @param recordsInSchema the number of records in the exsitings schema
	 * @param sampleProfiles the sample profiles that are merged together or into the existing schema
	 * @return the instance of the accumulator, initialized
	 * @throws MainTypeException should be thrown if there is no schema or sample profile to initialize the accumulator
	 */
	protected abstract AbstractProfileAccumulator initializeForSchemaAccumulation(
			Profile schemaProfile, int recordsInSchema, List<Profile> sampleProfiles) throws MainTypeException;
	
	/**
	 * Accumulate a value for a certain stage
	 * @param accumulationStage the current accumulation stage
	 * @param value the value
	 * @return the profile reflecting the addition of values
	 * @throws MainTypeException thrown if there is a type error with the value
	 */
	protected abstract Profile accumulate(Stage accumulationStage, Object value) throws MainTypeException;
	
	/**
	 * Finish any metrics that rely on walking field.  For example, calculate the average using the walking sum
	 * and walking count.
	 * @param accumulationStage the stage that is being finished
	 * @return The profile, with all applicable fields set based on the accumulated values
	 */
	protected abstract Profile finish(Stage accumulationStage);
}
