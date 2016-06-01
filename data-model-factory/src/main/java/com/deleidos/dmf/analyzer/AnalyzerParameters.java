package com.deleidos.dmf.analyzer;

/**
 * Analyzer Parameters interface for passing arbitrary data into an Analyzer implementation.
 * @author leegc
 *
 */
public interface AnalyzerParameters {
	
	/**
	 * Get the profiler bean.  As of 3/14/16, this is either a DataSample or Schema object.
	 *   
	 * @return A bean that represents the data that was analyzed.
	 */
	public Object getProfilerBean();
	
}
