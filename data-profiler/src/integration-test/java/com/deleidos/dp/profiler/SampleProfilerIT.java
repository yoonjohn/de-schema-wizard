package com.deleidos.dp.profiler;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.profiler.api.ProfilerRecord;

public class SampleProfilerIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(SampleProfilerIT.class);
	
	@Test
	public void testAccumulateAllNulls() throws DataAccessException {
		List<ProfilerRecord> records = new ArrayList<ProfilerRecord>();
		for(int i = 0; i < 10; i++) {
			DefaultProfilerRecord record = new DefaultProfilerRecord();
			record.put("num", i);
			record.put("null-test", null);
			records.add(record);
		}
		logger.warn("A MainTypeException should be thrown below.");
		DataSample sample = SampleProfiler.generateDataSampleFromProfilerRecords("Transportation", Tolerance.STRICT, records);
		assertTrue(!sample.getDsProfile().containsKey("null-test"));
	}
}
