package com.deleidos.dp.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.deleidos.dp.calculations.MetricsCalculationsFacade;

public class CalculationsFacadeTest {
	private boolean exponentsReady = true;

	@Test
	public void testHyphen() {
		assertFalse(MetricsCalculationsFacade.isPossiblyNumeric("4-5"));
	}
	
	@Test
	public void testExponent() {
		boolean exponent = MetricsCalculationsFacade.isPossiblyNumeric("4E5");
		assertTrue(exponentsReady==exponent);
	}
}
