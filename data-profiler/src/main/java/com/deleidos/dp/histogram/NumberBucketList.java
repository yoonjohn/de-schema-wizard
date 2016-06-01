package com.deleidos.dp.histogram;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
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

public class NumberBucketList extends AbstractCoalescingBucketList {
	public static final NumberBucketList EMPTY = new NumberBucketList();
	private static Logger logger = Logger.getLogger(NumberBucketList.class);
	private BigDecimal currentColumnWidth = BigDecimal.valueOf(-1);
	private LinkedList<NumberBucket> bucketList;

	private NumberBucketList(long numBuckets, final BigDecimal min, final BigDecimal max) {
		bucketList = new LinkedList<NumberBucket>();

		BigDecimal big = new BigDecimal(numBuckets);
		final BigDecimal width = (max.subtract(min).add(BigDecimal.ONE)).divide(big, AbstractProfileAccumulator.DEFAULT_CONTEXT);

		
		for(int i = 0; i < numBuckets; i++) {
			BigDecimal base = min.add(width.multiply(BigDecimal.valueOf(i)));
			NumberBucket b =  new NumberBucket(base, width.add(base));
			bucketList.add(b);
		}
		currentColumnWidth = width;
	}

	public NumberBucketList() {
		bucketList = new LinkedList<NumberBucket>();
	}

	@Override
	public boolean putValue(Object object) {
		try {
			int comp = currentColumnWidth.compareTo(BigDecimal.ZERO);
			if(comp == 1) {
				NumberBucket first = bucketList.getFirst();
				NumberBucket last = bucketList.getLast();
				if(first.belongs(object) == -1) { 
					NumberBucket bucket = new NumberBucket(first.minBoundary.subtract(currentColumnWidth), first.minBoundary);
					bucketList.addFirst(bucket);
					putValue(object);
					Collections.sort(bucketList);
					if(bucketList.size() >= getNumBucketsHigh()) {
						coalesce();
					}
					return true;
				} else if(last.belongs(object) == 1) { 
					NumberBucket bucket = new NumberBucket(last.maxBoundary, last.maxBoundary.add(currentColumnWidth));
					bucketList.addLast(bucket);
					putValue(object);
					Collections.sort(bucketList);
					if(bucketList.size() >= getNumBucketsHigh()) {
						coalesce();
					}
					return true;
				} 
			} else if(comp == 0) {
				return true;
			} else {
				if(bucketList.size() == 0) {
					NumberBucket bucket = new NumberBucket(new BigDecimal(object.toString()));
					bucket.incrementCount();
					bucketList.addFirst(bucket);
				} else if(bucketList.size() == 1) {
					if(bucketList.get(0).belongs(object) == 0) {
						bucketList.get(0).incrementCount();
					} else {
						NumberBucket bucket = new NumberBucket(new BigDecimal(object.toString()));
						bucket.incrementCount();
						bucketList.add(bucket);
						Collections.sort(bucketList);
					}
				} else {
					boolean added = simplePutValue(object);
					if(!added) {
						NumberBucket bucket = new NumberBucket(new BigDecimal(object.toString()));
						bucket.incrementCount();
						bucketList.add(bucket);
						Collections.sort(bucketList);
						if(bucketList.size() >= getNumBucketsLow()) {
							transformToRange();
						}
						return true;
					}
				}
				return true;
			}
		} catch (NumberFormatException e) {
			//log
			logger.error("Object " + object + " is not able to be parsed as a number.");
			return false;
		} catch (Exception e) {
			logger.error("Object " + object + " cannot be loaded into bucket.");
			e.printStackTrace();
			// log
			return false;
		}
		if(simplePutValue(object)) return true;
		else return false;
	}

	/**
	 * Try to put a value in the range of the histogram.  If it is not in the range of the histogram, no value will
	 * be added.
	 * @param object The object to be added to the histogram
	 * @return True if the object was added, false if it was not.
	 */
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
	}

	@JsonIgnore
	public BigDecimal getCurrentRange() {
		return currentColumnWidth;
	}

	@Override
	public void transformToRange() {
		NumberBucketList nbl = new NumberBucketList(bucketList.size(), bucketList.getFirst().minBoundary, bucketList.getLast().minBoundary);
		nbl.getBucketList().addLast(new NumberBucket(nbl.getBucketList().getLast().maxBoundary, nbl.getBucketList().getLast().maxBoundary.add(nbl.currentColumnWidth)));
		//nlogn could maybe do better
		for(int j = 0; j < bucketList.size(); j++) {
			BigInteger count = bucketList.get(j).count;
			BigDecimal value = bucketList.get(j).minBoundary;
			for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
				nbl.simplePutValue(value);
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
			NumberBucketList nbl = new NumberBucketList(getNumBucketsLow(), bucketList.getFirst().minBoundary, bucketList.getLast().minBoundary);
			nbl.getBucketList().addLast(new NumberBucket(nbl.getBucketList().getLast().maxBoundary, nbl.getBucketList().getLast().maxBoundary.add(nbl.currentColumnWidth)));
			//nlogn could maybe do better
			for(int j = 0; j < bucketList.size(); j++) {
				BigInteger count = bucketList.get(j).count;
				BigDecimal value = bucketList.get(j).minBoundary;
				for(int i = 0; i< count.intValue(); i++) { //possible loss of precision
					nbl.simplePutValue(value);
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
