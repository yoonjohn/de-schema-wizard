package com.deleidos.dp.histogram;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;

public class UniqueValuesNumberBucketList extends AbstractNumberBucketList {

	protected UniqueValuesNumberBucketList() {
		super(true);
		bucketList = new LinkedList<NumberBucket>();
	}
	
	protected UniqueValuesNumberBucketList(Histogram existingSchemaHistogram) {
		super(true);
		bucketList = new LinkedList<NumberBucket>();
		List<String> shortLabels = existingSchemaHistogram.getLabels();
		List<String> labels = existingSchemaHistogram.getLongLabels();
		List<Integer> counts = existingSchemaHistogram.getData();
		for(int i = 0; i < existingSchemaHistogram.getLongLabels().size(); i++) {
			NumberBucket bucket = new NumberBucket(labels.get(i), shortLabels.get(i), BigInteger.valueOf(counts.get(i)));
			bucketList.add(bucket);
		}
	}

	@Override
	public void coalesce() {
		throw new MainTypeRuntimeException("Transform should not be called by unique number bucket list!");
	}

	@Override
	public boolean putValue(Object object) {
		BigDecimal value = new BigDecimal(object.toString());
		boolean successfullyAdded;
		/*if(bucketList.size() == 0) {
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
				NumberBucket bucket = new NumberBucket(value);
				bucket.incrementCount();
				bucketList.add(bucket);
				Collections.sort(bucketList);
				successfullyAdded = true;
			}
		}*/
		successfullyAdded = binarySearchAdd(value);
		if(!successfullyAdded) {
			NumberBucket bucket = new NumberBucket(value);
			bucket.incrementCount();
			bucketList.add(bucket);
			Collections.sort(bucketList);
			successfullyAdded = true;
		}
		return successfullyAdded;
	}
}
