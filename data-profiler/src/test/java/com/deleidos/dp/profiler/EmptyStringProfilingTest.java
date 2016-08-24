package com.deleidos.dp.profiler;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.environ.DPMockUpEnvironmentTest;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucket;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class EmptyStringProfilingTest extends DPMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(EmptyStringProfilingTest.class);
	
	@Test
	public void a() {
		Map<String, String> m = new HashMap<String, String>();
		m.put("a", "hello");
		m.put("b", null);
		Map<String, String> n = new HashMap<String, String>();
		n.put("b", "test");
		n.forEach((k,v)->{
			if(m.containsKey(k)) m.remove(k);
		});
		m.forEach((k,v)->System.out.println(k + "->" + v));
		assertTrue(!m.containsKey("b"));
	}

	@Test
	public void testAccumulateEmptyStringAndEmptyValues() throws MainTypeException {
		SampleProfiler sampleProfiler = new SampleProfiler(Tolerance.STRICT);
		sampleProfiler.load(new DefaultProfilerRecord() {
			{
				put("", Arrays.asList("",""));
			}
		});
		DataSample ds = sampleProfiler.asBean();
		SampleSecondPassProfiler secondPass = new SampleSecondPassProfiler(ds);
		secondPass.load(new DefaultProfilerRecord() {
			{
				put("", Arrays.asList("",""));
			}
		});
		Map<String, Profile> p =	secondPass.asBean().getDsProfile();
		try {
			Optional<Histogram> h = p.get(DefaultProfilerRecord.EMPTY_FIELD_NAME_INDICATOR).getDetail().getHistogramOptional();
			assertTrue(h.isPresent());
			assertTrue(p.containsKey(DefaultProfilerRecord.EMPTY_FIELD_NAME_INDICATOR));
			logger.info(SerializationUtility.serialize(h.get()));
			assertTrue(h.get().getLabels().contains(AbstractBucket.EMPTY_STRING_INDICATOR));
			logger.info("Empty string indicator successfully added.");
		} catch (AssertionError e) {
			logger.error("Empty strings were not handled properly.");
			throw e;
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
