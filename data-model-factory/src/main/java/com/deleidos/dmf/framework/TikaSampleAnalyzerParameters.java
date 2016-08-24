package com.deleidos.dmf.framework;

import java.io.InputStream;
import java.util.Map;

import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.exception.AnalyticsRuntimeException;
import com.deleidos.dmf.framework.AbstractAnalyticsParser.ProgressUpdatingBehavior;
import com.deleidos.dmf.framework.TikaProfilerParameters.MostCommonFieldWithWalking;
import com.deleidos.dmf.progressbar.ProgressBarManager;
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
	private int recordsInSample = -1;
	private int reverseGeocodingCallsEstimate;
	private String uploadFileDir;
	private String mediaType;
	private Map<String, MostCommonFieldWithWalking> secondPassMostCommonFieldWithWalkingCount;
	
	private boolean persist;
	
	public TikaSampleAnalyzerParameters(Profiler profiler, ProgressBarManager progressBar, String uploadFileDir, String guid,
			InputStream stream, ContentHandler handler, Metadata metadata) {
		super(profiler, progressBar, uploadFileDir, guid);
		this.setStream(stream);
		this.setHandler(handler);
		this.setMetadata(metadata);
		this.persist = true;
		this.setProgressUpdatingBehavior(ProgressUpdatingBehavior.BY_CHARACTERS_READ);
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

	/*public boolean isDoReverseGeocode() {
		return doReverseGeocode;
	}

	public void setDoReverseGeocode(boolean doReverseGeocode) {
		this.doReverseGeocode = doReverseGeocode;
	}*/

	public boolean isPersistInH2() {
		return persist;
	}

	public void setPersistInH2(boolean persistInH2) {
		this.persist = persistInH2;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public Map<String, MostCommonFieldWithWalking> getSecondPassMostCommonFieldWithWalkingCount() {
		return secondPassMostCommonFieldWithWalkingCount;
	}

	public void setSecondPassMostCommonFieldWithWalkingCount(
			Map<String, MostCommonFieldWithWalking> secondPassMostCommonFieldWithWalkingCount) {
		this.secondPassMostCommonFieldWithWalkingCount = secondPassMostCommonFieldWithWalkingCount;
	}

	/*public boolean isReverseGeocodingPass() {
		return isReverseGeocodingPass;
	}

	public void setReverseGeocodingPass(boolean isReverseGeocodingPass) {
		this.isReverseGeocodingPass = isReverseGeocodingPass;
	}*/

}
