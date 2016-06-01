package com.deleidos.dp.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RowEntry {
	private List<CEntry> c;
	
	public RowEntry() {}
	
	public RowEntry(String key, Integer value) {
		setC(new ArrayList<CEntry>(
				Arrays.asList(
					new CEntry(key),
					new CEntry(value))
				)
			);
	}
	
	@JsonIgnore
	public String getKey() {
		return this.c.get(0).getV().toString();
	}
	
	@JsonIgnore
	public Integer getValue() {
		return Integer.valueOf(this.c.get(1).getV().toString());
	}
	
	public RowEntry(List<CEntry> c) {
		this.c = c;
	}

	public List<CEntry> getC() {
		return c;
	}

	public void setC(List<CEntry> c) {
		this.c = c;
	}
}
