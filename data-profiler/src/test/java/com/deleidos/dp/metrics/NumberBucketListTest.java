package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractCoalescingBucketList;
import com.deleidos.dp.histogram.AbstractNumberBucketList;
import com.deleidos.dp.histogram.NumberBucket;

public class NumberBucketListTest {
	private Logger logger = Logger.getLogger(NumberBucketListTest.class);

	/*@Test
	public void testTrimLength() {
		Random random = new Random(10);
		NumberBucketList bl = new NumberBucketList();
		for(int i = 0; i < 1000000; i++) {
			long r2 = random.nextLong() + 1000000;
			bl.putValue(r2);
		}
		int maxLengthA = 0;
		for(String x : bl.getLabels()) {
			maxLengthA = (x.length() > maxLengthA) ? x.length() : maxLengthA;
		}
		AbstractBucketList bl2 = AbstractBucketList.trimLabelLenght(bl, 4);
		System.out.println(SerializationUtility.serialize(bl));
		System.out.println(SerializationUtility.serialize(bl2));
	}*/
	
	@Test
	public void testBucketValueTrim() throws MainTypeException {
		List<Object> values = new ArrayList<Object>();
		Random random = new Random(1);
		for(int i = 0; i < 1000; i++) {
			values.add(random.nextDouble()*.1);
		}
		Profile profile = BundleProfileAccumulator.generateProfile("bucket-test", values);
		Histogram histogram = profile.getDetail().getHistogramOptional().get();
		logger.info("Testing " + histogram.getLabels().get(0));
		assertTrue(histogram.getLabels().get(0).contains("0.05"));
	}

	@Test
	public void testBucketInsert() throws MainTypeException {
		Random random = new Random(10);
		List<Object> values = new ArrayList<Object>();
		long expectedTotal = 1000000;
		for(int i = 0; i < expectedTotal; i++) {
			int r2 = random.nextInt(1000);
			values.add(r2);
		}
		Profile profile = BundleProfileAccumulator.generateFirstPassProfile("test-field", values);
		AbstractNumberBucketList bl = AbstractNumberBucketList.newNumberBucketList(Profile.getNumberDetail(profile));
		for(int i = 0; i < values.size(); i++) {
			int previousSize = bl.getBucketList().size();
			String bufferedOutput = "Size before coalesce: " + bl.getBucketList().size();
			int r2 = Integer.valueOf(values.get(i).toString());
			bl.putValue(r2);
			if(previousSize > bl.getBucketList().size()) {
				logger.info(bufferedOutput);
				logger.info("Size after coalesce: " + bl.getBucketList().size());
			}
		}
		int size = bl.getBucketList().size();
		long total = 0;
		for(NumberBucket nb : bl.getBucketList()) {
			total += nb.getCount().longValue();
		}
		try {
		assertTrue(expectedTotal == total);
		} catch (AssertionError e) {
			logger.error(expectedTotal + " != " + total);
			throw e;
		}
		assertTrue(size >= AbstractCoalescingBucketList.DEFAULT_NUM_BUCKETS_LOW && size <= AbstractCoalescingBucketList.DEFAULT_NUM_BUCKETS_HIGH);
		assertTrue(bl.getBucketList().get(0).getCount().compareTo(BigInteger.ZERO) > 0);
		logger.debug(SerializationUtility.serialize(bl.asBean()));
	}

	//@Test
	public void coalesceTest() throws MainTypeException {
		boolean beforeCoalesceIsOk = false;
		boolean afterCoalesceIsOk = false;
		List<Object> values = new ArrayList<Object>();
		for(int i = 1; i < 51; i++) { //started at 1 for human readability
			values.add(i);
		}
		AbstractNumberBucketList bl = AbstractNumberBucketList.newNumberBucketList(
				Profile.getNumberDetail(BundleProfileAccumulator.generateFirstPassProfile("test-field", values)));
		for(int i = 0; i < values.size(); i++) { //started at 1 for human readability
			if(i == 49) {
				logger.debug(bl);
				beforeCoalesceIsOk = isPreCoalesceOK(bl);
				bl.putValue(i);
				afterCoalesceIsOk = isPostCoalesceOK(bl);
				logger.debug(bl);
			} else {
				bl.putValue(i);
			}
		}
		if(!beforeCoalesceIsOk) {
			logger.error("Error before coalesce.");
		}
		if(!afterCoalesceIsOk) {
			logger.error("Error after coalesce.");
		}
		assertTrue(beforeCoalesceIsOk && afterCoalesceIsOk);
	}

	/*private boolean isPostCoalesceOK(OldNumberBucketList bl) {
		boolean isOk = true;
		int total = 0;
		for(NumberBucket nb : bl.getBucketList()) {
			total += nb.getCount().intValue();
		}
		isOk = total == 50;
		return isOk;
	}

	public boolean isPreCoalesceOK(OldNumberBucketList bl) {
		boolean isOk = true;
		int total = 0;
		for(NumberBucket nb : bl.getBucketList()) {
			total += nb.getCount().intValue();
		}
		isOk = total == 49;
		return isOk;
	}*/
	
	private boolean isPostCoalesceOK(AbstractNumberBucketList bl) {
		boolean isOk = true;
		for(int i = 0; i < bl.getBucketList().size(); i++) {
			NumberBucket nb = bl.getBucketList().get(i);
			if(nb.getCount().compareTo(BigInteger.ONE) != 0) {
				isOk = false;
			}
		}
		return isOk;
	}

	public boolean isPreCoalesceOK(AbstractNumberBucketList bl) {
		boolean isOk = true;
		for(NumberBucket nb : bl.getBucketList()) {
			if(nb.getCount().compareTo(BigInteger.ONE) != 0) {
				isOk = false;
			}
		}
		return isOk;
	}

	public boolean assureUnrangedBucketLabel(AbstractNumberBucketList bl) {
		boolean isOk = true;
		for(NumberBucket nb : bl.getBucketList()) {
			if(nb.getLabel().contains("[") || nb.getLabel().contains(",") || nb.getLabel().contains(")")) isOk = false;
		}
		return isOk;
	}

	public boolean assureBucketsHaveCountN(AbstractNumberBucketList bl, int n) {
		BigInteger big = BigInteger.valueOf(n);
		boolean err = false;
		for(NumberBucket nb : bl.getBucketList()) {
			if(nb.getCount().compareTo(big) != 0) {
				err = true;
			}
		}
		return !err;
	}
}
