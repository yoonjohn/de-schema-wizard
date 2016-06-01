package com.deleidos.dp.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchingField {
	@JsonProperty("matching-field")
	private String matchingField;
	@JsonProperty("confidence")
	private int confidence;
	
	public String getMatchingField() {
		return matchingField;
	}
	public void setMatchingField(String altName) {
		this.matchingField = altName;
	}
	public int getConfidence() {
		return confidence;
	}
	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}
}
