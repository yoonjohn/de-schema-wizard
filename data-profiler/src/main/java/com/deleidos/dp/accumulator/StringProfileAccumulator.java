package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.CharacterBucketList;
import com.deleidos.dp.histogram.ShortStringBucketList;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public class StringProfileAccumulator extends AbstractProfileAccumulator {
	private static final Logger logger = Logger.getLogger(StringProfileAccumulator.class);
	protected ShortStringBucketList shortStringBucketList;
	protected CharacterBucketList charBucketList;

	protected StringProfileAccumulator(String key) {
		super(key, MainType.STRING);
	}
	
	@Override
	protected void initializeDetailFields(String knownDetailType, Stage resultingStage) {
		setStringDetail(new StringDetail());
		if(knownDetailType != null) {
			getStringDetail().setDetailType(knownDetailType);
		}
		getStringDetail().setWalkingSum(BigDecimal.ZERO);
		getStringDetail().setWalkingCount(BigDecimal.ZERO);
		getStringDetail().setWalkingSquareSum(BigDecimal.ZERO);
	}

	public StringDetail getStringDetail() {
		return Profile.getStringDetail(profile);
	}

	protected void setStringDetail(StringDetail stringDetail) {
		profile.setDetail(stringDetail);
		accumulateHashes = false;
	}

	public void accumulateDetailType(Object value) {
		detailTypeTracker[MetricsCalculationsFacade.determineStringDetailType(value).getIndex()]++;
	}

	public void accumulateMinLength(String objectString) {
		int value = objectString.length();
		if(value < getStringDetail().getMinLength()) getStringDetail().setMinLength(value);
	}

	public void accumulateMaxLength(String objectString) {
		int value = objectString.length();
		if(value > getStringDetail().getMaxLength()) getStringDetail().setMaxLength(value);
	}

	public void accumulateWalkingFields(String objectString) {
		BigDecimal lengthValue = BigDecimal.valueOf(objectString.length());
		getStringDetail().setWalkingCount(getStringDetail().getWalkingCount().add(BigDecimal.ONE));
		getStringDetail().setWalkingSum(getStringDetail().getWalkingSum().add(lengthValue));
		BigDecimal adderSquare = lengthValue.pow(2, MetricsCalculationsFacade.DEFAULT_CONTEXT);
		getStringDetail().setWalkingSquareSum(getStringDetail().getWalkingSquareSum().add(adderSquare));
	}

	public void accumulateShortStringFreqHistogram(String objectString) {
		String currentDetailType = getStringDetail().getDetailType();
		if(!objectString.equals(DefaultProfilerRecord.EMPTY_FIELD_VALUE_INDICATOR) && (currentDetailType.equals(DetailType.TEXT.toString()))) {
			return;
		}
		else {
			shortStringBucketList.putValue(objectString);
		}
	}

	@Override
	protected AbstractProfileAccumulator initializeForSecondPassAccumulation(Profile profile) {
		shortStringBucketList = new ShortStringBucketList();
		return this;
	}

	protected AbstractProfileAccumulator initializeFirstValue(Stage stage, Object value) throws MainTypeException {
		String stringValue = value.toString();
		getStringDetail().setMinLength(stringValue.length());
		getStringDetail().setMaxLength(stringValue.length());
		getStringDetail().setAverageLength(stringValue.length());
		getStringDetail().setStdDevLength(0);
		accumulateHashes = true;
		return this;
	}

	@Override
	protected AbstractProfileAccumulator initializeForSchemaAccumulation(Profile schemaProfile, int recordsInSchema, List<Profile> sampleProfiles) throws MainTypeException {
		shortStringBucketList = new ShortStringBucketList();
		return this;
	}

	@Override
	protected Profile accumulate(Stage accumulationStage, Object value) throws MainTypeException {
		String objectString = value.toString();
		switch(accumulationStage) {
		case UNITIALIZED: throw new MainTypeException("Accumulator called but has not been initialized.");
		case SAMPLE_AWAITING_FIRST_VALUE: { 
			break;
		}
		case SCHEMA_AWAITING_FIRST_VALUE: {
			break;
		}
		case SAMPLE_FIRST_PASS: {
			accumulateDetailType(objectString);
			accumulateMaxLength(objectString);
			accumulateMinLength(objectString);
			accumulateNumDistinctValues(objectString);
			accumulateWalkingFields(objectString);
			break;
		}
		case SAMPLE_SECOND_PASS: {
			accumulateShortStringFreqHistogram(objectString);
			break;
		}
		case SCHEMA_PASS: {
			accumulateDetailType(objectString);
			accumulateMaxLength(objectString);
			accumulateMinLength(objectString);
			accumulateNumDistinctValues(objectString);
			accumulateWalkingFields(objectString);
			accumulateShortStringFreqHistogram(objectString);
			break;
		}
		default: throw new MainTypeException("Accumulator stuck in unknown stage.");
		}
		return profile;
	}

	@Override
	protected Profile finish(Stage accumulationStage) {
		if(this.getState().getPresence() < 0) {
			return profile;
		}

		if(accumulationStage.equals(Stage.SAMPLE_FIRST_PASS) 
				|| accumulationStage.equals(Stage.SCHEMA_PASS)) {
			BigDecimal average = getStringDetail().getWalkingSum()
					.divide(getStringDetail().getWalkingCount(), MetricsCalculationsFacade.DEFAULT_CONTEXT);

			getStringDetail().setAverageLength(average.doubleValue());


			BigDecimal twiceAverage = average.multiply(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
			BigDecimal summations = getStringDetail().getWalkingSquareSum()
					.subtract(twiceAverage
							.multiply(getStringDetail().getWalkingSum(), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
			BigDecimal finalNumerator = summations
					.add(getStringDetail().getWalkingCount()
							.multiply(average.pow(2), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
			BigDecimal withDivision = finalNumerator.divide(getStringDetail().getWalkingCount(), DEFAULT_CONTEXT);
			double stdDev = Math.sqrt(withDivision.doubleValue());
			getStringDetail().setStdDevLength(stdDev);

			if(accumulateHashes) {
				getStringDetail().setNumDistinctValues(String.valueOf(distinctValues.size()));
			}
		}
		if(accumulationStage.equals(Stage.SAMPLE_SECOND_PASS)
				|| accumulationStage.equals(Stage.SCHEMA_PASS)) {

			if(!getStringDetail().getDetailType().equals(DetailType.PHRASE)) {
				if(getAccumulationStage().equals(Stage.SAMPLE_SECOND_PASS) 
						|| getAccumulationStage().equals(Stage.SCHEMA_PASS)) {
					getStringDetail().setTermFreqHistogram(shortStringBucketList.asBean());
				}
			} else {
				getStringDetail().setTermFreqHistogram(null);
			}
		}
		return profile;
	}

}
