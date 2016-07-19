package com.deleidos.dmf.detector;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;
import com.deleidos.dmf.framework.AnalyticsDetectorWrapper;
import com.deleidos.dmf.framework.DMFMockUpEnvironmentTest;
import com.deleidos.dmf.loader.ResourceLoader;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2TestDatabase;
import com.google.common.collect.Iterables;

/**
 * Superclass for all detector test classes.  Extend this class to follow convention and reuse methods with detector tests.  Subclasses 
 * should inherit most of their functionality from this class by implementing the setup() method;
 * @author leegc
 *
 */
public abstract class DetectorTest extends DMFMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(DetectorTest.class);
	protected AnalyticsDetectorWrapper detector;

	/**
	 * Initialize all testing components.  Subclasses should call the setDetector() method within this method.
	 */
	public abstract void setup();

	/**
	 * Set the detector to test.
	 * @param detector The instance of the detector that will be tested
	 */
	public void setDetector(AnalyticsDetectorWrapper detector) {
		this.detector = detector;
	}


	/**
	 * Test detection on all streams defined in the TestLoader.  This method tests for proper diagnosis of a file whose definition matches the 
	 * detector.getDetectableTypes() singleton.
	 * @throws IOException 
	 */
	@Test
	public void validTest() throws IOException {
		testProperDiagnosisOfDefinedStreams(detector);
	}

	/**
	 * Test detection on all streams in the TestLoader.  This method tests tests for an 'improper diagnosis' of type based on files whose 
	 * definition does not match the detector.getDetectableTypes() singleton.
	 * @throws IOException 
	 */
	@Test
	public void invalidTest() throws IOException {
		testMisdiagnosisOfDefinedStreams(detector);
	}

	private void testProperDiagnosisOfDefinedStreams(AnalyticsDetectorWrapper detector) throws IOException {
		if(detector.getDetectableTypes().size() > 1) {
			logger.error("More than one detectable type found.  Testing suite not configured for this functionality.");
		} else {
			String detectableType = Iterables.getOnlyElement(detector.getDetectableTypes()).toString();
			logger.info("Testing for proper diagnosis of resources defined as " + detectableType + " files.");
			for(DefinedTestResource ds : streamSources) {
				InputStream is = ds.getStream();
				Metadata metadata = new Metadata();
				Object detected = detector.detect(is, metadata);
				is.close();

				if(!ds.getExpectedType().equals(detectableType)) {
					continue;
				} else {
					if(detected == null) {
						logger.error("\tDid not detect " + detectableType + " file: " + ds.getFilePath() + " as "+detectableType+".");
						assertTrue(false);
					} else {
						boolean assertEquals = detected.toString().equals(detectableType);
						if(assertEquals) {
							logger.info("\tSuccessfully detected " + detectableType + " for " + ds.getFilePath());
						} else {
							logger.error("Incorrectly detected " + detected.toString() + " when expecting " + detectableType + ".");
						}

						assertTrue(assertEquals);
					}
				}
			}
		}
	}

	private void testMisdiagnosisOfDefinedStreams(AnalyticsDetectorWrapper detector) throws IOException {
		if(detector.getDetectableTypes().size() > 1) {
			logger.error("More than one detectable type found.  Testing suite not configured for this functionality.");
		} else {
			String detectableType = Iterables.getOnlyElement(detector.getDetectableTypes()).toString();
			logger.info("Testing for improper diagnosis of resources not defined as " + detectableType + " files.");
			for(DefinedTestResource ds : streamSources) {

				InputStream is = ds.getStream();
				Metadata metadata = new Metadata();
				Object detected = detector.detect(is, metadata);
				is.close();

				if(ds.getExpectedType().equals(detectableType)) {
					continue;
				} else {
					if(detected == null) {
						//logger.info("\tFile " + ds.getFilePath() + " defined as " + ds.getExpectedType() + " correctly returned null from " + detector.getClass() + ".");
					} else if(detected.toString().equals(detectableType)) {
						if(detected.toString().equals(ds.getExpectedType())) {
							logger.warn("\tDetector successfully detected the file, though it was not defined explicitly to detect these types.  Composite detector?");
						} else {
							logger.error("\tIncorrectly detected file " + ds.getFilePath() + " (expected as " + ds.getExpectedType() + ") as " + detected.toString() + " from " + detector.getClass() + ".");
							assertTrue(false);
						}
					} else {
						//logger.info("\tFile " + ds.getFilePath() + " defined as " + ds.getExpectedType() + " returned " + detected.toString() + " from " + detector.getClass() + ".");
					}
				}

			}
		}
	}
}
