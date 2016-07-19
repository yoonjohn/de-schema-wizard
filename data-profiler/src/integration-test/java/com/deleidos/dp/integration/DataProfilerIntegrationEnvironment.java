package com.deleidos.dp.integration;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2Database;
import com.deleidos.hd.h2.H2TestDatabase;

public class DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(DataProfilerIntegrationEnvironment.class);
	private static final String INTEGRATION_CONFIG_PATH = "INTEGRATION_CONFIG_PATH";
	protected static boolean isStarted = false;
	public static int GEO_CODING_SLEEP = 0;
	protected static H2TestDatabase h2Test;
	//protected static TestReverseGeocodingDataAccessObject t;

	@BeforeClass
	public static void setupIntegrationEnvironment() throws Exception {
		if(!isStarted) {
			try {
				logger.info("Starting up Integration Environment.");
				H2DataAccessObject.setInstance(new H2Database().connect()).initConnection();
				InterpretationEngineFacade.setInstance(new IEConfig().load());
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
