package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.histogram.NumberBucketList;

public class NumberProfileAccumulator extends AbstractProfileAccumulator {
	private static Logger logger = Logger.getLogger(NumberProfileAccumulator.class);
	private NumberDetail numberDetail;
	private Set<Object> distinctHashes;

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

	public void setNumberDetail(NumberDetail numberDetail) {
		this.numberDetail = numberDetail;
	}

	@Override
	public boolean accumulate(Object value) {
		if(value == null) {
			return false;
		}
		BigDecimal numValue;
		try {
			numValue = new BigDecimal(value.toString());
		} catch (NumberFormatException e) {
			logger.warn("Dropping non-numeric value " + value + ".");
			return false;
		}
		accumulateMin(numValue);
		accumulateMax(numValue);
		accumulateWalkingFields(numValue);
		accumulateNumDistinct(numValue);
		accumulateDetailType(numValue);
		accumulateBucketCount(value);
		//accumulateWalkingInterpretationList(numValue);
		return true;
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

	public void accumulateNumDistinct(Object value) {
		distinctHashes.add(value);
	}

	public void accumulateBucketCount(Object value) {
		numberDetail.getFreqHistogram().putValue(value);
	}

	/*@Override
	protected void accumulateWalkingInterpretationList(Object value) {
		if(domain == null || !Interpretation.isUnknown(profile.getInterpretation())) {
			return;
		}
		Iterator<String> interpretations = domain.getInterpretationMap().keySet().iterator();
		while(interpretations.hasNext()) {
			String classificationName = interpretations.next();
			if(!domain.getInterpretationMap().get(classificationName).fitsNumberMetrics((BigDecimal)value)) {
				logger.debug("Removing " + classificationName + " from possible interpretations "
						+ "of \"" + getFieldName() + "\" because " + value + " does not fit.");
				interpretations.remove();
			}
		}
	}*/

	@Override
	public void setDetail(Detail detail) {
		this.numberDetail = (NumberDetail) detail;
	}

	@Override
	public boolean initFirstValue(Object value) {
		if(JSONObject.NULL.equals(value)) {
			return false;
		}
		BigDecimal numValue;
		try {
			numValue = new BigDecimal(value.toString());
		} catch (NumberFormatException e) {
			logger.warn("Dropping non-numeric value " + value + ".");
			return false;
		}
		
		numberDetail = new NumberDetail();
		numberDetail.setMin(numValue);
		numberDetail.setMax(numValue);
		numberDetail.setAverage(numValue);
		numberDetail.setFreqHistogram(new NumberBucketList());
		numberDetail.getFreqHistogram().putValue(numValue);
		numberDetail.setStdDev(0);
		numberDetail.setWalkingSum(numValue);
		numberDetail.setWalkingCount(BigDecimal.ONE);
		numberDetail.setWalkingSquareSum(numValue.pow(2));
		
		distinctHashes = new HashSet<Object>();
		distinctHashes.add(numValue);
		accumulateDetailType(numValue);
		presenceCount++;
		return true;
	}
	
	@Override
	public void finish() {
		super.finish();
		
		BigDecimal average = numberDetail.getWalkingSum().divide(numberDetail.getWalkingCount(), MetricsCalculationsFacade.DEFAULT_CONTEXT);
		
		numberDetail.setAverage(average);
		

		BigDecimal twiceAverage = average.multiply(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
		BigDecimal summations = numberDetail.getWalkingSquareSum().subtract(twiceAverage.multiply(numberDetail.getWalkingSum(), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal finalNumerator = summations.add(numberDetail.getWalkingCount().multiply(average.pow(2), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal withDivision = finalNumerator.divide(numberDetail.getWalkingCount(), DEFAULT_CONTEXT);
		double stdDev = Math.sqrt(withDivision.doubleValue());
		numberDetail.setStdDev(stdDev);
		
		numberDetail.setNumDistinctValues(distinctHashes.size());
	}

	@Override
	public Detail getDetail() {
		return numberDetail;
	}

}
