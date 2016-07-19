package com.deleidos.dp.histogram;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedList;
import java.util.List;

import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.SerializationUtility;

public class DefinedRangesNumberBucketList extends AbstractBucketList {
	private static final int numDistinctThatDictatesRange = 50;
	public static final MathContext HISTOGRAM_CONTEXT = MathContext.DECIMAL32;
	private boolean isRangedHistogram;
	private LinkedList<NumberBucket> bucketList;
	
	public DefinedRangesNumberBucketList(NumberDetail existingDetail) {
		bucketList = determineHistogram(existingDetail.getMin(), existingDetail.getMax(), existingDetail.getNumDistinctValues());
	}
	
	public DefinedRangesNumberBucketList(BigDecimal min, BigDecimal max, Integer numDistinct) {
		bucketList = determineHistogram(min, max, numDistinct);
	}

	private LinkedList<NumberBucket> determineHistogram(BigDecimal min, BigDecimal max, String numDistinct) {
		Integer doubleNumDistinct = MetricsCalculationsFacade
				.stripNumDistinctValuesChars(numDistinct);
		return determineHistogram(min, max, doubleNumDistinct);
	}
	
	private LinkedList<NumberBucket> determineHistogram(BigDecimal min, BigDecimal max, Integer numDistinct) {
		BigDecimal diff = max.subtract(min);
		LinkedList<NumberBucket> list = new LinkedList<NumberBucket>();
		if(numDistinct <= numDistinctThatDictatesRange) {
			isRangedHistogram = false;
		} else {
			isRangedHistogram = true;
			if(diff.compareTo(BigDecimal.ONE) > 0) {
				// TODO
				int roundingScale = 0; 
				BigDecimal roundedMax = max.setScale(0, BigDecimal.ROUND_CEILING);
				BigDecimal roundedMin = min.setScale(0, BigDecimal.ROUND_FLOOR);
			}
		}
		
		return list;
	}

	@Override
	public boolean putValue(Object object) {
		List<AbstractBucket> bucketList = this.getOrderedBuckets();
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

	@Override
	public List<AbstractBucket> getOrderedBuckets() {
		LinkedList<AbstractBucket> list = new LinkedList<AbstractBucket>();
		for(NumberBucket b : bucketList) {
			list.add(b);
		}
		return list;
	}

}
