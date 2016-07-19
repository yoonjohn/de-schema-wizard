package com.deleidos.dp.accumulator;

import java.math.BigDecimal;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.CharacterBucketList;
import com.deleidos.dp.histogram.TermBucketList;

public class StringProfileAccumulator extends AbstractProfileAccumulator {
	private boolean accumulateHashes = true;
	protected StringDetail stringDetail;
	protected TermBucketList termBucketList;
	protected CharacterBucketList charBucketList;

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

	private void setStringDetail(StringDetail numberDetail) {
		this.stringDetail = numberDetail;
		accumulateHashes = false;
	}

	@Override
	public void accumulate(Object value, boolean accumulatePresence) throws MainTypeException {
		super.accumulate(value, accumulatePresence);
		if(value == null) {
			return;
		}
		String objectString = value.toString();
		//accumulateCharacterFreqHistogram(objectString);
		accumulateDetailType(objectString);
		accumulateMaxLength(objectString);
		accumulateMinLength(objectString);
		accumulateNumDistinctValues(objectString);
		accumulateTermFreqHistogram(objectString);
		accumulateWalkingFields(objectString);
	}
	
	public void accumulateDetailType(Object value) {
		detailTypeTracker[MetricsCalculationsFacade.determineStringDetailType(value).getIndex()]++;
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

	/*public void accumulateCharacterFreqHistogram(String objectString) {
		charBucketList.putValue(objectString);
	}*/

	public void accumulateTermFreqHistogram(String objectString) {
		if(objectString.contains(" ")) return;
		else {
			termBucketList.putValue(objectString);
		}
	}

	@Override
	public void initializeFromExistingProfile(Profile profile) {
		super.initializeFromExistingProfile(profile);
		this.setStringDetail(Profile.getStringDetail(profile));
		profile.getDetail().getHistogramOptional().ifPresent(x->this.termBucketList = Histogram.toTermBucketList(x));
	}

	@Override
	public boolean initFirstValue(Object value) {
		if(value == null) {
			return false;
		} else {
			String stringValue = value.toString();
			stringDetail = new StringDetail();
			stringDetail.setMinLength(stringValue.length());
			stringDetail.setMaxLength(stringValue.length());
			stringDetail.setAverageLength(stringValue.length());
			termBucketList = new TermBucketList();
			termBucketList.putValue(stringValue);
			charBucketList = new CharacterBucketList();
			charBucketList.putValue(stringValue);
			/*stringDetail.setTermFreqHistogram(new TermBucketList());
			stringDetail.getTermFreqHistogram().putValue(stringValue);
			stringDetail.setCharFreqHistogram(new CharacterBucketList());
			stringDetail.getCharFreqHistogram().putValue(stringValue);*/
			stringDetail.setStdDevLength(0);
			stringDetail.setWalkingSum(BigDecimal.valueOf(stringValue.length()));
			stringDetail.setWalkingCount(BigDecimal.ONE);
			stringDetail.setWalkingSquareSum(BigDecimal.valueOf(stringValue.length()).pow(2));
			accumulateHashes = true;
			distinctValues.add(stringValue);
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
		if(this.getState().getPresence() < 0) {
			return;
		}
		BigDecimal average = stringDetail.getWalkingSum().divide(stringDetail.getWalkingCount()
				, MetricsCalculationsFacade.DEFAULT_CONTEXT);

		stringDetail.setAverageLength(average.doubleValue());


		BigDecimal twiceAverage = average.multiply(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
		BigDecimal summations = stringDetail.getWalkingSquareSum().subtract(twiceAverage.multiply(stringDetail.getWalkingSum(), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal finalNumerator = summations.add(stringDetail.getWalkingCount().multiply(average.pow(2), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
		BigDecimal withDivision = finalNumerator.divide(stringDetail.getWalkingCount(), DEFAULT_CONTEXT);
		double stdDev = Math.sqrt(withDivision.doubleValue());
		stringDetail.setStdDevLength(stdDev);
		
		if(accumulateHashes) {
			stringDetail.setNumDistinctValues(String.valueOf(distinctValues.size()));
		}
		
		if(!stringDetail.getDetailType().equals(DetailType.PHRASE)) {
			stringDetail.setHistogram(termBucketList.asBean());	
		} else {
			stringDetail.setHistogram(null);
		}

	}

}
