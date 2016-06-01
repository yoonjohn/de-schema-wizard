package com.deleidos.dp.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.SchemaMetaData;

/**
 * Data Access Object meant to communicate solely with schema tables in the
 * database.
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class H2SchemaDataAccessObject {
	private static final Logger logger = H2DataAccessObject.logger;
	private H2DataAccessObject h2;
	private Connection dbConnection;

	/**************************** DATABASE QUERIES ****************************/
	private final String ADD_SCHEMA_MODEL = "INSERT INTO schema_model VALUES (NULL, ?, ?, ?, ?, ?);";
	private final String ADD_SCHEMA_DATA_SAMPLES_MAPPING = "INSERT INTO schema_data_samples_mapping VALUES (?, ?);";
	private final String GET_SCHEMA_META_DATA_LIST = "SELECT * FROM schema_model "
			+ " INNER JOIN schema_data_samples_mapping ON (schema_model.schema_model_id = schema_data_samples_mapping.schema_model_id)"
			+ " INNER JOIN data_sample ON (schema_data_samples_mapping.data_sample_id = data_sample.data_sample_id)"
			+ " ORDER BY schema_model.s_name ASC;";
	private final String GET_SCHEMA_META_DATA_BY_GUID = "SELECT * FROM schema_model"
			+ " INNER JOIN schema_data_samples_mapping ON (schema_data_samples_mapping.schema_model_id = schema_model.schema_model_id)"
			+ " INNER JOIN data_sample ON (data_sample.data_sample_id = schema_data_samples_mapping.data_sample_id)"
			+ " WHERE schema_model.s_guid = ?;";
	/*
	 * private final String GET_SCHEMA_BY_GUID = "SELECT * FROM schema_model" +
	 * " INNER JOIN schema_data_samples_mapping ON (schema_model.schema_model_id = schema_data_samples_mapping.schema_model_id)"
	 * +
	 * " INNER JOIN data_sample ON (schema_data_samples_mapping.data_sample_id = data_sample.data_sample_id)"
	 * +
	 * " INNER JOIN schema_field ON (schema_model.schema_model_id = schema_field.schema_model_id);"
	 * ;
	 */
	private final String DELETE_SCHEMA_FROM_DELETION_QUEUE = "DELETE FROM deletion_queue WHERE guid = ?;";
	private final String GET_SCHEMA_DATA_SAMPLES_MAPPING = "SELECT * FROM schema_data_samples_mapping "
			+ "INNER JOIN schema_model ON (schema_model.schema_model_id = schema_data_samples_mapping.schema_model_id "
			+ "AND schema_model.s_guid = ?) "
			+ "INNER JOIN data_sample ON (schema_data_samples_mapping.data_sample_id = data_sample.data_sample_id); ";
	private final String DELETE_SCHEMA_BY_GUID = "DELETE FROM schema_model WHERE s_guid = ?";
	/**************************************************************************/

	// For testing
	private final String GET_ALL_SCHEMA = "SELECT * FROM schema_model;";

	public H2SchemaDataAccessObject(H2DataAccessObject h2) {
		this.h2 = h2;
		dbConnection = h2.getDBConnection();
	}

	/**
	 * Get sample guids associated with a given schema guid
	 * 
	 * @param schemaGuid
	 *            the desired schema guid
	 * @return a list of schema guids associated with the given schema
	 * @throws SQLException
	 */
	public List<String> getSampleGuidsFromSchemaGuid(String schemaGuid) {
		List<String> dataSampleGuids = new ArrayList<String>();
		PreparedStatement getMappingsStatement = null;

		try {
			getMappingsStatement = dbConnection.prepareStatement(GET_SCHEMA_DATA_SAMPLES_MAPPING);
			getMappingsStatement.setString(1, schemaGuid);
			ResultSet rs = getMappingsStatement.executeQuery();

			while (rs.next()) {
				String sampleGuid = rs.getString("ds_guid");
				dataSampleGuids.add(sampleGuid);
			}
		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (getMappingsStatement != null)
					getMappingsStatement.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
		return dataSampleGuids;
	}

	/**
	 * Adds a schema to the H2 database // TODO Add profile and data samples
	 * 
	 * @param schemaBean
	 * @return
	 * @throws SQLException
	 */
	public int addSchema(Schema schemaBean) {

		try {
			dbConnection.setAutoCommit(false);
		} catch (SQLException e1) {
			logger.error("Failed to set auto commit to false.");
			e1.printStackTrace();
		}
		int generatedId = -1;
		try {
			generatedId = addSchemaAndMapping(schemaBean.getSchemaModelId(), schemaBean.getsGuid(),
					schemaBean.getsName(), schemaBean.getsVersion(), schemaBean.getsLastUpdate(),
					schemaBean.getsDescription(), schemaBean.getsProfile(), schemaBean.getsDataSamples());
			dbConnection.commit();
			dbConnection.setAutoCommit(true);
		} catch (SQLException e) {
			logger.error("SQL Error adding schema.");
			logger.error(e);
		}
		return generatedId;
	}

	/**
	 * Returns all schema meta data
	 * 
	 * @return A list of SchemaMetaData beans
	 * @throws SQLException
	 *             Handled in H2DataAccessObject
	 */
	public List<SchemaMetaData> getAllSchemaMetaData() {
		List<SchemaMetaData> schemaList = new ArrayList<SchemaMetaData>();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(GET_SCHEMA_META_DATA_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			rs = ppst.executeQuery();

			while (rs.next()) {
				schemaList.add(populateSchemaMetaData(rs));
			}

		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
			try {
				if (ppst != null)
					ppst.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
		return schemaList;
	}

	/**
	 * Queries the schema_model table for a schema based on its GUID
	 * 
	 * @param guid
	 *            GUID of the schema
	 * @return Schema bean or if the schema does not exist, returns null
	 * 
	 * @throws SQLException
	 *             Handled in H2DataAccessObject
	 */
	public Schema getSchemaByGuid(String guid) {
		Schema schema = null;
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(GET_SCHEMA_DATA_SAMPLES_MAPPING, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ppst.setString(1, guid);
			rs = ppst.executeQuery();

			if (rs.next()) {
				schema = populateSchema(rs);
			} else {
				logger.warn("No schema found with guid " + guid + ".");
			}

		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
			try {
				if (ppst != null)
					ppst.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
		return schema;
	}

	/**
	 * Returns all schema meta data
	 * 
	 * @return A list of SchemaMetaData beans
	 * @throws SQLException
	 *             Handled in H2DataAccessObject
	 */
	public SchemaMetaData getSchemaMetaDataByGuid(String guid) {
		SchemaMetaData schemaMetaData = null;
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(GET_SCHEMA_META_DATA_BY_GUID, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ppst.setString(1, guid);
			rs = ppst.executeQuery();

			if (rs.next()) {
				schemaMetaData = populateSchemaMetaData(rs);
			}
		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
			try {
				if (ppst != null)
					ppst.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
		return schemaMetaData;
	}

	/**
	 * Gets the field-descriptor object.
	 * 
	 * @param guid
	 * @return
	 * @throws SQLException
	 */
	public Map<String, Profile> getSchemaFieldByGuid(String guid) {
		return h2.getH2Metrics().getFieldMappingBySchemaGuid(guid);
	}

	/**
	 * Delete a schema from the database by its guid
	 * 
	 * @param guid
	 * @throws SQLException
	 */
	public void deleteSchemaFromDeletionQueue(String guid) {
		PreparedStatement ppst = null;

		try {
			ppst = dbConnection.prepareStatement(DELETE_SCHEMA_FROM_DELETION_QUEUE);
			ppst.setString(1, guid);
			ppst.execute();
		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (ppst != null)
					ppst.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
	}

	public void deleteSchemaByGuid(String guid) {
		PreparedStatement ppst = null;

		try {
			ppst = dbConnection.prepareStatement(DELETE_SCHEMA_BY_GUID);
			ppst.setString(1, guid);
			ppst.execute();
		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (ppst != null)
					ppst.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
	}

	// Private Methods
	/**
	 * Create Schema bean based on ResultSet from a PreparedStatement
	 * 
	 * @param rs
	 *            ResultSet from SQL query
	 * @return SchemaMetaData bean
	 * 
	 * @throws SQLException
	 *             Handled in H2DataAccessObject
	 */
	private SchemaMetaData populateSchemaMetaData(ResultSet rs) throws SQLException {
		SchemaMetaData schemaMetaData = new SchemaMetaData();
		List<String> dataSampleGuids = new ArrayList<String>();

		String schemaGuid = rs.getString("s_guid");

		schemaMetaData.setSchemaModelId(rs.getInt("schema_model_id"));
		schemaMetaData.setsGuid(schemaGuid);
		schemaMetaData.setsName(rs.getString("s_name"));
		schemaMetaData.setsVersion(rs.getString("s_version"));
		schemaMetaData.setsLastUpdate(rs.getTimestamp("s_lastupdate"));
		schemaMetaData.setsDescription(rs.getString("s_description"));

		do {
			if (!rs.getString("s_guid").equals(schemaGuid)) {
				rs.previous();
				schemaMetaData.setsDataSamples(dataSampleGuids);
				return schemaMetaData;
			}
			dataSampleGuids.add(rs.getString("ds_guid"));
		} while (rs.next());

		schemaMetaData.setsDataSamples(dataSampleGuids);

		return schemaMetaData;
	}

	/**
	 * A method which populates the SchemaMetaData bean with a given result set
	 * 
	 * @param rs
	 *            ResultSet from the database call
	 * @return A built SchemaMetaData bean
	 * @throws SQLException
	 */
	private Schema populateSchema(ResultSet rs) throws SQLException {
		Schema schema = new Schema();
		List<DataSampleMetaData> dataSampleMetaData = new ArrayList<DataSampleMetaData>();

		String guid = rs.getString("s_guid");
		schema.setsGuid(guid);
		schema.setsName(rs.getString("s_name"));
		schema.setsVersion(rs.getString("s_version"));
		schema.setsLastUpdate(rs.getTimestamp("s_lastupdate"));
		schema.setsDescription(rs.getString("s_description"));
		schema.setSchemaModelId(rs.getInt("schema_model_id"));

		Map<String, Profile> profileMap = h2.getH2Metrics().getFieldMappingBySchemaGuid(guid);
		schema.setsProfile(profileMap);

		do {
			String currentResultSetSchemaGuid = rs.getString("s_guid");
			DataSampleMetaData dsmd = populateDsMetaData(rs);
			if(dsmd != null && currentResultSetSchemaGuid.equals(guid)) {
				dataSampleMetaData.add(dsmd);
			} else {
				if(!currentResultSetSchemaGuid.equals(guid)) {
					rs.previous();
				} else {
					logger.error("Data Sample Metadata not present for schema " + guid + ".");
				}
				break;
			}
		} while(rs.next());
		
		schema.setsDataSamples(dataSampleMetaData);

		return schema;
	}

	/**
	 * Populates a DataSampleMetaData bean based on the ResultSet given
	 * 
	 * @param rs
	 *            Result set from PreparedStatement
	 * @return DataSampleMetaData bean
	 * @throws SQLException
	 */
	private DataSampleMetaData populateDsMetaData(ResultSet rs) throws SQLException {
		DataSampleMetaData dsMetaData = new DataSampleMetaData();
		
		if(rs.getInt("data_sample_id") == 0) {
			return null;
		}
		dsMetaData.setDataSampleId(rs.getInt("data_sample_id"));
		dsMetaData.setDsGuid(rs.getString("ds_guid"));
		dsMetaData.setDsName(rs.getString("ds_name"));
		dsMetaData.setDsFileName(rs.getString("ds_file_name"));
		dsMetaData.setDsFileType(rs.getString("ds_file_type"));
		dsMetaData.setDsVersion(rs.getString("ds_version"));
		dsMetaData.setDsLastUpdate(rs.getTimestamp("ds_last_update"));
		dsMetaData.setDsDescription(rs.getString("ds_description"));

		return dsMetaData;
	}

	/**
	 * Inserts a record into the schema_model and schema_data_samples_mapping
	 * tables
	 * 
	 * @param schemaModelId
	 * @param name
	 * @param version
	 * @param lastUpdate
	 * @param description
	 * @param dataSamples
	 * 
	 * @return The key generated from executing the statement
	 * 
	 * @throws SQLException
	 */
	private int addSchemaAndMapping(int schemaModelId, String guid, String name, String version, Timestamp lastUpdate,
			String description, Map<String, Profile> profiles, List<DataSampleMetaData> dataSamples)
					throws SQLException {

		PreparedStatement addSchemaStatement;

		addSchemaStatement = dbConnection.prepareStatement(ADD_SCHEMA_MODEL);
		addSchemaStatement.setString(1, guid);
		addSchemaStatement.setString(2, name);
		addSchemaStatement.setString(3, version);
		addSchemaStatement.setTimestamp(4, lastUpdate);
		addSchemaStatement.setString(5, description);
		addSchemaStatement.execute();
		int schemaModelGeneratedId = h2.getGeneratedKey(addSchemaStatement);
		addSchemaStatement.close();

		for (String fieldName : profiles.keySet()) {
			Profile profile = profiles.get(fieldName);
			h2.getH2Metrics().addSchemaField(schemaModelGeneratedId, fieldName, profile);
		}

		if (dataSamples.size() == 0) {
			logger.warn("No data samples in schema object.");
		}
		for (DataSampleMetaData ds : dataSamples) {
			PreparedStatement addSchemaDataSamplesMappingStatement;
			int dataSampleId = ds.getDataSampleId();

			addSchemaDataSamplesMappingStatement = dbConnection.prepareStatement(ADD_SCHEMA_DATA_SAMPLES_MAPPING);
			addSchemaDataSamplesMappingStatement.setInt(1, schemaModelGeneratedId);
			addSchemaDataSamplesMappingStatement.setInt(2, dataSampleId);
			addSchemaDataSamplesMappingStatement.execute();
			addSchemaDataSamplesMappingStatement.close();
		}

		return schemaModelGeneratedId;
	}
}
