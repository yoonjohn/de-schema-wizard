package com.deleidos.dmf.analyzer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;
import com.deleidos.dmf.framework.AnalyticsDefaultDetector;
import com.deleidos.dmf.framework.DMFMockUpEnvironmentTest;
import com.deleidos.dmf.framework.TikaSampleAnalyzerParameters;
import com.deleidos.dmf.loader.ResourceLoader;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2TestDatabase;

public class TikaAnalyzerTest extends DMFMockUpEnvironmentTest {
	private static final Logger logger = Logger.getLogger(TikaAnalyzerTest.class);
	
	@Test
	public void tikaWorkerDetectionTest() throws Exception {
		AnalyticsDefaultDetector detector = new AnalyticsDefaultDetector();
		int counter = 0;
		for(DefinedTestResource ds : streamSources) {
			if(!ds.isDetectorTestReady()) {
				continue;
			}
			InputStream is = null;
			is = ds.getStream();
			logger.info("Reading " + ds.getFilePath());
			Metadata metadata = new Metadata();
			String type = detector.detect(is, metadata).toString();
			is.close();
			String expected = ds.getExpectedType();
			
			boolean equivalenceAssertion;
			
			if((expected.equals("data"))) {
				equivalenceAssertion = true;
			} else if(expected.equals("unstructured")) {
				equivalenceAssertion = true;
			} else if(expected.equals(type)) {
				equivalenceAssertion = true;
				logger.info("Expected: " + expected + ", returned: " + type);
			} else {
				equivalenceAssertion = false;
				logger.error("Expected: " + expected + ", returned: " + type);
			}
			
			assertMetadataHasBodyContentIfItShould(ds, metadata);
			
			counter++;
			assertTrue(equivalenceAssertion);
		}
		logger.info("Detection was successful for " + counter + " files.");
	}

	//@Ignore
	//now handled by indivudal classes...not really necessary though it technically tests a different 
	// "unit"
	@Test
	public void tikaWorkerParseTest() throws Exception {		
		TikaAnalyzer analyzer = new TikaAnalyzer();
		for(DefinedTestResource ds : streamSources) {
			if(!ds.isParserTestReady()) {
				continue;
			}
			/*if(!ds.getFilePath().contains("simplexml")) {
				continue;
			}*/
			InputStream is = null;
			String f = ds.getFilePath();
			
			is = ds.getStream();
			File file = new File("./target", ds.getFilePath());
			FileUtils.copyInputStreamToFile(is, file);
			is.close();

			FileInputStream fis = new FileInputStream(file);
			logger.info("Reading " + f);

			long t1 = System.currentTimeMillis();
			TikaSampleAnalyzerParameters params = TikaAnalyzer.generateSampleParameters(file.getAbsolutePath(), 
					"transportation", Tolerance.STRICT.toString(), "test-guid", 0, 1);
			params.setDoReverseGeocode(false);
			params.setPersistInH2(false);
			
			DataSample dataSample = analyzer.runSampleAnalysis(params).getProfilerBean();
			if(dataSample == null) {
				logger.error("\tData sample returned .");
				assertTrue(false);
			} else {
				JSONObject json = new JSONObject(SerializationUtility.serialize(dataSample));
				int profileSize = json.getJSONObject("dsProfile").keySet().size();
				boolean assertNonEmptyProfile = (profileSize > 0) ? true : false; 
				if(assertNonEmptyProfile) {
					logger.info("\tParsed " + profileSize + " keys out of " + dataSample.getDsFileName() + ".");
				} else {
					logger.error("\tEmpty profile after parsing.");
				}
				assertTrue(assertNonEmptyProfile);
			}
			String sampleGuid = dataSample.getDsGuid();
			long t2 = System.currentTimeMillis();
			long finalTime = (t2-t1)/(1000);
			String f3 = f;
			File file2 = new File(f3);
			logger.info("File size: " + file2.length()/(1024));
			logger.info("Input processing time " + finalTime);
			fis.close();
			assertTrue(sampleGuid != null);

		}
	}
	

	public static void assertMetadataHasBodyContentIfItShould(DefinedTestResource ds, Metadata metadata) {
		if(ds.getExpectedBodyContentType() != null) {
			boolean assertion = (metadata.get(AnalyticsDefaultDetector.HAS_BODY_CONTENT) != null 
					&& metadata.get(AnalyticsDefaultDetector.HAS_BODY_CONTENT).equals(Boolean.TRUE.toString()));
			String output = ds.getFilePath() + " should have contained a body content tag ";
			String append = (assertion) ? "and it did." : "but it didn\'t.";
			output += append + " This doesn\\'t necessarily mean it worked.";
			if(assertion) {
				logger.info(output);
			} else {
				logger.error(output);
			}
			assertTrue(assertion);
		} 
	}
}
