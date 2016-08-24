package com.deleidos.dp.histogram;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ShortStringBucketList extends AbstractCoalescingBucketList {
	public static final ShortStringBucketList EMPTY = new ShortStringBucketList();
	LinkedList<TermBucket> bucketList;
	private boolean uniqueBucketStage = true;

	public ShortStringBucketList() {
		bucketList = new LinkedList<TermBucket>();
	}

	@Override
	public boolean putValue(Object object) {
		String term = object.toString();
		if(!binarySearchAdd(object)) {
			if(bucketList.size() >= getNumBucketsHigh() - 1 && !uniqueBucketStage) {
				TermBucket tb = new TermBucket(term);
				tb.incrementCount();
				bucketList.add(tb);
				Collections.sort(bucketList);
				coalesce();
			} else if(bucketList.size() == getNumBucketsHigh() && uniqueBucketStage) {
				transformToRange();
				TermBucket tb = new TermBucket(term);
				tb.incrementCount();
				bucketList.add(tb);
				Collections.sort(bucketList);
				uniqueBucketStage = false;
			} else {
				TermBucket tb = new TermBucket(term);
				tb.incrementCount();
				bucketList.add(tb);
				Collections.sort(bucketList);
			}
		}
		return false;
	}

	@Override
	public void coalesce() {
		LinkedList<TermBucket> tmpBucketList = new LinkedList<TermBucket>();
		for(int i = 0; i < bucketList.size(); i+=2) {
			String low = bucketList.get(i).lowerBound;
			String high = (bucketList.get(i+1).upperBound == null) ? bucketList.get(i+1).lowerBound : bucketList.get(i+1).upperBound;
			TermBucket tmp = new TermBucket(low, high);
			tmp.count = bucketList.get(i).count.add(bucketList.get(i+1).count);
			tmpBucketList.add(tmp);
		}
		bucketList.clear();
		bucketList = tmpBucketList;
	}

	@Override
	public void transformToRange() {
		for(int i = 0; i < bucketList.size(); i++) {
			bucketList.get(i).upperBound = bucketList.get(i).lowerBound;
		}
		coalesce();
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

	@JsonIgnore
	public LinkedList<TermBucket> getBucketList() {
		return bucketList;
	}

	public void setBucketList(LinkedList<TermBucket> bucketList) {
		this.bucketList = bucketList;
	}

	@Override
	public List<AbstractBucket> getOrderedBuckets() {
		LinkedList<AbstractBucket> list = new LinkedList<AbstractBucket>();
		for(TermBucket b : bucketList) {
			list.add(b);
		}
		return list;
	}

}
