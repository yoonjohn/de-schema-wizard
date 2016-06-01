package com.deleidos.dp.profiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class VectorOrderProfilerRecord extends HashMap<VectorOrderedProfilerKey, Object> implements ProfilerRecord {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8352100950503446171L;

	private void fillWithAbsent() {
		Set<VectorOrderedProfilerKey> keySet = keySet();
		int deepestVectorLength = 0;
		for(VectorOrderedProfilerKey key : keySet) {
			if(key.getOrderVector().size() > deepestVectorLength) {
				deepestVectorLength = key.getOrderVector().size();
			}
		}
		for(VectorOrderedProfilerKey key: keySet) {
			int vectorLength = key.getOrderVector().size();
			for(int i = vectorLength; i < deepestVectorLength; i++) {
				key.getOrderVector().set(i, VectorOrderedProfilerKey.ABSENT);
			}
		}
	}

	@Override
	public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior) {
		fillWithAbsent();
		return null;
	}


}
