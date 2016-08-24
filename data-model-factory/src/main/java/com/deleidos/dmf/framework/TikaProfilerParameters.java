package com.deleidos.dmf.framework;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import com.deleidos.dmf.analyzer.AnalyzerParameters;
import com.deleidos.dmf.framework.AbstractAnalyticsParser.ProgressUpdatingBehavior;
import com.deleidos.dmf.progressbar.ProgressBarManager;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.profiler.api.Profiler;

public abstract class TikaProfilerParameters extends ParseContext implements AnalyzerParameters {
	private static final long serialVersionUID = 7094322716696643804L;
	private ProgressBarManager progress;
	private InputStream stream;
	private ContentHandler handler;
	private Metadata metadata;
	
	private String sessionId;
	private String domainName;
	private String tolerance;
	private String uploadFileDir;
	private String extractedContentDir;
	private String guid;
	
	private ProgressUpdatingBehavior progressUpdatingBehavior;
	
	private long streamLength = 0;
	private int charsRead = 0;

	public TikaProfilerParameters(Profiler profiler, ProgressBarManager progressBar, String uploadDir, String guid) {
		this.set(Profiler.class, profiler);
		this.setUploadFileDir(uploadDir);
		this.setGuid(guid);
		this.setProgress(progressBar);
	}
	
	public ProgressBarManager getProgressBar() {
		return progress;
	}

	public void setProgress(ProgressBarManager progress) {
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

	public ProgressUpdatingBehavior getProgressUpdatingBehavior() {
		return progressUpdatingBehavior;
	}

	public void setProgressUpdatingBehavior(ProgressUpdatingBehavior progressUpdatingBehavior) {
		this.progressUpdatingBehavior = progressUpdatingBehavior;
	}
	
	public static class MostCommonFieldWithWalking {
		private String fieldName;
		private Long walkingCount;
		private Float presence;
		public MostCommonFieldWithWalking(String field, Long walkingCount, Float presence) {
			setFieldName(field);
			setWalkingCount(walkingCount);
			setPresence(presence);
		}
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public Long getWalkingCount() {
			return walkingCount;
		}
		public void setWalkingCount(Long walkingCount) {
			this.walkingCount = walkingCount;
		}
		public Float getPresence() {
			return presence;
		}
		public void setPresence(Float presence) {
			this.presence = presence;
		}
		
	}
	
	/**
	 * Get the field that can best be used to gauge progress, or null if there is no appropriate field and record count
	 * should be used.  This is necessary because certain structured data sets will be passed as one enormous record.
	 * This prevents any progress updates based on record count, so this provides an optional progress update for 
	 * those cases.
	 * @param profileMap
	 * @param minimalPresence
	 * @return the appropriate field, or null if record count should be used
	 */
	public static MostCommonFieldWithWalking determineProgressRepresentativeField(Map<String, Profile> profileMap, float minimalPresence) {
		float max = 0f;
		String maxKey = null;
		long walkingCount = 0;
		List<MostCommonFieldWithWalking> candidates = new ArrayList<MostCommonFieldWithWalking>();
		for(String key : profileMap.keySet()) {
			Profile profile = profileMap.get(key);
			if(minimalPresence <= profile.getPresence()) {
				max = profile.getPresence();
				maxKey = key;
				walkingCount = profile.getDetail().getWalkingCount().longValue();
				candidates.add(new MostCommonFieldWithWalking(maxKey, walkingCount, max));
			}
		}
		if(candidates.size() == 0) {
			return null;
		}
		// sort such that highest walking count is first
		candidates.sort((MostCommonFieldWithWalking x,MostCommonFieldWithWalking y)
				->(int)(y.getWalkingCount()-x.getWalkingCount()));
		return candidates.get(0);
	}
	
	public static MostCommonFieldWithWalking determineProgressRepresentativeField(Map<String, Profile> profileMap) {
		return determineProgressRepresentativeField(profileMap, .8f);
	}

}
