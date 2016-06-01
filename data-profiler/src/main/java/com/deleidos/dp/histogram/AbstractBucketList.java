package com.deleidos.dp.histogram;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.deleidos.dp.beans.RegionData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractBucketList implements BucketList {
	private List<AbstractBucket> bucketList = null;
	private String type = "bar";
	private String series = "Values";
	private String yaxis = "Frequency";
	private List<String> labels;
	private List<Integer> data;
	private RegionData regionData = null;

	@JsonProperty("region-data")
	public RegionData getRegionData() {
		return regionData;
	}

	@JsonProperty("region-data")
	public void setRegionData(RegionData regionData) {
		this.regionData = regionData;
	}

	public AbstractBucketList() { }

	@JsonIgnore
	public abstract List<AbstractBucket> getOrderedBuckets();

	/*public JSONArray asJSONArray() {
		List<AbstractBucket> bucketList = getOrderedBuckets();
		JSONArray jArr = new JSONArray();
		JSONObject obj = new JSONObject();
		JSONArray labels = new JSONArray();
		JSONArray data = new JSONArray();
		obj.put("type", "bar");
		obj.put("series", "Values");
		obj.put("yaxis", "Frequency");
		for(int i = 0; i < bucketList.size(); i++) {
			AbstractBucket a = bucketList.get(i);
			labels.put(a.getLabel());
			data.put(a.getCount().intValue());
		}
		obj.put("labels", labels);
		obj.put("data", data);
		jArr.put(obj);
		return jArr;
	}

	public JSONObject asJSONObject() {
		List<AbstractBucket> bucketList = getOrderedBuckets();
		JSONObject obj = new JSONObject();
		JSONArray labels = new JSONArray();
		JSONArray data = new JSONArray();
		obj.put("type", "bar");
		obj.put("series", "Values");
		obj.put("yaxis", "Frequency");
		for(int i = 0; i < bucketList.size(); i++) {
			AbstractBucket a = bucketList.get(i);
			labels.put(a.getLabel());
			data.put(a.getCount().intValue());
		}
		obj.put("labels", labels);
		obj.put("data", data);
		return obj;
	}*/

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

	public String getYaxis() {
		return yaxis;
	}

	public void setYaxis(String yaxis) {
		this.yaxis = yaxis;
	}

	public List<String> getLabels() {
		if(labels != null) {
			return labels;
		} else {
			bucketList = getOrderedBuckets();
			labels = new ArrayList<String>();
			for(int i = 0; i < bucketList.size(); i++) {
				AbstractBucket a = bucketList.get(i);
				labels.add(a.getLabel());
			}
			return labels;
		}
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public List<Integer> getData() {
		if(data != null) {
			return data;
		}else {
			bucketList = getOrderedBuckets();
			data = new ArrayList<Integer>();
			for(int i = 0; i < bucketList.size(); i++) {
				AbstractBucket a = bucketList.get(i);
				data.add(a.getCount().intValue());
			}
			return data;
		}
	}

	public void setData(List<Integer> data) {
		this.data = data;
	}

}
