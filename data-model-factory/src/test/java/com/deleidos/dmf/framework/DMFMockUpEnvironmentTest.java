package com.deleidos.dmf.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.junit.BeforeClass;

import com.deleidos.dmf.analyzer.Analyzer;
import com.deleidos.dmf.analyzer.TikaAnalyzer;
import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;
import com.deleidos.dmf.handler.AnalyticsProgressTrackingContentHandler;
import com.deleidos.dmf.loader.ResourceLoader;
import com.deleidos.dmf.progressbar.ProgressBarManager;
import com.deleidos.dmf.progressbar.SimpleProgressUpdater;
import com.deleidos.dmf.web.SchemaWizardSessionUtility;
import com.deleidos.dp.enums.Tolerance;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.dp.profiler.SampleProfiler;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;
import com.deleidos.hd.h2.H2TestDatabase;

/**
 * An abstract class for tests that should run with an embedded H2 instance and Built-in Interpretation Engine.
 * Extend this class and the Interpretation Engine and H2DataAccessObject singletons will be initialized for you.
 * 
 * @author leegc
 *
 */
public abstract class DMFMockUpEnvironmentTest extends ResourceLoader {
	private static final Logger logger = Logger.getLogger(DMFMockUpEnvironmentTest.class);
	private static boolean running = false;

	@BeforeClass
	public static void setupUnitTestingEnvironment() throws DataAccessException, ClassNotFoundException, SQLException, InterruptedException, IOException {
		if(!running) {
			InterpretationEngineFacade.setInstance(IEConfig.BUILTIN_CONFIG);
			logger.info("Setting up built-in Interpretation Engine.");
			H2TestDatabase h2Test = new H2TestDatabase();
			h2Test.startTestServer(h2Test.getConfig());
			H2DataAccessObject.setInstance(h2Test);
			SchemaWizardSessionUtility.getInstance(new TestingWebSocketUtility()
					
					/*new SchemaWizardSessionUtility() {
				{
					setPerformFakeUpdates(false);
				}
				@Override
				public Boolean isCancelled(String sessionId) {
					return false;
				}
				@Override
				public void updateProgress(ProgressBarManager updateBean, String sessionId) {
					super.updateProgress(updateBean, sessionId);
				}
			}*/);
			SchemaWizardSessionUtility.register();
			File uploadDir = new File(AbstractAnalyzerTestWorkflow.uploadDir);
			if(!uploadDir.exists() && !uploadDir.mkdirs()) {
				throw new IOException("Could not create upload directory base.");
			}
			running = true;
		}
	}
	
	public static TikaSampleAnalyzerParameters generateTestSampleParameters(DefinedTestResource dtr, int sampleNumber, int totalNumberSamples) throws FileNotFoundException, IOException {
		String sessionId = AbstractAnalyzerTestWorkflow.testSessionId;
		String guid = Analyzer.generateUUID();
		String uploadFileDir = AbstractAnalyzerTestWorkflow.uploadDir;
		String domainName = "Transportation";
		String tolerance = Tolerance.STRICT.toString();
		String name = new File(dtr.getFilePath()).getName();

		InputStream is = TikaAnalyzer.class.getResourceAsStream(dtr.getFilePath());
		if(is == null) {
			is = new FileInputStream(dtr.getFilePath());
		}
		File file = new File(uploadFileDir, name);
		IOUtils.copy(is, new FileOutputStream(file.getAbsolutePath()));
		String sampleFilePath = file.getAbsolutePath();

		if(!file.exists()) {
			throw new IOException("Uploaded file does not exist at " + file + ".");
		}
		
		List<String> namePlaceholders = new ArrayList<String>();
		for(int i = 0; i < totalNumberSamples; i++) {
			if(i == sampleNumber) {
				namePlaceholders.add(name);
			} else {
				namePlaceholders.add("");
			}
		}
		ProgressBarManager progressBar = 
				ProgressBarManager.sampleProgressBar(namePlaceholders, sampleNumber);

		SampleProfiler sampleProfiler = new SampleProfiler(Tolerance.fromString(tolerance));

		TikaSampleAnalyzerParameters tikaProfilerParams = new TikaSampleAnalyzerParameters(sampleProfiler, progressBar,
				uploadFileDir, guid, is, new AnalyticsProgressTrackingContentHandler(), new Metadata());
		tikaProfilerParams.set(ProfilingProgressUpdateHandler.class, new SimpleProgressUpdater(sessionId, progressBar, file.length(), false));
		tikaProfilerParams.setUploadFileDir(uploadFileDir);
		tikaProfilerParams.setDomainName(domainName);
		tikaProfilerParams.setNumSamplesUploading(totalNumberSamples);
		tikaProfilerParams.setSampleFilePath(sampleFilePath);
		tikaProfilerParams.setSampleNumber(sampleNumber);
		tikaProfilerParams.setTolerance(tolerance);
		tikaProfilerParams.setStream(is);
		tikaProfilerParams.setSessionId(sessionId);
		tikaProfilerParams.setStreamLength(file.length());
		tikaProfilerParams.set(File.class, file);
		return tikaProfilerParams;
	}
}
