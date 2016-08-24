package com.deleidos.dp.histogram;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A coalescing histogram for numerical values.  Until <i>L</i> is reached, values will be added into unique buckets.
 * This means no bucket in the histogram will have a value of 0.  Once |<i>B</i>| >= <i>L</i>, the buckets will be 
 * redefined by (max value) - (min value) / <i>L</i>.  One additional bucket will be added at the time of this transformation
 * because the ranges are inclusive on the lower bound, but exclusive on the upper bound. 
 * @author leegc
 *
 * @param <T> The bucket type
 */

public class AutoCoalescingNumberBucketList extends AbstractCoalescingBucketList {
	public static final int bucketExpansionLimit = 1000000;
	public static final AutoCoalescingNumberBucketList EMPTY = new AutoCoalescingNumberBucketList();
	private static Logger logger = Logger.getLogger(AutoCoalescingNumberBucketList.class);
	private BigDecimal currentColumnWidth = BigDecimal.valueOf(-1);
	private LinkedList<NumberBucket> bucketList;

	private AutoCoalescingNumberBucketList(long numBuckets, final BigDecimal min, final BigDecimal max) {
		bucketList = new LinkedList<NumberBucket>();

		BigDecimal big = new BigDecimal(numBuckets);
		final BigDecimal width = (max.subtract(min)).divide(big.subtract(BigDecimal.ONE), AbstractProfileAccumulator.DEFAULT_CONTEXT);

		for(int i = 0; i < numBuckets; i++) {
			BigDecimal base = min.add(width.multiply(BigDecimal.valueOf(i)));
			NumberBucket b =  new NumberBucket(base, width.add(base));
			bucketList.add(b);
		}
		currentColumnWidth = width;
	}

	public AutoCoalescingNumberBucketList() {
		bucketList = new LinkedList<NumberBucket>();
	}

	private boolean dynamicallyResizeHistogramAndAddValue(BigDecimal value) throws ArithmeticException {
		NumberBucket first = bucketList.getFirst();
		NumberBucket last = bucketList.getLast();
		if(first.belongs(value) < 0) {
			double numNecessaryBuckets = first.minBoundary.subtract(value)
					.divide(currentColumnWidth, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
			if(numNecessaryBuckets > bucketExpansionLimit) {
				throw new ArithmeticException("Magnitude of number too big for dynamic histogram.");
			}
			numNecessaryBuckets = Math.ceil(numNecessaryBuckets);
			int numTotalBuckets = (int)numNecessaryBuckets + bucketList.size();
			if(numTotalBuckets >= getNumBucketsHigh()) {		
				int numDoublesNecessary = (int)(Math.log((double)numTotalBuckets/getNumBucketsLow())/Math.log(2)) + 1;
				BigDecimal doubleCoefficient = BigDecimal.valueOf(Math.pow(2, numDoublesNecessary));
				BigDecimal newWidth = currentColumnWidth.multiply(doubleCoefficient);
				BigDecimal tempIncludedMinimum = last.minBoundary.subtract(BigDecimal.valueOf(getNumBucketsLow()-1).multiply(newWidth));		
				AutoCoalescingNumberBucketList newNumberBucketList = new AutoCoalescingNumberBucketList(getNumBucketsLow(), tempIncludedMinimum, last.minBoundary);
				for(int j = 0; j < bucketList.size(); j++) {
					NumberBucket bucketToAddTo = newNumberBucketList.getBucketList().get(j/doubleCoefficient.intValue());
					BigInteger count = bucketList.get(j).count;
					for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
						bucketToAddTo.incrementCount();
					}
				}
				currentColumnWidth = newNumberBucketList.getCurrentRange();
				bucketList = newNumberBucketList.getBucketList();
				Collections.sort(bucketList);
				numNecessaryBuckets = first.minBoundary.subtract(value)
						.divide(currentColumnWidth, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
				numNecessaryBuckets = Math.ceil(numNecessaryBuckets);
			}
			for(int i = 0; i < numNecessaryBuckets; i++) {
				BigDecimal newMin = bucketList.getFirst().minBoundary.subtract(currentColumnWidth);
				NumberBucket nb = new NumberBucket(newMin, newMin.add(currentColumnWidth));
				bucketList.addFirst(nb);
			}
		} else if(last.belongs(value) > 0) { 
			// determine number of buckets necessary to adjust the histogram to hold the new value
			double numNecessaryBuckets = value.subtract(last.maxBoundary)
					.divide(currentColumnWidth, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
			if(numNecessaryBuckets > bucketExpansionLimit) {
				// if it's too enormous throw the exception 
				throw new ArithmeticException("Number is too drastic of an outlier for dynamic histogram.");
			}
			// because range is exclusive on the upper bound, need to add an additional bucket if the value being added falls exactly on the upper end of a boundary
			// otherwise, it will be included in the final added bucket, so ceiling the number required
			numNecessaryBuckets = (numNecessaryBuckets == (int)numNecessaryBuckets) ? numNecessaryBuckets + 1 : Math.ceil(numNecessaryBuckets);
			int numTotalBuckets = (int)numNecessaryBuckets + bucketList.size();
			if(numTotalBuckets >= getNumBucketsHigh()) {
				int numDoublesNecessary = (int)(Math.log((double)numTotalBuckets/getNumBucketsLow())/Math.log(2));
				BigDecimal doubleCoefficient = BigDecimal.valueOf(Math.pow(2, numDoublesNecessary));
				BigDecimal newWidth = currentColumnWidth.multiply(doubleCoefficient);
				BigDecimal tempIncludedMaxValue = first.minBoundary.add(BigDecimal.valueOf(getNumBucketsLow()-1).multiply(newWidth));
				AutoCoalescingNumberBucketList newNumberBucketList = new AutoCoalescingNumberBucketList(getNumBucketsLow(), first.minBoundary, tempIncludedMaxValue);
				for(int j = 0; j < bucketList.size(); j++) {
					NumberBucket bucketToAddTo = newNumberBucketList.getBucketList().get(j/doubleCoefficient.intValue());
					BigInteger count = bucketList.get(j).count;
					for(int i = 0; i < count.intValue(); i++) { //possible loss of precision
						bucketToAddTo.incrementCount();
					}
				}
				currentColumnWidth = newNumberBucketList.getCurrentRange();
				bucketList = newNumberBucketList.getBucketList();
				Collections.sort(bucketList);
				// re-asses number of necessary buckets with new histogram
				numNecessaryBuckets = value.subtract(bucketList.getLast().maxBoundary)
						.divide(currentColumnWidth, MetricsCalculationsFacade.DEFAULT_CONTEXT).doubleValue();
				numNecessaryBuckets = (numNecessaryBuckets == (int)numNecessaryBuckets) ? numNecessaryBuckets + 1 : Math.ceil(numNecessaryBuckets);
			}
			// create leftover buckets
			for(int i = 0; i < numNecessaryBuckets; i++) {
				BigDecimal newMax = bucketList.getLast().maxBoundary.add(currentColumnWidth);
				NumberBucket nb = new NumberBucket(newMax.subtract(currentColumnWidth), newMax);
				bucketList.addLast(nb);
			}
		} 
		return binarySearchAdd(value);
	}

	@Override
	public boolean putValue(Object object) {
		boolean successfullyAdded = false;
		int comp = currentColumnWidth.compareTo(BigDecimal.ZERO);
		BigDecimal value = new BigDecimal(object.toString());
		if(comp > 0) {
			try {
				successfullyAdded = dynamicallyResizeHistogramAndAddValue(value);
			} catch (ArithmeticException e) {
				logger.info(e);
				return false;
			}
		} else if(comp < 0){
			if(bucketList.size() == 0) {
				NumberBucket bucket = new NumberBucket(value);
				bucket.incrementCount();
				bucketList.addFirst(bucket);
				successfullyAdded = true;
			} else if(bucketList.size() == 1) {
				if(bucketList.get(0).belongs(object) == 0) {
					bucketList.get(0).incrementCount();
				} else {
					NumberBucket bucket = new NumberBucket(value);
					bucket.incrementCount();
					bucketList.add(bucket);
					Collections.sort(bucketList);
				}
				successfullyAdded = true;
			} else {
				successfullyAdded = binarySearchAdd(object);
				if(!successfullyAdded) {
					if(bucketList.size() >= getNumBucketsLow()) {
						transformToRange();
						try {
							successfullyAdded = dynamicallyResizeHistogramAndAddValue(value);
						} catch (ArithmeticException e) {
							logger.error(e);
							return false;
						}
					} else {
						NumberBucket bucket = new NumberBucket(value);
						bucket.incrementCount();
						bucketList.add(bucket);
						Collections.sort(bucketList);
						successfullyAdded = true;
					}
				}
			}
		} else {
			throw new RuntimeException("Unexpected current column width: 0.");
		}	
		return successfullyAdded;
	}

	/**
	 * Try to put a value in the range of the histogram.  If it is not in the range of the histogram, no value will
	 * be added.
	 * @param object The object to be added to the histogram
	 * @return True if the object was added, false if it was not.

	public boolean simplePutValue(Object object) {
		int min = 0;
		int max = bucketList.size() - 1;
		int half = max/2;
		int c = 0;
		while(min <= max) {
			c = bucketList.get(half).belongs(object);
			if(c > 0) {
				min = half + 1;
			} else if(c < 0){
				max = half - 1;
			} else {
				bucketList.get(half).incrementCount();
				return true;
			}
			half = (max - min)/2 + min;
		}
		return false;
	}*/

	@JsonIgnore
	public BigDecimal getCurrentRange() {
		return currentColumnWidth;
	}

	@Override
	public void transformToRange() {
		AutoCoalescingNumberBucketList nbl = new AutoCoalescingNumberBucketList(bucketList.size(), bucketList.getFirst().minBoundary, bucketList.getLast().minBoundary);
		//nlogn could maybe do better
		for(int j = 0; j < bucketList.size(); j++) {
			BigInteger count = bucketList.get(j).count;
			BigDecimal value = bucketList.get(j).minBoundary;
			for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
				nbl.binarySearchAdd(value);
			}
		}
		currentColumnWidth = nbl.currentColumnWidth;
		bucketList = nbl.getBucketList();
	}

	@Override
	public void coalesce() {

		Collections.sort(bucketList);
		if(currentColumnWidth.compareTo(BigDecimal.ZERO) == -1) {
			transformToRange();
			return;
		} else {
			AutoCoalescingNumberBucketList nbl = new AutoCoalescingNumberBucketList(getNumBucketsLow(), bucketList.getFirst().minBoundary, bucketList.getLast().minBoundary);
			nbl.getBucketList().addLast(new NumberBucket(nbl.getBucketList().getLast().maxBoundary, nbl.getBucketList().getLast().maxBoundary.add(nbl.currentColumnWidth)));
			//nlogn could maybe do better
			for(int j = 0; j < bucketList.size(); j++) {
				BigInteger count = bucketList.get(j).count;
				BigDecimal value = bucketList.get(j).minBoundary;
				for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
					nbl.binarySearchAdd(value);
				}
			}
			currentColumnWidth = nbl.currentColumnWidth;
			bucketList = nbl.getBucketList();
		}
	}

	@JsonIgnore
	public LinkedList<NumberBucket> getBucketList() {
		return bucketList;
	}

	public void setBucketList(LinkedList<NumberBucket> histogram) {
		bucketList = histogram;
	}

	@Override
	public String toString() {
		JSONArray jArr = new JSONArray();
		for(int i = 0; i < bucketList.size(); i++) {
			JSONObject obj = new JSONObject();
			//obj.put("definition", bucketList.get(i).getLabel());
			//obj.put("count", bucketList.get(i).getCount());
			obj.put(bucketList.get(i).getLabel(), bucketList.get(i).getCount());
			jArr.put(obj);
		}
		return jArr.toString();
	}

	@Override
	public List<AbstractBucket> getOrderedBuckets() {
		LinkedList<AbstractBucket> list = new LinkedList<AbstractBucket>();
		for(NumberBucket b : bucketList) {
			list.add(b);
		}
		return list;
	}
	
}
