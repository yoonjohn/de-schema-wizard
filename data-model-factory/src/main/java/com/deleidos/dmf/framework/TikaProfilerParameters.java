package com.deleidos.dmf.framework;

import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.analyzer.AnalyzerParameters;
import com.deleidos.dmf.analyzer.AnalyzerProgressUpdater;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dp.profiler.api.Profiler;

public abstract class TikaProfilerParameters extends ParseContext implements AnalyzerParameters {
	private static final long serialVersionUID = 7094322716696643804L;
	private ProgressBar progress;
	private InputStream stream;
	private ContentHandler handler;
	private Metadata metadata;
	
	private String sessionId;
	private String domainName;
	private String tolerance;
	private String uploadFileDir;
	private String extractedContentDir;
	private String guid;
	
	private long streamLength = 0;
	private int charsRead = 0;

	public TikaProfilerParameters(Profiler profiler, AnalyzerProgressUpdater progressUpdater, String uploadDir, String guid) {
		this.set(Profiler.class, profiler);
		this.set(AnalyzerProgressUpdater.class, progressUpdater);
		this.setUploadFileDir(uploadDir);
		this.setGuid(guid);
	}

	public ProgressBar getProgress() {
		return progress;
	}

	public void setProgress(ProgressBar progress) {
		this.progress = progress;
	}

	public InputStream getStream() {
		return stream;
	}

	public void setStream(InputStream stream) {
		this.stream = stream;
	}

	public ContentHandler getHandler() {
		return handler;
	}

	public void setHandler(ContentHandler handler) {
		this.handler = handler;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getTolerance() {
		return tolerance;
	}

	public void setTolerance(String tolerance) {
		this.tolerance = tolerance;
	}

	public String getUploadFileDir() {
		return uploadFileDir;
	}

	public void setUploadFileDir(String uploadFileDir) {
		this.uploadFileDir = uploadFileDir;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getExtractedContentDir() {
		return extractedContentDir;
	}

	public void setExtractedContentDir(String extractedContentDir) {
		this.extractedContentDir = extractedContentDir;
	}

	public int getCharsRead() {
		return charsRead;
	}

	public void setCharsRead(int charsRead) {
		this.charsRead = charsRead;
	}

	public long getStreamLength() {
		return streamLength;
	}

	public void setStreamLength(long streamLength) {
		this.streamLength = streamLength;
	}
	

}
