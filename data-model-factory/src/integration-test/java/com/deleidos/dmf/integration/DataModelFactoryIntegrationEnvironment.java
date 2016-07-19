package com.deleidos.dmf.integration;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import com.deleidos.dmf.analyzer.workflows.AbstractAnalyzerTestWorkflow;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2Database;
import com.deleidos.hd.h2.H2TestDatabase;

public class DataModelFactoryIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(DataModelFactoryIntegrationEnvironment.class);
	static boolean isStarted = false;
	public static String testSessionId = AbstractAnalyzerTestWorkflow.testSessionId;
	protected static H2TestDatabase h2TestDatabase;
	

	@BeforeClass
	public static void setupIntegrationEnvironment() throws Exception {
		if(!isStarted) {
			try {
				logger.info("Starting up Integration Environment.");
				H2DataAccessObject.setInstance(new H2Database());
				H2DataAccessObject.getInstance().initConnection();
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
