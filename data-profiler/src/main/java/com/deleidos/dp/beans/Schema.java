package com.deleidos.dp.beans;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class Schema {
	private static final Logger logger = Logger.getLogger(Schema.class);
	private int schemaModelId;
	private String sGuid;
	private String sName;
	private String sVersion;
	private Timestamp sLastUpdate;
	private String sDescription;
	private String sDomainName;
	private int recordsParsedCount;
	private Map<String, Profile> sProfile;
	private List<DataSampleMetaData> sDataSamples;

	public Schema() {
		sProfile = new HashMap<String, Profile>();
		sDataSamples = new ArrayList<DataSampleMetaData>();
	}
	
	public static Schema schemaFromSamples(String guid, int schemaId, String name, String version, Timestamp lastUpdate, String description, List<DataSample> samples) {
		return schemaFromSamples(guid, schemaId, name, version, lastUpdate, description, samples, true);
	}

	public static Schema schemaFromSamples(String guid, int schemaId, String name, String version, Timestamp lastUpdate, String description, List<DataSample> samples,
			boolean showHistogram) {
		Schema schema = new Schema();
		schema.setSchemaModelId(schemaId);
		schema.setsGuid(guid);
		schema.setsDescription(description);
		schema.setsName(name);
		schema.setsLastUpdate(lastUpdate);
		schema.setsVersion(version);
		//List<DataSampleMetaData> sampleMetaDataList = new ArrayList<DataSampleMetaData>();
		for(DataSample sample : samples) {
			DataSampleMetaData dsmd = new DataSampleMetaData();
			dsmd.setDataSampleId(sample.getDataSampleId());
			dsmd.setDsGuid(sample.getDsGuid());
			dsmd.setDsDescription(sample.getDsDescription());
			dsmd.setDsFileName(sample.getDsFileName());
			dsmd.setDsFileType(sample.getDsFileType());
			dsmd.setDsLastUpdate(sample.getDsLastUpdate());
			dsmd.setDsVersion(sample.getDsVersion());
			dsmd.setDsName(sample.getDsName());
			List<DataSampleMetaData> sampleMetaDataList = schema.getsDataSamples();
			sampleMetaDataList.add(dsmd);

			Map<String, Profile> schemaProfile = schema.getsProfile();
			for(String key : sample.getDsProfile().keySet()) {
				Profile profile = sample.getDsProfile().get(key);
				if(profile.isUsedInSchema()) {
					schemaProfile.put(key, profile);
				}
			}
		}
		if(!showHistogram) {
			for(String key : schema.getsProfile().keySet()) {
				Profile profile = schema.getsProfile().get(key);
				Detail detail = profile.getDetail();
				if(detail instanceof NumberDetail) {
					NumberDetail nd = (NumberDetail)profile.getDetail();
					nd.setFreqHistogram(null);
				} else if(detail instanceof StringDetail) {
					StringDetail sd = ((StringDetail)profile.getDetail());
					sd.setTermFreqHistogram(null);
				} else if(detail instanceof BinaryDetail) {
					//TODO
					logger.error("Got binary");
				}
				profile.setDetail(detail);
			}
		}
		return schema;
	}

	@JsonProperty("schema_model_id")
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

	@JsonProperty("sProfile")
	public Map<String, Profile> getsProfile() {
		return sProfile;
	}

	public void setsProfile(Map<String, Profile> sProfile) {
		this.sProfile = sProfile;
	}

	@JsonProperty("sDataSamples")
	public List<DataSampleMetaData> getsDataSamples() {
		return sDataSamples;
	}

	public void setsDataSamples(List<DataSampleMetaData> sDataSamples) {
		this.sDataSamples = sDataSamples;
	}

	@JsonProperty("sTotalSampleRecs")
	public int getRecordsParsedCount() {
		return recordsParsedCount;
	}

	@JsonProperty("sTotalSampleRecs")
	public void setRecordsParsedCount(int recordsParsedCount) {
		this.recordsParsedCount = recordsParsedCount;
	}

	public String getsDomainName() {
		return sDomainName;
	}

	public void setsDomainName(String sDomainName) {
		this.sDomainName = sDomainName;
	}


}
