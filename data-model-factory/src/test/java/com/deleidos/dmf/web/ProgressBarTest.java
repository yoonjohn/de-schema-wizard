package com.deleidos.dmf.web;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.deleidos.dmf.progressbar.ProgressBar;
import com.deleidos.dmf.progressbar.ProgressState;
import com.deleidos.dp.deserializors.SerializationUtility;

public class ProgressBarTest {
	private static final Logger logger = Logger.getLogger(ProgressBarTest.class);
	private ProgressBar progressBar;
	private List<Integer> orderedValues;
	
	private static ProgressState progressFromInt(int i) {
		if(i < 25) {
			return ProgressState.detectStage;
		} else if(i >24 && i < 50) {
			return ProgressState.sampleParsingStage;
		} else if(i >49 && i < 100) {
			return ProgressState.geocodingStage;
		} else if(i > 99) {
			return ProgressState.complete;
		}
		return null;
	}

	@Before
	public void testMonotonicallyIncreasingValue() {
		orderedValues = new ArrayList<Integer>();
		int[] range = new int[100];
		for(int i = 0; i < range.length; i++) {
			range[i] = i+1;
		}

		ProgressState state = null;
		progressBar = new ProgressBar("example-file-name.txt", 0, 3, ProgressState.detectStage);

		for(int i : range) {
			progressBar.setCurrentState(progressFromInt(i));
			progressBar.updateCurrentSampleNumerator(i);
			progressBar.updateCurrentSampleNumerator(i-1);
			progressBar.updateCurrentSampleNumerator(10000);
			if(!progressBar.getCurrentState().equals(state)) {
				state = progressBar.getCurrentState();
			}
			orderedValues.add(progressBar.getNumerator());
		}

		progressBar = new ProgressBar("example-file-name.txt", 1, 3, ProgressState.detectStage);

		for(int i : range) {
			if(i == 25) {
				progressBar.setCurrentState(ProgressState.lock);
			} else if(i ==75) {
				progressBar.setCurrentState(ProgressState.unlock);
			}
			progressBar.setCurrentState(progressFromInt(i));
			progressBar.updateCurrentSampleNumerator(i);
			progressBar.updateCurrentSampleNumerator(i-1);
			progressBar.updateCurrentSampleNumerator(10000);
			if(!progressBar.getCurrentState().equals(state)) {
				state = progressBar.getCurrentState();
			}
			orderedValues.add(progressBar.getNumerator());
		}

		progressBar = new ProgressBar("example-file-name.txt", 2, 3, ProgressState.detectStage);

		progressBar.setCurrentState(ProgressState.sampleParsingStage);
		progressBar.setCurrentStateSplits(3);
		for(int j = 0; j < 3; j++) {
			progressBar.setCurrentStateSplitIndex(j);
			for(int i = 25; i < 50; i++) {
				progressBar.setCurrentState(progressFromInt(i));
				progressBar.updateCurrentSampleNumerator(i);
				progressBar.updateCurrentSampleNumerator(i-1);
				progressBar.updateCurrentSampleNumerator(10000);
				if(!progressBar.getCurrentState().equals(state)) {
					state = progressBar.getCurrentState();
				}
				orderedValues.add(progressBar.getNumerator());
			}
		}
		progressBar.setCurrentState(ProgressState.geocodingStage);
		progressBar.setCurrentState(ProgressState.complete);
	}


	@Test
	public void testMonotonicallyIncreased() {
		boolean monotonical = assertMonotonicallyIncreasing(orderedValues);
		if(monotonical) {
			logger.info("Monotonically increased progress.");
		} else {
			logger.error("Did not monotonically increase.");
			assertTrue(false);
		}
	}

	@Test
	public void testSerialize() {

		JSONObject j = new JSONObject(SerializationUtility.serialize(progressBar));
		boolean a1 = j.has("numerator");
		boolean a2 = j.has("denominator");
		boolean a3 = j.has("description");
		boolean assertion = a1 && a2 && a3;
		if(assertion) {
			logger.info("Progress bar object has expected fields.");
		} else {
			logger.error("Progress bar object does not have expected fields.");
		}
	}

	private boolean assertMonotonicallyIncreasing(List<Integer> values) {
		for(int i = 0; i < values.size()-1; i++) {
			Integer value = values.get(i);
			for(int j = 0; j < i; j++) {
				Integer before = values.get(j);
				if(before > value) {
					return false;
				}
			}
		}
		return true;
	}
}
