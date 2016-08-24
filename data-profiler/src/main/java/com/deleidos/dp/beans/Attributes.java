package com.deleidos.dp.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Attributes {
	private String identifier;
	private String categorical;
	private String quantitative;
	private String relational;
	private String ordinal;
	private final String UNKNOWN = "Unknown";
	
	@JsonProperty("identifier")
	public String getIdentifier() {
		return identifier;
	}
	
	@JsonProperty("identifier")
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	@JsonProperty("categorical")
	public String getCategorical() {
		return categorical;
	}
	
	@JsonProperty("categorical")
	public void setCategorical(String categorical) {
		this.categorical = categorical;
	}
	
	@JsonProperty("quantitative")
	public String getQuantitative() {
		return quantitative;
	}
	
	@JsonProperty("quantitative")
	public void setQuantitative(String quantitative) {
		this.quantitative = quantitative;
	}
	
	@JsonProperty("relational")
	public String getRelational() {
		return relational;
	}
	
	@JsonProperty("relational")
	public void setRelational(String relational) {
		this.relational = relational;
	}
	
	@JsonProperty("ordinal")
	public String getOrdinal() {
		return ordinal;
	}
	
	@JsonProperty("ordinal")
	public void setOrdinal(String ordinal) {
		this.ordinal = ordinal;
	}
	
	public boolean isUnknown(Attributes attribute) {
		if (!attribute.getCategorical().equals(UNKNOWN)) { return false; }
		if (!attribute.getIdentifier().equals(UNKNOWN)) { return false; }
		if (!attribute.getOrdinal().equals(UNKNOWN)) { return false; }
		if (!attribute.getQuantitative().equals(UNKNOWN)) { return false; }
		if (!attribute.getRelational().equals(UNKNOWN)) { return false; }
		
		return true;
	}
}
