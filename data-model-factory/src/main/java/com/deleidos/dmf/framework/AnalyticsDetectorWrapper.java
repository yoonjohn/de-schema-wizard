package com.deleidos.dmf.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

/**
 * Wrapper detector built for use with media types that Tika can recognize by default.  "Wrap" an existing Tika detector
 * by passing it into the constructor.  The detector will then work with the framework (more specifically, the ranking
 * system).  As of 1/19/2016, this class does not need to be subclassed.
 * <br>"Note: this class has a natural ordering that is inconsistent with equals."
 * @author leegc
 *
 */
public class AnalyticsDetectorWrapper implements Detector, Comparable<AnalyticsDetectorWrapper> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7004302637534333152L;
	private boolean hasBodyPlainTextContent;
	protected Detector detector;
	protected Set<MediaType> detectableTypes;
	protected float confidenceInterval = 0.0f;

	public AnalyticsDetectorWrapper() { }
	
	public AnalyticsDetectorWrapper(Detector detector) {
		this.detector = detector;
		if(detector instanceof AbstractMarkSupportedAnalyticsDetector) {
			setHasBodyPlainTextContent(((AbstractMarkSupportedAnalyticsDetector)detector).hasBodyPlainTextContent());
		} else {
			setHasBodyPlainTextContent(true);
		}
	}
	
	public AnalyticsDetectorWrapper(Detector detector, Set<MediaType> detectableType) {
		this(detector);
		this.detectableTypes = detectableType;
	}
	
	public void setConfidenceInterval(float minimumConfidence) {
		confidenceInterval = minimumConfidence;
	}

	public float getConfidenceInterval() {
		return confidenceInterval;
	}

	/**
	 * Provides a default "wrapper" around the global detector.  This assigns the necessary <i>confidence</i> and
	 * <i> detectableTypes </i> variables that the Analytics detecting framework needs.  Parsers that do not subclass
	 * the AnalyticsMarkSupportedTikaDetector class will be given a default confidence of .01 because they are
	 * assumed to be a lower priority than Analytics Detectors.
	 */
	@Override
	public MediaType detect(InputStream input, Metadata metadata)
			throws IOException {
		MediaType type = detector.detect(input, metadata);
		if(detector instanceof AnalyticsDetectorWrapper) {
        	confidenceInterval = ((AnalyticsDetectorWrapper)detector).getConfidenceInterval();
        	detectableTypes = ((AnalyticsDetectorWrapper)detector).getDetectableTypes();
        } else {
        	confidenceInterval = (type == null) ? 0.0f : .01f;
        	detectableTypes = Collections.singleton(type);
        }
		return type;
	}

	@Override
	public int compareTo(AnalyticsDetectorWrapper other) {
		if(other.confidenceInterval > this.confidenceInterval) {
			return 1; 
		} else if(Float.floatToRawIntBits(other.confidenceInterval) == Float.floatToRawIntBits(this.confidenceInterval)) {
			return 0;
		} else {
			return -1;
		}
	}
	
	public Set<MediaType> getDetectableTypes() {
		return detectableTypes;
	}

	public void setDetectableTypes(Set<MediaType> detectableTypes) {
		this.detectableTypes = detectableTypes;
	}

	public boolean hasBodyPlainTextContent() {
		return hasBodyPlainTextContent;
	}

	public void setHasBodyPlainTextContent(boolean hasBodyPlainTextContent) {
		this.hasBodyPlainTextContent = hasBodyPlainTextContent;
	}
	
}
