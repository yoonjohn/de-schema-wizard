package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.interpretation.builtin.BuiltinDomain;
import com.deleidos.dp.profiler.SampleProfiler;


public class BundleAccumulatorTest {
	private Logger logger = Logger.getLogger(BundleAccumulatorTest.class);
	
	@Test
	public void testDetemineNumberWithDecimalAndInteger() throws MainTypeException {
		Profile p = BundleProfileAccumulator.generateProfile("test", Arrays.asList("1.1", "80"));
		assertTrue(p.getDetail().getDetailType().equals(DetailType.DECIMAL.toString()));
	}
	
	@Test
	public void testPseudoEmptyExampleValues() {
		try {
			BundleProfileAccumulator.generateProfile("test", Arrays.asList("", "", null));
			logger.error("Exception not thrown with empty example data.");
			assertTrue(false);
		} catch (MainTypeException e) {
			logger.info("Expected exception was thrown for pseduo empty data.");
			assertTrue(true);
		}
	}
	
	@Test
	public void testEmptyExampleValues() {
		try {
			BundleProfileAccumulator.generateProfile("test", new ArrayList<Object>());
			logger.error("Exception not thrown with empty example data.");
			assertTrue(false);
		} catch (MainTypeException e) {
			logger.info("Expected exception was thrown.");
			assertTrue(true);
		}
	}
	
	@Test
	public void accumulateList() throws MainTypeException {
		List<Object> test = Arrays.asList("5", "10", "15", "20");
		Profile profile = BundleProfileAccumulator.generateProfile("test", test);
		try {
			assertTrue(profile.getMainType().equals(MainType.NUMBER.toString()));
			assertTrue(profile.getExampleValues().size() == 4);
			logger.info("List profile accumulated successfully.");
		} catch (AssertionError e) {
			logger.error("Profile did not accumulate as expected.");
			throw e;
		}
	}
	
	@Test
	public void testProperAccumulationOfNull() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.MODERATE);
		int numNums = 974;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		bundleAccumulator.accumulate(null);
		assertTrue(bundleAccumulator.getBestGuessProfile().getMainType().equals(MainType.NUMBER.toString()));
	}
	
	@Test
	public void testRelaxedToleranceShouldBeString() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.MODERATE);
		int numNums = 974;
		int numStrings = 26;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test relaxed with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getMainType().equals(MainType.STRING.toString()));
	}
	
	
	@Test
	public void testRelaxedToleranceShouldBeNumber() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.RELAXED);
		int numNums = 950;
		int numStrings = 50;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test relaxed with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getMainType().equals(MainType.NUMBER.toString()));
	}
	
	@Test
	public void testModerateToleranceShouldBeNumber() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.MODERATE);
		int numNums = 975;
		int numStrings = 25;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test moderate with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getMainType().equals(MainType.NUMBER.toString()));
	}
	
	@Test
	public void testModerateToleranceShouldBeString() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.RELAXED);
		int numNums = 949;
		int numStrings = 51;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test moderate with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getMainType().equals(MainType.STRING.toString()));
	}
	
	@Test
	public void testStrictTolerance() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		int numNums = 1000;
		int numStrings = 1;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test strict with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getMainType().equals(MainType.STRING.toString()));
	}
	
	@Test
	public void testAccumulateDecimal() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		bundleAccumulator.accumulate("0.00");
		bundleAccumulator.accumulate("0.03");
		bundleAccumulator.accumulate("0.04");
		bundleAccumulator.accumulate("0.03");
		bundleAccumulator.accumulate("0.00");
		bundleAccumulator.accumulate("1.00");
		bundleAccumulator.accumulate("1.04");
		bundleAccumulator.accumulate("1.03");
		bundleAccumulator.accumulate("1.02");
		bundleAccumulator.accumulate("1.01");
		logger.info("Test decimal type: " + bundleAccumulator.getBestGuessProfile().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getDetail().getDetailType().equals(DetailType.DECIMAL.toString()));
	}
	
	@Test
	public void testAccumulatePhrase() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		bundleAccumulator.accumulate("how are you");
		bundleAccumulator.accumulate("what's up");
		bundleAccumulator.accumulate("how's it goin");
		bundleAccumulator.accumulate("nice to meet you");
		logger.info("Test phrase type: " + bundleAccumulator.getBestGuessProfile().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getDetail().getDetailType().equals(DetailType.PHRASE.toString()));
	}
	
	@Test
	public void testAccumulateTerm() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		bundleAccumulator.accumulate("hey");
		bundleAccumulator.accumulate("hi");
		bundleAccumulator.accumulate("yo");
		bundleAccumulator.accumulate("hello");
		bundleAccumulator.accumulate("greetings");
		logger.info("Test term type: " + bundleAccumulator.getBestGuessProfile().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getDetail().getDetailType().equals(DetailType.TERM.toString()));
	}
	
	@Test
	public void testAccumulateInteger() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("1");
		bundleAccumulator.accumulate("0");
		bundleAccumulator.accumulate("0");
		bundleAccumulator.accumulate("0");
		bundleAccumulator.accumulate("0");
		bundleAccumulator.accumulate("0");
		bundleAccumulator.accumulate("0");
		logger.info("Test accumulate integer type: " + bundleAccumulator.getBestGuessProfile().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile().getDetail().getDetailType().equals(DetailType.INTEGER.toString()));
	}
	
	@Test
	public void testDistinctValuesToExampleValueList() {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", BuiltinDomain.name, Tolerance.STRICT);
		for(int i = 0; i < 100; i++) {
			bundleAccumulator.accumulate(i);
		}
		Profile p = bundleAccumulator.getBestGuessProfile();
		assertTrue(p.getExampleValues() != null && p.getExampleValues().size() > 0);
	}
}
