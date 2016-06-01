package com.deleidos.dp.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Profile;

/**
 * Data Access Object meant to communicate solely with sample data in the H2
 * database.
 * 
 * @author leegc
 * @author yoonj1
 *
 */

public class H2SampleDataAccessObject {
	private static final Logger logger = H2DataAccessObject.logger;
	private H2DataAccessObject h2;
	private Connection dbConnection;
	private final String ADD_DATA_SAMPLE = "INSERT INTO data_sample VALUES (NULL, ?, ?, ?, ?, ? ,? ,?, ?);";
	private final String QUERY_SAMPLE_NAMES = "SELECT ds_name, ds_file_type FROM data_sample ORDER BY ds_name ASC";
	private final String QUERY_SAMPLE_IDS_BY_NAME = "SELECT data_sample_id FROM data_sample WHERE ds_name = ?";
	private final String QUERY_SAMPLE_IDS_BY_GUID = "SELECT data_sample_id FROM data_sample WHERE ds_guid = ?";
	private final String ADD_GUID = "INSERT INTO guid_list VALUES (?)";
	private final String QUERY_ALL_SAMPLES = "SELECT * FROM data_sample" + " ORDER BY ds_name ASC";
	private final String QUERY_SAMPLE_BY_GUID = "SELECT * FROM data_sample WHERE data_sample.ds_guid = ?";
	private final String QUERY_SAMPLE_META_DATA_BY_GUID = "SELECT * FROM data_sample WHERE data_sample.ds_guid = ?";
	private final String QUERY_SAMPLE_FIELD_ID_BY_NAME_AND_GUID = "SELECT data_sample_field_id FROM data_sample_field"
			+ " INNER JOIN data_sample ON (data_sample_field.data_sample_id = data_sample.data_sample_id"
			+ "	AND data_sample.ds_guid = ?" + " AND data_sample_field.field_name = ?); ";
	private final String DELETE_SAMPLE_BY_GUID = "DELETE FROM data_sample WHERE ds_guid = ?";
	private final String DELETE_SAMPLE_FROM_DELETION_QUEUE = "DELETE FROM deletion_queue WHERE guid = ?;";

	public H2SampleDataAccessObject(H2DataAccessObject h2) {
		this.h2 = h2;
		dbConnection = h2.getDBConnection();
	}

	public int getSampleFieldIdBySampleGuidAndName(String sampleGuid, String fieldName) {
		PreparedStatement ppst = null;
		ResultSet rs = null;
		int id = -1;

		try {
			ppst = dbConnection.prepareStatement(QUERY_SAMPLE_FIELD_ID_BY_NAME_AND_GUID);
			ppst.setString(1, sampleGuid);
			ppst.setString(2, fieldName);
			rs = ppst.executeQuery();

			if (rs.next()) {
				id = rs.getInt("data_sample_field_id");
				if (rs.next()) {
					logger.error("More than one sample field found with " + "guid " + sampleGuid + " and key "
							+ fieldName + ".");
				}
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
		return id;
	}

	/**
	 * Gets DataSampleMetaData by guid
	 * 
	 * @param guid
	 * @return DataSampleMetaData
	 * @throws SQLException
	 */
	public DataSample getSampleByGuid(String guid) {
		DataSample ds = new DataSample();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_SAMPLE_BY_GUID);
			ppst.setString(1, guid);
			rs = ppst.executeQuery();

			if (rs.next()) {
				ds = populateDataSample(rs);
			} else {
				logger.warn("No samples found with guid " + guid);
				return null;
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
		return ds;
	}

	/**
	 * Retrieve the data samples meta data
	 * 
	 * @param guid
	 *            the guid of the sample
	 * @return the DataSampleMetaData object representing the sample
	 * @throws SQLException
	 *             exception thrown to main H2DAO
	 */
	public DataSampleMetaData getDataSampleMetaDataByGuid(String guid) {
		DataSampleMetaData dsMeta = new DataSampleMetaData();
		PreparedStatement ppst = null;
		ResultSet rs = null;
		try {
			ppst = dbConnection.prepareStatement(QUERY_SAMPLE_META_DATA_BY_GUID);
			ppst.setString(1, guid);
			rs = ppst.executeQuery();

			if (rs.next()) {
				dsMeta = populateDataSampleMetaData(rs);
			} else {
				logger.warn("No samples found with guid " + guid);
				return null;
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
		return dsMeta;
	}

	/**
	 * Gets the field-descriptor object.
	 * 
	 * @param guid
	 * @return
	 * @throws SQLException
	 */
	public Map<String, Profile> getSampleFieldByGuid(String guid) {
		return h2.getH2Metrics().getFieldMappingBySampleGuid(guid);
	}

	/**
	 * Gets a list of DataSampleMetaData for the catalog
	 * 
	 * @return List<DataSampleMetaData>
	 * @throws SQLException
	 */
	public List<DataSampleMetaData> getAllSampleMetaData() {
		List<DataSampleMetaData> dsList = new ArrayList<DataSampleMetaData>();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_ALL_SAMPLES);
			rs = ppst.executeQuery();

			while (rs.next()) {
				DataSampleMetaData dsMetaData = populateDataSampleMetaData(rs);
				dsList.add(dsMetaData);
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
		return dsList;
	}

	/**
	 * The a list of all existing sample names paired with their file type
	 * 
	 * @return a mapping of existing samples name with file types
	 * @throws SQLException
	 */
	public Map<String, String> getExistingSampleNames() {
		Map<String, String> existingSampleNames = new HashMap<String, String>();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_SAMPLE_NAMES);

			ppst.execute();
			rs = ppst.getResultSet();

			while (rs.next()) {
				existingSampleNames.put(rs.getString(1), rs.getString(2));
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
		return existingSampleNames;
	}

	/**
	 * Get a new sample name based on an existing set of sample names.
	 * Concatenates a counter to the end of the file name.
	 * 
	 * @param name
	 *            The name of the file without a path or extension
	 * @param existingSampleNames
	 * @return
	 */
	public static String generateNewSampleName(String name, Set<String> existingSampleNames) {
		String baseName = String.copyValueOf(name.toCharArray());
		if (existingSampleNames.contains(name)) {
			int sampleNumber = 1;
			String sampleName = baseName + "(" + sampleNumber + ")";
			boolean alreadyExists = false;
			do {
				if (existingSampleNames.contains(sampleName)) {
					sampleNumber++;
					sampleName = baseName + "(" + sampleNumber + ")";
					alreadyExists = true;
				} else {
					alreadyExists = false;
				}
			} while (alreadyExists);
			return sampleName;
		} else {
			return name;
		}
	}

	/**
	 * Add a data sample to the database
	 * 
	 * @param sample
	 *            the DataSample bean
	 * @return the guid of the newly added data sample
	 * @throws SQLException
	 */
	public String addSample(DataSample sample) throws SQLException {
		dbConnection.setAutoCommit(false);
		DataSample updatedBean = adjustDataSampleBean(sample);
		int dataSampleId = addSample(updatedBean.getDsGuid(), updatedBean.getDsName(), updatedBean.getDsVersion(),
				updatedBean.getDsLastUpdate(), updatedBean.getDsDescription(), updatedBean.getDsFileName(),
				updatedBean.getDsFileType(), updatedBean.getDsExtractedContentDir());
		for (String fieldName : sample.getDsProfile().keySet()) {
			Profile profile = sample.getDsProfile().get(fieldName);
			h2.getH2Metrics().addSampleField(dataSampleId, fieldName, profile);

		}

		dbConnection.setAutoCommit(true);
		return sample.getDsGuid();
	}

	// TODO
	public void addSampleMetaData(DataSampleMetaData sampleMetaData) {
		addSample(sampleMetaData.getDsGuid(), sampleMetaData.getDsName(), sampleMetaData.getDsVersion(),
				sampleMetaData.getDsLastUpdate(), sampleMetaData.getDsDescription(), sampleMetaData.getDsFileName(),
				sampleMetaData.getDsFileType(), sampleMetaData.getDsExtractedContentDir());
	}

	/**
	 * Delete a Data Sample from the database by its GUID
	 * 
	 * @param guid
	 * @throws SQLException
	 */
	public void deleteSchemaFromDeletionQueue(String guid) {
		PreparedStatement ppst = null;
		try {
			ppst = dbConnection.prepareStatement(DELETE_SAMPLE_FROM_DELETION_QUEUE);
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

	/**
	 * Deletes a Data Sample given its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Data Sample
	 * @throws SQLException
	 */
	public void deleteSampleByGuid(String guid) {
		PreparedStatement ppst = null;

		try {
			ppst = dbConnection.prepareStatement(DELETE_SAMPLE_BY_GUID);
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
	 * Populates a DataSample bean based on the ResultSet given
	 * 
	 * @param rs
	 *            Result set from PreparedStatement
	 * @return DataSampleMetaData bean
	 * @throws SQLException
	 */
	private DataSample populateDataSample(ResultSet rs) throws SQLException {
		DataSample dataSample = new DataSample();

		dataSample.setDataSampleId(rs.getInt("data_sample_id"));
		dataSample.setDsGuid(rs.getString("ds_guid"));
		dataSample.setDsName(rs.getString("ds_name"));
		dataSample.setDsFileName(rs.getString("ds_file_name"));
		dataSample.setDsFileType(rs.getString("ds_file_type"));
		dataSample.setDsVersion(rs.getString("ds_version"));
		dataSample.setDsLastUpdate(rs.getTimestamp("ds_last_update"));
		dataSample.setDsDescription(rs.getString("ds_description"));
		dataSample.setDsExtractedContentDir(rs.getString("ds_extracted_content_dir"));

		Map<String, Profile> dsProfile = h2.getH2Metrics().getFieldMappingBySampleGuid(dataSample.getDsGuid());
		dataSample.setDsProfile(dsProfile);
		return dataSample;
	}

	/**
	 * Populates a DataSampleMetaData bean based on the ResultSet given
	 * 
	 * @param rs
	 *            Result set from PreparedStatement
	 * @return DataSampleMetaData bean
	 * @throws SQLException
	 */
	private DataSampleMetaData populateDataSampleMetaData(ResultSet rs) throws SQLException {
		DataSampleMetaData dsMetaData = new DataSampleMetaData();

		dsMetaData = new DataSampleMetaData();
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
	 * Inserts a record into the data_sample table
	 * 
	 * @param name
	 * @param version
	 * @param timestamp
	 * @param description
	 * @param fileName
	 * @param fileType
	 * @return
	 * @throws SQLException
	 */
	private int addSample(String guid, String name, String version, Timestamp timestamp, String description,
			String fileName, String fileType, String extractedContentDir) {
		PreparedStatement ppst = null;
		int dataSampleId = -1;

		try {
			ppst = dbConnection.prepareStatement(ADD_DATA_SAMPLE);
			ppst.setString(1, guid);
			ppst.setString(2, name);
			ppst.setString(3, fileName);
			ppst.setString(4, fileType);
			ppst.setString(5, version);
			ppst.setTimestamp(6, timestamp);
			ppst.setString(7, description);
			ppst.setString(8, extractedContentDir);
			ppst.execute();

			dataSampleId = h2.getGeneratedKey(ppst);

			PreparedStatement ppst2 = dbConnection.prepareStatement(ADD_GUID);
			ppst2.setString(1, guid);
			ppst2.execute();
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
		return dataSampleId;
	}

	private DataSample adjustDataSampleBean(DataSample dataSample) {
		String sourceNameNoPath = dataSample.getDsFileName();
		int slashIndex = sourceNameNoPath.lastIndexOf("/");
		if (slashIndex >= 0 && slashIndex < sourceNameNoPath.length() - 1) {
			sourceNameNoPath = sourceNameNoPath.substring(slashIndex + 1);
		}
		int backSlashIndex = sourceNameNoPath.lastIndexOf("\\");
		if (backSlashIndex > 0 && backSlashIndex < sourceNameNoPath.length() - 1) {
			sourceNameNoPath = sourceNameNoPath.substring(backSlashIndex + 1);
		}

		String sourceNameNoExtension = sourceNameNoPath;
		if (sourceNameNoPath.contains(".")) {
			sourceNameNoExtension = sourceNameNoExtension.substring(0, sourceNameNoPath.lastIndexOf("."));
		}

		Map<String, String> existingSampleNames = h2.getExistingSampleNames();
		while (existingSampleNames.containsKey(sourceNameNoExtension)) {
			sourceNameNoExtension = generateNewSampleName(sourceNameNoExtension, existingSampleNames.keySet());
		}

		dataSample.setDsFileName(sourceNameNoPath);
		dataSample.setDsName(sourceNameNoExtension);

		return dataSample;
	}

	/*
	 * private int addSample(String sourceName, String guid, String mediaType,
	 * String version, String description, Timestamp lastUpdate) throws
	 * SQLException { PreparedStatement ppst =
	 * dbConnection.prepareStatement(QUERY_SAMPLE_IDS_BY_GUID);
	 * ppst.setString(1, guid); ppst.execute(); ResultSet pprs =
	 * ppst.getResultSet(); int dataSourceId;
	 * 
	 * if (pprs.next()) { dataSourceId = ppst.getResultSet().getInt(1); } else {
	 * dbConnection.setAutoCommit(false); PreparedStatement ppst2 =
	 * dbConnection.prepareStatement(ADD_DATA_SAMPLE); ppst2.setString(1, guid);
	 * String sourceNameNoPath = sourceName; int slashIndex =
	 * sourceNameNoPath.lastIndexOf("/"); if (slashIndex >= 0 && slashIndex <
	 * sourceNameNoPath.length() - 1) { sourceNameNoPath =
	 * sourceNameNoPath.substring(slashIndex + 1); } int backSlashIndex =
	 * sourceNameNoPath.lastIndexOf("\\"); if (backSlashIndex > 0 &&
	 * backSlashIndex < sourceNameNoPath.length() - 1) { sourceNameNoPath =
	 * sourceNameNoPath.substring(backSlashIndex + 1); }
	 * 
	 * String sourceNameNoExtension = sourceNameNoPath; if
	 * (sourceNameNoPath.contains(".")) { sourceNameNoExtension =
	 * sourceNameNoExtension.substring(0, sourceNameNoPath.lastIndexOf(".")); }
	 * 
	 * Map<String, String> existingSampleNames = h2.getExistingSampleNames();
	 * while(existingSampleNames.containsKey(sourceNameNoExtension)) {
	 * sourceNameNoExtension = generateNewSampleName(sourceNameNoExtension,
	 * existingSampleNames.keySet()); }
	 * 
	 * ppst2.setString(2, sourceNameNoExtension); ppst2.setString(3,
	 * sourceNameNoPath); ppst2.setString(4, mediaType); ppst2.setString(5,
	 * version); ppst2.setTimestamp(6, lastUpdate); ppst2.setString(7,
	 * description); ppst2.execute();
	 * 
	 * dataSourceId = h2.getGeneratedKey(ppst2); ppst2 =
	 * dbConnection.prepareStatement(ADD_GUID); ppst2.setString(1, guid);
	 * ppst2.execute();
	 * 
	 * dbConnection.commit(); dbConnection.setAutoCommit(true); } ppst.close();
	 * return dataSourceId; }
	 */
}
