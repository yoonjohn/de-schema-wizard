package com.deleidos.dp.beans;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Set;

import com.deleidos.dp.deserializors.SerializationUtility;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class DataSampleMetaData {
	private int dataSampleId;
	private String dsGuid;
	private String dsName;
	private String dsFileName;
	private String dsFileType;
	private String dsVersion;
	private int dsFileSize;
	private Timestamp dsLastUpdate;
	private String dsDescription;
	private String dsExtractedContentDir;

	@JsonProperty("data-sample-id")
	public int getDataSampleId() {
		return dataSampleId;
	}
	
	@JsonProperty("dsId")
	public String getDsGuid() {
		return dsGuid;
	}

	@JsonProperty("dsName")
	public String getDsName() {
		return dsName;
	}

	@JsonProperty("dsFileName")
	public String getDsFileName() {
		return dsFileName;
	}

	@JsonProperty("dsFileType")
	public String getDsFileType() {
		return dsFileType;
	}

	@JsonProperty("dsVersion")
	public String getDsVersion() {
		return dsVersion;
	}

	@JsonProperty("dsLastUpdate")
	public Timestamp getDsLastUpdate() {
		return dsLastUpdate;
	}

	@JsonProperty("dsDescription")
	public String getDsDescription() {
		return dsDescription;
	}

	public void setDataSampleId(int dataSampleId) {
		this.dataSampleId = dataSampleId;
	}

	public void setDsGuid(String dsGuid) {
		this.dsGuid = dsGuid;
	}

	public void setDsName(String dsName) {
		this.dsName = dsName;
	}

	public void setDsFileName(String dsFileName) {
		this.dsFileName = dsFileName;
	}

	public void setDsFileType(String dsFileType) {
		this.dsFileType = dsFileType;
	}

	public void setDsVersion(String dsVersion) {
		this.dsVersion = dsVersion;
	}

	public void setDsLastUpdate(Timestamp dsLastUpdate) {
		this.dsLastUpdate = dsLastUpdate;
	}

	public void setDsDescription(String dsDescription) {
		this.dsDescription = dsDescription;
	}
	
	@Override
	public String toString() {
		return SerializationUtility.serialize(this);
	}

	public String getDsExtractedContentDir() {
		return dsExtractedContentDir;
	}

	public void setDsExtractedContentDir(String dsExtractedContentDir) {
		this.dsExtractedContentDir = dsExtractedContentDir;
	}

	public int getDsFileSize() {
		return dsFileSize;
	}

	public void setDsFileSize(int dsFileSize) {
		this.dsFileSize = dsFileSize;
	}
	

	/**
	 * Get a new sample name based on an existing set of sample names.
	 * Concatenates a counter to the end of the file name.
	 * 
	 * @param name
	 *            The name of the file without a path or extension
	 * @param existingSampleNames
	 * @return
	 */
	public static String generateNewSampleName(String name, Set<String> existingSampleNames) {
		String baseName = String.copyValueOf(name.toCharArray());
		if (existingSampleNames.contains(name)) {
			int sampleNumber = 1;
			String sampleName = baseName + "(" + sampleNumber + ")";
			boolean alreadyExists = false;
			do {
				if (existingSampleNames.contains(sampleName)) {
					sampleNumber++;
					sampleName = baseName + "(" + sampleNumber + ")";
					alreadyExists = true;
				} else {
					alreadyExists = false;
				}
			} while (alreadyExists);
			return sampleName;
		} else {
			return name;
		}
	}
}
