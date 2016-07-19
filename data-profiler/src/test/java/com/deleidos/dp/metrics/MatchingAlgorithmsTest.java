package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.accumulator.StringProfileAccumulator;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;

public class MatchingAlgorithmsTest {
	private Logger logger = Logger.getLogger(MatchingAlgorithmsTest.class);
	private String s1 = "Volume";
	private String s2 = "Vol";
	private String s3 = "lat";
	private String s4 = "lng";
	private String s5 = "waypoints.lat";
	private String s6 = "waypoints.lng";
	
	@Test
	public void jaroWinklerTest() {
		double jwMatch = MetricsCalculationsFacade.jaroWinklerComparison(s1, s2);
		logger.info(s1 + " and " + s2 + " matched with " + jwMatch + " confidence.");
		assertTrue(jwMatch > .80);
		jwMatch = MetricsCalculationsFacade.jaroWinklerComparison(s3, s4);
		logger.info(s3 + " and " + s4 + " matched with " + jwMatch + " confidence.");
		assertTrue(jwMatch > .50);
		jwMatch = MetricsCalculationsFacade.jaroWinklerComparison(s5, s6);
		logger.info(s5 + " and " + s6 + " matched with " + jwMatch + " confidence.");
		assertTrue(jwMatch > .50);
	}
	
	@Test
	public void matchingTest() {
		StringProfileAccumulator sma = new StringProfileAccumulator("Region", "MD");
		sma.accumulate("MD");
		sma.accumulate("MD");
		sma.accumulate("MD");
		sma.accumulate("NJ");
		sma.accumulate("NJ");
		sma.accumulate("NJ");
		sma.accumulate("NJ");
		sma.finish();
		
		StringProfileAccumulator sma2 = new StringProfileAccumulator("Rgn", "NJ");
		sma2.accumulate("NJ");
		sma2.accumulate("NJ");
		sma2.accumulate("NJ");

		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.finish();
		double similarity = MetricsCalculationsFacade.match(sma.getFieldName(), sma.getState(), sma2.getFieldName(), sma2.getState());
		logger.info("Similarity for desired-to-be-matching string fields " + similarity);
		assertTrue(similarity > .80);
		
		double newSimilarity = MetricsCalculationsFacade.match(sma.getFieldName(), sma.getState(), sma2.getFieldName(), sma2.getState());
		logger.info("New similarity is " +newSimilarity);
	}
	
	@Test
	public void noMatchingTest() {
		StringProfileAccumulator sma = new StringProfileAccumulator("Name", "John");
		sma.accumulate("Greg");
		sma.accumulate("Aaron");
		sma.accumulate("Jamal");
		sma.accumulate("Matt");
		sma.accumulate("Jim");
		sma.accumulate("Dave");
		sma.accumulate("Kevin");
		sma.finish();
		
		StringProfileAccumulator sma2 = new StringProfileAccumulator("Rgn", "NJ");
		sma2.accumulate("NJ");
		sma2.accumulate("NJ");
		sma2.accumulate("NJ");

		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.finish();
		double similarity = MetricsCalculationsFacade.match(sma.getFieldName(), sma.getState(), sma2.getFieldName(), sma2.getState());
		logger.info("Similarity for desired-to-be-not-matching string fields " + similarity);
		assertTrue(similarity < .80);
		
		double newSimilarity = MetricsCalculationsFacade.match(sma.getFieldName(), sma.getState(), sma2.getFieldName(), sma2.getState());
		logger.info("New similarity is " +newSimilarity);
	}
	
	@Test
	public void numberMetricsMatchingTest() {
		
		Profile numberProfile = new Profile();
		NumberDetail nm = new NumberDetail();
		String profile1Name = "Cls";
		numberProfile.setMainType(MainType.NUMBER.toString());
		nm.setDetailType(DetailType.DECIMAL.toString());
		nm.setMin(BigDecimal.valueOf(2059.19));
		nm.setMax(BigDecimal.valueOf(2096.88));
		nm.setAverage(BigDecimal.valueOf(2080.972));
		nm.setStdDev(10.70483);
		nm.setNumDistinctValues("5");
		numberProfile.setDetail(nm);
		
		String profile2Name = "Close";
		Profile numberProfile2 = new Profile();
		NumberDetail nm2 = new NumberDetail();
		numberProfile2.setMainType(MainType.NUMBER.toString());
		nm2.setDetailType(DetailType.DECIMAL.toString());
		nm2.setMin(BigDecimal.valueOf(2075.37));
		nm2.setMax(BigDecimal.valueOf(2116.4));
		nm2.setAverage(BigDecimal.valueOf(2099.88));
		nm2.setStdDev(7.639143);
		nm2.setNumDistinctValues("5");
		numberProfile2.setDetail(nm2);
		
		double similarity = MetricsCalculationsFacade.match(profile1Name, numberProfile, profile2Name, numberProfile2);
		logger.info("Similarity for desired-to-be-matching number fields " + similarity);
		assertTrue(Math.abs(similarity - .97333) < .01);
		
		double newSimilarity = MetricsCalculationsFacade.match(profile1Name, numberProfile, profile2Name, numberProfile2);
		logger.info("New similarity is " +newSimilarity);
	}
	
	@Test
	public void latLngTest() {
		NumberProfileAccumulator nma = new NumberProfileAccumulator("lat", 16.0);
		nma.accumulate(20);
		nma.finish();
		
		NumberProfileAccumulator nma2 = new NumberProfileAccumulator("latitude", 15.0);
		nma2.accumulate(22);
		nma2.finish();
		
		double similarity = MetricsCalculationsFacade.match(nma.getFieldName(), nma.getState(), nma2.getFieldName(), nma2.getState());
		logger.info("Similarity for desired-to-be-matching number fields " + similarity);
		assertTrue(similarity > .80);
	}
	
	@Test
	public void dtgFieldMatchingTest() {
		Profile dtg1 = new Profile();
		dtg1.setMainType(MainType.STRING.toString());
		StringDetail sd = new StringDetail();
		sd.setDetailType(DetailType.PHRASE.toString());
		sd.setAverageLength(14.12);
		sd.setMinLength(13);
		sd.setMaxLength(15);
		sd.setStdDevLength(.74);
		sd.setNumDistinctValues("495");
		dtg1.setDetail(sd);
		
		Profile dtg2 = new Profile();
		dtg2.setMainType(MainType.STRING.toString());
		StringDetail sd2 = new StringDetail();
		sd2.setDetailType(DetailType.PHRASE.toString());
		sd2.setAverageLength(14.32);
		sd2.setMinLength(13);
		sd2.setMaxLength(15);
		sd2.setStdDevLength(.64);
		sd2.setNumDistinctValues("498");
		dtg2.setDetail(sd2);
		
		double d = MetricsCalculationsFacade.match("DTG", dtg1, "DTG", dtg2);
		logger.info("DTG similarity calculated to be " + d +".");
	}
}
