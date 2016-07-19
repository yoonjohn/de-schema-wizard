package com.deleidos.dp.accumulator;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucketList;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public abstract class AbstractProfileAccumulator implements Accumulator<Profile> {
	public static final String EMPTY_FIELD_NAME_INDICATOR = "(Blank Field Name)";
	public static final MathContext DEFAULT_CONTEXT = MathContext.DECIMAL128;
	private static Logger logger = Logger.getLogger(AbstractProfileAccumulator.class);
	protected String fieldName;
	protected int[] detailTypeTracker;
	protected List<Object> distinctValues;
	//protected JavaDomain domain;
	private boolean hasGeoSpatialData = false;
	private boolean isFinished;
	protected int presenceCount;
	protected Profile profile;

	@Override
	public Profile getState() {
		return profile;
	}

	public AbstractProfileAccumulator(String key) {
		fieldName = key;
		isFinished = false;
		profile = new Profile();
		profile.setInterpretation(Interpretation.UNKNOWN);
		detailTypeTracker = new int[DetailType.values().length]; 
		presenceCount = 0;
		distinctValues = new ArrayList<Object>();
	}

	public AbstractProfileAccumulator(String key, Object firstValue) {
		this(key);
		try {
			initFirstValue(firstValue);
		} catch (MainTypeException e) {
			logger.error("Error accumulating first value.", e);
		}
	}

	public abstract Detail getDetail();

	public void initializeFromExistingProfile(Profile profile) {
		this.profile = profile;
		this.setPresenceCount((int)(profile.getDetail().getWalkingCount().intValue() * profile.getPresence()));
	}

	public boolean isEmpty() {
		return (getState().getPresence() < 0) ? true : false;
	}

	public void accumulate(Object object) {
		try {
			accumulate(this.getState().getMainTypeClass().createAppropriateObject(object), true);
		} catch (MainTypeException e) {
			logger.error(e);
		}
	}

	@Override
	public void accumulate(Object object, boolean accumulatePresence) throws MainTypeException {
		if(accumulatePresence) {
			presenceCount++;
		}
	}

	public void accumulateNumDistinctValues(Object object) {
		if(!distinctValues.contains(object)) {
			distinctValues.add(object);
		}
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
		/*if(hasGeoSpatialData) {
			getState().getDetail().getBucketListIfApplicable().ifPresent(x->x.setType("map"));
		}*/
		this.hasGeoSpatialData = hasGeoSpatialData;
	}

	@Override
	public void finish() {
		isFinished = true;
		Detail detail = getDetail();
		if(detail.getDetailType() == null) {
			String detailTypeString = MetricsCalculationsFacade.getDetailTypeFromDistribution(getState().getMainType(), getDetailTypeTracker()).toString();
			detail.setDetailType(detailTypeString);
		}
		getState().setDetail(detail);
		getState().setExampleValues(getDistinctValuesAsExampleValuesList());
	}

	public List<Object> getDistinctValuesAsExampleValuesList() {
		final int maxExampleListSize = 50;
		List<Object> exampleValues = null;
		if(distinctValues != null) {
			exampleValues = new ArrayList<Object>();
			//purposely lose precision
			int exampleListSize = (distinctValues.size() > maxExampleListSize) ? maxExampleListSize : distinctValues.size();
			float valueChooserRange = (float)distinctValues.size()/(float)exampleListSize;
			for(int i = 0; i < exampleListSize; i++) {
				exampleValues.add(distinctValues.get((int)valueChooserRange*i));
			}
			return exampleValues;
		} else {
			logger.error("Null distinct values object while trying to generate example values.");
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

	public boolean isFinished() {
		return isFinished;
	}

	public void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}

	public List<Object> getDistinctValues() {
		return distinctValues;
	}

	public void setDistinctValues(List<Object> distinctHashes) {
		this.distinctValues = distinctHashes;
	}
}
