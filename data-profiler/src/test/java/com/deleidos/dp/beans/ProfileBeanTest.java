package com.deleidos.dp.beans;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.accumulator.StringProfileAccumulator;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ProfileBeanTest {
	private Logger logger = Logger.getLogger(ProfileBeanTest.class);

	@Test
	public void testOutputProfileNumber() throws JsonProcessingException {
		NumberProfileAccumulator nma = new NumberProfileAccumulator("testField", 15);
		nma.accumulate(15);
		nma.accumulate(20);
		nma.accumulate(11);
		nma.accumulate(80);
		Profile profile = nma.getState();

		String s = SerializationUtility.serialize(profile);
		logger.debug(s);
		assertTrue(s != null);
		logger.info("Number profile serialized.");

	}

	@Test
	public void testOutputProfileStrings() throws JsonProcessingException {
		StringProfileAccumulator sma = new StringProfileAccumulator("test-field", "hey");
		sma.accumulate("hey");
		sma.accumulate("hi");
		sma.accumulate("hello");
		sma.accumulate("what's up");
		Profile profile = sma.getState();
		String s = null;

		s = SerializationUtility.serialize(profile);
		logger.debug(s);


		assertTrue(s != null);
		logger.info("String profile serialized.");
	}
}
