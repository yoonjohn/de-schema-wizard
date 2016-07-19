package com.deleidos.dp.domain;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.interpretation.builtin.AbstractBuiltinInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinDomain;
import com.deleidos.dp.interpretation.builtin.BuiltinLatitudeInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinLongitudeInterpretation;
import com.deleidos.dp.interpretation.builtin.BuiltinUnknownInterpretation;

public class TransportationDomainTest {
	Logger logger = Logger.getLogger(TransportationDomainTest.class);
	
	@Test
	public void constructorHasLatAndLongTest() {
		BuiltinDomain domain = new BuiltinDomain();
		Map<String, AbstractBuiltinInterpretation> iMap = domain.getInterpretationMap();
		assertTrue(iMap.containsKey(new BuiltinLatitudeInterpretation().getInterpretationName().toLowerCase()));
		assertTrue(iMap.containsKey(new BuiltinLongitudeInterpretation().getInterpretationName().toLowerCase()));
		logger.info("Built in domain has lat and lon.");
	}
	
}
