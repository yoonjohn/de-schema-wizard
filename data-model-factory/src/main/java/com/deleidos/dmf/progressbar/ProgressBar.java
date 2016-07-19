package com.deleidos.dmf.progressbar;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 	Each sample has 0 - 100 to show progress<br>
 *	0 - 24 are for detection (STAGE_1)<br>
 *	25 - 49 are for first parsing pass (STAGE_2)<br>
 *	50 - 100 are for second parsing pass (STAGE_3)<br>
 *	During STAGE_2, progress may be "LOCKED," which will not let the progress exceed 74% until it
 *	is explicitly "UNLOCKED"<br>
 *	Together, samples have 100 * <number of samples> as a denominator for progress.  So a progress of 
 *	200/400 means there a 4 data samples, and the second data sample analysis has been completed. 
 *		<br>
 *	Schemas use the same logic, but do not have a second pass, so they will progress much faster.   
 * @author leegc
 *
 */
public class ProgressBar {


	private static final Logger logger = Logger.getLogger(ProgressBar.class);
	private float currentSampleNumerator;
	private float globalNumerator;
	private int denominator;
	private int totalSamples;
	private int sampleIndex;
	private int currentStateSplitIndex;
	private int currentStateSplits;
	private boolean isSamplePass;
	private String name;
	private ProgressState currentStateClazz;

	public ProgressBar(String name, int sampleIndex, int totalSamples, ProgressState initialState) { 
		this.totalSamples = totalSamples;
		this.sampleIndex = sampleIndex;
		this.denominator = totalSamples * 100;
		this.globalNumerator = sampleIndex * 100;
		this.currentSampleNumerator = 0;
		this.currentStateClazz = initialState;
		this.name = name;
		this.currentStateSplits = 1;
		this.currentStateSplitIndex = 0;
	}

	public ProgressBar(String name, int sampleIndex, int totalSamples, int numerator, ProgressState initialState) {
		this(name, sampleIndex, totalSamples, initialState);
		this.currentSampleNumerator = numerator;
	}

	@JsonIgnore
	public ProgressState getCurrentState() {
		return currentStateClazz;
	}

	@JsonIgnore
	// prevents any non-monotonical increases and weighs down split states based on how many splits there are
	// TODO very unhappy with how messy this is...but it works for now
	public void updateCurrentSampleNumerator(int currentSampleNumerator) {
		if(this.currentStateSplits == 1) {// || !currentStateClazz.equals(ProgressState.sampleParsingStage)) {
			
			if(currentSampleNumerator + (100 * sampleIndex) < globalNumerator) {
				// do not update if the progress bar would go backwards
				return;
			} else if(!currentStateClazz.withinRange(currentSampleNumerator)) {
				return;
			} else if(currentSampleNumerator > 100) {
				currentSampleNumerator = 100;
			}
			this.currentSampleNumerator = currentSampleNumerator;
			this.globalNumerator = (100 * this.sampleIndex) + currentSampleNumerator;
		} else if (this.currentStateSplits > 1) {
			
			float splitDifference = ((float)currentStateClazz.rangeLength()/(float)currentStateSplits)*(currentStateSplitIndex+1);
			
			float splitCurrentSampleNumerator = currentStateClazz.getStartValue() + splitDifference;
			if(splitCurrentSampleNumerator + (100 * sampleIndex) < globalNumerator) {
				// do not update if the progress bar would go backwards
				return;
			} else if(!currentStateClazz.withinRange((int)splitCurrentSampleNumerator)) {
				return;
			} else if(splitCurrentSampleNumerator > 100) {
				splitCurrentSampleNumerator = 100;
			}
			this.currentSampleNumerator = splitCurrentSampleNumerator;
			this.globalNumerator = (100 * this.sampleIndex) + splitCurrentSampleNumerator;
		}
	}

	@JsonIgnore
	public int getCurrentStateSplits() {
		return currentStateSplits;
	}

	@JsonIgnore
	public void setCurrentStateSplits(int currentStateSplits) {
		this.currentStateSplits = currentStateSplits;
	}

	@JsonIgnore
	public void setCurrentState(ProgressState progressUpdate) {
		if(currentStateClazz.equals(ProgressState.lock) 
				&& !progressUpdate.equals(ProgressState.unlock)) {
			//if embedded parsing has not been explicitly stopped, do not change the state
			return;
		} else {
			currentStateClazz = progressUpdate;
			updateCurrentSampleNumerator(currentStateClazz.getStartValue());
		}
	}

	@JsonIgnore
	public int getCurrentStateSplitIndex() {
		return currentStateSplitIndex;
	}

	@JsonIgnore
	public void setCurrentStateSplitIndex(int currentStateSplitIndex) {
		this.currentStateSplitIndex = currentStateSplitIndex;
	}

	public String getDescription() {
		return name + ": " + currentStateClazz.getDescription();
	}

	public int getNumerator() {
		return (int)globalNumerator;
	}

	public int getDenominator() {
		return denominator;
	}

	@JsonIgnore
	public int getTotalSamples() {
		return totalSamples;
	}

	@JsonIgnore
	private int getCurrentSampleNumerator() {
		return (int)currentSampleNumerator;
	}

	@JsonIgnore
	private void setTotalSamples(int totalSamples) {
		this.totalSamples = totalSamples;
	}

	@JsonIgnore
	private int getSampleIndex() {
		return sampleIndex;
	}

	@JsonIgnore
	private void setSampleIndex(int sampleIndex) {
		this.sampleIndex = sampleIndex;
	}

}
