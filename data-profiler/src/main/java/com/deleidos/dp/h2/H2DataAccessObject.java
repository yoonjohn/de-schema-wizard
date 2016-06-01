package com.deleidos.dp.h2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.SchemaMetaData;
import com.deleidos.dp.beans.StringDetail;

/**
 * Data access object to persist and retrieve schemas, samples, and metrics from
 * the H2 server.
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class H2DataAccessObject {
	public static final Logger logger = Logger.getLogger(H2DataAccessObject.class);
	private static final String DB_DRIVER = "org.h2.Driver";
	protected static Server server;
	private static String h2InitFile = "/h2-init.properties";
	protected static String DB_DIR;
	protected static String DB_NAME;
	protected static String DB_TCP_CONNECTION;
	protected static String DB_USER;
	protected static String DB_PASSWORD;
	protected Connection dbConnection = null;
	public int emptyHistogramId = 1;
	public int unknownClassificationId = 1;
	protected static H2DataAccessObject h2DAO = null;
	protected static H2MetricsDataAccessObject h2Metrics;
	protected static H2SampleDataAccessObject h2Samples;
	protected static H2SchemaDataAccessObject h2Schema;
	private static volatile boolean shutdownFlag = false;
	public static final boolean debug = false;

	/**
	 * Protected constructor for H2DAO. Connect to database and instantiate the
	 * object mapper.
	 */
	protected H2DataAccessObject() {
	}

	/**
	 * Initialize the H2DAO. If the environmental variable "H2_INIT_PROPERTIES"
	 * is set, properties will be pulled from that file. Otherwise,
	 * environmental variables "H2_DB_DIR," "H2_DB_NAME," "H2_DB_USER," and
	 * "H2_DB_PASSWORD" will be considered. If neither of these are set,
	 * defaults are DB_DIR = ~/h2, DB_NAME = data, DB_USER = sa, DB_PASSWORD =
	 * <empty string>
	 */
	public static void init() {
		h2InitFile = (System.getenv("H2_INIT_PROPERTIES") != null) ? System.getenv("H2_INIT_PROPERTIES") : null;

		if (h2InitFile == null) {
			DB_TCP_CONNECTION = (System.getenv("H2_DB_PORT") != null && DB_TCP_CONNECTION == null)
					? System.getenv("H2_DB_PORT") : "tcp://localhost:9123";
			DB_DIR = (System.getenv("H2_DB_DIR") != null && DB_DIR == null) ? System.getenv("H2_DB_DIR") : "~/h2";
			DB_NAME = (System.getenv("H2_DB_NAME") != null && DB_NAME == null) ? System.getenv("H2_DB_NAME") : "data";
			DB_USER = (System.getenv("H2_DB_USER") != null && DB_USER == null) ? System.getenv("H2_DB_USER") : "sa";
			DB_PASSWORD = (System.getenv("H2_DB_PASSWORD") != null && DB_PASSWORD == null)
					? System.getenv("H2_DB_PASSWORD") : "";
		} else {

			InputStream initStream;
			initStream = H2DataAccessObject.class.getResourceAsStream("/h2-init.properties");
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
				logger.error(e);
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

	/**
	 * Get or instantiate the static instance of the H2 Data Access Object.
	 * 
	 * @return The static H2DataAccessObject
	 */
	public static H2DataAccessObject getInstance() {
		if (h2DAO == null) {
			H2DataAccessObject.init();

			h2DAO = new H2DataAccessObject();

			String DB_CONNECTION = "jdbc:h2:" + DB_TCP_CONNECTION + "/" + DB_DIR + File.separator + DB_NAME;
			logger.info("Connecting with " + DB_CONNECTION);
			Connection conn = h2DAO.initDBConnection(DB_CONNECTION);
			h2DAO.setDbConnection(conn);

			h2Metrics = new H2MetricsDataAccessObject(h2DAO);
			h2Samples = new H2SampleDataAccessObject(h2DAO);
			h2Schema = new H2SchemaDataAccessObject(h2DAO);
		}
		return h2DAO;
	}

	/**
	 * 
	 * @return test instance of HsDataAccessObject
	 */
	public static H2DataAccessObject getInstance(Connection existingConnection) {
		if (h2DAO == null) {

			h2DAO = new H2DataAccessObject();

			h2DAO.setDbConnection(existingConnection);
			logger.info("Connection given directly to H2DataAccessObject.  This should only be used for testing.");

			h2Metrics = new H2MetricsDataAccessObject(h2DAO);
			h2Samples = new H2SampleDataAccessObject(h2DAO);
			h2Schema = new H2SchemaDataAccessObject(h2DAO);
		}
		return h2DAO;
	}

	/**
	 * Remove all files in the database directory with the database name.
	 */
	public void purge() {
		DeleteDbFiles.execute(DB_DIR, DB_NAME, true);
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
	public int getGeneratedKey(Statement stmt) throws SQLException {
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

	/**
	 * Run a query in H2.
	 * 
	 * @param sql
	 *            The string of the SQL query.
	 * @return The result set from executing this query.
	 * @throws SQLException
	 *             Thrown if there is an error in the query.
	 */
	public ResultSet query(String sql) throws SQLException {
		if (debug) {
			return queryWithOutput(sql);
		} else {
			return getDBConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
					.executeQuery(sql);
		}
	}

	/**
	 * Run a query in H2 and log the output at the debug level.
	 * 
	 * @param sql
	 *            The string of the SQL query.
	 * @return The result set from executing this query.
	 * @throws SQLException
	 *             Thrown if there is an error in the query.
	 */
	public ResultSet queryWithOutput(String sql) throws SQLException {
		ResultSet rs = query(sql);
		ResultSetMetaData rsmd = rs.getMetaData();
		int c = rsmd.getColumnCount();
		StringBuilder sb = new StringBuilder();
		logger.info(sql);
		for (int i = 1; i <= c; i++) {
			sb.append(rsmd.getColumnName(i) + "\t");
		}
		logger.info(sb);
		while (rs.next()) {
			StringBuilder s = new StringBuilder();
			for (int i = 1; i <= c; i++) {
				s.append(rs.getString(i) + "\t");
			}
			logger.info(s.toString());
		}
		rs.beforeFirst();
		return rs;
	}

	public Connection getDBConnection() {
		// return new connection
		// call driver once
		return dbConnection;
	}

	/**
	 * Initialize the database connection with the given connection string.
	 * 
	 * @param connectionString
	 *            The string to connect with.
	 * @return The connection.
	 */
	public Connection initDBConnection(String connectionString) {
		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		try {
			dbConnection = DriverManager.getConnection(connectionString);
			return dbConnection;
		} catch (SQLException e) {
			logger.error(e);
			logger.error("H2 database connection failed to be established!");
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

	/**
	 * Add a schema to H2.
	 * 
	 * @param schemaBean
	 *            the schema object as a bean
	 * @return The guid of the schema
	 */
	public String addSchema(Schema schemaBean) {
		if (schemaBean.getsName() == null) {
			schemaBean.setsName(schemaBean.getsGuid());
		}
		if (schemaBean.getsDescription() == null) {
			schemaBean.setsDescription("This is a schema generated by the Schema Wizard.");
		}
		if (schemaBean.getsVersion() == null) {
			schemaBean.setsVersion("1.0");
		}
		if (schemaBean.getsLastUpdate() == null) {
			schemaBean.setsLastUpdate(Timestamp.from(Instant.now()));
		}
		h2Schema.addSchema(schemaBean);
		return schemaBean.getsGuid();
	}

	/**
	 * Get a list of the schema meta data in H2
	 * 
	 * @return a list of SchemaMetaData beans
	 */
	public List<SchemaMetaData> getAllSchemaMetaData() {
		return h2Schema.getAllSchemaMetaData();
	}

	/**
	 * Get a list of the sample meta data in H2
	 * 
	 * @return a list of SampleMetaDataBeans
	 */
	public List<DataSampleMetaData> getAllSampleMetaData() {
		return h2Samples.getAllSampleMetaData();
	}

	/**
	 * Get a specific schema meta data object by its GUID
	 * 
	 * @param guid
	 *            the desired guid
	 * @return the SchemaMetaData bean that coincides with the GUID
	 */
	public SchemaMetaData getSchemaMetaDataByGuid(String guid) {
		SchemaMetaData schemaMetaData = new SchemaMetaData();
		schemaMetaData = h2Schema.getSchemaMetaDataByGuid(guid);
		return schemaMetaData;
	}

	/**
	 * Get a schema by its guid
	 * 
	 * @param guid
	 *            the schema's guid
	 * @param showHistogram
	 *            true if histogram data should be displayed, false if it should
	 *            be removed
	 * @return the Schema bean
	 */
	public Schema getSchemaByGuid(String guid, boolean showHistogram) {
		Schema schema = new Schema();
		schema = h2Schema.getSchemaByGuid(guid);

		if (!showHistogram) {
			for (String key : schema.getsProfile().keySet()) {
				Profile profile = schema.getsProfile().get(key);
				Detail detail = profile.getDetail();
				if (detail instanceof NumberDetail) {
					NumberDetail nm = ((NumberDetail) Profile.getNumberDetail(profile));
					nm.setFreqHistogram(null);
				} else if (detail instanceof StringDetail) {
					StringDetail sm = ((StringDetail) Profile.getStringDetail(profile));
					sm.setTermFreqHistogram(null);
					sm.setCharFreqHistogram(null);
				} else if (detail instanceof BinaryDetail) {
					logger.error("Detected as binary in " + getClass().getName() + ".");
				}
				profile.setDetail(detail);
			}
		}
		return schema;
	}

	/**
	 * Gets the field-descriptor
	 * 
	 * @param guid
	 *            The Schema's Guid
	 * @param showHistogram
	 *            True if histogram data should be displayed; False if it should
	 *            be removed
	 * @return
	 */
	public Map<String, Profile> getSchemaFieldByGuid(String guid, boolean showHistogram) {
		Map<String, Profile> map = new HashMap<String, Profile>();
		map = h2Schema.getSchemaFieldByGuid(guid);

		if (!showHistogram) {
			for (String key : map.keySet()) {
				Profile profile = map.get(key);
				Detail detail = profile.getDetail();
				if (detail instanceof NumberDetail) {
					NumberDetail nm = ((NumberDetail) Profile.getNumberDetail(profile));
					nm.setFreqHistogram(null);
				} else if (detail instanceof StringDetail) {
					StringDetail sm = ((StringDetail) Profile.getStringDetail(profile));
					sm.setTermFreqHistogram(null);
					sm.setCharFreqHistogram(null);
				} else if (detail instanceof BinaryDetail) {
					logger.error("Detected as binary in " + getClass().getName() + ".");
				}
				profile.setDetail(detail);
			}
		}
		return map;
	}

	/**
	 * Delete schema based on its guid
	 * 
	 * @param guid
	 */
	public void deleteSchemaFromDeletionQueue(String guid) {
		logger.info("Deleting schema " + guid +" from database.");
		h2Schema.deleteSchemaFromDeletionQueue(guid);
	}

	/**
	 * Add a sample
	 * 
	 * @param sample
	 *            the DataSample bean to be added
	 */
	public String addSample(DataSample sample) {
		try {
			return h2Samples.addSample(sample);
		} catch (SQLException e) {
			logger.error("Error adding sample");
			logger.error(e);
			return null;
		}
	}

	/**
	 * Call underlying H2SampleDAO class to retrieve a mapping of all the sample
	 * names to their respective media types in the database.
	 * 
	 * @return
	 */
	public Map<String, String> getExistingSampleNames() {
		return h2Samples.getExistingSampleNames();
	}

	/**
	 * Get a list of samples by their guids
	 * 
	 * @param guids
	 *            ordered list of guids
	 * @return an ordered list of DataSample beans
	 * @throws SQLException
	 */
	public List<DataSample> getSamplesByGuids(String[] guids) {
		List<DataSample> samples = new ArrayList<DataSample>();
		for (String guid : guids) {
			samples.add(h2Samples.getSampleByGuid(guid));
		}
		return samples;
	}

	/**
	 * Gets a given Data Sample bean given its Guid
	 * 
	 * @param guid
	 * @return
	 */
	public DataSample getSampleByGuid(String guid) {
		DataSample sample = new DataSample();

		sample = h2Samples.getSampleByGuid(guid);
		return sample;
	}

	/**
	 * Gets a Data Sample Meta Data bean given its Guid
	 * 
	 * @param guid
	 * @return
	 */
	public DataSampleMetaData getSampleMetaDataByGuid(String guid) {
		DataSampleMetaData sampleMetaData;

		sampleMetaData = h2Samples.getDataSampleMetaDataByGuid(guid);
		return sampleMetaData;
	}

	/**
	 * Gets the field-descriptor
	 * 
	 * @param guid
	 * @return
	 */
	public Map<String, Profile> getSampleFieldByGuid(String guid, boolean showHistogram) {
		Map<String, Profile> map = new HashMap<String, Profile>();
		map = h2Samples.getSampleFieldByGuid(guid);

		if (!showHistogram) {
			for (String key : map.keySet()) {
				Profile profile = map.get(key);
				Detail detail = profile.getDetail();
				if (detail instanceof NumberDetail) {
					NumberDetail nm = ((NumberDetail) Profile.getNumberDetail(profile));
					nm.setFreqHistogram(null);
				} else if (detail instanceof StringDetail) {
					StringDetail sm = ((StringDetail) Profile.getStringDetail(profile));
					sm.setTermFreqHistogram(null);
					sm.setCharFreqHistogram(null);
				} else if (detail instanceof BinaryDetail) {
					logger.error("Detected as binary in " + getClass().getName() + ".");
				}
				profile.setDetail(detail);
			}
		}
		return map;
	}

	public void deleteSchemaByGuid(String guid) {
		h2Schema.deleteSchemaByGuid(guid);
	}

	public void deleteSampleByGuid(String guid) {
		h2Samples.deleteSampleByGuid(guid);
	}

	/**
	 * Has logic to determine if a GUID is a Schema or Data Sample.
	 * 
	 * @param guid
	 *            An ambiguous GUID belonging to either a Schema or Data Sample
	 */
	public void deleteByGuid(String guid) {
		Schema schema = h2Schema.getSchemaByGuid(guid);
		DataSample sample = h2Samples.getSampleByGuid(guid);

		if (schema != null) {
			h2Schema.deleteSchemaByGuid(guid);
		} else if (sample != null) {
			h2Samples.deleteSampleByGuid(guid);
		} else {
			logger.error("No such guid exists in the database.");
		}
	}

	public static String getDB_PORT() {
		String[] splits = DB_TCP_CONNECTION.split(":");
		return splits[splits.length - 1];
	}

	public static void setDB_PORT(String dB_PORT) {
		if (DB_TCP_CONNECTION == null) {
			DB_TCP_CONNECTION = "tcp::" + dB_PORT;
		}
		String[] splits = DB_TCP_CONNECTION.split(":");
		splits[splits.length - 1] = dB_PORT;
		String together = null;
		for (String split : splits) {
			together += split;
		}
		DB_TCP_CONNECTION = together;
	}

	public static void setDB_HOST(String dB_HOST) {
		if (DB_TCP_CONNECTION == null) {
			DB_TCP_CONNECTION = "tcp:" + dB_HOST + ":";
		}
		String[] splits = DB_TCP_CONNECTION.split(":");
		splits[1] = dB_HOST;
		String together = "";
		for (String split : splits) {
			together += split + ":";
		}
		together = together.substring(0, together.length() - 1);
		DB_TCP_CONNECTION = together;
	}

	public boolean isShutdownFlag() {
		return shutdownFlag;
	}

	public void setShutdownFlag() {
		H2DataAccessObject.shutdownFlag = true;
	}

	public static H2DataAccessObject getH2DAO() {
		return h2DAO;
	}

	public static void setH2DAO(H2DataAccessObject h2dao) {
		h2DAO = h2dao;
	}

	public Connection getDbConnection() {
		return dbConnection;
	}

	public void setDbConnection(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}

	public int getEmptyHistogramId() {
		return emptyHistogramId;
	}

	public static String getH2InitFile() {
		return h2InitFile;
	}

	/**
	 * Set the file that the static H2 server will use to initialize.
	 * 
	 * @param h2InitFile
	 *            The path to the file.
	 */
	public static void setH2InitFile(String h2InitFile) {
		H2DataAccessObject.h2InitFile = h2InitFile;
	}

	public void setEmptyHistogramId(int emptyHistogramId) {
		this.emptyHistogramId = emptyHistogramId;
	}

	public int getUnknownInterpretationId() {
		return unknownClassificationId;
	}

	public void setUnknownClassificationId(int unknownClassificationId) {
		this.unknownClassificationId = unknownClassificationId;
	}

	public static String getDB_DIR() {
		return DB_DIR;
	}

	public static void setDB_DIR(String dB_DIR) {
		DB_DIR = dB_DIR;
	}

	public static String getDB_NAME() {
		return DB_NAME;
	}

	public static void setDB_NAME(String dB_NAME) {
		DB_NAME = dB_NAME;
	}

	public H2MetricsDataAccessObject getH2Metrics() {
		return h2Metrics;
	}

	public H2SampleDataAccessObject getH2Samples() {
		return h2Samples;
	}

	public H2SchemaDataAccessObject getH2Schema() {
		return h2Schema;
	}

	/**
	 * Convenience method to turn a prepared statement into a string. Should not
	 * be used for anything other than debugging
	 * 
	 * @param ppst
	 * @return
	 * @throws SQLException
	 */
	private static String preparedStatementToString(PreparedStatement ppst) throws SQLException {

		String queryString = ppst.toString();
		queryString = queryString.substring(queryString.indexOf(':') + 1);
		if (!queryString.contains("{")) {
			return queryString;
		}
		String[] splits = queryString.split("\\{", 2);
		queryString = splits[0];
		String params = "{" + splits[1];
		String[] values = params.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		for (int i = 0; i < values.length; i++) {
			String substitution = values[i].substring(values[i].indexOf(":") + 1);
			int markIndex = queryString.indexOf("?");
			queryString = queryString.substring(0, markIndex) + substitution.trim()
					+ queryString.substring(markIndex + 1);
		}
		queryString = queryString.substring(0, queryString.lastIndexOf("}"))
				+ queryString.substring(queryString.lastIndexOf("}") + 1);
		return queryString;
	}
}
