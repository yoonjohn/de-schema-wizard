package com.deleidos.dp.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.profiler.ReverseGeocodingLoader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegionData {
	private String latitudeKey;
	private String longitudeKey;
	private List<ColsEntry> cols;
	private List<RowEntry> rows;
	
	public RegionData() {
		setCols(new ArrayList<ColsEntry>(Arrays.asList(new ColsEntry("Country"), new ColsEntry("Frequency"))));
		setRows(new ArrayList<RowEntry>());
	}
	
	public List<ColsEntry> getCols() {
		return cols;
	}
	public void setCols(List<ColsEntry> cols) {
		this.cols = cols;
	}
	public List<RowEntry> getRows() {
		return rows;
	}
	public void setRows(List<RowEntry> rows) {
		this.rows = rows;
	}
	
	@JsonIgnore
	public Map<String, Integer> toMap() {
		Map<String, Integer> map = (rows.size() > 0) ? new HashMap<String, Integer>() : null;
		for(RowEntry rowEntry : rows) {
			map.put(rowEntry.getKey(), rowEntry.getValue());
		}
		return map;
	}

	@JsonProperty("latitude-key")
	public String getLatitudeKey() {
		return latitudeKey;
	}
	@JsonProperty("latitude-key")
	public void setLatitudeKey(String latitudeKey) {
		this.latitudeKey = latitudeKey;
	}
	@JsonProperty("longitude-key")
	public String getLongitudeKey() {
		return longitudeKey;
	}
	@JsonProperty("longitude-key")
	public void setLongitudeKey(String longitudeKey) {
		this.longitudeKey = longitudeKey;
	}
	
	public static List<RowEntry> rowsFromMapping(Map<String, Integer> singleFieldFrequencyMapping) {
		List<RowEntry> rows = new ArrayList<RowEntry>();
		for(String key : singleFieldFrequencyMapping.keySet()) {
			rows.add(new RowEntry(key, singleFieldFrequencyMapping.get(key)));
		}
		return rows;
	}
	
	public static RegionData add(RegionData regionData, RegionData otherRegionData) {
		RegionData newRegionData = new RegionData();
		newRegionData.setCols(regionData.getCols());
		newRegionData.setLatitudeKey(regionData.getLatitudeKey());
		newRegionData.setLongitudeKey(regionData.getLongitudeKey());
		newRegionData.setRows(regionData.getRows());
		for(RowEntry row : otherRegionData.getRows()) {
			String name = row.getKey();
			int index = ReverseGeocodingLoader.getCountryIndexByName(newRegionData.getRows(), name);
			if(index > -1) {
				Integer value = newRegionData.getRows().get(index).getValue() + row.getValue();
				newRegionData.getRows().get(index).setValue(value);
			} else {
				newRegionData.getRows().add(row);
			}
		}
		return newRegionData;
	}

}
