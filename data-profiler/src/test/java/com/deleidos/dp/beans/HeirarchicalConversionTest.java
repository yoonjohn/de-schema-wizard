package com.deleidos.dp.beans;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.deserializors.ConversionUtility;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.environ.DPMockUpEnvironmentTest;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class HeirarchicalConversionTest extends DPMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(HeirarchicalConversionTest.class);

	private static Object randomVal() {
		List<Object> values = Arrays.asList("a","b","c","d", 1, 2, 3, 4);
		return values.get((int)(Math.random()*values.size()));
	}
	
	private static String randomKey() {
		String letters = "abcdefghijklmnopqrstuvwxyz";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < Math.random()*10; i++) {
			sb.append(letters.charAt(((int)(Math.random()*letters.length()))));
		}
		return sb.toString();
	}

	@Test
	public void testFlatToHeirarchical() throws DataAccessException {
		List<ProfilerRecord> records = new ArrayList<ProfilerRecord>();
		List<String> someStrings = Arrays.asList("root", "depth1", "depth2", "depth3", "nestedName");
		for(int i = 0; i < 100; i++) {
			DefaultProfilerRecord record = new DefaultProfilerRecord();
			Map<String, Object> heirarchicalMap = new HashMap<String, Object>();
			Map<String, Object> nestedMap1 = new HashMap<String, Object>();
			nestedMap1.put(someStrings.get(2), randomVal());
			nestedMap1.put(someStrings.get(4), randomVal());
			for(int j = 0; j < 3; j++) {
				nestedMap1.put(randomKey(), randomVal());
			}
			Map<String, Object> nestedMap2 = new HashMap<String, Object>();
			nestedMap2.put(someStrings.get(3), randomVal());
			nestedMap2.put(someStrings.get(4), randomVal());
			for(int j = 0; j < 3; j++) {
				nestedMap2.put(randomKey(), randomVal());
			}

			heirarchicalMap.put(someStrings.get(1), nestedMap1);
			heirarchicalMap.put(someStrings.get(1)+"a", nestedMap2);

			record.put("root-val", randomVal());
			record.put(someStrings.get(0), heirarchicalMap);
			records.add(record);
		}
		DataSample sample = SampleProfiler.generateDataSampleFromProfilerRecords("Transportation", Tolerance.STRICT, records);
		try {
			assertTrue(sample.isDsContainsStructuredData());
		} catch (AssertionError e) {
			logger.error("Contains structured data flag not set.");
			throw e;
		}
		List <StructuredNode> heirarchicalProfiles = sample.getDsStructuredProfile();
		logger.debug(SerializationUtility.serialize(heirarchicalProfiles));
		//Map<String, Profile> backwardsConversion = ConversionUtility.convertToFlattenedMap(heirarchicalProfiles);
		//logger.debug(SerializationUtility.serialize(backwardsConversion));

		for(StructuredNode node : heirarchicalProfiles) {
			String key = node.getField();
			try {
				assertTrue(!key.contains(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER));
				if(key.equals("root")) {
					assertTrue(node.getChildren().size() > 0);
				}
				assertTrue(verifyOrderedChildren(node));
			} catch (AssertionError e) {
				logger.error("Found structured object appender: " + key + ".");
				throw e;
			}
		}
	}

	private boolean verifyOrderedChildren(StructuredNode structuredNode) {
		final int LEAF = 0;
		final int TREE = 1;
		int currentType = (structuredNode.getChildren().size() > 0) ? TREE : LEAF;
		logger.debug((currentType == LEAF) ? "leaf" : "tree");
		if(currentType == LEAF) {
			return true;
		} else {
			int numDifTypeSwitches = 0;
			int previousType = (structuredNode.getChildren().size() > 0) ? TREE : LEAF;
			for(StructuredNode child : structuredNode.getChildren()) {
				int type = (child.getChildren().size() > 0) ? TREE : LEAF;
				if(verifyOrderedChildren(child)) {
					if(type != previousType) {
						numDifTypeSwitches++;
						if(numDifTypeSwitches > 1) {
							logger.info("Children of " + child.getField() + " were not sorted.");
							return false;
						}
					} 
					previousType = type;
				} else {
					return false;
				}
			}
			return true;
		}
	}
}
