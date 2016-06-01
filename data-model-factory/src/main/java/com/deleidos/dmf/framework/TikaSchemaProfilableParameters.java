package com.deleidos.dmf.framework;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsRuntimeException;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.profiler.SchemaProfiler;
import com.deleidos.dp.profiler.api.Profiler;

public class TikaSchemaProfilableParameters extends TikaProfilerParameters {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2710971238389516736L;
	
	private JSONArray modifiedSampleList;
	private JSONObject proposedSchemaAnalysis;		
	private List<DataSample> userModifiedSampleList;

	public TikaSchemaProfilableParameters(Profiler profiler, AnalyzerProgressUpdater progressUpdater, String uploadDir, String guid,
			List<DataSample> dataSampleList) {
		super(profiler, progressUpdater, uploadDir, guid);
		userModifiedSampleList = dataSampleList;
	}

	public JSONArray getModifiedSampleList() {
		return modifiedSampleList;
	}

	public void setModifiedSampleList(JSONArray modifiedSampleList) {
		this.modifiedSampleList = modifiedSampleList;
	}

	public JSONObject getProposedSchemaAnalysis() {
		return proposedSchemaAnalysis;
	}

	public void setProposedSchemaAnalysis(JSONObject proposedSchemaAnalysis) {
		this.proposedSchemaAnalysis = proposedSchemaAnalysis;
	}

	public List<DataSample> getUserModifiedSampleList() {
		return userModifiedSampleList;
	}

	public void setUserModifiedSampleList(List<DataSample> userModifiedSampleList) {
		this.userModifiedSampleList = userModifiedSampleList;
	}

	@Override
	public Schema getProfilerBean() {
		SchemaProfiler profiler = (SchemaProfiler)this.get(Profiler.class);
		Object bean = profiler.asBean();
		if(bean instanceof Schema) {
			Schema schemaBean = (Schema) bean;
			schemaBean.setsGuid(getGuid());
			schemaBean.setRecordsParsedCount(profiler.getRecordsParsed());
			schemaBean.setsDataSamples(profiler.getDataSampleMetaDataList());
			return schemaBean;
		} else {
			throw new AnalyticsRuntimeException("Undefined profiler in Sample Profilable Parameters.");
		}
	}

}
