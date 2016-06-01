package com.deleidos.dp.profiler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class BinaryProfilerRecord implements ProfilerRecord {
	private String binaryName;
	private ByteBuffer byteBuffer;

	public BinaryProfilerRecord(String binaryName, ByteBuffer bytes) {
		this.binaryName = binaryName;
		this.byteBuffer = bytes;
	}

	@Override
	public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior) {
		Map<String, List<Object>> imageMapping = new HashMap<String, List<Object>>();
		imageMapping.put(binaryName, Arrays.asList(byteBuffer));
		return imageMapping;
	}
}
