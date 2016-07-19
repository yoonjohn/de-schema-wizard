package com.deleidos.dp.enums;

import java.nio.ByteBuffer;

import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.exceptions.MainTypeException;

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
	
	/**
	 * Returns the appropriate object based based on the main type.  A null object will return null.
	 * @param object
	 * @return
	 * @throws MainTypeException thrown if the value if determined to be a certain main type, but encounters an error
	 * when attempting to convert it to that main type.
	 */
	public Object createAppropriateObject(Object object) throws MainTypeException {
		if(object == null) {
			return null;
		}
		switch(this) {
		case NUMBER: {
			return createNumber(object);
		}
		case STRING: {
			return createString(object);
		}
		case BINARY: {
			return createBinary(object);
		}
		default: {
			return null;
		}
		}
	}
	
	private Number createNumber(Object object) throws MainTypeException {
		return MetricsCalculationsFacade.createNumberWithDoublePrecisionOrLower(object);
	}
	
	private String createString(Object object) throws MainTypeException {
		return object.toString();
	}
	
	private ByteBuffer createBinary(Object object) throws MainTypeException {
		if(!(object instanceof ByteBuffer)) {
			throw new MainTypeException("Value "+object.getClass().getName()+" is not a byte buffer.");
		} else {
			return (ByteBuffer)object;
		}
	}
}
