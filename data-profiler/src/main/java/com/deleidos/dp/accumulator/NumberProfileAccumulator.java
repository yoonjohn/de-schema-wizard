package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.NumberBucketList;

public class NumberProfileAccumulator extends AbstractProfileAccumulator {
	private static Logger logger = Logger.getLogger(NumberProfileAccumulator.class);
	private NumberDetail numberDetail;
	private boolean accumulateHashes = true;
	protected NumberBucketList numberHistogram;

	public NumberProfileAccumulator(String key, Object value) {
		super(key, value);
		profile.setMainType(MainType.NUMBER.toString());
	}
	
	public NumberProfileAccumulator(String key) {
		super(key);
		profile.setMainType(MainType.NUMBER.toString());
	}

	public NumberDetail getNumberDetail() {
		return numberDetail;
	}

	private void setNumberDetail(NumberDetail numberDetail) {
		this.numberDetail = numberDetail;
		accumulateHashes = false;
	}

	@Override
	public void accumulate(Object value, boolean accumulatePresence) throws MainTypeException {
		super.accumulate(value, accumulatePresence);
		if(value == null) {
			return;
		}
		BigDecimal numValue = new BigDecimal(value.toString());
		accumulateMin(numValue);
		accumulateMax(numValue);
		accumulateWalkingFields(numValue);
		accumulateDetailType(numValue);
		accumulateNumDistinctValues(numValue);
		accumulateBucketCount(value);
		return;
	}
	
	public void accumulateDetailType(Object value) {
		detailTypeTracker[MetricsCalculationsFacade.determineNumberDetailType(value).getIndex()]++;
	}

	public void accumulateMin(BigDecimal value) {
		if(numberDetail.getMin() == null || value.compareTo(numberDetail.getMin()) == -1) numberDetail.setMin(value);
	}

	public void accumulateMax(BigDecimal value) {
		if(numberDetail.getMax() == null || value.compareTo(numberDetail.getMax()) == 1) numberDetail.setMax(value);
	}

	public void accumulateWalkingFields(BigDecimal value) {
		numberDetail.setWalkingCount(numberDetail.getWalkingCount().add(BigDecimal.ONE));
		numberDetail.setWalkingSum(numberDetail.getWalkingSum().add(value));
		numberDetail.setAverage(null);
		BigDecimal adderSquare = value.pow(2, MetricsCalculationsFacade.DEFAULT_CONTEXT);
		numberDetail.setWalkingSquareSum(numberDetail.getWalkingSquareSum().add(adderSquare));
	}

	public void accumulateBucketCount(Object value) {
		if(!numberHistogram.putValue(value)) {
			logger.warn("Did not successfully put " + value + " into histogram.");
		}
	}

	@Override
	public void initializeFromExistingProfile(Profile profile) {
		super.initializeFromExistingProfile(profile);
		this.setNumberDetail(Profile.getNumberDetail(profile));
		profile.getDetail().getHistogramOptional().ifPresent(x->this.numberHistogram = Histogram.toNumberBucketList(x));
	}

	@Override
	public boolean initFirstValue(Object value) throws MainTypeException {
		if(value == null) {
			return false;
		}
		BigDecimal numValue;
		try {
			numValue = new BigDecimal(value.toString());
		} catch (NumberFormatException e) {
			logger.warn("Dropping non-numeric value " + value + ".");
			throw new MainTypeException("Non-numeric initial value " + value + ".");
		}
		
		numberDetail = new NumberDetail();
		numberDetail.setMin(numValue);
		numberDetail.setMax(numValue);
		numberDetail.setAverage(numValue);
		numberHistogram = new NumberBucketList();
		numberHistogram.putValue(numValue);
		numberDetail.setStdDev(0);
		numberDetail.setWalkingSum(numValue);
		numberDetail.setWalkingCount(BigDecimal.ONE);
		numberDetail.setWalkingSquareSum(numValue.pow(2));
		
		accumulateHashes = true;
		distinctValues.add(numValue);
		accumulateDetailType(numValue);
		presenceCount++;
		return true;
	}
	
	@Override
	public void finish() {
		super.finish();
		if(this.getState().getPresence() < 0) {
			return;
		}
		BigDecimal average = numberDetail.getWalkingSum().divide(numberDetail.getWalkingCount(), MetricsCalculationsFacade.DEFAULT_CONTEXT);
		numberDetail.setAverage(average);

		BigDecimal twiceAverage = average.multiply(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
		BigDecimal summations = numberDetail.getWalkingSquareSum().subtract(twiceAverage.multiply(numberDetail.getWalkingSum(), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal finalNumerator = summations.add(numberDetail.getWalkingCount().multiply(average.pow(2), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal withDivision = finalNumerator.divide(numberDetail.getWalkingCount(), DEFAULT_CONTEXT);
		double stdDev = Math.sqrt(withDivision.doubleValue());
		numberDetail.setStdDev(stdDev);
		
		if(accumulateHashes) {
			numberDetail.setNumDistinctValues(String.valueOf(distinctValues.size()));
		}
		
		numberDetail.setHistogram(numberHistogram.asBean());
	}

	@Override
	public Detail getDetail() {
		return numberDetail;
	}

}
