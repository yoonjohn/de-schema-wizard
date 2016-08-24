package com.deleidos.dp.enums;

public enum DetailType {
	INTEGER, DECIMAL, EXPONENT, DATE_TIME, BOOLEAN, TERM, PHRASE, IMAGE, VIDEO_FRAME, AUDIO_SEGMENT, TEXT;
	
	public void incrementCount(int[] detailTypeTracker) {
		detailTypeTracker[ordinal()]++;
	}
	
	public static DetailType getTypeByIndex(int index) {
		switch(index) {
		case 0: {
			return INTEGER;
		}
		case 1:{
			return DECIMAL;
		}
		case 2: {
			return EXPONENT;
		} 
		case 3: {
			return DATE_TIME;
		}
		case 4: {
			return BOOLEAN;
		}
		case 5: {
			return TERM;
		}
		case 6: {
			return PHRASE;
		}
		case 7: {
			return IMAGE;
		}
		case 8: {
			return VIDEO_FRAME;
		}
		case 9: {
			return AUDIO_SEGMENT;
		}
		case 10: {
			return TEXT;
		}
		default : {
			return null;
		}
		}
	}
	public MainType getMainType() {
		switch(this) {
		case INTEGER: return MainType.NUMBER; case DECIMAL: return MainType.NUMBER; case EXPONENT: return MainType.NUMBER;
		case DATE_TIME: return MainType.STRING; case BOOLEAN: return MainType.STRING; case TERM: return MainType.STRING; case PHRASE: return MainType.STRING;
		case IMAGE: return MainType.BINARY; case VIDEO_FRAME: return MainType.BINARY; case AUDIO_SEGMENT: return MainType.BINARY;
		case TEXT: return MainType.STRING;
		default: return null;
		}
	}
	public int getIndex() {
		return ordinal();
	}
	@Override
	public String toString() {
		String s = super.toString();
		return s.toLowerCase();
	}
	public static DetailType fromString(String string) {
		string = string.toLowerCase();
		if(string.equals("integer")) {
			return INTEGER;
		} else if(string.equals("decimal")) {
			return DECIMAL;
		} else if(string.equals("exponent")) {
			return EXPONENT;
		} else if(string.equals("date_time")) {
			return DATE_TIME;
		} else if(string.equals("boolean")) {
			return BOOLEAN;
		} else if(string.equals("term")) {
			return TERM;
		} else if(string.equals("phrase")) {
			return PHRASE;
		} else if(string.equals("image")) {
			return IMAGE;
		} else if(string.equals("video_frame")) {
			return VIDEO_FRAME;
		} else if(string.equals("audio_segment")) {
			return AUDIO_SEGMENT;
		} else if(string.equals("text")) {
			return TEXT;
		} else {
			return null;
		}
	}
}
