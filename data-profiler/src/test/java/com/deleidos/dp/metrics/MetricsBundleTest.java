package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.interpretation.builtin.BuiltinDomain;

public class MetricsBundleTest {
	private Logger logger = Logger.getLogger(MetricsBundleTest.class);
	@Test
	public void testMetricsBundle() {
		BundleProfileAccumulator bundle = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		bundle.accumulate(6.7);
		bundle.finish();
		assertTrue(bundle.getBestGuessProfile().getMainType().equals(MainType.NUMBER.toString()));
	}
}
