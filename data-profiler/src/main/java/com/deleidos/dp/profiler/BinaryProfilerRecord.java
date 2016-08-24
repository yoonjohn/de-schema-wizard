package com.deleidos.dp.profiler;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.GroupingBehavior;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class BinaryProfilerRecord implements ProfilerRecord {
	private String binaryName;
	private ByteBuffer byteBuffer;
	private final int length;
	private DetailType detailType;

	public BinaryProfilerRecord(String binaryName, DetailType detailType, ByteBuffer bytes) {
		this.binaryName = binaryName;
		this.byteBuffer = bytes;
		this.length = bytes.array().length;
		this.detailType = detailType;
	}
	
	@Override
	public Map<String, List<Object>> normalizeRecord(GroupingBehavior groupingBehavior) {
		Map<String, List<Object>> imageMapping = new HashMap<String, List<Object>>();
		imageMapping.put(binaryName, Arrays.asList(byteBuffer));
		return imageMapping;
	}

	public DetailType getDetailType() {
		return detailType;
	}

	public void setDetailType(DetailType detailType) {
		this.detailType = detailType;
	}

	public String getBinaryName() {
		return binaryName;
	}

	public void setBinaryName(String binaryName) {
		this.binaryName = binaryName;
	}
}
