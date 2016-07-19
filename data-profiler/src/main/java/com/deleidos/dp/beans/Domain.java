package com.deleidos.dp.beans;

import java.sql.Timestamp;
import java.util.List;

public class Domain {
	private String dId;
	private String dName;
	private String dVersion;
	private Timestamp dLastUpdate;
	private String dDescription;
	private List<Interpretation> dInterpretations;

	public List<Interpretation> getdInterpretations() {
		return dInterpretations;
	}
	public void setdInterpretations(List<Interpretation> dInterpretations) {
		this.dInterpretations = dInterpretations;
	}
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
	public Timestamp getdLastUpdate() {
		return dLastUpdate;
	}
	public void setdLastUpdate(Timestamp dLastUpdate) {
		this.dLastUpdate = dLastUpdate;
	}
	public String getdDescription() {
		return dDescription;
	}
	public void setdDescription(String dDescription) {
		this.dDescription = dDescription;
	}


}
