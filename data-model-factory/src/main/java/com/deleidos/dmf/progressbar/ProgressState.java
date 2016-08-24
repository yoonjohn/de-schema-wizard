package com.deleidos.dmf.progressbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProgressState {
	public enum STAGE {
		UPLOAD, DETECT, FIRST_PASS, INTERPRET, SECOND_PASS, MATCHING, SCHEMA_PASS, SAMPLE_COMPLETE, COMPLETE, SPLIT
	}
	private STAGE stage;
	private float startValue;
	private float endValue;
	private String description;
	
	private ProgressState(float startValue, float endValue, String description, STAGE stage) {
		this.startValue = startValue;
		this.endValue = endValue;
		this.description = description;
		this.stage = stage;
	}
	
	public boolean isSplit() {
		return stage.equals(STAGE.SPLIT);
	}
	
	public float rangeLength() {
		return endValue - startValue;
	}

	public boolean withinRange(float progress) {
		return progress >= this.startValue && progress < this.endValue;
	}
	
	@Override
	public String toString() {
		return description;
	}
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof ProgressState) {
			return this.stage == ((ProgressState)object).stage;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public static ProgressState uploading(float start, float end) {
		return new ProgressState(start, end, "Uploading files.", STAGE.UPLOAD);
	}
	public static ProgressState globalComplete = new ProgressState(100, Integer.MAX_VALUE, "Analysis complete.", STAGE.COMPLETE);
	public static ProgressState sampleComplete(String sampleName, float progressMark) {
		return new ProgressState(progressMark, progressMark, sampleName +": Analysis complete.", STAGE.SAMPLE_COMPLETE);
	}
	public static ProgressState schemaProgress(String sampleName, float start, float end) {
		return new ProgressState(start, end, "Merging "+sampleName+" into schema.", STAGE.SCHEMA_PASS);
	}
	public static ProgressState matching(int numSamples, float start, float end) {
		return new ProgressState(start, end, "Matching " + numSamples + " sample files.", STAGE.MATCHING);
	}
	public static ProgressState secondPass(String sampleName, float start, float end) {
		return new ProgressState(start, end, sampleName+": Building data visualizations.", STAGE.SECOND_PASS);
	}
	public static ProgressState interpreting(String sampleName, float start, float end) {
		return new ProgressState(start, end, sampleName+": Interpretting data.", STAGE.INTERPRET);
	}
	public static ProgressState sampleParsingStage(String sampleName, float start, float end) {
		return new ProgressState(start, end, sampleName+": Parsing fields.", STAGE.FIRST_PASS);
	}
	public static ProgressState detectStage(String sampleName, float start, float end) {
		return new ProgressState(start, end, sampleName + ": Detecting type.", STAGE.DETECT);  
	}
	public static List<ProgressState> split(float startValue, float endValue, String description, int numSplits) {
		List<ProgressState> splits = new ArrayList<ProgressState>();
		float width = (endValue - startValue)/(float)numSplits;
		for(int i = 0; i < numSplits; i++) {
			float start = startValue + (width*i);
			float end = startValue + (width*(i+1));
			splits.add(new ProgressState(start, end, description, STAGE.SPLIT));
		}
		return splits;
	}
	
	private static final float secondPassStartPercentage = .7f;
	private static final float interpretStartPercentage = .5f;
	private static final float firstPassStartPercentage = .1f;
	private static final float detectStartPercentage = 0;
	
	public static List<ProgressState> sampleFlow(String sampleName, float globalStart, float globalEnd) {
		float range = globalEnd - globalStart;
		float detectStart = globalStart;
		float firstPassStart = (range * firstPassStartPercentage) + globalStart;
		float interpretStart = (range * interpretStartPercentage) + globalStart;
		float secondPassStart = (range * secondPassStartPercentage) + globalStart;
		float complete = globalEnd;
		return Arrays.asList(
				detectStage(sampleName, detectStart, firstPassStart),
				sampleParsingStage(sampleName, firstPassStart, interpretStart), 
				interpreting(sampleName, interpretStart, secondPassStart), 
				secondPass(sampleName, secondPassStart, globalEnd),
				sampleComplete(sampleName, complete)
				);
	}
	
	public float getStartValue() {
		return startValue;
	}

	public void setStartValue(float startValue) {
		this.startValue = startValue;
	}

	public float getEndValue() {
		return endValue;
	}

	public void setEndValue(float endValue) {
		this.endValue = endValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public STAGE getStage() {
		return stage;
	}

	public void setStage(STAGE stage) {
		this.stage = stage;
	}
	
}
