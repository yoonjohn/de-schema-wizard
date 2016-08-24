package com.deleidos.dp.histogram;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;
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

public abstract class AbstractNumberBucketList extends AbstractCoalescingBucketList {
	public static final int bucketExpansionLimit = 1000000;
	private static Logger logger = Logger.getLogger(AbstractNumberBucketList.class);
	protected final boolean areBucketsUnique;
	protected LinkedList<NumberBucket> bucketList;

	protected AbstractNumberBucketList(boolean areBucketsUnique) {
		this.areBucketsUnique = areBucketsUnique;
	}

	@Override
	public void transformToRange() {
		throw new MainTypeRuntimeException("Transform should not be called by new number bucket list!");
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

	public static AbstractNumberBucketList newNumberBucketList(NumberDetail firstPassNumberDetail) {
		Integer distinctValues = 
				MetricsCalculationsFacade.stripNumDistinctValuesChars(firstPassNumberDetail.getNumDistinctValues());
		if(distinctValues < 50) {
			return new UniqueValuesNumberBucketList();
		} else {
			return new DefinedRangesNumberBucketList(firstPassNumberDetail.getMin(), firstPassNumberDetail.getMax()); 
		}
	}

	public static AbstractNumberBucketList newNumberBucketList(NumberDetail schemaNumberDetail, List<NumberDetail> sampleNumberDetails) {
		// need to determine to use unique buckets or ranges
		// if histogram data is in unique bucket format, try to keep it that way until switching to ranges is necessary
		List<BigDecimal> distinct = new ArrayList<BigDecimal>();
		NumberDetail firstNonNullDetail = (schemaNumberDetail != null) ? schemaNumberDetail : sampleNumberDetails.get(0);
		Optional<BigDecimal> maxOrNull = Optional.ofNullable(NumberBucket.parseLabel(firstNonNullDetail.getFreqHistogram().getLabels().get(0))[1]);
		boolean isRanged = maxOrNull.isPresent();

		if(schemaNumberDetail != null && isRanged) {
			return new DefinedRangesNumberBucketList(schemaNumberDetail.getFreqHistogram());
		} 
		BigDecimal min = firstNonNullDetail.getMin();
		BigDecimal max = firstNonNullDetail.getMax();
		if(schemaNumberDetail != null) {
			// already know that schema freq histogram is not ranged, so add all the mins to distinct list
			for(String label : schemaNumberDetail.getFreqHistogram().getLongLabels()) {
				BigDecimal[] minAndMax = NumberBucket.parseLabel(label);
				if(!distinct.contains(minAndMax[0])) {
					distinct.add(minAndMax[0]);
				}
			}
		}
		for(int i = 0; i < sampleNumberDetails.size(); i++) {
			NumberDetail numberMetrics = sampleNumberDetails.get(i);
			min = (min.compareTo(numberMetrics.getMin()) > 0) ? numberMetrics.getMin() : min;
			max = (max.compareTo(numberMetrics.getMax()) < 0) ? numberMetrics.getMax() : max;
			if(!isRanged) {
				for(String label : numberMetrics.getFreqHistogram().getLongLabels()) {
					BigDecimal[] minAndMax = NumberBucket.parseLabel(label);
					if(minAndMax[1] != null) {
						isRanged = true;
						break;
					} else {
						if(!distinct.contains(minAndMax[0])) {
							distinct.add(minAndMax[0]);
						}
					}
					if(distinct.size() >= 50) {
						isRanged = true;
						break;
					}
				}
			}
		}
		Integer numBuckets = (isRanged) ? 50 : distinct.size();
		// must default to 50 buckets here - case where schema is ranged is handled above
		if(numBuckets >= 50) {
			return new DefinedRangesNumberBucketList(min, max);
		} else if(schemaNumberDetail != null) {
			return new UniqueValuesNumberBucketList(schemaNumberDetail.getFreqHistogram());
		} else {
			return new UniqueValuesNumberBucketList();
		}
	}

}
