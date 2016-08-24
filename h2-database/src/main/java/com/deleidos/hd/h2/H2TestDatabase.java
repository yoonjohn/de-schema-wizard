package com.deleidos.hd.h2;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.RunScript;
import org.h2.tools.Server;

public class H2TestDatabase extends H2Database {
	private Logger logger = Logger.getLogger(H2TestDatabase.class);

	/**
	 * The constructor for the TestH2Database class. Calling this constructor
	 * will not start the server. Use startTestServer() to start the server.
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 */
	public H2TestDatabase() throws ClassNotFoundException, SQLException {
		super(H2Config.TEST_CONFIG, true);
	}

	/**
	 * Runs through the methods to start the server.
	 * purge(), startServer(), initDbConnection(), testConnection(), initalizeDatabase().
	 * @throws InterruptedException 
	 * @throws ClassNotFoundException 
	 */
	public H2TestDatabase startTestServer(H2Config config) throws SQLException, ClassNotFoundException, InterruptedException {
		final Server server = startServer(config);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.info("Server shutting down.");
				server.stop();
				purge();
			}
		});
		Connection connection = getNewConnection();
		runSchemaWizardStartupScript(connection);
		if(!testConnection(connection)) {
			connection.close();
			throw new SQLException("Server not successfully started.");
		}
		populateDatabase(connection);
		connection.close();
		return this;
	}

	public void populateDatabase(Connection dbConnection) {
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

	// Private Methods
	/**
	 * Tests if the connection is made.
	 * @throws SQLException 
	 */
	private boolean testConnection(Connection dbConnection) throws SQLException {
		try {
			Thread.sleep(1000);
			if (dbConnection != null) {
				return dbConnection.isValid(10);
			} else {
				throw new NullPointerException("Connection tested was null.");
			}
		} catch (InterruptedException e) {
			logger.error(e);
			dbConnection.close();
			return false;
		}
	}

	/*private void initTestServerVariables() {
		DB_HOST = "localhost";
		DB_PORT = "9124";
		DB_DIR = "~/test-files/";
		DB_NAME = "testdb";
		DB_TCP_CONNECTION = "tcp://" + DB_HOST + ":" + DB_PORT;
		DB_USER = "sa";
		DB_PASSWORD = "";
	}

	public Server getServer() {
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
