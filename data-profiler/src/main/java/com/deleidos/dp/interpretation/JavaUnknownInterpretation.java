package com.deleidos.dp.interpretation;

import com.deleidos.dp.beans.Profile;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Default unknown interpretation.  Matches and fits with everything, but confidence is minimal so any other possibility takes priority.
 * @author leegc
 *
 */
public class JavaUnknownInterpretation extends AbstractJavaInterpretation {
	
	public JavaUnknownInterpretation() {
		setConfidence(.01);
	}

	@Override
	public String initInterpretationName() {
		return "Unknown";
	}

	@Override
	public double matches(String name, Profile profile) {
		return 0.1f;
	}

	@Override
	public boolean fitsNumberMetrics(Number value) {
		return true;
	}

	@Override
	public boolean fitsStringMetrics(String value) {
		return true;
	}

	@Override
	public boolean fitsBinaryMetrics(Object value) {
		return true;
	}

}
