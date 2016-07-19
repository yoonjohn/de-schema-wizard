package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.deserializors.SerializationUtility;

public class NumberProfileAccumulatorTest {
	private Logger logger = Logger.getLogger(NumberProfileAccumulatorTest.class);
	
	@Test
	public void testLargeExponentAccumulate() {
		NumberProfileAccumulator num = new NumberProfileAccumulator("test", 2);
		for(int i = 0; i < 100; i++) {
			num.accumulate(i);
		}
		num.accumulate(1000000);
		logger.warn("Exception should be thrown here.");
		num.accumulate("4E34");
		num.finish();
		int size = num.getState().getDetail().getHistogramOptional().get().getLabels().size();
		logger.info("Size is " + size);
		List<String> labels = num.getState().getDetail().getHistogramOptional().get().getLabels();
		String label = labels.get(labels.size()-1);
		String max = label.substring(label.indexOf(',')+1, label.indexOf(')'));
		BigDecimal numMax = new BigDecimal(max);
		assertTrue(num.getState().getDetail().getHistogramOptional().get().getLabels().size()>=50);
		assertTrue(num.getState().getDetail().getHistogramOptional().get().getLabels().size()<101);
		assertTrue(numMax.compareTo(BigDecimal.valueOf(1000000)) > 0);
	}
	
	@Test
	public void testLargeNegativeValueAccumulate() {
		NumberProfileAccumulator num = new NumberProfileAccumulator("test", 2);
		for(int i = 0; i < 100; i++) {
			num.accumulate(-i);
		}
		num.accumulate(-1000000);
		num.finish();
		logger.info("Size is " + num.getState().getDetail().getHistogramOptional().get().getLabels().size());
		assertTrue(num.getState().getDetail().getHistogramOptional().get().getLabels().size()>=50);
		assertTrue(num.getState().getDetail().getHistogramOptional().get().getLabels().size()<101);
	}

	@Test
	public void testEpochAccumulation() {
		NumberProfileAccumulator num = new NumberProfileAccumulator("epoch", "1442222200");
		String base = "1442222200";
		for(int i = 0; i < 10000; i++) {
			String add = String.valueOf(i);
			String replace = base.substring(0, base.length()-add.length()) + add;
			num.accumulate(replace);
		}
		num.finish();
		assertTrue(num.getState().getDetail().getHistogramOptional().get().getLabels().size()>49
				&& num.getState().getDetail().getHistogramOptional().get().getLabels().size()<101);
	}
	
	@Test
	public void testMinMaxAccumulate() {
		NumberProfileAccumulator num = new NumberProfileAccumulator("test", 2);
		num.accumulate(4);
		num.accumulate(4);
		num.accumulate(4);
		num.accumulate(5);
		num.accumulate(5);
		num.accumulate(7);
		num.accumulate(9);		
		num.finish();
		NumberDetail n = num.getNumberDetail();
		assertTrue(n.getMin().compareTo(BigDecimal.valueOf(2)) == 0);
		assertTrue(n.getMax().compareTo(BigDecimal.valueOf(9)) == 0);
		assertTrue(n.getAverage().compareTo(BigDecimal.valueOf(5)) == 0);
		assertTrue(Double.doubleToLongBits(n.getStdDev()) == Double.doubleToLongBits(2.0));
		logger.info("Test min max accumulate successfully.");
		logger.debug(SerializationUtility.serialize(num.getState()));
	}

	@Test
	public void testNonEmptyBucket() throws SQLException {
		NumberProfileAccumulator num = new NumberProfileAccumulator("test-field", 2);
		num.accumulate(4);
		num.accumulate(4);
		num.accumulate(4);
		num.accumulate(5);
		num.accumulate(5);
		num.accumulate(7);
		num.accumulate(9);		
		num.finish();
		NumberDetail n = num.getNumberDetail();
		int size = n.getFreqHistogram().getLabels().size();
		logger.debug("Bucket size: " + size);
		assertTrue(size > 0);
	}

	@Test
	public void testNonNullBucketEntries() throws SQLException {
		NumberProfileAccumulator num = new NumberProfileAccumulator("test-field", 2);
		num.accumulate(4);
		num.accumulate(4);
		num.accumulate(4);
		num.accumulate(5);
		num.accumulate(5);
		num.accumulate(7);
		num.accumulate(9);		
		num.finish();
		NumberDetail n = num.getNumberDetail();
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
