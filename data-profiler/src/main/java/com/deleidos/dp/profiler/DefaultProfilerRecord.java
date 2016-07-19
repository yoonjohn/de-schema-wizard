package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.api.ProfilerRecord;

/**
 * Profiler Record that accepts structured values in the form of a map.  Sub-objects will be made unique by appending
 * names with a "." between structure levels.  Sub-lists will be accumulated as multiple values under the same name.
 * 
 * For example
 * <pre>
DefaultProfilerRecord record = new DefaultProfilerRecord();
List<Object> subList = Arrays.asList("1","2","3");
Map<String, Object> subObject = new HashMap<String, Object>();
subObject.put("x", "10");
subObject.put("y", "20");
record.put("a", "root-level");
record.put("b", subList);
record.put("c", subObject);
record.normalizeRecord().forEach((k,v)->System.out.println(k + "->" + v));
</pre>
 * will output:
 * <pre>
a->[root-level]
b->[1, 2, 3]
c.x->[10]
c.y->[20]
 * </pre>
 * @author leegc
 *
 */
public class DefaultProfilerRecord extends HashMap<String, Object> implements ProfilerRecord {
	private static final long serialVersionUID = 6313190276596879359L;
	public static final String STRUCTURED_OBJECT_APPENDER = ".";
	private int recordProgress;
	
	public DefaultProfilerRecord() {
		
	}

	public DefaultProfilerRecord(Map<String, Object> map) {
		super(map);
	}
	
	@SuppressWarnings("unchecked")
	protected static void flattenMap(String currentPath, Object baseObject, 
			Map<String, List<Object>> flattenedMap, GroupingBehavior behavior) {
		if (baseObject instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) baseObject;
			String pathPrefix = currentPath.isEmpty() ? "" : currentPath + STRUCTURED_OBJECT_APPENDER;

			for (String key : map.keySet()) {
				flattenMap(pathPrefix + key, map.get(key), flattenedMap, behavior);
			}
		} else if (baseObject instanceof List) {
			List<?> list = (List<?>) baseObject;
			for (int i = 0; i < list.size(); i++) {
				Object o = list.get(i);
				String updatedPathKey = currentPath;
				if(behavior.equals(GroupingBehavior.UNIQUE_ARRAY_VALUES)) {
					updatedPathKey = currentPath + "[" + i + "]";
				} 
				flattenMap(updatedPathKey, o, flattenedMap, behavior);
			}
		} else {
			if(flattenedMap.containsKey(currentPath)) {
				flattenedMap.get(currentPath).add(baseObject);
			} else {
				List<Object> valuesList = new ArrayList<Object>();
				valuesList.add(baseObject);
				flattenedMap.put(currentPath, valuesList);
			}
		}

	}

	@Override
	public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior) {
		Map<String, List<Object>> normalizedMap = new HashMap<String, List<Object>>();
		String startingString = "";
		flattenMap(startingString, this, normalizedMap, groupingBehavior);
		return normalizedMap;
	}

	public int getRecordProgress() {
		return recordProgress;
	}

	public void setRecordProgress(int recordProgress) {
		this.recordProgress = recordProgress;
	}
	
	@Override
	public int recordProgressWeight() {
		return recordProgress;
	}

}
