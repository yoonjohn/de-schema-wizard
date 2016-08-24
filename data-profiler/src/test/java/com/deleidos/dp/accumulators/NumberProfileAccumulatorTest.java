package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.histogram.AbstractBucketList;

public class NumberProfileAccumulatorTest {
	private Logger logger = Logger.getLogger(NumberProfileAccumulatorTest.class);
	
	@Test
	public void testLargeExponentAccumulate() throws MainTypeException {
		List<Object> values = new ArrayList<Object>();
		//NumberProfileAccumulator num = new NumberProfileAccumulator("test", 2);
		values.add(2);
		for(int i = 0; i < 100; i++) {
			values.add(i);
			//num.accumulate(i);
		}
		values.add(1000000);
		values.add("4.543234723453264323462345217E315324523454");
		//num.finish();
		logger.warn("Exception should be thrown here.");
		Profile profile = BundleProfileAccumulator.generateProfile("test", values);
		int size = Profile.getNumberDetail(profile).getHistogramOptional().get().getLabels().size();
		logger.debug("Size is " + size);
		String label = Profile.getNumberDetail(profile).getHistogramOptional().get().getLabels().get(size-1);
		String max = label.substring(label.indexOf(',')+1, label.indexOf(')'));
		BigDecimal numMax = new BigDecimal(max);
		logger.debug(SerializationUtility.serialize(profile));
		assertTrue(Profile.getNumberDetail(profile).getHistogramOptional().get().getLabels().size()>=50);
		assertTrue(Profile.getNumberDetail(profile).getHistogramOptional().get().getLabels().size()<101);
		assertTrue(numMax.compareTo(BigDecimal.valueOf(1000000)) > 0);
	}
	
	@Test
	public void testLargeNegativeValueAccumulate() throws MainTypeException {
		List<Object> values = new ArrayList<Object>();
		values.add(2);
		//NumberProfileAccumulator num = new NumberProfileAccumulator("test", 2);
		for(int i = 0; i < 100; i++) {
			values.add(-i);
		}
		values.add(-1000000);
		//num.finish();
		Profile p = BundleProfileAccumulator.generateProfile("test", values);
		logger.debug(SerializationUtility.serialize(p));
		
		logger.info("Size is " + Profile.getNumberDetail(p).getHistogramOptional().get().getLabels().size());
		assertTrue(Profile.getNumberDetail(p).getHistogramOptional().get().getLabels().size()>=50
				&& Profile.getNumberDetail(p).getHistogramOptional().get().getLabels().size()<101);
	}

	@Test
	public void testEpochAccumulation() throws MainTypeException {
		List<Object> values = new ArrayList<Object>();
		//NumberProfileAccumulator num = new NumberProfileAccumulator("epoch", "1442222200");
		values.add("1442222200");
		String base = "1442222200";
		for(int i = 0; i < 10000; i++) {
			String add = String.valueOf(i);
			String replace = base.substring(0, base.length()-add.length()) + add;
			values.add(replace);
		}
		Profile profile = BundleProfileAccumulator.generateProfile("epoc", values);
		logger.debug(SerializationUtility.serialize(profile));
		assertTrue(Profile.getNumberDetail(profile).getHistogramOptional().get().getLabels().size()>49
				&& Profile.getNumberDetail(profile).getHistogramOptional().get().getLabels().size()<101);
	}
	
	@Test
	public void testMinMaxAccumulate() throws MainTypeException {
		Profile p = BundleProfileAccumulator
				.generateProfile("test-field", Arrays.asList(2,4,4,4,5,5,7,9));
		NumberDetail n = Profile.getNumberDetail(p);
		assertTrue(n.getMin().compareTo(BigDecimal.valueOf(2)) == 0);
		assertTrue(n.getMax().compareTo(BigDecimal.valueOf(9)) == 0);
		assertTrue(n.getAverage().compareTo(BigDecimal.valueOf(5)) == 0);
		assertTrue(Double.doubleToLongBits(n.getStdDev()) == Double.doubleToLongBits(2.0));
		logger.info("Test min max accumulate successfully.");
		logger.debug(SerializationUtility.serialize(n));
	}

	@Test
	public void testNonEmptyBucket() throws SQLException, MainTypeException {
		NumberDetail n = Profile.getNumberDetail(BundleProfileAccumulator
				.generateProfile("test-field", Arrays.asList(2,4,4,4,5,5,7,9)));
		int size = n.getFreqHistogram().getLabels().size();
		logger.debug("Bucket size: " + size);
		assertTrue(size > 0);
	}

	@Test
	public void testNonNullBucketEntries() throws SQLException, MainTypeException {
		NumberDetail n = Profile.getNumberDetail(BundleProfileAccumulator
				.generateProfile("test-field", Arrays.asList(2,4,4,4,5,5,7,9)));
		int size = n.getFreqHistogram().getLabels().size();
		logger.debug("Bucket size: " + size);
		assertTrue(size > 0);
		assertTrue(noNullValuesInHistogram(n.getFreqHistogram()));
	}

	public boolean noNullValuesInHistogram(Histogram histogram) {
		boolean hasNullValue = false;
		for(String label : histogram.getLabels()) {
			if(label.contains("null")) {
				hasNullValue = true;
			}
		}
		if(hasNullValue) return false;
		else return true;
	}
}
