package com.deleidos.hd.h2;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PopulateDatabaseTest {
	private static Logger logger = Logger.getLogger(PopulateDatabaseTest.class);
	private static boolean isH2Started;
	public static H2TestDatabase h2;

	@Test
	public void setUpBase() throws SQLException, ClassNotFoundException, InterruptedException {
		if (!isH2Started) {
			isH2Started = true;
			logger.info("Beginning tests.");
			
			h2 = new H2TestDatabase();
			h2.startTestServer(h2.getConfig());
		}
	}
	
	@Test
	public void parseDockerVar() {
		String tcpPort = "tcp://172.17.0.3:9123";
		tcpPort = tcpPort.substring(6, tcpPort.length());
		String[] splits = tcpPort.split(":");
		assertTrue(splits[0].equals("172.17.0.3"));
		assertTrue(splits[1].equals("9123"));
	}
	
}
