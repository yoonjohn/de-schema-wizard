package com.deleidos.dp.reversegeocoding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoordinateProfile {
	private int index;
	private String latitude;
	private String longitude;
	private List<Double[]> undeterminedCoordinateBuffer;
	private Map<String, Integer> countryFrequencyMapping;

	public CoordinateProfile(String latKey, String lngKey) {
		latitude = latKey;
		longitude = lngKey;
		undeterminedCoordinateBuffer = new ArrayList<Double[]>();
		countryFrequencyMapping = new HashMap<String, Integer>();
	}

	public List<Double[]> getUndeterminedCoordinateBuffer() {
		return undeterminedCoordinateBuffer;
	}

	public void setUndeterminedCoordinateBuffer(List<Double[]> undeterminedCoordinateBuffer) {
		this.undeterminedCoordinateBuffer = undeterminedCoordinateBuffer;
	}

	public Map<String, Integer> getCountryFrequencyMapping() {
		return countryFrequencyMapping;
	}

	public void setCountryFrequencyMapping(Map<String, Integer> countryFrequencyMapping) {
		this.countryFrequencyMapping = countryFrequencyMapping;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj != null && obj instanceof CoordinateProfile) ? 
				((CoordinateProfile)obj).getLatitude().equals(latitude) &&
				((CoordinateProfile)obj).getLongitude().equals(longitude) : false;
	}
	
	@Override
	public int hashCode() {
		return new String[] {latitude, longitude}.hashCode();
	}

}