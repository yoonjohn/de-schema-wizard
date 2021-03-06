package com.deleidos.dp.histogram;

import java.util.ArrayList;
import java.util.List;

import com.deleidos.dp.beans.Histogram;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractBucketList implements BucketList {
	public static final int STRING_BUCKET_LENGTH_CUTOFF = 15;
	public static final int NUMBER_BUCKET_LENGTH_CUTOFF = 6;
	public abstract List<AbstractBucket> getOrderedBuckets();
	
	
	public Histogram asBean() {
		Histogram histogram = new Histogram();
		List<String> longLabels =  new ArrayList<String>();
		List<String> labels = new ArrayList<String>();
		List<Integer> data = new ArrayList<Integer>();
		
		for(AbstractBucket bucket : getOrderedBuckets()) {
			String label = bucket.getLabel();
			if(bucket instanceof NumberBucket) {
				labels.add(NumberBucket.trimRawLabel(label, NUMBER_BUCKET_LENGTH_CUTOFF));
			} else if(bucket instanceof TermBucket) {
				labels.add(TermBucket.trimRawLabel(label, STRING_BUCKET_LENGTH_CUTOFF));
			} else {
				labels.add("".equals(bucket.getLabel()) ? AbstractBucket.EMPTY_STRING_INDICATOR : bucket.getLabel());
			}
			longLabels.add("".equals(label) ? AbstractBucket.EMPTY_STRING_INDICATOR : label);
			data.add(bucket.getCount().intValue());
		}
		
		histogram.setLabels(labels);
		histogram.setLongLabels(longLabels);
		histogram.setData(data);
		return histogram;
	}

}
