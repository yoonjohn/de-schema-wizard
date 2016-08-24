package com.deleidos.dp.h2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;

public class H2DataAccessStartupIT extends DataProfilerIntegrationEnvironment {

	@Test
	public void testStartup() throws SQLException, DataAccessException {
		for(int i = 0; i < 100; i++) {
			Connection connection = H2DataAccessObject.getInstance().getH2Database().getNewConnection();
			try {
				assertTrue(H2DataAccessObject.getInstance().testConnection(connection));
				connection.close();
			} catch(AssertionError e) {
				connection.close();
				fail("Connection test failed.");
			}
		}
	}

}
