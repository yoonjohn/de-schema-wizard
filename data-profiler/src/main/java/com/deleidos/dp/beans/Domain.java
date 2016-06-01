package com.deleidos.dp.beans;

import java.util.List;

public class Domain {
	private String name;
	private List<Interpretation> interpretations;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Interpretation> getInterpretations() {
		return interpretations;
	}
	public void setInterpretations(List<Interpretation> interpretations) {
		this.interpretations = interpretations;
	}
	
}
