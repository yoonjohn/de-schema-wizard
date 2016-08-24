package com.deleidos.dp.profiler.api;

public interface Profiler {
	/**
	 * Load a Profiler Record object into the profiler
	 * @param metrics The metrics that the object will be loaded into.
	 * @param jsonObject Any flat JSON object.  The object should be pushed to the appropriate metrics loader in the
	 * interfacing class.
	 */
	public int load(ProfilerRecord record);

	/**
	 * Return a bean of the data that has been profiled.
	 * @return A bean (as of 2/8/16, either DataSample or Schema
	 */
	public Object asBean();

}
