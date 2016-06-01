package com.deleidos.dp.profiler.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.enums.GroupingBehavior;

/**
 * Interface for records that a profiler gathers.  Meant to allow flexibility for data that does not have a definitive concept 
 * of a record.
 * @author leegc
 *
 */
public interface ProfilerRecord {
	/**
	 * Return a normalized mapping of the key and their associated value(s) that this entry gathered.
	 * @param groupingBehavior The desired grouping behavior when handling structured data. 
	 * @return A flat mapping of keys to <i>n</i> number of values. 
	 */
	public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior);
	
	default int recordProgressWeight() {
		return 1;
	}
}
