package com.deleidos.dp.h2;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;

public class H2DataAccessStartupIT extends DataProfilerIntegrationEnvironment{
	private static final Logger logger = Logger.getLogger(H2DataAccessStartupIT.class);

	@Test
	public void testStartup() throws IOException, DataAccessException {
		if(!H2DataAccessObject.getInstance().testConnection()) {
			throw new DataAccessException("Integration Environment not correctly initialized.");
		}
	}

}
