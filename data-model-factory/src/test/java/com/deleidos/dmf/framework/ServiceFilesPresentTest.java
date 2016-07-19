package com.deleidos.dmf.framework;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;
import org.apache.tika.detect.Detector;
import org.apache.tika.parser.Parser;
import org.junit.Test;

public class ServiceFilesPresentTest {
	private static final Logger logger = Logger.getLogger(ServiceFilesPresentTest.class);

	@Test
	public void detectorServiceFilePresent() {
		ServiceLoader<Detector> serviceLoader = ServiceLoader.load(Detector.class);
		
		Iterator<Detector> i = serviceLoader.iterator();
		boolean hasAnalyticsPackage = false;
		List<AbstractMarkSupportedAnalyticsDetector> analyticsDetectors = new ArrayList<AbstractMarkSupportedAnalyticsDetector>();
		
		while(i.hasNext()) {
			Detector o = i.next();
			if(o instanceof AbstractMarkSupportedAnalyticsDetector) {
				hasAnalyticsPackage = true;
				analyticsDetectors.add((AbstractMarkSupportedAnalyticsDetector)o);
			}
		}
		
		if(!hasAnalyticsPackage) {
			logger.error("No analytics detectors found in detector service file.  Ensure it is included on the classpath.");
			assertTrue(false);
		} else {
			logger.info("Analytics detectors:");
			for(AbstractMarkSupportedAnalyticsDetector d : analyticsDetectors) {
				logger.info("\t" + d.getClass().getName());
			}
		}
	}
	
	@Test
	public void parserServiceFilePresent() {
		ServiceLoader<Parser> serviceLoader = ServiceLoader.load(Parser.class);
		Iterator<Parser> i = serviceLoader.iterator();
		boolean hasAnalyticsPackage = false;
		List<AbstractAnalyticsParser> analyticsParsers = new ArrayList<AbstractAnalyticsParser>();

		
		while(i.hasNext()) {
			Parser o = i.next();
			if(o instanceof AbstractAnalyticsParser) {
				hasAnalyticsPackage = true;
				analyticsParsers.add((AbstractAnalyticsParser)o);
			}
		}
		
		if(!hasAnalyticsPackage) {
			logger.error("No analytics parsers found in parser service file.  Ensure it is included on the classpath.");
			assertTrue(false);
		} else {
			logger.info("Analytics parsers:");
			for(AbstractAnalyticsParser p : analyticsParsers) {
				logger.info("\t" + p.getClass().getName());
			}
		}
		
	}
}
