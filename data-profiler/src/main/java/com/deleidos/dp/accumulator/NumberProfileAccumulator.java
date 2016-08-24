package com.deleidos.dp.accumulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.DefinedRangesNumberBucketList;
import com.deleidos.dp.histogram.AbstractNumberBucketList;

public class NumberProfileAccumulator extends AbstractProfileAccumulator {
	private static Logger logger = Logger.getLogger(NumberProfileAccumulator.class);
	protected AbstractNumberBucketList numberHistogram;

	protected NumberProfileAccumulator(String key) {
		super(key, MainType.NUMBER);
	}
	
	@Override
	protected void initializeDetailFields(String knownDetailType, Stage resultingStage) {
		setNumberDetail(new NumberDetail());
		if(knownDetailType != null) {
			getNumberDetail().setDetailType(knownDetailType);
		}
		getNumberDetail().setWalkingSum(BigDecimal.ZERO);
		getNumberDetail().setWalkingCount(BigDecimal.ZERO);
		getNumberDetail().setWalkingSquareSum(BigDecimal.ZERO);
	}

	public NumberDetail getNumberDetail() {
		return Profile.getNumberDetail(profile);
	}

	protected void setNumberDetail(NumberDetail numberDetail) {
		profile.setDetail(numberDetail);
		accumulateHashes = false;
	}

	public void accumulateDetailType(Object value) {
		detailTypeTracker[MetricsCalculationsFacade.determineNumberDetailType(value).getIndex()]++;
	}

	public void accumulateMin(BigDecimal value) {
		if(value.compareTo(getNumberDetail().getMin()) == -1) getNumberDetail().setMin(value);
	}

	public void accumulateMax(BigDecimal value) {
		if(value.compareTo(getNumberDetail().getMax()) == 1) getNumberDetail().setMax(value);
	}

	public void accumulateWalkingFields(BigDecimal value) {
		getNumberDetail().setWalkingCount(getNumberDetail().getWalkingCount().add(BigDecimal.ONE));
		getNumberDetail().setWalkingSum(getNumberDetail().getWalkingSum().add(value));
		//getNumberDetail().setAverage(null);
		BigDecimal adderSquare = value.pow(2, MetricsCalculationsFacade.DEFAULT_CONTEXT);
		getNumberDetail().setWalkingSquareSum(getNumberDetail().getWalkingSquareSum().add(adderSquare));
	}

	public void accumulateBucketCount(Object value) {
		if(!numberHistogram.putValue(value)) {
			logger.warn("Did not successfully put " + value + " into histogram.");
		}
	}

	protected AbstractProfileAccumulator initializeFirstValue(Stage stage, Object value) throws MainTypeException {
		BigDecimal numValue;
		try {
			numValue = new BigDecimal(value.toString());
		} catch (NumberFormatException e) {
			logger.warn("Dropping non-numeric value " + value + ".");
			throw new MainTypeException("Non-numeric initial value " + value + ".");
		}

		getNumberDetail().setMin(numValue);
		getNumberDetail().setMax(numValue);
		getNumberDetail().setAverage(numValue);
		getNumberDetail().setStdDev(0);
		accumulateHashes = true;
		
		return this;
	}

	@Override
	protected AbstractProfileAccumulator initializeForSecondPassAccumulation(Profile profile) {
		//only things needed is histogram stuff
		numberHistogram = AbstractNumberBucketList.newNumberBucketList(getNumberDetail());
		return this;
	}

	@Override
	protected AbstractProfileAccumulator initializeForSchemaAccumulation(Profile schemaProfile, int schemaRecords, List<Profile> sampleProfiles) throws MainTypeException {
		NumberDetail schemaNumberDetail = (schemaProfile != null) ? Profile.getNumberDetail(schemaProfile) : null;
		List<NumberDetail> sampleNumberDetails = new ArrayList<NumberDetail>();
		for(Profile sampleProfile : sampleProfiles) {
			Optional<NumberDetail> numberDetailOptional = Profile.getNumberDetailOptional(sampleProfile);
			if(numberDetailOptional.isPresent()) {
				sampleNumberDetails.add(numberDetailOptional.get());
			} else {
				logger.info("Profile for " + sampleProfile.getDisplayName() + " dropped due to type mismatch.");
			}	
		}
		numberHistogram = AbstractNumberBucketList.newNumberBucketList(
				schemaNumberDetail, sampleNumberDetails);
		return this;
	}

	@Override
	protected Profile accumulate(Stage accumulationStage, Object value) throws MainTypeException {
		BigDecimal numValue = new BigDecimal(value.toString());
		switch(accumulationStage) {
		case UNITIALIZED: throw new MainTypeException("Accumulator called but has not been initialized.");
		case SAMPLE_AWAITING_FIRST_VALUE: { 
			break;
		}
		case SCHEMA_AWAITING_FIRST_VALUE: {
			break;
		}
		case SAMPLE_FIRST_PASS: {
			accumulateMin(numValue);
			accumulateMax(numValue);
			accumulateWalkingFields(numValue);
			accumulateDetailType(numValue);
			accumulateNumDistinctValues(numValue);
			break;
		}
		case SAMPLE_SECOND_PASS: {
			accumulateBucketCount(numValue);
			break;
		}
		case SCHEMA_PASS: {
			accumulateMin(numValue);
			accumulateMax(numValue);
			accumulateWalkingFields(numValue);
			accumulateNumDistinctValues(numValue);
			accumulateBucketCount(numValue);
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
			BigDecimal average = getNumberDetail().getWalkingSum().divide(getNumberDetail().getWalkingCount(), MetricsCalculationsFacade.DEFAULT_CONTEXT);

			getNumberDetail().setAverage(average);


			BigDecimal twiceAverage = average.multiply(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
			BigDecimal summations = getNumberDetail().getWalkingSquareSum().subtract(twiceAverage.multiply(getNumberDetail().getWalkingSum(), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
			BigDecimal finalNumerator = summations.add(getNumberDetail().getWalkingCount().multiply(average.pow(2), DEFAULT_CONTEXT), DEFAULT_CONTEXT);
			BigDecimal withDivision = finalNumerator.divide(getNumberDetail().getWalkingCount(), DEFAULT_CONTEXT);
			double stdDev = Math.sqrt(withDivision.doubleValue());
			getNumberDetail().setStdDev(stdDev);

			if(accumulateHashes) {
				getNumberDetail().setNumDistinctValues(String.valueOf(distinctValues.size()));
			}
		}
		if(getAccumulationStage().equals(Stage.SAMPLE_SECOND_PASS) || 
				getAccumulationStage().equals(Stage.SCHEMA_PASS)) {
			getNumberDetail().setFreqHistogram(this.numberHistogram.asBean());
		}
		return profile;
	}
}
