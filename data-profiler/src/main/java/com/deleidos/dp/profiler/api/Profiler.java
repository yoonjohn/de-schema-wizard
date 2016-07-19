package com.deleidos.dp.profiler.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.profiler.DefaultProfilerRecord;

public interface Profiler {
	/**
	 * Load a Profiler Record object into the profiler
	 * @param metrics The metrics that the object will be loaded into.
	 * @param jsonObject Any flat JSON object.  The object should be pushed to the appropriate metrics loader in the
	 * interfacing class.
	 */
	public void load(ProfilerRecord record);

	/**
	 * Return a bean of the data that has been profiled.
	 * @return A bean (as of 2/8/16, either DataSample or Schema
	 */
	public Object asBean();

}
