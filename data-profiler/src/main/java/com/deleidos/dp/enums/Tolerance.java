package com.deleidos.dp.enums;

public enum Tolerance {
	STRICT, MODERATE, RELAXED;
	public float getAcceptableErrorsPercentage() {
		switch(this) {
		case STRICT: return 0.0f;
		case MODERATE: return 0.025f;
		case RELAXED: return .05f;
		default: return 0.0f;
		}
	}
	public static Tolerance fromString(String toleranceString) {
		if(toleranceString.toLowerCase().equals("strict")) {
			return STRICT;
		} else if(toleranceString.toLowerCase().equals("moderate")) {
			return MODERATE;
		} else if(toleranceString.toLowerCase().equals("relaxed")) {
			return RELAXED;
		} else {
			return null;
		}
	}
}
