package com.deleidos.dp.accumulator;

/**
 * Interface for accumulators that take in fields with the ultimate goal of calculating metrics 
 * @author leegc
 *
 * @param <T> The type that will be affected by the accumulation.
 */
public interface Accumulator<T> {
	/**
	 * Accumulate data into the Accumulator's metric.  The intention is to do minimal calculations until they are 
	 * required (e.g. don't calculate the average until it is required)
	 * @param value The value to accumulate into the metric.
	 */
	public boolean accumulate(Object value);
	
	/**
	 * 
	 * @param value
	 */
	public boolean initFirstValue(Object value);
	
	/**
	 * Get a copy of the current state of the metric.  Do all calculations in this method.
	 * @return A copy of the metric and all of its calculated fields.
	 */
	public T getState();
	
	/**
	 * Clean up and finish the accumulation of fields.
	 */
	public void finish();
}
