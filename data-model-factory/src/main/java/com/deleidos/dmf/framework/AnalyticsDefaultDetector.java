package com.deleidos.dmf.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.pkg.PackageParser;

import com.deleidos.dmf.parser.JNetPcapTikaParser;
import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dmf.web.SchemaWizardWebSocketUtility;
import com.google.common.collect.Iterables;

/**
 * Detector class that contains an instance of all other detectors.  This class will iterate through the other detectors
 * and attempt to find the match with the highest confidence.  This class is not meant to be subclassed.
 * @author leegc
 *
 */
public class AnalyticsDefaultDetector extends DefaultDetector {
	private static final Logger logger = Logger.getLogger(AnalyticsDefaultDetector.class);
	private MediaTypeRegistry registry; 
	public static final String HAS_BODY_CONTENT = "has-body-content";
	public static final String BODY_CONTENT_TYPE = "body-content-type";
	//public static final String FILE_SIZE = "file-size";
	private static final long FILE_CUTOFF_IN_BYTES = 1024 * 1024 * 1024;
	private ProgressBar progressBar = null;
	private String sessionId = null;

	public AnalyticsDefaultDetector() {
		this.registry = MimeTypes.getDefaultMimeTypes().getMediaTypeRegistry();
		blackListStaticInit();
	}
	
	public void enableProgressUpdates(String sessionId, ProgressBar progressBar) {
		this.sessionId = sessionId;
		this.progressBar = progressBar;
	}
	
	public void disableProgressUpdates() {
		this.progressBar = null;
		this.sessionId = null;
	}

	@Override
	public MediaType detect(InputStream input, Metadata metadata) throws IOException { 
		ArrayList<AnalyticsDetectorWrapper> confidenceList = new ArrayList<AnalyticsDetectorWrapper>();
		
		if(metadata.get(HAS_BODY_CONTENT) != null 
				&& metadata.get(HAS_BODY_CONTENT).equals(Boolean.FALSE.toString())
				&& metadata.get(Metadata.CONTENT_TYPE) != null) {
			return MediaType.parse(metadata.get(Metadata.CONTENT_TYPE));
		}
		
		MediaType type = MediaType.OCTET_STREAM;
		
		long fileSize = metadata.get(Metadata.CONTENT_LENGTH) != null ? Long.valueOf(metadata.get(Metadata.CONTENT_LENGTH)) : -1;
		
		List<Detector> detectors;
		if(fileSize > FILE_CUTOFF_IN_BYTES) {
			detectors = getStateBasedDetectors();
		} else {
			detectors = getDetectors();
		}
		
		int numDetectors = detectors.size();
		int i = 0;
		if(progressBar != null) {
			progressBar.setCurrentState(ProgressState.detectStage);
			SchemaWizardWebSocketUtility.getInstance().updateProgress(progressBar, sessionId);
			progressBar.setCurrentStateSplits(numDetectors);
		}
		
		for (Detector detector : detectors) {
			AnalyticsDetectorWrapper wrapper = new AnalyticsDetectorWrapper(detector);
			//need field in H2 that is body-content-type
			MediaType detected = wrapper.detect(input, metadata);
			
			if(progressBar != null) {
				progressBar.setCurrentStateSplitIndex(i);
				progressBar.updateCurrentSampleNumerator(ProgressState.detectStage.getEndValue());
				SchemaWizardWebSocketUtility.getInstance().updateProgress(progressBar, sessionId);
			}
			i++;
			
			if(detected == null) {
				continue;
			}
			// wrapper.hasPlainTextBodyContent()
			// add metadata tag for has plain text body content
			if (registry.isSpecializationOf(detected, type)) {
				for(AnalyticsDetectorWrapper w : confidenceList) {
					if(Iterables.getOnlyElement(w.getDetectableTypes()).equals(type)) {
						float f = w.getConfidenceInterval();
						if(f >= wrapper.getConfidenceInterval()) {
							wrapper.setConfidenceInterval(f+.01f);
						}
					}
				}
				type = detected;
			}
			if(wrapper.getConfidenceInterval() > 0.0 && !type.equals(MediaType.OCTET_STREAM)) {
				confidenceList.add(wrapper);
			}
		}
		
		if(progressBar != null) {
			progressBar.setCurrentStateSplits(1);
		}
		
		if(confidenceList.size() == 0) {
			return null;	
		}
		
		Collections.sort(confidenceList);
		
		AnalyticsDetectorWrapper winner = confidenceList.get(0);
		
		if(metadata.get(HAS_BODY_CONTENT) == null) {
			//only change if it has not been set (parser will use this to stop recursion)
			if(isBodyContentDisabled(type, metadata)) {
				logger.info("Body content parsing disabled for " + type + ".");
			}
			boolean hasBodyContent = winner.hasBodyPlainTextContent() && !isBodyContentDisabled(type, metadata);				
			
			metadata.set(AnalyticsDefaultDetector.HAS_BODY_CONTENT, Boolean.valueOf(hasBodyContent).toString());

		}
		
		MediaType finalType = Iterables.getOnlyElement(winner.getDetectableTypes());
		confidenceList.clear();
		return finalType;
	}
	
	private static Set<MediaType> bodyContentBlackList;
	
	private static void blackListStaticInit() {
		bodyContentBlackList = new HashSet<MediaType>();
		PackageParser packageParser = new PackageParser();
		bodyContentBlackList.addAll(packageParser.getSupportedTypes(null));
		bodyContentBlackList.add(JNetPcapTikaParser.CONTENT_TYPE);
		bodyContentBlackList.add(MediaType.TEXT_PLAIN);
		bodyContentBlackList.add(MediaType.OCTET_STREAM);
		packageParser = null;
	}
	
	private boolean isBodyContentDisabled(MediaType type, Metadata metadata) {
		return bodyContentBlackList.contains(type); 
	}

	public List<Detector> getStateBasedDetectors() {
		List<Detector> detectors = getDetectors();
		Iterator<Detector> iDetector = detectors.iterator();
		while(iDetector.hasNext()) {
			Detector detector = iDetector.next();
			if((detector instanceof AbstractMarkSupportedAnalyticsDetector)) {
				iDetector.remove();
			}
		}
		return detectors;
	}
	
}
