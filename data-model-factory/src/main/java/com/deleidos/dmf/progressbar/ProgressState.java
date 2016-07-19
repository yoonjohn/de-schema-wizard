package com.deleidos.dmf.progressbar;

public class ProgressState {
	private int startValue;
	private int endValue;
	private String description;
	
	public ProgressState(int startValue, int endValue, String description) {
		this.startValue = startValue;
		this.endValue = endValue;
		this.description = description;
	}
	
	public int rangeLength() {
		return endValue - startValue;
	}

	public boolean withinRange(int progress) {
		return progress >= this.startValue && progress < this.endValue;
	}
	
	@Override
	public String toString() {
		return description;
	}
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof ProgressState) {
			return this.startValue == ((ProgressState)object).startValue && this.endValue == ((ProgressState)object).endValue; 
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	public static ProgressState detectStage = new ProgressState(0,25,"Detecting type.");  
	public static ProgressState sampleParsingStage = new ProgressState(25,50,"Parsing fields.");
	public static ProgressState schemaProgress = new ProgressState(0,100,"Merging sample into schema.");
	public static ProgressState geocodingStage = new ProgressState(50,100,"Mapping geographical data.");
	public static ProgressState complete = new ProgressState(100, Integer.MAX_VALUE, "Analysis complete.");
	public static ProgressState matching = new ProgressState(100, Integer.MAX_VALUE, "Performing matching analysis.");
	public static ProgressState lock = new ProgressState(0, 0, "Parsing embedded documents.");
	public static ProgressState unlock = new ProgressState(0, 0, "Embedded document parsing completed.");

	public int getStartValue() {
		return startValue;
	}

	public void setStartValue(int startValue) {
		this.startValue = startValue;
	}

	public int getEndValue() {
		return endValue;
	}

	public void setEndValue(int endValue) {
		this.endValue = endValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
