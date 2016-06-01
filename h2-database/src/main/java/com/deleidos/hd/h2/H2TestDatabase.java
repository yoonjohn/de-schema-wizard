package com.deleidos.hd.h2;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.h2.tools.RunScript;

public class H2TestDatabase extends H2Database {
	private Logger logger = Logger.getLogger(H2TestDatabase.class);
	private final String[] SERVER_ARGUMENTS = new String[] { "-tcpPort", "9124" };

	/**
	 * The constructor for the TestH2Database class. Calling this constructor
	 * will not start the server. Use startTestServer() to start the server.
	 */
	public H2TestDatabase() {
		super();
	}
	
	/**
	 * Runs through the methods to start the server.
	 * purge(), startServer(), initDbConnection(), testConnection(), initalizeDatabase().
	 */
	public void startTestServer() {
		initTestServerVariables();
		purge();
		startServer(SERVER_ARGUMENTS);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.info("Server shutting down.");
				server.stop();
				purge();
			}
		});
		try {
			runSchemaWizardStartupScript();
		} catch (SQLException e) {
			logger.error("Did not find startup script.");
			e.printStackTrace();
		}
		testConnection();
	}
	
	public void populateDatabase() {
		InputStreamReader isr;
		String filePath = "/scripts/";
		String fileName = "populate_database.sql";
		
		try {
			isr = new InputStreamReader(
					getClass().getResourceAsStream(filePath + fileName));
			RunScript.execute(dbConnection, isr);
			isr.close();
			logger.info("Database populate script executed successfully.");
		} catch (SQLException e) {
			logger.error(e.getMessage());
			fail("Failed to execute database population script.");
		} catch (IOException e) {
			logger.error(e.getMessage());
			fail("Could not read from file " + filePath + fileName);
		}	
	}
	
	// Getters and setters
	/**
	 * 
	 * @return The database connection.
	 */
	public Connection getDBConnection() {
		return dbConnection;
	}

	// Private Methods
	/**
	 * Tests if the connection is made.
	 */
	private void testConnection() {
		try {
			Thread.sleep(1000);
			if (dbConnection != null) {
				logger.info("Successfully established connection to test server.");
			} else {
				logger.warn("Test server not found... Trying again.");
				Thread.sleep(5000);
				if (dbConnection != null) {
					logger.info("Test connection established.");
				} else {
					logger.error("Test connection not made.  Terminating application.");
				}
			}
		} catch (InterruptedException e) {
			logger.error(e);
			e.printStackTrace();
		}
	}
	
	private void initTestServerVariables() {
		DB_HOST = "localhost";
		DB_PORT = "9124";
		DB_DIR = "~/test-files/";
		DB_NAME = "testdb";
		DB_TCP_CONNECTION = "tcp://" + DB_HOST + ":" + DB_PORT;
		DB_USER = "sa";
		DB_PASSWORD = "";
	}

	/*public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Connection getDbConnection() {
		return dbConnection;
	}

	public void setDbConnection(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}

	public String getDB_DRIVER() {
		return DB_DRIVER;
	}

	public String getDB_DIR() {
		return DB_DIR;
	}

	public String getDB_NAME() {
		return DB_NAME;
	}

	public String getDB_PORT() {
		return DB_PORT;
	}

	public String getDB_HOST() {
		return DB_HOST;
	}

	public String getDB_TCP_CONNECTION() {
		return DB_TCP_CONNECTION;
	}

	public String getDB_CONNECTION() {
		return DB_CONNECTION;
	}

	public String[] getSERVER_ARGUMENTS() {
		return SERVER_ARGUMENTS;
	}*/
}
