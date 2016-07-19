package com.deleidos.dmf.parser;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.Parser;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.deleidos.dmf.analyzer.TikaAnalyzer;
import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;
import com.deleidos.dmf.framework.AbstractAnalyticsParser;
import com.deleidos.dmf.framework.AnalyticsDefaultDetector;
import com.deleidos.dmf.framework.AnalyticsDefaultParser;
import com.deleidos.dmf.framework.AnalyticsEmbeddedDocumentExtractor;
import com.deleidos.dmf.framework.TikaSampleAnalyzerParameters;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.loader.ResourceLoader;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2TestDatabase;
import com.deleidos.dmf.exception.AnalyzerException;

public abstract class ParserTest extends ResourceLoader {
	private static final Logger logger = Logger.getLogger(ParserTest.class);
	protected AbstractAnalyticsParser parser;
	protected DataSample dataSample;
	private boolean debugOutput = false;

	public abstract void setup() throws Exception;

	public void setParser(AbstractAnalyticsParser parser) {
		this.parser = parser;
	}

	@Test
	public void testNonEmptyDataSampleBean() throws IOException, SAXException, TikaException, AnalyzerException {
		boolean outputTestIntro = true;
		int i = 0;
		for(DefinedTestResource ds : streamSources) {

			//try {
			//SampleProfiler sampleProfiler = new SampleProfiler("transportation", Tolerance.STRICT);
			//File file = new File(ds.getFilePath());

			/*new TikaSampleProfilableParameters(sampleProfiler, progressUpdater, uploadDir,
					"test-guid", ds.getStream(), new AnalyticsContentHandler(), new Metadata());
			params.setProgress(new SchemaWizardFractionForProgressBar(new File(ds.getFilePath()).getName(), 0, 1));
			params.setNumSamplesUploading(1);
			params.setSampleFilePath(ds.getFilePath());
			params.setSampleNumber(0);
			params.setStream(ds.getStream());
			params.setSessionId("parser-test");
			params.set(File.class, new File(ds.getFilePath()));*/
			if(!ds.isParserTestReady()) {
				continue;
			}			

			TikaSampleAnalyzerParameters params = TikaAnalyzer.generateSampleParameters(ds.getFilePath(), "transportation", Tolerance.STRICT.toString(), "test-session-id", 0, 1);
			if(new File(ds.getFilePath()).exists()) {
				params.set(File.class, new File(ds.getFilePath()));
			}

			AnalyticsDefaultDetector detector =  new AnalyticsDefaultDetector();
			params.set(Detector.class, detector);
			params.set(Parser.class, new AnalyticsDefaultParser(detector, params));
			params.set(EmbeddedDocumentExtractor.class, new AnalyticsEmbeddedDocumentExtractor(params));
			InputStream is = ds.getStream();
			params.setStream(is);
			params.setHandler(new AnalyticsProgressTrackingContentHandler());
			params.setMetadata(new Metadata());

			Set<MediaType> supportedTypes = parser.getSupportedTypes(params);

			if(!supportedTypes.contains(MediaType.parse(ds.getExpectedType()))) {
				continue;
			} else {
				if(outputTestIntro) {
					logger.info("Testing for proper diagnosis of resources defined as " + ds.getExpectedType() + " files.");
					outputTestIntro = false;
				}

				//sampleProfiler.setSource(ds.getFilePath());
				//Metadata metadata = new Metadata();
				//metadata.set(SampleProfiler.SOURCE_NAME, ds.getFilePath());
				//SampleProfilerHandler sampleHandler = new SampleProfilerHandler(metadata);

				i++;
				parser.runSampleAnalysis(params);
				is.close();

				dataSample = params.getProfilerBean();

				if(dataSample == null) {
					logger.error("\tData sample returned .");
					assertTrue(false);
				} else {
					JSONObject json = new JSONObject(SerializationUtility.serialize(dataSample));
					if(debugOutput) {
						logger.info(json);
					}
					int profileSize = json.getJSONObject("dsProfile").keySet().size();
					boolean assertNonEmptyProfile = (profileSize > 0) ? true : false; 
					if(assertNonEmptyProfile) {
						logger.info("\tParsed " + profileSize + " keys out of " + dataSample.getDsFileName() + ".");
					} else {
						logger.error("\tEmpty profile after parsing.");
					}
					assertTrue(assertNonEmptyProfile);
				}
			}
		}
		if(debugOutput) {
			logger.info("Parsed "+ i +" samples.");
		}
	}

	public boolean isDebugOutput() {
		return debugOutput;
	}

	public void setDebugOutput(boolean debugOutput) {
		this.debugOutput = debugOutput;
	}
}
