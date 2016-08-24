package com.deleidos.dp.beans;

import java.util.ArrayList;
import java.util.List;

import com.deleidos.dp.enums.MainType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
@Deprecated
public class StructuredProfile extends Profile {
	private static final String INAPPLICABLE = "n/a";
	private static final String NULL_PLACEHOLDER = "~";
	private List<StructuredProfile> children;
	private int id;
	private String field;
	
	public StructuredProfile() {
		children = new ArrayList<StructuredProfile>();
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public List<StructuredProfile> getChildren() {
		return children;
	}

	public void setChildren(List<StructuredProfile> children) {
		this.children = children;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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
		switch(getMainTypeClass()) {
		case NUMBER: return Profile.getNumberDetail(this).getMin().toPlainString();
		case STRING: return String.valueOf(Profile.getStringDetail(this).getMinLength());
		case BINARY: return INAPPLICABLE;
		default: return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailMax")
	public String getDetailMax() {
		switch(getMainTypeClass()) {
		case NUMBER: return Profile.getNumberDetail(this).getMax().toPlainString();
		case STRING: return String.valueOf(Profile.getStringDetail(this).getMaxLength());
		case BINARY: return Profile.getBinaryDetail(this).getLength().toString();
		default : return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailAvg")
	public String getDetailAvg() {
		switch(getMainTypeClass()) {
		case NUMBER: return Profile.getNumberDetail(this).getMin().toPlainString();
		case STRING: return String.valueOf(Profile.getStringDetail(this).getMinLength());
		case BINARY : return INAPPLICABLE;
		default: return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailStdDev")
	public String getDetailStdDev() {
		switch(getMainTypeClass()) {
		case NUMBER: return String.valueOf(Profile.getNumberDetail(this).getStdDev());
		case STRING: return String.valueOf(Profile.getStringDetail(this).getStdDevLength());
		case BINARY: return INAPPLICABLE;
		default: return NULL_PLACEHOLDER;
		}
	}
	
	@JsonProperty("detailNumDistinct")
	public String getNumDistinctNoHypens() {
		if(getDetail() == null) {
			return INAPPLICABLE;
		}
		return this.getDetail().getNumDistinctValues();
	}
	
	public static StructuredProfile objectProfile() {
		StructuredProfile profile = new StructuredProfile();
		profile.setMainType(MainType.OBJECT.toString());
		return profile;
	}
	
	public Profile toProfile() {
		Profile profile = new Profile();
		profile.setAliasNames(this.getAliasNames());
		/*Detail superDetail = super.getDetail();
		Detail detail = null;
		if(superDetail.isBinaryDetail()) {
			BinaryDetail bDetail = new BinaryDetail();
			detail = bDetail;
		} else if(superDetail.isNumberDetail()) {
			NumberDetail nDetail = new NumberDetail();
			nDetail.setFreqHistogram(null);
			nDetail.setRegionDataIfApplicable(null);
			detail = nDetail;
		} else if(superDetail.isStringDetail()) {
			StringDetail sDetail = new StringDetail();
			sDetail.setTermFreqHistogram(null);
			detail = sDetail;
		}*/
		profile.setDetail(this.getDetail());
		profile.setDisplayName(this.getDisplayName());
		profile.setExampleValues(this.getExampleValues());
		profile.setInterpretation(this.getInterpretation());
		profile.setInterpretations(this.getInterpretations());
		profile.setMainType(this.getMainType());
		profile.setMatchingFields(this.getMatchingFields());
		profile.setMergedInto(this.isMergedInto());
		profile.setOriginalName(this.getOriginalName());
		profile.setPresence(this.getPresence());
		profile.setUsedInSchema(this.isUsedInSchema());
		return profile;
	}
	
	@JsonIgnore
	@Override
	public Detail getDetail() {
		return super.getDetail();
	}

	@JsonIgnore
	@Override
	public List<AliasNameDetails> getAliasNames() {
		return super.getAliasNames();
	}
	
	@JsonIgnore
	@Override
	public Attributes getAttributes() {
		// TODO Auto-generated method stub
		return super.getAttributes();
	}
	
	@JsonIgnore
	@Override
	public List<Object> getExampleValues() {
		// TODO Auto-generated method stub
		return super.getExampleValues();
	}
	
	@JsonIgnore
	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return super.getDisplayName();
	}
	
	@JsonIgnore
	@Override
	public Interpretation getInterpretation() {
		// TODO Auto-generated method stub
		return super.getInterpretation();
	}
	
	@JsonIgnore
	@Override
	public List<MatchingField> getMatchingFields() {
		// TODO Auto-generated method stub
		return super.getMatchingFields();
	}
	
	@JsonIgnore
	@Override
	public boolean isUsedInSchema() {
		// TODO Auto-generated method stub
		return super.isUsedInSchema();
	}
	
	@JsonIgnore
	@Override
	public boolean isMergedInto() {
		// TODO Auto-generated method stub
		return super.isMergedInto();
	}
	
	@JsonIgnore
	@Override
	public String getMainType() {
		// TODO Auto-generated method stub
		return super.getMainType();
	}
	
	@JsonIgnore
	@Override
	public String getOriginalName() {
		// TODO Auto-generated method stub
		return super.getOriginalName();
	}
	
}
