package com.deleidos.dmf.integration;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.BeforeClass;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dmf.web.SchemaWizardSessionUtility;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2Database;
import com.deleidos.hd.h2.H2TestDatabase;
import com.deleidos.dmf.progressbar.ProgressBarManager;

public class DataModelFactoryIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(DataModelFactoryIntegrationEnvironment.class);
	static boolean isStarted = false;
	public static String testSessionId = AbstractAnalyzerTestWorkflow.testSessionId;
	protected static H2TestDatabase h2TestDatabase;

	/*@BeforeClass
	public static void startH2TestServer() throws Exception {
		if(!isStarted) {
			
			logger.info("Setting up data-model-factory integration environment.");
			h2TestDatabase = new H2TestDatabase();
			h2TestDatabase.startTestServer();
			H2DataAccessObject.getInstance(h2TestDatabase.getDbConnection());
			reverseGeocodingDAO = new TestReverseGeocodingDataAccessObject(0);
			ReverseGeocodingDataAccessObject.getInstance(reverseGeocodingDAO);
			logger.info("Dummy mongo environment initialized.");

			TestingWebSocketUtility.getInstance();
			
			InterpretationEngineFacade.getInstance(new BuiltinInterpretationEngine());
			
			Map<String, String> sessionID = new HashMap<String, String>();
			sessionID.put("sessionId", testSessionId);
			wsClient = new TestWebSocketClient(TestWebSocketClient.WS_ENDPOINT, null);
			wsClient.connect();
			wsClient.sendObject(sessionID);
			
			logger.info("Dummy websocket connection initialized.");
			isStarted = true;
			
		}
		if(!isStarted) {
			logger.info("Starting up Integration Environment.");
			H2DataAccessObject.getInstance();
			InterpretationEngineFacade.setInstance(new HttpInterpretationEngine());
			isStarted = true;
		}
	}*/

	@BeforeClass
	public static void setupIntegrationEnvironment() throws Exception {
		if(!isStarted) {
			try {
				logger.info("Starting up Integration Environment.");
				SchemaWizardSessionUtility.getInstance(new SchemaWizardSessionUtility() {
					{
						setPerformFakeUpdates(false);
					}
					@Override
					public Boolean isCancelled(String sessionId) {
						return false;
					}
					@Override
					public void updateProgress(ProgressBarManager updateBean, String sessionId) {
						return;
					}
				});
				SchemaWizardSessionUtility.register();
				H2DataAccessObject.setInstance(new H2Database());
				InterpretationEngineFacade.setInstance(new IEConfig().load());
				Map<String, String> sessionID = new HashMap<String, String>();
				sessionID.put("sessionId", testSessionId);
				isStarted = true;
			} catch (Exception e) {
				logger.error(e);
				throw new IntegrationException("Integration environment not correctly initialized.");
			}
		}
	}
	
	private static class IntegrationException extends Exception {
		public IntegrationException(String message) {
			super(message);
		}
	}
}
