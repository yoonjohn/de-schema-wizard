package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.histogram.AbstractBucketList;
import com.deleidos.dp.histogram.NumberBucket;
import com.deleidos.dp.histogram.NumberBucketList;

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
	public void testBucketInsert() {
		Random random = new Random(10);
		NumberBucketList bl = new NumberBucketList();
		for(int i = 0; i < 1000000; i++) {
			int previousSize = bl.getBucketList().size();
			String bufferedOutput = "Size before coalesce: " + bl.getBucketList().size();
			int r2 = random.nextInt(1000);
			bl.putValue(r2);
			if(previousSize > bl.getBucketList().size()) {
				logger.info(bufferedOutput);
				logger.info("Size after coalesce: " + bl.getBucketList().size());
			}
		}
		int size = bl.getBucketList().size();
		assertTrue(size >= NumberBucketList.DEFAULT_NUM_BUCKETS_LOW && size <= NumberBucketList.DEFAULT_NUM_BUCKETS_HIGH);
		logger.debug(SerializationUtility.serialize(bl));
	}

	@Test
	public void coalesceTest() {
		boolean beforeCoalesceIsOk = false;
		boolean afterCoalesceIsOk = false;
		NumberBucketList bl = new NumberBucketList();
		for(int i = 1; i < 51; i++) { //started at 1 for human readability
			if(i == 50) {
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

	private boolean isPostCoalesceOK(NumberBucketList bl) {
		boolean isOk = true;
		for(int i = 0; i < bl.getBucketList().size(); i++) {
			NumberBucket nb = bl.getBucketList().get(i);
			if(nb.getCount().compareTo(BigInteger.ONE) != 0) {
				isOk = false;
			}
		}
		return isOk;
	}

	public boolean isPreCoalesceOK(NumberBucketList bl) {
		boolean isOk = true;
		for(NumberBucket nb : bl.getBucketList()) {
			if(nb.getCount().compareTo(BigInteger.ONE) != 0) {
				isOk = false;
			}
		}
		return isOk;
	}

	@Test
	public void testUniqueUnrangedBucket() {
		NumberBucketList bl = new NumberBucketList();
		for(int i = 0; i < 10; i++) {
			bl.putValue(i);
			bl.putValue(i);
			bl.putValue(i);
			bl.putValue(i);
			bl.putValue(i);
		}
		assertTrue(assureUnrangedBucketLabel(bl) && bl.getBucketList().size() == 10);
	}

	@Test
	public void testRigidBucketInsert() {
		NumberBucketList bl = new NumberBucketList();
		for(int i = 0; i < 110; i++) {
			bl.putValue(i);
			bl.putValue(i);
		}
		logger.debug(bl);
		assertTrue(assureBucketsHaveCountN(bl, 4));
	}

	public boolean assureUnrangedBucketLabel(NumberBucketList bl) {
		boolean isOk = true;
		for(NumberBucket nb : bl.getBucketList()) {
			if(nb.getLabel().contains("[") || nb.getLabel().contains(",") || nb.getLabel().contains(")")) isOk = false;
		}
		return isOk;
	}

	public boolean assureBucketsHaveCountN(NumberBucketList bl, int n) {
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
