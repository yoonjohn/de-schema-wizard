package com.deleidos.hd.h2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.RunScript;
import org.h2.tools.Server;

/*
 * TODO break out test functionality into different class
 * when you instantiate H2Database, add a shutdown hook that calls a stop and purge
 * 
 * Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					logger.info("Server shutting down.");
					server.stop();
					h2.purge();
				}
			});
 * 
 * you will have to remove the shutdown hook in startServer() method
 * and add it to the main
 * 
 * rename: init() -> determineStartupProperties()
 * rename: startup() -> initializeH2Schema()
 */

/**
 * Data access object to persist and retrieve schemas, samples, and metrics from
 * the H2 server.
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class H2Database {
	// split out starting vs. initializing functions
	// make things private where possible
	// public constructor - not a singleton anymore
	protected static Server server;
	public static Logger logger = Logger.getLogger(H2Database.class);
	private static final String DB_DRIVER = "org.h2.Driver";
	private static String h2InitFile = null;
	private JdbcConnectionPool connectionPool;
	protected static String DB_TCP_CONNECTION = "tcp://localhost:9123";
	protected String DB_DIR;
	protected String DB_NAME;
	protected String DB_PORT;
	protected String DB_HOST;
	protected String DB_USER;
	protected String DB_PASSWORD;
	protected Connection dbConnection = null;
	public int emptyHistogramId = 1;
	public int unknownInterpretationId = 1;
	private static volatile boolean shutdownFlag = false;
	public static boolean debug = false;

	protected H2Database() {

	}

	/**
	 * *** FOR TESTING PURPOSES ONLY ***
	 * 
	 * This is a constructor for unit testing. It only initializes the
	 * parameters.
	 * 
	 * @param test
	 *            This parameter is superficial
	 * 
	 *            public H2Database(boolean test) { logger.info(
	 *            "Initializing test server variables."); initTestServer();
	 *            logger.info("Purging test database."); purge(); }
	 */

	public void scanForConnectionStringVariables() {
		h2InitFile = (System.getenv("H2_INIT_PROPERTIES") != null) ? System.getenv("H2_INIT_PROPERTIES") : null;

		if (h2InitFile == null) {
			DB_TCP_CONNECTION = (System.getenv("H2_DB_PORT") != null) ? System.getenv("H2_DB_PORT")
					: "tcp://localhost:9123";
			DB_DIR = (System.getenv("H2_DB_DIR") != null) ? System.getenv("H2_DB_DIR") : "~/h2";
			DB_NAME = (System.getenv("H2_DB_NAME") != null) ? System.getenv("H2_DB_NAME") : "data";
			DB_USER = (System.getenv("H2_DB_USER") != null) ? System.getenv("H2_DB_USER") : "sa";
			DB_USER = (System.getenv("H2_DB_PASSWORD") != null) ? System.getenv("H2_DB_PASSWORD") : "";
		} else {

			InputStream initStream;
			initStream = H2Database.class.getResourceAsStream("/h2-init.properties");
			Properties h2Properties = new Properties();
			String DB_HOST = null;
			String DB_PORT = null;
			try {
				h2Properties.load(initStream);

				DB_HOST = h2Properties.getProperty("h2.DB_HOST");
				DB_DIR = h2Properties.getProperty("h2.DB_DIR");
				DB_NAME = h2Properties.getProperty("h2.DB_NAME");
				DB_USER = h2Properties.getProperty("h2.DB_USER");
				DB_PORT = h2Properties.getProperty("h2.DB_PORT");
				DB_PASSWORD = h2Properties.getProperty("h2.DB_PASSWORD");
				DB_TCP_CONNECTION = "tcp://" + DB_HOST + ":" + DB_PORT;
			} catch (IOException e) {
				logger.error("H2 Init file not found in path.  Using defaults.");
				DB_HOST = "localhost";
				DB_DIR = "~/h2";
				DB_NAME = "data";
				DB_USER = "sa";
				DB_PORT = "9123";
				DB_PASSWORD = "";
				DB_TCP_CONNECTION = "tcp://" + DB_HOST + ":" + DB_PORT;
			}

		}
	}

	public void initializeConnection() {
		String DB_CONNECTION = "jdbc:h2:" + DB_TCP_CONNECTION + "/" + DB_DIR + File.separator + DB_NAME;
		logger.info("Connecting with " + DB_CONNECTION);
		
		dbConnection = initDBConnection(DB_CONNECTION);
	}

	/**
	 * Start the server with defaults.
	 * 
	 * @param args
	 *            Command line arguments for H2.
	 * @throws IOException
	 */
	public static void main(String[] args) {
		try {

			if (System.getenv("LOCAL_DEBUG") != null && System.getenv("LOCAL_DEBUG").equals("true")) {
				H2Database.debug = true;
			}

			H2Database h2 = new H2Database();
			h2.scanForConnectionStringVariables();
			h2.startServer(args);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					logger.info("Server shutting down.");
					server.stop();
				}
			});
			h2.runSchemaWizardStartupScript();

			if (H2Database.debug) {
				Server.startWebServer(h2.getDBConnection());
			}

			h2.join();
		} catch (SQLException e) {
			logger.error(e);
			logger.error(e.getMessage());
		}
	}

	/**
	 * Override method to run the server in its own thread. Able to implement
	 * maintenance here.
	 */
	public void join() {
		long t1 = System.currentTimeMillis();
		logger.info("Server running at " + server.getURL());
		while (true) {
			long currentTime = System.currentTimeMillis();
			if ((currentTime - t1) > 360000 && t1 != currentTime) {
				System.out.println("Server running at " + server.getURL());
				t1 = currentTime;
			}
			if (shutdownFlag) {
				logger.info("Server thread signaled to stop.");
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
				e.printStackTrace();
			}
		}
		logger.info("Server thread ended.");
	}

	/**
	 * Start up the server with command line arguments. Unless the init(String
	 * initFile) method is called, the server will be started with properties
	 * from src/main/resources/h2-init.properties
	 * 
	 * @param args
	 *            Arguments to start the server. These should match the
	 *            properties file. TODO if empty, use properties file
	 */
	public void startServer(String[] args) {
		try {
			logger.info("Starting up H2 server.");
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-tcpPort") && System.getenv("H2_DB_PORT") != null) {
					setDB_PORT(System.getenv("H2_DB_PORT"));
					args[i + 1] = getDB_PORT();
					break;
				}
			}
			server = Server.createTcpServer(args);
			logger.info("Server started at " + server.getURL());
			server.setOut(System.out);
			server.start();
			try {
				Thread.sleep(1000);
				initializeConnection();
				if (getDBConnection() != null) {
					logger.info("Successfully established connection to server.");
				} else {
					logger.warn("H2 server not found... Trying again.");
					Thread.sleep(5000);
					initializeConnection();
					if (getDBConnection() != null) {
						logger.info("H2 connection established.");
					} else {
						logger.error("H2 connection not made.  Terminating application.");
						throw new IOException("H2 server not successfully initialized.");
					}
				}
				logger.info("Database initialized.");
			} catch (InterruptedException e) {
				logger.error(e);
				e.printStackTrace();
			}
		} catch (IOException e) {
			logger.error(e);
			e.printStackTrace();
		} catch (SQLException e) {
			logger.error("Error running startup script for H2.");
			e.printStackTrace();
		}
	}

	private enum DATA_TYPE {
		STRING, NUMBER, BINARY, OBJECT, ARRAY, NULL;
		public int getIndex() {
			return ordinal();
		}

		public static DATA_TYPE getTypeByIndex(int index) {
			return DATA_TYPE.values()[index];
		}

		public void incrementCount(int[] typeTracker) {
			typeTracker[ordinal()]++;
		}

		@Override
		public String toString() {
			String s = super.toString();
			return s.toLowerCase();
		}
	}

	private enum DETAIL_TYPE {
		INTEGER, DECIMAL, EXPONENT, DATE_TIME, BOOLEAN, TERM, PHRASE, IMAGE, VIDEO_FRAME, AUDIO_SEGMENT;
		public void incrementCount(int[] detailTypeTracker) {
			detailTypeTracker[ordinal()]++;
		}

		public static DETAIL_TYPE getTypeByIndex(int index) {
			return DETAIL_TYPE.values()[index];
		}

		public DATA_TYPE getMainType() {
			switch (this) {
			case INTEGER:
				return DATA_TYPE.NUMBER;
			case DECIMAL:
				return DATA_TYPE.NUMBER;
			case EXPONENT:
				return DATA_TYPE.NUMBER;
			case DATE_TIME:
				return DATA_TYPE.STRING;
			case BOOLEAN:
				return DATA_TYPE.STRING;
			case TERM:
				return DATA_TYPE.STRING;
			case PHRASE:
				return DATA_TYPE.STRING;
			case IMAGE:
				return DATA_TYPE.BINARY;
			case VIDEO_FRAME:
				return DATA_TYPE.BINARY;
			case AUDIO_SEGMENT:
				return DATA_TYPE.BINARY;
			default:
				return null;
			}
		}

		public int getIndex() {
			return ordinal();
		}

		@Override
		public String toString() {
			String s = super.toString();
			return s.toLowerCase();
		}
	}

	/**
	 * Generate the schema wizard's schema in the H2 database.
	 * 
	 * @throws SQLException
	 *             If there is an exception executing the startup script.
	 */
	public void runSchemaWizardStartupScript() throws SQLException {
		try {
			InputStreamReader isr = new InputStreamReader(
					getClass().getResourceAsStream("/scripts/init_field_characterization.sql"));
			RunScript.execute(dbConnection, isr);
			logger.info("Initialization script executed.");
			isr.close();

			String countMainType = "SELECT * FROM main_type";
			PreparedStatement ppstCheckMain = dbConnection.prepareStatement(countMainType);
			ResultSet rsMainType = ppstCheckMain.executeQuery();
			if (!rsMainType.next()) {
				for (DATA_TYPE type : DATA_TYPE.values()) {
					int id = type.getIndex();
					String name = type.name();
					String insertIntoMainType = "INSERT INTO main_type VALUES (? , ?)";
					PreparedStatement ppst = dbConnection.prepareStatement(insertIntoMainType);
					ppst.setInt(1, id);
					ppst.setString(2, name);
					ppst.execute();

				} // 3 num,4 string,3 bin
				logger.info("Data types inserted.");
			} else {
				logger.info("Data types exist.");
			}

			String countDetailType = "SELECT * FROM detail_type";
			PreparedStatement ppstCheckDetail = dbConnection.prepareStatement(countDetailType);
			ResultSet rsDetailType = ppstCheckDetail.executeQuery();
			if (!rsDetailType.next()) {
				for (DETAIL_TYPE type : DETAIL_TYPE.values()) {
					int id = type.getIndex();
					String name = type.name();
					String insertIntoDetailType = "INSERT INTO detail_type VALUES (?, ?)";
					PreparedStatement ppst = dbConnection.prepareStatement(insertIntoDetailType);
					ppst.setInt(1, id);
					ppst.setString(2, name);
					ppst.execute();

					String insertIntoTypeMapping = "INSERT INTO type_mapping VALUES (NULL, ?, ?)";
					PreparedStatement ppst2 = dbConnection.prepareStatement(insertIntoTypeMapping);
					ppst2.setLong(1, type.getMainType().getIndex());
					ppst2.setInt(2, id);
					ppst2.execute();
				}
				logger.info("Detail types inserted.");
			} else {
				logger.info("Detail types exist.");
			}

			String countHistogram = "SELECT * FROM histogram";
			PreparedStatement ppstCheckHistogram = dbConnection.prepareStatement(countHistogram);
			ResultSet rsHistogram = ppstCheckHistogram.executeQuery();
			if (!rsHistogram.next()) {
				String insertIntoHistogram = "INSERT INTO histogram VALUES (NULL, NULL, NULL, NULL)";
				PreparedStatement ppstHistogram = dbConnection.prepareStatement(insertIntoHistogram);
				ppstHistogram.execute();
				setEmptyHistogramId(getGeneratedKey(ppstHistogram));
				logger.info("Empty histogram inserted.");
			} else {
				logger.info("Histograms exists.");
			}

			String countInterpretation = "SELECT * FROM interpretation";
			PreparedStatement ppstCheckInterpretation = dbConnection.prepareStatement(countInterpretation);
			ResultSet rsInterpretation = ppstCheckInterpretation.executeQuery();
			if (!rsInterpretation.next()) {
				String insertIntoInterpretation = "INSERT INTO interpretation (i_name) VALUES (?) ";
				PreparedStatement ppstInterpretation = dbConnection.prepareStatement(insertIntoInterpretation,
						PreparedStatement.RETURN_GENERATED_KEYS);
				ppstInterpretation.setString(1, "unknown");
				ppstInterpretation.execute();
				setUnknownInterpretationId(getGeneratedKey(ppstInterpretation));
				logger.info("Empty interpretation inserted.");
			} else {
				logger.info("Interpretations exists.");
			}
		} catch (IOException e) {
			logger.error(e);
			e.printStackTrace();
		} catch (SQLException e) {
			logger.error(e);
			e.printStackTrace();
		}
	}

	/**
	 * Remove all files in the database directory with the database name. The
	 * database must be closed before calling this method.
	 */
	public void purge() {
		DeleteDbFiles.execute(DB_DIR, DB_NAME, true);
	}

	public Connection getDBConnection() {
		return dbConnection;
	}

	/**
	 * Initialize the database connection with the given connection string.
	 * 
	 * @param connectionString
	 *            The string to connect with.
	 * @return The connection.
	 */ 
	private Connection initDBConnection(String connectionString) {
		/*connectionPool = JdbcConnectionPool.create(connectionString, DB_USER, DB_PASSWORD);
		try {
			dbConnection = connectionPool.getConnection();
		} catch (SQLException e) {
			logger.error("Failed to retrieve connection.");
			e.printStackTrace();
		}*/
		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage());
		}
		try {
			dbConnection = DriverManager.getConnection(connectionString);
			return dbConnection;
		} catch (SQLException e) {
			logger.error(e.getMessage());
			logger.error("H2 database must be running!");
		}
		return dbConnection;
	}

	/**
	 * Close the connection.
	 */
	public void closeConnection() {
		try {
			dbConnection.close();
		} catch (SQLException e) {
			logger.warn("H2 connection not successfully closed.");
			logger.warn(e);
		}
	}

	/**
	 * Stop the server and properly terminate its thread.
	 */
	public static void stopServer() {
		if (server != null) {
			server.stop();
			logger.info("Shutting down server.");
			shutdownFlag = true;
		}
	}

	public boolean isShutdownFlag() {
		return shutdownFlag;
	}

	public void setShutdownFlag() {
		H2Database.shutdownFlag = true;
	}

	// Getters and Setters
	public int getEmptyHistogramId() {
		return emptyHistogramId;
	}

	public String getH2InitFile() {
		return h2InitFile;
	}

	/**
	 * Set the file that the static H2 server will use to initialize.
	 * 
	 * @param h2InitFile
	 *            The path to the file.
	 */
	public void setH2InitFile(String h2InitFile) {
		H2Database.h2InitFile = h2InitFile;
	}

	public void setEmptyHistogramId(int emptyHistogramId) {
		this.emptyHistogramId = emptyHistogramId;
	}

	public int getUnknownInterpretationId() {
		return unknownInterpretationId;
	}

	public void setUnknownInterpretationId(int unknownInterpretationId) {
		this.unknownInterpretationId = unknownInterpretationId;
	}

	public String getDB_DIR() {
		return DB_DIR;
	}

	public void setDB_DIR(String dB_DIR) {
		DB_DIR = dB_DIR;
	}

	public String getDB_NAME() {
		return DB_NAME;
	}

	public void setDB_NAME(String dB_NAME) {
		DB_NAME = dB_NAME;
	}

	public String getDB_PORT() {
		String[] splits = DB_TCP_CONNECTION.split(":");
		return splits[splits.length - 1];
	}

	public void setDB_PORT(String dB_PORT) {
		String[] splits = DB_TCP_CONNECTION.split(":");
		splits[splits.length - 1] = dB_PORT;
		String together = "";
		for (String split : splits) {
			together += split + ":";
		}
		together = together.substring(0, together.length() - 1);
		DB_TCP_CONNECTION = together;
	}

	public String getDB_HOST() {
		String[] splits = DB_TCP_CONNECTION.split(":");
		return splits[1];
	}

	public void setDB_HOST(String dB_HOST) {
		String[] splits = DB_TCP_CONNECTION.split(":");
		splits[1] = dB_HOST;
		String together = "";
		for (String split : splits) {
			together += split + ":";
		}
		together = together.substring(0, together.length() - 1);
		DB_TCP_CONNECTION = together;
	}

	public String getDB_USER() {
		return DB_USER;
	}

	public void setDB_USER(String dB_USER) {
		DB_USER = dB_USER;
	}

	public String getDB_PASSWORD() {
		return DB_PASSWORD;
	}

	public void setDB_PASSWORD(String dB_PASSWORD) {
		DB_PASSWORD = dB_PASSWORD;
	}

	public String getDbDriver() {
		return DB_DRIVER;
	}

	public String getTcpConnection() {
		return "tcp://" + getDB_HOST() + ":" + getDB_PORT();
	}

	/**
	 * Return the generated key from a statement (H2 only allows a maximum of
	 * one to be returned per query). Calling this method will not execute the
	 * statement.
	 * 
	 * @param stmt
	 *            The executed statement
	 * @return The key generated by executing this statement
	 * @throws SQLException
	 *             Thrown if there is an error in the query.
	 */
	protected int getGeneratedKey(Statement stmt) throws SQLException {
		ResultSet gKeys = stmt.getGeneratedKeys();
		int fieldId = -1;
		if (gKeys.next()) {
			fieldId = gKeys.getInt(1);
			stmt.close();
		} else {
			stmt.close();
			return -1;
		}
		return fieldId;
	}

	public Connection getDbConnection() {
		/*try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			logger.error("Unable to retrieve connection from pool.");
			e.printStackTrace();
		}
		return null;*/
		return dbConnection;
	}

	public void setDbConnection(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}
}