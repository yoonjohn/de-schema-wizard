package com.deleidos.dp.enums;

public enum MainType {
	STRING, NUMBER, BINARY, OBJECT, ARRAY, NULL;
	
	public int getIndex() {
		if(this.equals(STRING)) {
			return 0;
		} else if(this.equals(NUMBER)) {
			return 1;
		} else if(this.equals(BINARY)) {
			return 2;
		} else {
			return -1;
		}
	}
	
	public static MainType getTypeByIndex(int index) {
		return MainType.values()[index];
	}
	
	public void incrementCount(int[] typeTracker) {
		typeTracker[getIndex()]++;
	}
	
	@Override
	public String toString() {
		String s = super.toString();
		return s.toLowerCase();
	}
	
	public static MainType fromString(String string) {
		string = string.toLowerCase();
		if(string.equals("number")) {
			return NUMBER;
		} else if(string.equals("string")) {
			return STRING;
		} else if(string.equals("binary")) {
			return BINARY;
		} else {
			return null;
		}
	}
}
