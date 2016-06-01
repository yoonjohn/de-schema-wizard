package com.deleidos.dp.beans;

public class FieldDescriptor {
	private String mainType;
	private String detailType;
	private String interpretation;

	public String getMainType() {
		return mainType;
	}

	public String getDetailType() {
		return detailType;
	}

	public String getInterpretation() {
		return interpretation;
	}

	public void setMainType(String mainType) {
		this.mainType = mainType;
	}

	public void setDetailType(String detailType) {
		this.detailType = detailType;
	}

	public void setInterpretation(String interpretation) {
		this.interpretation = interpretation;
	}
}
