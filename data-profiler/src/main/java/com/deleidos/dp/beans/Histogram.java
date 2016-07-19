package com.deleidos.dp.beans;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.ByteBucket;
import com.deleidos.dp.histogram.ByteBucketList;
import com.deleidos.dp.histogram.NumberBucket;
import com.deleidos.dp.histogram.NumberBucketList;
import com.deleidos.dp.histogram.TermBucket;
import com.deleidos.dp.histogram.TermBucketList;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Histogram {
	private List<Object> exampleValues;
	private String type = "bar";
	private String series = "Values";
	private String yaxis = "Frequency";
	private List<String> labels;
	private List<String> longLabels;
	private List<Integer> data;
	private RegionData regionData = null;
	
	public Histogram() {
		labels = new ArrayList<String>();
		longLabels = new ArrayList<String>();
		data = new ArrayList<Integer>();
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getSeries() {
		return series;
	}
	
	public void setSeries(String series) {
		this.series = series;
	}
	
	public List<String> getLabels() {
		return labels;
	}
	
	public void setLabels(List<String> labels) {
		this.labels = labels;
	}
	
	@JsonProperty("long-labels")
	public List<String> getLongLabels() {
		return longLabels;
	}
	
	@JsonProperty("long-labels")
	public void setLongLabels(List<String> longLabels) {
		this.longLabels = longLabels;
	}
	
	public List<Integer> getData() {
		return data;
	}
	
	public void setData(List<Integer> data) {
		this.data = data;
	}
	
	@JsonProperty("region-data")
	public RegionData getRegionData() {
		return regionData;
	}
	
	@JsonProperty("region-data")
	public void setRegionData(RegionData regionData) {
		this.regionData = regionData;
	}

	public List<Object> getExampleValues() {
		return exampleValues;
	}

	public void setExampleValues(List<Object> exampleValues) {
		this.exampleValues = exampleValues;
	}

	public String getYaxis() {
		return yaxis;
	}

	public void setYaxis(String yaxis) {
		this.yaxis = yaxis;
	}
	
	public static ByteBucketList toByteBucketList(Histogram histogram) {
		ByteBucketList byteBucketList = new ByteBucketList();
		for(int i = 0; i < histogram.getLabels().size(); i++) {
			String definition = histogram.getLongLabels().get(i);
			byte byteDef = Byte.valueOf(definition);
			int count = histogram.getData().get(i);
			ByteBucket nb = new ByteBucket(byteDef, BigInteger.valueOf(count));
			byteBucketList.getBucketList().add(nb);
		}
		return byteBucketList;
	}
	
	public static TermBucketList toTermBucketList(Histogram histogram) {
		TermBucketList termBucketList = new TermBucketList();
		for(int i = 0; i < histogram.getLabels().size(); i++) {
			String definition = histogram.getLongLabels().get(i);
			int count = histogram.getData().get(i);
			TermBucket nb = new TermBucket(definition, BigInteger.valueOf(count));
			termBucketList.getBucketList().add(nb);
		}
		return termBucketList;
	}
	
	public static NumberBucketList toNumberBucketList(Histogram histogram) {
		NumberBucketList numberBucketList = new NumberBucketList();
		for(int i = 0; i < histogram.getLabels().size(); i++) {
			String shortDef = histogram.getLabels().get(i);
			String longDef = histogram.getLongLabels().get(i);
			int count = histogram.getData().get(i);
			NumberBucket nb = new NumberBucket(longDef, shortDef, BigInteger.valueOf(count));
			numberBucketList.getBucketList().add(nb);
		}
		return numberBucketList;
	}
	

}
