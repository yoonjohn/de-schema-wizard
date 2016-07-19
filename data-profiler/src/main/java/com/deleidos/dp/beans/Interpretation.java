package com.deleidos.dp.beans;

import java.util.List;
import java.util.Map;

import com.deleidos.dp.interpretation.builtin.BuiltinLatitudeInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinLongitudeInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinUnknownInterpretation;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Interpretation {
	public static Interpretation UNKNOWN = new Interpretation("Unknown");
	private String iDomainId;
	private String iId;
	private String iName;
	private String iDescription;
	private Map<String, Object> iConstraints;
	private String iScript;
	private List<String> iMatchingNames;
	
	public Interpretation() { }
	
	private Interpretation(String name) {
		this.iName = name;
	}
	
	public String getInterpretation() {
		return iName;
	}
	public void setInterpretation(String interpretation) {
		this.iName = interpretation;
	}

	private static final transient String latInterpretationName = new BuiltinLatitudeInterpretation().getInterpretationName();
	private static final transient String lonInterpretationName = new BuiltinLongitudeInterpretation().getInterpretationName();
	private static final transient String unknownInterpretationName = new BuiltinUnknownInterpretation().getInterpretationName();
	
	public static boolean isLatitude(Interpretation interpretation) {
		return (interpretation == null) ? false : interpretation.getiName().contains("Latitude");
	}
	public static boolean isLongitude(Interpretation interpretation) {
		return (interpretation == null) ? false : interpretation.getiName().contains("Longitude");
	}
	public static boolean isCoordinate(Interpretation interpretation) {
		return isLatitude(interpretation) || isLongitude(interpretation);
	}
	public static boolean isUnknown(Interpretation interpretation) {
		return (interpretation == null) ? false : interpretation.getiName().equals(unknownInterpretationName);
	}

	public String getiId() {
		return iId;
	}

	public void setiId(String iId) {
		this.iId = iId;
	}

	public String getiName() {
		return iName;
	}

	public void setiName(String iName) {
		this.iName = iName;
	}

	public Map<String, Object> getiConstraints() {
		return iConstraints;
	}

	public void setiConstraints(Map<String, Object> iConstraints) {
		this.iConstraints = iConstraints;
	}

	public String getiScript() {
		return iScript;
	}

	public void setiScript(String iScript) {
		this.iScript = iScript;
	}
	
	@JsonProperty("iDomainId")
	public String getiDomainId() {
		return iDomainId;
	}

	@JsonProperty("iDomainId")
	public void setiDomainId(String iDomainId) {
		this.iDomainId = iDomainId;
	}

	@JsonProperty("iDescription")
	public String getiDescription() {
		return iDescription;
	}

	@JsonProperty("iDescription")
	public void setiDescription(String iDescription) {
		this.iDescription = iDescription;
	}

	public List<String> getiMatchingNames() {
		return iMatchingNames;
	}

	public void setiMatchingNames(List<String> iMatchingNames) {
		this.iMatchingNames = iMatchingNames;
	}	
}
