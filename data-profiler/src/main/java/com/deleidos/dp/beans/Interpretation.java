package com.deleidos.dp.beans;

import com.deleidos.dp.interpretation.JavaLatitudeInterpretation;
import com.deleidos.dp.interpretation.JavaLongitudeInterpretation;
import com.deleidos.dp.interpretation.JavaUnknownInterpretation;

public class Interpretation {
	public static Interpretation UNKNOWN = new Interpretation("Unknown");
	private String interpretation;
	private boolean quantized;
	private boolean ordered;
	private boolean categorical;
	private boolean ordinal;
	private boolean relational;
	
	public Interpretation() { }
	
	private Interpretation(String name) {
		this.interpretation = name;
	}
	
	public String getInterpretation() {
		return interpretation;
	}
	public void setInterpretation(String interpretation) {
		this.interpretation = interpretation;
	}
	public boolean isQuantized() {
		return quantized;
	}
	public void setQuantized(boolean quantized) {
		this.quantized = quantized;
	}
	public boolean isOrdered() {
		return ordered;
	}
	public void setOrdered(boolean ordered) {
		this.ordered = ordered;
	}
	public boolean isCategorical() {
		return categorical;
	}
	public void setCategorical(boolean categorical) {
		this.categorical = categorical;
	}
	public boolean isOrdinal() {
		return ordinal;
	}
	public void setOrdinal(boolean ordinal) {
		this.ordinal = ordinal;
	}
	public boolean isRelational() {
		return relational;
	}
	public void setRelational(boolean relational) {
		this.relational = relational;
	}
	

	private static final transient String latInterpretationName = new JavaLatitudeInterpretation().getInterpretationName();
	private static final transient String lonInterpretationName = new JavaLongitudeInterpretation().getInterpretationName();
	private static final transient String unknownInterpretationName = new JavaUnknownInterpretation().getInterpretationName();
	
	public static boolean isLatitude(Interpretation interpretation) {
		return interpretation.getInterpretation().equals(latInterpretationName);
	}
	public static boolean isLongitude(Interpretation interpretation) {
		return interpretation.getInterpretation().equals(lonInterpretationName);
	}
	public static boolean isCoordinate(Interpretation interpretation) {
		return isLatitude(interpretation) || isLongitude(interpretation);
	}
	public static boolean isUnknown(Interpretation interpretation) {
		return interpretation.getInterpretation().equals(unknownInterpretationName);
	}
	
}
