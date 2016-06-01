package com.deleidos.dmf.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Example implementation of ProfilerRecord interface
 * @author leegc
 *
 */
public class URLAnalyzerRecord extends ArrayList<String> implements ProfilerRecord {

	/**
	 * The idea of this interface is, "it doesn't matter what the implementation does, as long as it returns a normalized 
	 * mapping of values."  This example analyzer record will just load whatever it gets as values for the key "URL."
	 */
	@Override
	public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior) {
		Map<String, List<Object>> normalizedUrls = new HashMap<String, List<Object>>();
		normalizedUrls.put("URL", Arrays.asList(this));
		return normalizedUrls;
	}

}
