package com.deleidos.dp.beans;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class SchemaMetaData {
	private int schemaModelId;
	private String sGuid;
	private String sName;
	private String sVersion;
	private Timestamp sLastUpdate;
	private String sDescription;
	private List<String> sDataSampleGuids;

	@JsonProperty("schema-id")
	public int getSchemaModelId() {
		return schemaModelId;
	}
	
	public void setSchemaModelId(int schemaModelId) {
		this.schemaModelId = schemaModelId;
	}
	
	@JsonProperty("sId")
	public String getsGuid() {
		return sGuid;
	}

	public void setsGuid(String sGuid) {
		this.sGuid = sGuid;
	}

	@JsonProperty("sName")
	public String getsName() {
		return sName;
	}

	public void setsName(String sName) {
		this.sName = sName;
	}

	@JsonProperty("sVersion")
	public String getsVersion() {
		return sVersion;
	}

	public void setsVersion(String sVersion) {
		this.sVersion = sVersion;
	}

	@JsonProperty("sLastUpdate")
	public Timestamp getsLastUpdate() {
		return sLastUpdate;
	}

	public void setsLastUpdate(Timestamp sLastUpdate) {
		this.sLastUpdate = sLastUpdate;
	}

	@JsonProperty("sDescription")
	public String getsDescription() {
		return sDescription;
	}

	public void setsDescription(String sDescription) {
		this.sDescription = sDescription;
	}

	@JsonProperty("sDataSamples")
	public List<String> getsDataSamples() {
		return sDataSampleGuids;
	}

	public void setsDataSamples(List<String> sDataSamples) {
		this.sDataSampleGuids = sDataSamples;
	}
}
