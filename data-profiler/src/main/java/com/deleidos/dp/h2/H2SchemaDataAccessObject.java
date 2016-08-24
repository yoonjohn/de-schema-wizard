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
import com.deleidos.dp.exceptions.DataAccessException;

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

	/**************************** DATABASE QUERIES ****************************/
	private final String ADD_SCHEMA_MODEL = "INSERT INTO schema_model VALUES (NULL, ?, ?, ?, ?, ?, ?, ?);";
	private final String ADD_SCHEMA_DATA_SAMPLES_MAPPING = "INSERT INTO schema_data_samples_mapping VALUES (?, ?);";
	private final String GET_SCHEMA_META_DATA_LIST = "SELECT * FROM schema_model "
			+ " INNER JOIN schema_data_samples_mapping ON (schema_model.schema_model_id = schema_data_samples_mapping.schema_model_id)"
			+ " INNER JOIN data_sample ON (schema_data_samples_mapping.data_sample_id = data_sample.data_sample_id)"
			+ " ORDER BY schema_model.s_name ASC;";
	private final String GET_SCHEMA_META_DATA_BY_GUID = "SELECT * FROM schema_model"
			+ " INNER JOIN schema_data_samples_mapping ON (schema_data_samples_mapping.schema_model_id = schema_model.schema_model_id)"
			+ " INNER JOIN data_sample ON (data_sample.data_sample_id = schema_data_samples_mapping.data_sample_id)"
			+ " WHERE schema_model.s_guid = ?;";
	private final String UPDATE_DATA_SAMPLE_NAME_BY_GUID = "UPDATE data_sample SET ds_name = ? WHERE ds_guid = ?;";

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
	}

	/**
	 * Get sample guids associated with a given schema guid
	 * 
	 * @param schemaGuid
	 *            the desired schema guid
	 * @return a list of schema guids associated with the given schema
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public List<String> getSampleGuidsFromSchemaGuid(Connection dbConnection, String schemaGuid) throws DataAccessException, SQLException {
		List<String> dataSampleGuids = new ArrayList<String>();
		PreparedStatement getMappingsStatement = null;

		getMappingsStatement = dbConnection.prepareStatement(GET_SCHEMA_DATA_SAMPLES_MAPPING);
		getMappingsStatement.setString(1, schemaGuid);
		ResultSet rs = getMappingsStatement.executeQuery();

		while (rs.next()) {
			String sampleGuid = rs.getString("ds_guid");
			dataSampleGuids.add(sampleGuid);
		}
		getMappingsStatement.close();
		return dataSampleGuids;
	}

	/**
	 * Adds a schema to the H2 database // TODO Add profile and data samples
	 * 
	 * @param schemaBean
	 * @return
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public int addSchema(Connection dbConnection, Schema schemaBean) throws DataAccessException, SQLException {
		dbConnection.setAutoCommit(false);
		int generatedId = -1;
		generatedId = addSchemaAndMapping(dbConnection, schemaBean.getSchemaModelId(), schemaBean.getsGuid(),
				schemaBean.getsName(), schemaBean.getsVersion(), schemaBean.getsLastUpdate(),
				schemaBean.getsDescription(), schemaBean.getRecordsParsedCount(), schemaBean.getsDomainName(),
				schemaBean.getsProfile(), schemaBean.getsDataSamples());
		dbConnection.commit();
		dbConnection.setAutoCommit(true);
		return generatedId;
	}

	/**
	 * Returns all schema meta data
	 * 
	 * @return A list of SchemaMetaData beans
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public List<SchemaMetaData> getAllSchemaMetaData(Connection dbConnection) throws DataAccessException, SQLException {
		List<SchemaMetaData> schemaList = new ArrayList<SchemaMetaData>();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		ppst = dbConnection.prepareStatement(GET_SCHEMA_META_DATA_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		rs = ppst.executeQuery();

		while (rs.next()) {
			schemaList.add(populateSchemaMetaData(dbConnection, rs));
		}

		ppst.close();
		return schemaList;
	}

	/**
	 * Queries the schema_model table for a schema based on its GUID
	 * 
	 * @param guid
	 *            GUID of the schema
	 * @return Schema bean or if the schema does not exist, returns null
	 * @throws DataAccessException
	 * @throws SQLException 
	 * 
	 */
	public Schema getSchemaByGuid(Connection dbConnection, String guid) throws DataAccessException, SQLException {
		Schema schema = null;
		PreparedStatement ppst = null;
		ResultSet rs = null;

		ppst = dbConnection.prepareStatement(GET_SCHEMA_DATA_SAMPLES_MAPPING, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		ppst.setString(1, guid);
		rs = ppst.executeQuery();

		if (rs.next()) {
			schema = populateSchema(dbConnection, rs);
		} else if (guid != null) {
			logger.debug("No schema found with guid " + guid + ".");
		} else {
			logger.debug("Null guid pass to get schema call.");
		}

		ppst.close();

		return schema;
	}

	/**
	 * Returns all schema meta data
	 * 
	 * @return A list of SchemaMetaData beans
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public SchemaMetaData getSchemaMetaDataByGuid(Connection dbConnection, String guid) throws DataAccessException, SQLException {
		SchemaMetaData schemaMetaData = null;
		PreparedStatement ppst = null;
		ResultSet rs = null;

		ppst = dbConnection.prepareStatement(GET_SCHEMA_META_DATA_BY_GUID, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		ppst.setString(1, guid);
		rs = ppst.executeQuery();

		if (rs.next()) {
			schemaMetaData = populateSchemaMetaData(dbConnection, rs);
		}
		ppst.close();

		return schemaMetaData;
	}

	/**
	 * Gets the field-descriptor object.
	 * 
	 * @param guid
	 * @return
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public Map<String, Profile> getSchemaFieldByGuid(Connection dbConnection, String guid) throws DataAccessException, SQLException {
		return h2.getH2Metrics().getFieldMappingBySchemaGuid(dbConnection, guid);
	}

	/**
	 * Delete a schema from the database by its guid
	 * 
	 * @param guid
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public void deleteSchemaFromDeletionQueue(Connection dbConnection, String guid) throws DataAccessException, SQLException {
		PreparedStatement ppst = null;

		ppst = dbConnection.prepareStatement(DELETE_SCHEMA_FROM_DELETION_QUEUE);
		ppst.setString(1, guid);
		ppst.execute();
		ppst.close();
	}

	public void deleteSchemaByGuid(Connection dbConnection, String guid) throws DataAccessException, SQLException {
		PreparedStatement ppst = null;

		ppst = dbConnection.prepareStatement(DELETE_SCHEMA_BY_GUID);
		ppst.setString(1, guid);
		ppst.execute();
		ppst.close();
	}

	// Private Methods
	/**
	 * Create Schema bean based on ResultSet from a PreparedStatement
	 * 
	 * @param rs
	 *            ResultSet from SQL query
	 * @return SchemaMetaData bean
	 * @throws DataAccessException
	 * @throws SQLException 
	 * 
	 */
	private SchemaMetaData populateSchemaMetaData(Connection dbConnection, ResultSet rs) throws DataAccessException, SQLException {
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
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	private Schema populateSchema(Connection dbConnection, ResultSet rs) throws DataAccessException, SQLException {
		Schema schema = new Schema();
		List<DataSampleMetaData> dataSampleMetaData = new ArrayList<DataSampleMetaData>();

		String guid = rs.getString("s_guid");
		schema.setsGuid(guid);
		schema.setsName(rs.getString("s_name"));
		schema.setsVersion(rs.getString("s_version"));
		schema.setsLastUpdate(rs.getTimestamp("s_lastupdate"));
		schema.setsDescription(rs.getString("s_description"));
		schema.setSchemaModelId(rs.getInt("schema_model_id"));
		schema.setRecordsParsedCount(rs.getInt("s_sum_sample_records"));
		schema.setsDomainName(rs.getString("s_domain_name"));

		Map<String, Profile> profileMap = h2.getH2Metrics().getFieldMappingBySchemaGuid(dbConnection, guid);
		schema.setsProfile(profileMap);

		do {
			String currentResultSetSchemaGuid = rs.getString("s_guid");
			DataSampleMetaData dsmd = populateDsMetaData(dbConnection, rs);
			if (dsmd != null && currentResultSetSchemaGuid.equals(guid)) {
				dataSampleMetaData.add(dsmd);
			} else {
				if (!currentResultSetSchemaGuid.equals(guid)) {
					rs.previous();
				} else {
					logger.error("Data Sample Metadata not present for schema " + guid + ".");
				}
				break;
			}
		} while (rs.next());

		schema.setsDataSamples(dataSampleMetaData);

		return schema;

	}

	/**
	 * Populates a DataSampleMetaData bean based on the ResultSet given
	 * 
	 * @param rs
	 *            Result set from PreparedStatement
	 * @return DataSampleMetaData bean
	 * @throws DataAccessException 
	 * @throws SQLException 
	 */
	private DataSampleMetaData populateDsMetaData(Connection dbConnection, ResultSet rs) throws DataAccessException, SQLException {
		DataSampleMetaData dsMetaData = new DataSampleMetaData();

		if (rs.getInt("data_sample_id") == 0) {
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
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	private int addSchemaAndMapping(Connection dbConnection, int schemaModelId, String guid, String name, String version, Timestamp lastUpdate,
			String description, int sumOfSampleRecords, String domainName, Map<String, Profile> profiles,
			List<DataSampleMetaData> dataSamples) throws DataAccessException, SQLException {

		PreparedStatement addSchemaStatement = dbConnection.prepareStatement(ADD_SCHEMA_MODEL);
		addSchemaStatement.setString(1, guid);
		addSchemaStatement.setString(2, name);
		addSchemaStatement.setString(3, version);
		addSchemaStatement.setTimestamp(4, lastUpdate);
		addSchemaStatement.setString(5, description);
		addSchemaStatement.setInt(6, sumOfSampleRecords);
		addSchemaStatement.setString(7, domainName);
		addSchemaStatement.execute();
		int schemaModelGeneratedId = h2.getGeneratedKey(addSchemaStatement);
		addSchemaStatement.close();

		for (String fieldName : profiles.keySet()) {
			Profile profile = profiles.get(fieldName);
			h2.getH2Metrics().addSchemaField(dbConnection, schemaModelGeneratedId, fieldName, profile);
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


			PreparedStatement updateDataSampleName;
			String dataSampleGuid = ds.getDsGuid();
			String dataSampleName = ds.getDsName();

			updateDataSampleName = dbConnection.prepareStatement(UPDATE_DATA_SAMPLE_NAME_BY_GUID);
			updateDataSampleName.setString(1, dataSampleName);
			updateDataSampleName.setString(2, dataSampleGuid);
			updateDataSampleName.execute();
			updateDataSampleName.close();
		}

		return schemaModelGeneratedId;

	}
}
