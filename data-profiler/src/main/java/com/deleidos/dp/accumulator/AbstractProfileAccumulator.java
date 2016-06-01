package com.deleidos.dp.accumulator;

import java.math.MathContext;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.domain.JavaDomain;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.interpretation.AbstractJavaInterpretation;
import com.deleidos.dp.interpretation.JavaLatitudeInterpretation;
import com.deleidos.dp.interpretation.JavaLongitudeInterpretation;
import com.deleidos.dp.interpretation.JavaUnknownInterpretation;

public abstract class AbstractProfileAccumulator implements Accumulator<Profile> {
	public static final MathContext DEFAULT_CONTEXT = MathContext.DECIMAL128;
	private static Logger logger = Logger.getLogger(AbstractProfileAccumulator.class);
	private String fieldName;
	protected int[] detailTypeTracker;
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
		isFinished = false;
		profile = new Profile();
		profile.setInterpretation(Interpretation.UNKNOWN);
		this.fieldName = key;
		detailTypeTracker = new int[DetailType.values().length]; 
		presenceCount = 0;
	}

	public AbstractProfileAccumulator(String key, Object firstValue) {
		this(key);
		initFirstValue(firstValue);
	}

	public abstract Detail getDetail();

	public abstract void setDetail(Detail metrics);

	public boolean isEmpty() {
		return (getState().getPresence() < 0) ? true : false;
	}

	@Override
	public abstract boolean accumulate(Object jsonObject);

	public void accumulate(Object jsonObject, boolean accumulatePresence) {
		if(accumulatePresence) {
			presenceCount++;
		}
		accumulate(jsonObject);
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
			getState().getDetail().getBucketListIfApplicable().setType("map");
		}
		this.hasGeoSpatialData = hasGeoSpatialData;
	}

	@Override
	public void finish() {
		isFinished = true;
		String detailType = MetricsCalculationsFacade.getDetailTypeFromDistribution(getState().getMainType(), getDetailTypeTracker()).toString();
		Detail detail = getDetail();
		detail.setDetailType(detailType);
		getState().setDetail(detail);
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
}
