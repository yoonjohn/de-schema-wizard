package com.deleidos.dp.beans;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DomainMetaData {
	private String dId;
	private String dName;
	private String dVersion;
	private Timestamp dLastUpdate;
	private String dDescription;
	public String getdId() {
		return dId;
	}
	public void setdId(String dId) {
		this.dId = dId;
	}
	public String getdName() {
		return dName;
	}
	public void setdName(String dName) {
		this.dName = dName;
	}
	public String getdVersion() {
		return dVersion;
	}
	public void setdVersion(String dVersion) {
		this.dVersion = dVersion;
	}
	@JsonProperty("dLastUpdate")
	public Timestamp getdLastUpdate() {
		return dLastUpdate;
	}
	@JsonProperty("dLastUpdate")
	public void setdLastUpdate(Timestamp dLastUpdate) {
		this.dLastUpdate = dLastUpdate;
	}
	@JsonProperty("dDescription")
	public String getdDescription() {
		return dDescription;
	}
	@JsonProperty("dDescription")
	public void setdDescription(String dDescription) {
		this.dDescription = dDescription;
	}
}
