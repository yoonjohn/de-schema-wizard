package com.deleidos.dp.beans;

import java.util.ArrayList;
import java.util.List;

import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.interpretation.AbstractJavaInterpretation;
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
	private String mainType;
	private Interpretation interpretation;
	private Detail detail;
	private List<AliasNameDetails> aliasNames;
	private List<MatchingField> matchingFields;
	private boolean usedInSchema = false;
	private boolean mergedInto = false;

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

	public void setMainType(String mainType) {
		this.mainType = mainType;
	}

	public void setInterpretation(Interpretation interpretation) {
		this.interpretation = interpretation;
	}

	public void setDetail(Detail detail) {
		this.detail = detail;
	}

	@JsonProperty("main-type")
	public String getMainType() {
		return mainType;
	}

	public Interpretation getInterpretation() {
		return interpretation;
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

	public float getPresence() {
		return presence;
	}

	public void setPresence(float presence) {
		this.presence = presence;
	}
	
	public static MainType getProfileDataType(Profile profile) {
		return MainType.fromString(profile.mainType);
	}
	
	public static NumberDetail getNumberDetail(Profile profile) {
		if(profile.getDetail().isNumberDetail()) {
			return (NumberDetail) profile.getDetail();
		} else {
			return null;
		}
	}
	
	public static StringDetail getStringDetail(Profile profile) {
		if(profile.getDetail().isStringDetail()) {
			return (StringDetail) profile.getDetail();
		} else {
			return null;
		}
	}
	
	public static BinaryDetail getBinaryDetail(Profile profile) {
		if(profile.getDetail().isBinaryDetail()) {
			return (BinaryDetail) profile.getDetail();
		} else {
			return null;
		}
	}
}
