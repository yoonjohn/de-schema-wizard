package com.deleidos.dp.interpretation.builtin;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.Profile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
/**
 * Abstract class for interpretations.  New interpretations should extend this class rather than implement Interpretation directly.
 * @author leegc
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractBuiltinInterpretation {
	public static final Logger logger = Logger.getLogger(AbstractBuiltinInterpretation.class);
	private String name;
	@JsonIgnore
	private Double confidence;
	
	public AbstractBuiltinInterpretation(String name) {
		this.name = name;
	}
	
	/**
	 * Boolean if the interpretation is the default UnknownInterpretation
	 * @return true if instanceof UnknownInterpretation, false if not
	 */
	public boolean isUnknown() {
		if(this instanceof BuiltinUnknownInterpretation) return true;
		return false;
	}

	@JsonProperty("interpretation")
	public String getInterpretationName() {
		return name;
	}
	
	@JsonProperty("interpretation")
	public void setInterpretationName(String name) {
		this.name = name;
	}

	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		this.confidence = confidence;
	}

	/*private static Map<String, Class<? extends AbstractBuiltinInterpretation>> interpretationList = null;

	private static void staticInit() {
		interpretationList = new HashMap<String, Class<? extends AbstractBuiltinInterpretation>>();
		try {
			for(String domain : JavaDomain.getDomainList().keySet()) {
				Map<String, AbstractBuiltinInterpretation> domainInterpretations = JavaDomain.getDomainList().get(domain.toLowerCase()).newInstance().getInterpretationMap();
				for(String interpretation : domainInterpretations.keySet()) {
					AbstractBuiltinInterpretation aInterpretation = domainInterpretations.get(interpretation.toLowerCase());
					Class<? extends AbstractBuiltinInterpretation> interpretationClass = aInterpretation.getClass();
					interpretationList.put(interpretation, interpretationClass);
				}
			}
		} catch (InstantiationException e) {
			logger.error(e);
		} catch (IllegalAccessException e) {
			logger.error(e);
		}
	}
	
	public static AbstractBuiltinInterpretation getInterpretationByName(String name) {
		if(interpretationList == null) {
			staticInit();
		}
		try {
			return interpretationList.get(name.toLowerCase()).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error(e);
		}
		return null;
	}*/
	
	public abstract double matches(String name, Profile profile);
	
	/**
	 * Determine if a value fits into this interpretation
	 * @param value Number value to be tested
	 * @return true if it fits and the interpretation is a possibility, false if this value should not be viewed as a likely interpretation 
	 */
	public abstract boolean fitsNumberMetrics(Number value);

	/**
	 * Determine if a value fits into this interpretation
	 * @param value String value to be tested
	 * @return true if it fits and the interpretation is a possibility, false if this value should not be viewed as a likely interpretation
	 */
	public abstract boolean fitsStringMetrics(String value);
	
	/**
	 * Determine if a value fits into this interpretation
	 * @param value Object value to be tested
	 * @return true if it fits and the interpretation is a possibility, false if this value should not be viewed as a likely interpretation 
	 */
	public abstract boolean fitsBinaryMetrics(Object value);
}
