package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Test;

import com.deleidos.dp.accumulator.StringProfileAccumulator;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.histogram.AbstractBucketList;

public class StringProfileAccumulatorTest {
	private Logger logger = Logger.getLogger(StringProfileAccumulatorTest.class);
	@Test
	public void testStringMetricsAccumulation() throws SQLException {
		StringProfileAccumulator str = new StringProfileAccumulator("test-field", "hello");
		str.accumulate("hello");
		str.accumulate("hello world");
		str.accumulate("greetings");
		str.finish();
		StringDetail s = Profile.getStringDetail(str.getState());
		assertTrue(Double.doubleToLongBits(s.getAverageLength()) == Double.doubleToLongBits(7.5));
		assertTrue(s.getMinLength() == 5);
		assertTrue(s.getMaxLength() == 11);
		assertTrue(Integer.valueOf(s.getNumDistinctValues()) == 3);
		assertTrue((s.getStdDevLength() - 2.59) < .01);
		logger.info("Average, min, max, number of distinct, and standard deviation successfully accumulated.");
		logger.debug(s.getTermFreqHistogram());

		logger.debug(SerializationUtility.serialize(s));
		assertTrue(noNullValuesInHistogram(s.getTermFreqHistogram()));

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

	@Test
	public void testTermBucketIsNotEmpty() throws SQLException {
		StringProfileAccumulator str = new StringProfileAccumulator("test-field", "hello");
		str.accumulate("hello");
		str.accumulate("hello world");
		str.accumulate("greetings");
		str.finish();
		StringDetail s = Profile.getStringDetail(str.getState());

		assertTrue(Double.doubleToLongBits(s.getAverageLength()) == Double.doubleToLongBits(7.5));
		assertTrue(s.getMinLength() == 5);
		assertTrue(s.getMaxLength() == 11);
		assertTrue(Integer.valueOf(s.getNumDistinctValues()) == 3);
		assertTrue((s.getStdDevLength() - 2.59) < .01);		
		logger.info("Average, min, max, number of distinct, and standard deviation successfully accumulated.");
		logger.debug(s.getTermFreqHistogram());

		int size = s.getTermFreqHistogram().getLabels().size();
		logger.info("Size: " + size);
		assertTrue(size > 0);

	}

	
}
