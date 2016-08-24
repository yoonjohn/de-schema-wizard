package com.deleidos.dp.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class Profile {
	protected float presence;
	private String originalName;
	private MainType mainType;
	private Interpretation interpretation;
	private List<Interpretation> interpretations;
	private Detail detail;
	private List<AliasNameDetails> aliasNames;
	private List<MatchingField> matchingFields;
	private List<Object> exampleValues;
	private boolean usedInSchema = false;
	private boolean mergedInto = false;
	private String displayName;
	private Attributes attributes;

	@JsonProperty("original-name")
	public String getOriginalName() {
		return originalName;
	}

	@JsonProperty("original-name")
	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	@JsonProperty("used-in-schema")
	public boolean isUsedInSchema() {
		return usedInSchema;
	}

	@JsonProperty("used-in-schema")
	public void setUsedInSchema(boolean usedInSchema) {
		this.usedInSchema = usedInSchema;
	}

	@JsonProperty("merged-into-schema")
	public boolean isMergedInto() {
		return mergedInto;
	}

	@JsonProperty("merged-into-schema")
	public void setMergedInto(boolean mergedInto) {
		this.mergedInto = mergedInto;
	}

	public Profile() {
		matchingFields = new ArrayList<MatchingField>();
	}

	@JsonProperty("main-type")
	public void setMainType(String mainType) {
		this.mainType = MainType.fromString(mainType);
	}

	public void setDetail(Detail detail) {
		this.detail = detail;
	}

	@JsonIgnore
	public MainType getMainTypeClass() {
		return mainType;
	}

	@JsonProperty("main-type")
	public String getMainType() {
		return mainType.toString();
	}

	public Detail getDetail() {
		return detail;
	}

	@JsonProperty("matching-fields")
	public List<MatchingField> getMatchingFields() {
		return matchingFields;
	}

	@JsonProperty("matching-fields")
	public void setMatchingFields(List<MatchingField> matchingFields) {
		this.matchingFields = matchingFields;
	}

	@JsonProperty("alias-names")
	public List<AliasNameDetails> getAliasNames() {
		return aliasNames;
	}

	@JsonProperty("alias-names")
	public void setAliasNames(List<AliasNameDetails> aliasNames) {
		this.aliasNames = aliasNames;
	}

	@JsonProperty("attributes")
	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	@JsonProperty("attributes")
	public Attributes getAttributes() {
		return attributes;
	}

	public float getPresence() {
		return presence;
	}

	public void setPresence(float presence) {
		this.presence = presence;
	}

	public static MainType getProfileDataType(Profile profile) {
		return profile.mainType;
	}

	public static Optional<NumberDetail> getNumberDetailOptional(Profile profile) {
		return profile.getDetail().isNumberDetail() 
				? Optional.ofNullable((NumberDetail) profile.getDetail())
						: Optional.empty();

	}

	public static Optional<StringDetail> getStringDetailOptional(Profile profile) {
		return profile.getDetail().isStringDetail()
				? Optional.ofNullable((StringDetail) profile.getDetail())
						: Optional.empty();
	}

	public static Optional<BinaryDetail> getBinaryDetailOptional(Profile profile) {
		return profile.getDetail().isBinaryDetail()
				? Optional.ofNullable((BinaryDetail) profile.getDetail()) 
						: Optional.empty();
	}

	public static NumberDetail getNumberDetail(Profile profile) {
		return getNumberDetailOptional(profile).orElseThrow(() -> new MainTypeRuntimeException());
	}

	public static StringDetail getStringDetail(Profile profile) {
		return getStringDetailOptional(profile).orElseThrow(() -> new MainTypeRuntimeException());
	}

	public static BinaryDetail getBinaryDetail(Profile profile) {
		return getBinaryDetailOptional(profile).orElseThrow(() -> new MainTypeRuntimeException());
	}

	public List<Object> getExampleValues() {
		return exampleValues;
	}

	public void setExampleValues(List<Object> exampleValues) {
		this.exampleValues = exampleValues;
	}

	public List<Interpretation> getInterpretations() {
		return interpretations;
	}

	public void setInterpretations(List<Interpretation> interpretations) {
		this.interpretations = interpretations;
		if(this.interpretations != null && !this.interpretations.isEmpty()) {
			this.interpretation = this.interpretations.get(0);
		} else {
			this.interpretation = Interpretation.UNKNOWN;
		}
	}

	public void setInterpretation(Interpretation interpretation) {
		this.interpretation = interpretation;
		if(interpretations == null) {
			interpretations = new ArrayList<Interpretation>();
			interpretations.add(interpretation);
		}
	}

	public Interpretation getInterpretation() {
		return this.interpretation;
	}

	@JsonProperty("display-name")
	public String getDisplayName() {
		return displayName;
	}

	@JsonProperty("display-name")
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/*public StructuredProfile toStructuredProfile(String fieldName) {
		String[] splits = fieldName.split("\\"+DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER);
		fieldName = splits[splits.length-1];
		StructuredProfile profile = new StructuredProfile();
		profile.setAliasNames(this.getAliasNames());
		profile.setDetail(this.getDetail());
		profile.setDisplayName(this.getDisplayName());
		profile.setExampleValues(this.getExampleValues());
		profile.setField(fieldName);
		profile.setInterpretation(this.getInterpretation());
		profile.setInterpretations(this.getInterpretations());
		profile.setMainType(this.getMainType());
		profile.setMatchingFields(this.getMatchingFields());
		profile.setMergedInto(this.isMergedInto());
		profile.setOriginalName(this.getOriginalName());
		profile.setPresence(this.getPresence());
		profile.setUsedInSchema(this.isUsedInSchema());
		return profile;
	}*/
	
	/*
	 * Fields specifically added for structured display. 
	 * 
	 */
	
	private static final String INAPPLICABLE = "n/a";
	private static final String NULL_PLACEHOLDER = "~";
	
	public static Profile objectProfile() {
		Profile profile = new Profile();
		profile.setMainType(MainType.OBJECT.toString());
		return profile;
	}
	
	@JsonProperty("mainType")
	public String getMainTypeCamelCase() {
		if(getMainTypeClass().equals(MainType.OBJECT)) {
			return NULL_PLACEHOLDER;
		}
		return getMainType();
	}
	
	@JsonProperty("detailType")
	public String getDetailTypeCamelCase() {
		if(getDetail() != null) {
			return getDetail().getDetailType();
		}
		return NULL_PLACEHOLDER;
	}
	
	@JsonProperty("detailMin")
	public String getDetailMin() {
		if(this.getPresence() < 0) {
			return INAPPLICABLE;
		}
		switch(getMainTypeClass()) {
		case NUMBER: return Profile.getNumberDetail(this).getMin().toPlainString();
		case STRING: return String.valueOf(Profile.getStringDetail(this).getMinLength());
		case BINARY: return INAPPLICABLE;
		default: return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailMax")
	public String getDetailMax() {
		if(this.getPresence() < 0) {
			return INAPPLICABLE;
		}
		switch(getMainTypeClass()) {
		case NUMBER: return Profile.getNumberDetail(this).getMax().toPlainString();
		case STRING: return String.valueOf(Profile.getStringDetail(this).getMaxLength());
		case BINARY: return Profile.getBinaryDetail(this).getLength().toString();
		default : return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailAvg")
	public String getDetailAvg() {
		if(this.getPresence() < 0) {
			return INAPPLICABLE;
		}
		switch(getMainTypeClass()) {
		case NUMBER: return Profile.getNumberDetail(this).getMin().toPlainString();
		case STRING: return String.valueOf(Profile.getStringDetail(this).getMinLength());
		case BINARY : return INAPPLICABLE;
		default: return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailStdDev")
	public String getDetailStdDev() {
		if(this.getPresence() < 0) {
			return INAPPLICABLE;
		}
		switch(getMainTypeClass()) {
		case NUMBER: return String.valueOf(Profile.getNumberDetail(this).getStdDev());
		case STRING: return String.valueOf(Profile.getStringDetail(this).getStdDevLength());
		case BINARY: return INAPPLICABLE;
		default: return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailNumDistinct")
	public String getNumDistinctNoHypens() {
		if(this.getPresence() < 0) {
			return INAPPLICABLE;
		}
		if(getDetail() == null) {
			return INAPPLICABLE;
		}
		return this.getDetail().getNumDistinctValues();
	}
}
