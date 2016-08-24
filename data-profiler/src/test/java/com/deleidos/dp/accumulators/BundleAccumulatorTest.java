package com.deleidos.dp.accumulators;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.interpretation.builtin.BuiltinDomain;
import com.deleidos.dp.profiler.DefaultProfilerRecord;


public class BundleAccumulatorTest {
	private Logger logger = Logger.getLogger(BundleAccumulatorTest.class);
	
	@Test
	public void testDetemineNumberWithDecimalAndInteger() throws MainTypeException {
		Profile p = BundleProfileAccumulator.generateProfile("test", Arrays.asList("1.1", "80"));
		assertTrue(p.getDetail().getDetailType().equals(DetailType.DECIMAL.toString()));
	}
	
	@Test
	public void testCommaAccumulate() {
		try {
			BundleProfileAccumulator.generateProfile("test", Arrays.asList(",", ",", ","));
			logger.info("Exception not thrown while accumulating commas.");
		} catch (Exception e) {
			logger.error("Expected exception was thrown for pseduo empty data.");
			assertTrue(false);
		}
	}
	
	@Test
	public void testPseudoEmptyExampleValues() {
		try {
			Profile p = BundleProfileAccumulator.generateProfile("test", Arrays.asList("", "", null));
			logger.error("Exception not thrown with empty example data.");
			logger.info(p.getMainType());
			assertTrue(p.getExampleValues().contains(""));
		} catch (MainTypeException e) {
			logger.error("Main type exception was unexpectedly thrown for pseduo empty data.");
			assertTrue(false);
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
	public void testProperAccumulationOfNull() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.MODERATE);
		int numNums = 974;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		bundleAccumulator.accumulate(null);
		assertTrue(bundleAccumulator.getBestGuessProfile(numNums).get().getMainType().equals(MainType.NUMBER.toString()));
	}
	
	@Test
	public void testRelaxedToleranceShouldBeString() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.MODERATE);
		int numNums = 974;
		int numStrings = 26;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test relaxed with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile(numNums).get().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile(numNums+numStrings).get().getMainType().equals(MainType.STRING.toString()));
	}
	
	
	@Test
	public void testRelaxedToleranceShouldBeNumber() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.RELAXED);
		int numNums = 950;
		int numStrings = 50;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test relaxed with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType().equals(MainType.NUMBER.toString()));
	}
	
	@Test
	public void testModerateToleranceShouldBeNumber() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.MODERATE);
		int numNums = 975;
		int numStrings = 25;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test moderate with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType().equals(MainType.NUMBER.toString()));
	}
	
	@Test
	public void testModerateToleranceShouldBeString() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.RELAXED);
		int numNums = 949;
		int numStrings = 51;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test moderate with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType().equals(MainType.STRING.toString()));
	}
	
	@Test
	public void testStrictTolerance() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.STRICT);
		int numNums = 1000;
		int numStrings = 1;
		for(int i = 0; i < numNums; i++) {
			bundleAccumulator.accumulate(Math.random());
		}
		for(int i = 0; i < numStrings; i++) {
			bundleAccumulator.accumulate("hello");
		}
		logger.info("Test strict with "+numStrings+" strings and "+numNums+" numbers: " + bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType());
		assertTrue(bundleAccumulator.getBestGuessProfile(numStrings+numNums).get().getMainType().equals(MainType.STRING.toString()));
	}
	
	@Test
	public void testAccumulateDecimal() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.STRICT);
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
		logger.info("Test decimal type: " + bundleAccumulator.getBestGuessProfile(10).get().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile(10).get().getDetail().getDetailType().equals(DetailType.DECIMAL.toString()));
	}
	
	@Test
	public void testAccumulatePhrase() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.STRICT);
		bundleAccumulator.accumulate("how are you");
		bundleAccumulator.accumulate("what's up");
		bundleAccumulator.accumulate("how's it goin");
		bundleAccumulator.accumulate("nice to meet you");
		logger.info("Test phrase type: " + bundleAccumulator.getBestGuessProfile(4).get().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile(4).get().getDetail().getDetailType().equals(DetailType.PHRASE.toString()));
	}
	
	@Test
	public void testAccumulateTerm() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.STRICT);
		bundleAccumulator.accumulate("hey");
		bundleAccumulator.accumulate("hi");
		bundleAccumulator.accumulate("yo");
		bundleAccumulator.accumulate("hello");
		bundleAccumulator.accumulate("greetings");
		logger.info("Test term type: " + bundleAccumulator.getBestGuessProfile(5).get().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile(5).get().getDetail().getDetailType().equals(DetailType.TERM.toString()));
	}
	
	@Test
	public void testAccumulateInteger() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.STRICT);
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
		logger.info("Test accumulate integer type: " + bundleAccumulator.getBestGuessProfile(13).get().getDetail().getDetailType());
		assertTrue(bundleAccumulator.getBestGuessProfile(13).get().getDetail().getDetailType().equals(DetailType.INTEGER.toString()));
	}
	
	@Test
	public void testDistinctValuesToExampleValueList() throws MainTypeException {
		BundleProfileAccumulator bundleAccumulator = new BundleProfileAccumulator("test", Tolerance.STRICT);
		for(int i = 0; i < 100; i++) {
			bundleAccumulator.accumulate(i);
		}
		Profile p = bundleAccumulator.getBestGuessProfile(100).get();
		assertTrue(p.getExampleValues() != null && p.getExampleValues().size() > 0);
	}
}
