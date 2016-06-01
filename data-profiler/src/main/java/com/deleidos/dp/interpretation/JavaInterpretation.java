package com.deleidos.dp.interpretation;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;

/**
 * Interface for interpretations that allow the framework to narrow choices down and make a final decision.
 * @author leegc
 *
 */
public interface JavaInterpretation {
	
	public double matches(String name, Profile profile);
	
	/**
	 * Determine if a value fits into this interpretation
	 * @param value Number value to be tested
	 * @return true if it fits and the interpretation is a possibility, false if this value should not be viewed as a likely interpretation 
	 */
	public boolean fitsNumberMetrics(Number value);

	/**
	 * Determine if a value fits into this interpretation
	 * @param value String value to be tested
	 * @return true if it fits and the interpretation is a possibility, false if this value should not be viewed as a likely interpretation
	 */
	public boolean fitsStringMetrics(String value);
	
	/**
	 * Determine if a value fits into this interpretation
	 * @param value Object value to be tested
	 * @return true if it fits and the interpretation is a possibility, false if this value should not be viewed as a likely interpretation 
	 */
	public boolean fitsBinaryMetrics(Object value);
	
}
