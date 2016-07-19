package com.deleidos.dmf.framework;

import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.exception.AnalyticsRuntimeException;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.profiler.api.Profiler;

public class TikaSampleAnalyzerParameters extends TikaProfilerParameters {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1734197480952109880L;
	
	private String sampleFilePath; 
	private int sampleNumber;
	private int numSamplesUploading;
	private int recordsInSample;
	private int reverseGeocodingCallsEstimate;
	private String uploadFileDir;
	private String mediaType;
	
	private boolean isReverseGeocodingPass;
	private boolean doReverseGeocode;
	private boolean persistInH2;
	
	public TikaSampleAnalyzerParameters(Profiler profiler, AnalyzerProgressUpdater progressUpdater, String uploadFileDir, String guid,
			InputStream stream, ContentHandler handler, Metadata metadata) {
		super(profiler, progressUpdater, uploadFileDir, guid);
		this.setStream(stream);
		this.setHandler(handler);
		this.setMetadata(metadata);
	}

	@Override
	public DataSample getProfilerBean() {
		Profiler profiler = get(Profiler.class);
		Object bean = profiler.asBean();
		if(bean instanceof DataSample) {
			DataSample dataSampleBean = (DataSample) bean;
			dataSampleBean.setDsGuid(getGuid());
			dataSampleBean.setDsFileType(getMediaType());
			dataSampleBean.setDsFileName(getSampleFilePath());
			dataSampleBean.setDsFileType(getMediaType());
			dataSampleBean.setDsExtractedContentDir(getExtractedContentDir());
			return dataSampleBean;
		} else {
			throw new AnalyticsRuntimeException("Undefined profiler in Sample Profilable Parameters.");
		}
	}

	public String getSampleFilePath() {
		return sampleFilePath;
	}

	public void setSampleFilePath(String sampleFilePath) {
		this.sampleFilePath = sampleFilePath;
	}

	public int getSampleNumber() {
		return sampleNumber;
	}

	public void setSampleNumber(int sampleNumber) {
		this.sampleNumber = sampleNumber;
	}

	public int getNumSamplesUploading() {
		return numSamplesUploading;
	}

	public void setNumSamplesUploading(int numSamplesUploading) {
		this.numSamplesUploading = numSamplesUploading;
	}

	public int getRecordsInSample() {
		return recordsInSample;
	}

	public void setRecordsInSample(int recordsInSample) {
		this.recordsInSample = recordsInSample;
	}

	public int getReverseGeocodingCallsEstimate() {
		return reverseGeocodingCallsEstimate;
	}

	public void setReverseGeocodingCallsEstimate(int reverseGeocodingCallsEstimate) {
		this.reverseGeocodingCallsEstimate = reverseGeocodingCallsEstimate;
	}

	public String getUploadFileDir() {
		return uploadFileDir;
	}

	public void setUploadFileDir(String uploadFileDir) {
		this.uploadFileDir = uploadFileDir;
	}

	public boolean isDoReverseGeocode() {
		return doReverseGeocode;
	}

	public void setDoReverseGeocode(boolean doReverseGeocode) {
		this.doReverseGeocode = doReverseGeocode;
	}

	public boolean isPersistInH2() {
		return persistInH2;
	}

	public void setPersistInH2(boolean persistInH2) {
		this.persistInH2 = persistInH2;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public boolean isReverseGeocodingPass() {
		return isReverseGeocodingPass;
	}

	public void setReverseGeocodingPass(boolean isReverseGeocodingPass) {
		this.isReverseGeocodingPass = isReverseGeocodingPass;
	}

}
