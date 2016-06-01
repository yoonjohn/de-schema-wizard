package com.deleidos.dp.interpretation;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.domain.JavaDomain;
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
public abstract class AbstractJavaInterpretation implements JavaInterpretation {
	public static final Logger logger = Logger.getLogger(AbstractJavaInterpretation.class);
	private String name;
	@JsonIgnore
	private Double confidence;
	
	public AbstractJavaInterpretation() {
		name = initInterpretationName();
	}
	
	/**
	 * Boolean if the interpretation is the default UnknownInterpretation
	 * @return true if instanceof UnknownInterpretation, false if not
	 */
	public boolean isUnknown() {
		if(this instanceof JavaUnknownInterpretation) return true;
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

	private static Map<String, Class<? extends AbstractJavaInterpretation>> interpretationList = null;

	private static void staticInit() {
		interpretationList = new HashMap<String, Class<? extends AbstractJavaInterpretation>>();
		try {
			for(String domain : JavaDomain.getDomainList().keySet()) {
				Map<String, AbstractJavaInterpretation> domainInterpretations = JavaDomain.getDomainList().get(domain.toLowerCase()).newInstance().getInterpretationMap();
				for(String interpretation : domainInterpretations.keySet()) {
					AbstractJavaInterpretation aInterpretation = domainInterpretations.get(interpretation.toLowerCase());
					Class<? extends AbstractJavaInterpretation> interpretationClass = aInterpretation.getClass();
					interpretationList.put(interpretation, interpretationClass);
				}
			}
		} catch (InstantiationException e) {
			logger.error(e);
		} catch (IllegalAccessException e) {
			logger.error(e);
		}
	}
	
	/**
	 * Get an interpretation instance by its name.
	 * @param name Name of the interpretation
	 * @return A new instance that by default returns <i>name</i> when getInterpretationName() is called. 
	 */
	public static AbstractJavaInterpretation getInterpretationByName(String name) {
		if(interpretationList == null) {
			staticInit();
		}
		try {
			return interpretationList.get(name.toLowerCase()).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error(e);
		}
		return null;
	}
	
	/**
	 * Set the default interpretation name.  Will be called in the AbstractInterpretation constructor so name is always defined.
	 * @return
	 */
	@JsonIgnore
	protected abstract String initInterpretationName();
}
