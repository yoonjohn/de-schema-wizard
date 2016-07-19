package com.deleidos.dp.classification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.deleidos.dp.interpretation.builtin.BuiltinDomain;
import com.deleidos.dp.interpretation.builtin.BuiltinLatitudeInterpretation;

public class LatitudeInterpretationTest {
	private Logger logger = Logger.getLogger(LatitudeInterpretationTest.class);
	private final String s1 = "89 34 42";
	private final String s2 = "-89 34 42";
	private final String s3 = "88* 34 42";
	private final String s4 = "+88 34 42";
	private final String s5 = "45* 12' 33\"";
	private final String s6 = "45 12' 22\"";
	private final String s7 = "90:00:00.0N";
	private final String s8 = "64.8";
	private final String s9 = "-33.3107";
	private final String sf1 = "-100 38 38";
	private final String sf2 = "123 32 34";
	private final String sf3 = "40 1000 8";
	private final String sf4 = "100* 38 38";
	private final String sf5 = "99:00:00.0N";
	private String[] shouldPassStrings;
	private String[] shouldFailStrings;
	private final float n1 = 64.8f;
	private final float n2 = -64.8f;
	private final float nf1 = 100f;
	private final float nf2 = -111f;
	private float[] shouldPassNumbers;
	private float[] shouldFailNumbers;
	
	@Before
	public void initTestStrings() {
		shouldPassStrings = new String[9];
		shouldPassStrings[0] = s1;
		shouldPassStrings[1] = s2;
		shouldPassStrings[2] = s3;
		shouldPassStrings[3] = s4;
		shouldPassStrings[4] = s5;
		shouldPassStrings[5] = s6;
		shouldPassStrings[6] = s7;
		shouldPassStrings[7] = s8;
		shouldPassStrings[8] = s9;
		shouldFailStrings = new String[5];
		shouldFailStrings[0] = sf1;
		shouldFailStrings[1] = sf2;
		shouldFailStrings[2] = sf3;
		shouldFailStrings[3] = sf4;
		shouldFailStrings[4] = sf5;
		shouldPassNumbers = new float[2];
		shouldPassNumbers[0] = n1;
		shouldPassNumbers[1] = n2;
		shouldFailNumbers = new float[2];
		shouldFailNumbers[0] = nf1;
		shouldFailNumbers[1] = nf2;
	}
	
	@Test
	public void testFitsNumberMetrics() {
		BuiltinLatitudeInterpretation latClass = new BuiltinLatitudeInterpretation();
		logger.info("Should pass");
		for(int i = 0; i < shouldPassNumbers.length; i++) {
			boolean fits = latClass.fitsNumberMetrics(shouldPassNumbers[i]);
			logger.info(shouldPassNumbers[i] + ": " + fits);
			assertTrue(fits);
		}
		logger.info("Should fail");
		for(int i = 0; i < shouldFailNumbers.length; i++) {
			boolean fits = latClass.fitsNumberMetrics(shouldFailNumbers[i]);
			logger.info(shouldFailNumbers[i] + ": " + fits);
			assertFalse(fits);
		}
	}
	
	@Test
	public void testFitsStringMetrics() {
		BuiltinLatitudeInterpretation latClass = new BuiltinLatitudeInterpretation();
		logger.info("Should pass");
		for(int i = 0; i < shouldPassStrings.length; i++) {
			boolean fits = latClass.fitsStringMetrics(shouldPassStrings[i]);
			logger.info(shouldPassStrings[i] + ": " + fits);
			assertTrue(fits);
		}
		logger.info("Should fail");
		for(int i = 0; i < shouldFailStrings.length; i++) {
			boolean fits = latClass.fitsStringMetrics(shouldFailStrings[i]);
			logger.info(shouldFailStrings[i] + ": " + fits);
			assertFalse(fits);
		}
	}
	
	@Test
	public void testConvertMinutesDegreesSecondsToDecimal() {
		double decimal = BuiltinDomain.degreesMinutesSecondsToDecimal(122, 45, 45);
		assertTrue(Double.doubleToLongBits(decimal) == Double.doubleToLongBits(122.7625));
	}
}
