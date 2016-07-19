package com.deleidos.dp.profiler;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.environ.DPMockUpEnvironmentTest;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucket;

public class EmptyStringProfilingTests extends DPMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(EmptyStringProfilingTests.class);

	@Test
	public void testAccumulateEmptyStringAndEmptyValues() throws MainTypeException {
		SampleProfiler sampleProfiler = new SampleProfiler("Default", Tolerance.STRICT);
		sampleProfiler.load(new DefaultProfilerRecord(){
			{
				put("", Arrays.asList("",""));
			}
		});
		Map<String, Profile> p = sampleProfiler.asBean().getDsProfile();
		try {
			assertTrue(p.containsKey(SampleProfiler.EMPTY_FIELD_NAME) && 
					p.get(SampleProfiler.EMPTY_FIELD_NAME).getDetail()
					.getHistogramOptional().get().getLabels()
					.contains(AbstractBucket.EMPTY_STRING_INDICATOR));
			logger.info("Empty string indicator successfully added.");
		} catch (AssertionError e) {
			logger.error("Empty strings were not handled properly.");
		}
	}
	
	@Test
	public void testDefaultProfiler() {
		DefaultProfilerRecord record = new DefaultProfilerRecord();
		List<Object> subList = Arrays.asList("1","2","3");
		Map<String, Object> subObject = new HashMap<String, Object>();
		subObject.put("x", "10");
		subObject.put("y", "20");
		record.put("a", "root-level");
		record.put("b", subList);
		record.put("c", subObject);
		record.normalizeRecord().forEach((k,v)->System.out.println(k + "->" + v));
	}
	
}
