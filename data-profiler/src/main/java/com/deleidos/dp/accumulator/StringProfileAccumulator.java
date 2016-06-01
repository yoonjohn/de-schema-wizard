package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.histogram.CharacterBucketList;
import com.deleidos.dp.histogram.TermBucketList;

public class StringProfileAccumulator extends AbstractProfileAccumulator {
	private Set<Object> distinctHashes;
	private static Logger logger = Logger.getLogger(StringProfileAccumulator.class);
	protected StringDetail stringDetail;

	public StringProfileAccumulator(String key, Object value) {
		super(key, value);
		profile.setMainType(MainType.STRING.toString());
	}

	public StringProfileAccumulator(String key) {
		super(key);
		profile.setMainType(MainType.STRING.toString());
	}

	public StringDetail getStringDetail() {
		return stringDetail;
	}

	public void setstringDetail(StringDetail numberDetail) {
		this.stringDetail = numberDetail;
	}

	@Override
	public boolean accumulate(Object value) {
		if(value == null) {
			return false;
		}
		String objectString = value.toString();
		accumulateCharacterFreqHistogram(objectString);
		accumulateDetailType(objectString);
		accumulateMaxLength(objectString);
		accumulateMinLength(objectString);
		accumulateNumDistinctValues(objectString);
		accumulateTermFreqHistogram(objectString);
		accumulateWalkingFields(objectString);
		//accumulateWalkingInterpretationList(objectString);
		return true;
	}
	
	public void accumulateDetailType(Object value) {
		detailTypeTracker[MetricsCalculationsFacade.determineStringDetailType(value).getIndex()]++;
	}

	public void accumulateNumDistinctValues(String objectString) {
		distinctHashes.add(objectString);
	}

	public void accumulateMinLength(String objectString) {
		int value = objectString.length();
		if(stringDetail.getMinLength() < 0 || value < stringDetail.getMinLength()) stringDetail.setMinLength(value);
	}

	public void accumulateMaxLength(String objectString) {
		int value = objectString.length();
		if(stringDetail.getMaxLength() < 0 || value > stringDetail.getMaxLength()) stringDetail.setMaxLength(value);
	}

	public void accumulateWalkingFields(String objectString) {
		BigDecimal lengthValue = BigDecimal.valueOf(objectString.length());
		stringDetail.setWalkingCount(stringDetail.getWalkingCount().add(BigDecimal.ONE));
		stringDetail.setWalkingSum(stringDetail.getWalkingSum().add(lengthValue));
		stringDetail.setAverageLength(-1);
		BigDecimal adderSquare = lengthValue.pow(2, MetricsCalculationsFacade.DEFAULT_CONTEXT);
		stringDetail.setWalkingSquareSum(stringDetail.getWalkingSquareSum().add(adderSquare));
	}

	public void accumulateCharacterFreqHistogram(String objectString) {
		stringDetail.getCharFreqHistogram().putValue(objectString);
	}

	public void accumulateTermFreqHistogram(String objectString) {
		if(objectString.contains(" ")) return;
		else {
			stringDetail.getTermFreqHistogram().putValue(objectString);
		}
	}

	/*@Override
	protected void accumulateWalkingInterpretationList(Object value) {
		if(domain == null || !Interpretation.isUnknown(profile.getInterpretation())) {
			return;
		}
		Iterator<String> interpretations = domain.getInterpretationMap().keySet().iterator();
		while(interpretations.hasNext()) {
			String classificationName = interpretations.next();
			if(!domain.getInterpretationMap().get(classificationName).fitsStringMetrics(value.toString())) {
				logger.debug("Removing " + classificationName + " from possible interpretations "
						+ "of \"" + getFieldName() + "\" because " + value + " does not fit.");
				interpretations.remove();
			}
		}
	}*/

	@Override
	public void setDetail(Detail detail) {
		this.stringDetail = (StringDetail) detail;
	}

	@Override
	public boolean initFirstValue(Object value) {
		if(JSONObject.NULL.equals(value)) {
			return false;
		} else {
			String stringValue = value.toString();
			stringDetail = new StringDetail();
			stringDetail.setMinLength(stringValue.length());
			stringDetail.setMaxLength(stringValue.length());
			stringDetail.setAverageLength(stringValue.length());
			stringDetail.setTermFreqHistogram(new TermBucketList());
			stringDetail.getTermFreqHistogram().putValue(stringValue);
			stringDetail.setCharFreqHistogram(new CharacterBucketList());
			stringDetail.getCharFreqHistogram().putValue(stringValue);
			stringDetail.setStdDevLength(0);
			stringDetail.setWalkingSum(BigDecimal.valueOf(stringValue.length()));
			stringDetail.setWalkingCount(BigDecimal.ONE);
			stringDetail.setWalkingSquareSum(BigDecimal.valueOf(stringValue.length()).pow(2));
			distinctHashes = new HashSet<Object>();
			distinctHashes.add(stringValue);
			accumulateDetailType(stringValue);
			presenceCount++;
			return true;	
		}
	}

	@Override
	public Detail getDetail() {
		return stringDetail;
	}

	@Override
	public void finish() {
		super.finish();

		BigDecimal average = stringDetail.getWalkingSum().divide(stringDetail.getWalkingCount()
				, MetricsCalculationsFacade.DEFAULT_CONTEXT);

		stringDetail.setAverageLength(average.doubleValue());


		BigDecimal twiceAverage = average.multiply(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
		BigDecimal summations = stringDetail.getWalkingSquareSum().subtract(twiceAverage.multiply(stringDetail.getWalkingSum(), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal finalNumerator = summations.add(stringDetail.getWalkingCount().multiply(average.pow(2), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal withDivision = finalNumerator.divide(stringDetail.getWalkingCount(), DEFAULT_CONTEXT);
		double stdDev = Math.sqrt(withDivision.doubleValue());
		stringDetail.setStdDevLength(stdDev);
		
		stringDetail.setNumDistinctValues(distinctHashes.size());

	}

}
