package com.deleidos.dp.beans;

public class CEntry {
	private Object v;
	
	public CEntry() {}
	
	public CEntry(String v) {
		this.v = v;
	}
	
	public CEntry(Integer v) {
		this.v = v;
	}

	public Object getV() {
		return v;
	}

	public void setV(Object v) {
		this.v = v;
	}
}
