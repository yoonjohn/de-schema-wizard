package com.deleidos.dp.h2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.deleidos.dp.beans.AliasNameDetails;
import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.histogram.AbstractBucketList;
import com.deleidos.dp.histogram.ByteBucket;
import com.deleidos.dp.histogram.ByteBucketList;
import com.deleidos.dp.histogram.CharacterBucket;
import com.deleidos.dp.histogram.CharacterBucketList;
import com.deleidos.dp.histogram.NumberBucket;
import com.deleidos.dp.histogram.NumberBucketList;
import com.deleidos.dp.histogram.TermBucket;
import com.deleidos.dp.histogram.TermBucketList;
import com.deleidos.dp.interpretation.AbstractJavaInterpretation;

/**
 * Data Access Object meant to handle external communications with the H2
 * database.
 * 
 * @author leegc
 *
 */
public class H2MetricsDataAccessObject {
	public static final Logger logger = H2DataAccessObject.logger;
	private H2DataAccessObject h2;
	private Connection dbConnection;
	private static final String schema_field = "schema_field";
	private static final String data_sample_field = "data_sample_field";
	private static final String dot = ".";
	private static final String field_name_key = "field_name";
	private static final String field_order_key = "field_order";
	private static final String num_distinct_key = "num_distinct";
	private static final String count_key = "count";
	private static final String walking_square_sum_key = "walking_square_sum";
	private static final String walking_sum_key = "walking_sum";
	private static final String presence_key = "presence";
	private static final String data_sample_id_key = "data_sample_id";
	private static final String number_histogram_key = "number_histogram";
	private static final String string_character_histogram_key = "string_character_histogram";
	private static final String string_term_histogram_key = "string_term_histogram";
	private static final String binary_character_histogram_key = "binary_character_histogram";
	private static final String detail_type_id_key = "detail_type_id";
	private static final String interpretation_id_key = "interpretation_id";
	private static final String number_min_key = "number_min";
	private static final String number_max_key = "number_max";
	private static final String number_average_key = "number_average";
	private static final String number_std_dev_key = "number_std_dev";
	private static final String string_min_length_key = "string_min_length";
	private static final String string_max_length_key = "string_max_length";
	private static final String string_average_length_key = "string_average_length";
	private static final String string_std_dev_length_key = "string_std_dev_length";
	private static final String binary_mime_type_key = "binary_mime_type";
	private static final String binary_length_key = "binary_length";
	private static final String binary_hash_key = "binary_hash";
	private static final String binary_entropy_key = "binary_entropy";
	private static final String interpretation_name_key = "i_name";

	private static final String ADD_SAMPLE_NUMBER_FIELD = "INSERT INTO data_sample_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	data_sample_id, number_histogram, detail_type_id, interpretation_id,"
			+ "	number_min, number_max, number_average, number_std_dev)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_SCHEMA_NUMBER_FIELD = "INSERT INTO schema_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	schema_model_id, number_histogram, detail_type_id, interpretation_id,"
			+ "	number_min, number_max, number_average, number_std_dev)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_SAMPLE_STRING_FIELD = "INSERT INTO data_sample_field("
			+ " field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence, "
			+ " data_sample_id, string_character_histogram, string_term_histogram, detail_type_id, interpretation_id,"
			+ "	string_min_length, string_max_length, string_average_length, string_std_dev_length) "
			+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?); ";

	private static final String ADD_SCHEMA_STRING_FIELD = "INSERT INTO schema_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	schema_model_id, string_character_histogram, string_term_histogram, detail_type_id, interpretation_id,"
			+ "	string_min_length, string_max_length, string_average_length, string_std_dev_length)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?); ";

	private static final String ADD_SAMPLE_BINARY_FIELD = "INSERT INTO data_sample_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	data_sample_id, binary_character_histogram, detail_type_id, interpretation_id,"
			+ "	binary_mime_type, binary_length, binary_hash, binary_entropy)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_SCHEMA_BINARY_FIELD = "INSERT INTO schema_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	schema_model_id, binary_character_histogram, detail_type_id, interpretation_id,"
			+ "	binary_mime_type, binary_length, binary_hash, binary_entropy)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_EMPTY_SCHEMA_FIELD = "INSERT INTO schema_field("
			+ " schema_model_id, field_name, detail_type_id, presence) VALUES (?, ?, ?, ?); ";

	private static final String QUERY_METRICS_BY_SAMPLE_GUID = "SELECT * FROM data_sample_field "
			+ "	INNER JOIN data_sample ON (data_sample.ds_guid = ? "
			+ "	AND data_sample_field.data_sample_id = data_sample.data_sample_id) "
			+ " INNER JOIN interpretation ON (interpretation.interpretation_id = data_sample_field.interpretation_id)"
			+ " ORDER BY data_sample_field.field_name; ";

	private static final String QUERY_METRICS_BY_SCHEMA_GUID = "SELECT * FROM schema_field "
			+ "	INNER JOIN schema_model ON (schema_model.s_guid = ? "
			+ "	AND schema_field.schema_model_id = schema_model.schema_model_id)"
			+ "	INNER JOIN interpretation ON (interpretation.interpretation_id = schema_field.interpretation_id)"
			+ " INNER JOIN schema_alias_mapping ON (schema_field.schema_field_id = schema_alias_mapping.schema_field_id)"
			+ " INNER JOIN data_sample_field ON (data_sample_field.data_sample_field_id = schema_alias_mapping.data_sample_field_id)"
			+ " INNER JOIN data_sample ON (data_sample.data_sample_id = data_sample_field.data_sample_id)"
			+ " ORDER BY schema_field.field_name; ";

	private static final String QUERY_EMPTY_SCHEMA_METRICS_BY_SCHEMA_GUID = "SELECT * FROM schema_field"
			+ " INNER JOIN schema_model ON (schema_model.s_guid = ? AND schema_field.schema_model_id = schema_model.schema_model_id) "
			+ " WHERE schema_field.presence < 0; ";

	private static final String QUERY_REGION_HISTOGRAM_BY_PARENT_HISTOGRAM_ID = "SELECT b_definition, b_count, "
			+ "region_histogram.longitude_key_for_region_histograms, region_histogram.latitude_key_for_region_histograms "
			+ "FROM bucket " + "INNER JOIN histogram AS base " + "ON (base.histogram_id = ?)"
			+ "INNER JOIN histogram AS region_histogram "
			+ "ON (base.histogram_id = region_histogram.base_histogram_id_for_region_histograms "
			+ "AND bucket.histogram_id = region_histogram.histogram_id);";

	private static final String QUERY_BUCKETS_BY_HISTOGRAM_ID = "SELECT * FROM bucket WHERE histogram_id = ? ORDER BY b_order ASC;";
	private static final String ADD_HISTOGRAM = "INSERT INTO histogram VALUES (NULL, ?, ?, ?)";
	private static final String QUERY_INTERPRETATION_BY_NAME = "SELECT * FROM interpretation WHERE (i_name = ?);";
	private static final String ADD_INTERPRETATION = "INSERT INTO interpretation (interpretation_id, i_name) VALUES (NULL, ?);";
	private static final String ADD_INTERPRETATION_MAPPING = "INSERT INTO interpretation_field_mapping VALUES (?, ?, ?);";
	private static final String ADD_FIELD = "INSERT INTO field(data_sample_id, f_name) VALUES (?, ?);";
	private static final String ADD_SCHEMA_FIELD = "INSERT INTO schema_field VALUES (NULL, ?, ?, ?, 0);";
	private static final String ADD_ALIAS_NAME = "INSERT INTO schema_alias_mapping VALUES (?, ?); ";

	public H2MetricsDataAccessObject(H2DataAccessObject h2) {
		this.h2 = h2;
		dbConnection = h2.getDBConnection();
	}

	/**
	 * Add a sample field to the database
	 * 
	 * @param data_sample_id
	 *            the required data sample Id
	 * @param fieldName
	 *            the name of the field being added
	 * @param fieldProfile
	 *            the profile of the field
	 * @return the generated key of the newly inserted field
	 * @throws SQLException
	 *             exception thrown to main H2DataAccessObject
	 */
	public int addSampleField(int data_sample_id, String fieldName, Profile fieldProfile) {
		int fieldId = -1;
		PreparedStatement ppst = null;

		try {
			if (fieldProfile.getDetail().isNumberDetail()) {
				ppst = dbConnection.prepareStatement(ADD_SAMPLE_NUMBER_FIELD);
				fieldId = addNumberField(ppst, data_sample_id, fieldName, fieldProfile);
			} else if (fieldProfile.getDetail().isStringDetail()) {
				ppst = dbConnection.prepareStatement(ADD_SAMPLE_STRING_FIELD);
				fieldId = addStringField(ppst, data_sample_id, fieldName, fieldProfile);
			} else if (fieldProfile.getDetail().isBinaryDetail()) {
				logger.error("Detected as binary.");
				ppst = dbConnection.prepareStatement(ADD_SAMPLE_BINARY_FIELD);
				fieldId = addBinaryField(ppst, data_sample_id, fieldName, fieldProfile);
			} else {
				logger.error("Sample field \"" + fieldName + "\" not detected as number, string, or binary!");
				return -1;
			}
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
		return fieldId;
	}

	/**
	 * Add a schema field to the database
	 * 
	 * @param schema_model_id
	 *            the database ID of the schema
	 * @param fieldName
	 *            the name of the field in the schema
	 * @param fieldProfile
	 *            the profile of the schema
	 * @return the generated key of the field
	 * @throws SQLException
	 *             exception thrown to main H2DAO
	 */
	public int addSchemaField(int schema_model_id, String fieldName, Profile fieldProfile) {
		int fieldId = -1;
		PreparedStatement ppst = null;

		try {
			if (Float.floatToRawIntBits(fieldProfile.getPresence()) < 0) {
				ppst = dbConnection.prepareStatement(ADD_EMPTY_SCHEMA_FIELD);
				ppst.setInt(1, schema_model_id);
				ppst.setString(2, fieldName);
				ppst.setInt(3, DetailType.fromString(fieldProfile.getDetail().getDetailType()).getIndex());
				ppst.setDouble(4, fieldProfile.getPresence());
				ppst.execute();
				int id = h2.getGeneratedKey(ppst);
				ppst.close();
				return id;
			} else if (fieldProfile.getDetail().isNumberDetail()) {
				ppst = dbConnection.prepareStatement(ADD_SCHEMA_NUMBER_FIELD);
				fieldId = addNumberField(ppst, schema_model_id, fieldName, fieldProfile);
			} else if (fieldProfile.getDetail().isStringDetail()) {
				ppst = dbConnection.prepareStatement(ADD_SCHEMA_STRING_FIELD);
				fieldId = addStringField(ppst, schema_model_id, fieldName, fieldProfile);
			} else if (fieldProfile.getDetail().isBinaryDetail()) {
				logger.error("Detected as binary.");
				ppst = dbConnection.prepareStatement(ADD_SCHEMA_BINARY_FIELD);
				fieldId = addBinaryField(ppst, schema_model_id, fieldName, fieldProfile);
			} else {
				logger.error("Schema field \"" + fieldName + "\" not detected as number, string, or binary!");
				return -1;
			}
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

		addAliasNames(fieldId, fieldProfile.getAliasNames());

		return fieldId;
	}

	private void addAliasNames(int schemaFieldId, List<AliasNameDetails> aliasNames) {
		PreparedStatement ppst = null;

		try {
			for (AliasNameDetails and : aliasNames) {
				int sampleFieldId = H2DataAccessObject.h2Samples.getSampleFieldIdBySampleGuidAndName(and.getDsGuid(),
						and.getAliasName());
				if (sampleFieldId <= 0) {
					logger.error("Not adding alias name.");
				} else {
					ppst = dbConnection.prepareStatement(ADD_ALIAS_NAME);
					ppst.setInt(1, schemaFieldId);
					ppst.setInt(2, sampleFieldId);
					ppst.execute();
				}
			}
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
	 * Get the field mapping of a desired sample
	 * 
	 * @param sampleGuid
	 *            the guid of the desired sample
	 * @return a mapping of all fields associated with this sample
	 * @throws SQLException
	 *             exception thrown to main H2DAO
	 */
	public Map<String, Profile> getFieldMappingBySampleGuid(String sampleGuid) {
		Map<String, Profile> fieldMapping = new HashMap<String, Profile>();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_METRICS_BY_SAMPLE_GUID, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ppst.setString(1, sampleGuid);
			rs = ppst.executeQuery();
			if (rs.next()) {
				do {
					putResultSetProfileInFieldMapping(fieldMapping, rs, data_sample_field);
				} while (rs.next());
			} else {
				logger.warn("Empty result set from sample guid: " + sampleGuid);
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
		return fieldMapping;
	}

	/**
	 * Get the field mapping of a desired schema
	 * 
	 * @param schemaGuid
	 *            the guid of the desired schema
	 * @return a mapping of all fields associated with this schema
	 * @throws SQLException
	 *             exception thrown to main H2DAO
	 */
	public Map<String, Profile> getFieldMappingBySchemaGuid(String schemaGuid) {
		Map<String, Profile> fieldMapping = new HashMap<String, Profile>();
		// h2.queryWithOutput("SELECT * from schema_field; ");
		PreparedStatement ppst = null;
		ResultSet rs = null;
		ResultSet rs2 = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_METRICS_BY_SCHEMA_GUID, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ppst.setString(1, schemaGuid);
			rs = ppst.executeQuery();
			if (rs.next()) {
				do {
					putResultSetProfileInFieldMapping(fieldMapping, rs, schema_field);
				} while (rs.next());
			} else {
				logger.warn("Empty result set from schema guid: " + schemaGuid);
			}

			PreparedStatement ppst2 = dbConnection.prepareStatement(QUERY_EMPTY_SCHEMA_METRICS_BY_SCHEMA_GUID,
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ppst2.setString(1, schemaGuid);
			rs2 = ppst2.executeQuery();
			if (rs2.next()) {
				do {
					putEmptyMetricsInProfileFieldMapping(fieldMapping, rs2);
				} while (rs2.next());
			} else {
				logger.info("No manually created fields for schema with guid :" + schemaGuid + ".");
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
				if (rs2 != null)
					rs2.close();
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
		return fieldMapping;
	}

	private void putEmptyMetricsInProfileFieldMapping(Map<String, Profile> fieldMapping, ResultSet rs2) throws SQLException {
		String fieldName = rs2.getString(field_name_key);
		float presence = rs2.getFloat(presence_key);
		int detailIndex = rs2.getInt(detail_type_id_key);
		DetailType detailType = DetailType.getTypeByIndex(detailIndex);
		MainType mainType = detailType.getMainType();
		Profile profile = new Profile();
		Detail detail = null;
		switch (mainType) {
		case NUMBER: {
			detail = new NumberDetail();
			break;
		}
		case STRING: {
			detail = new StringDetail();
			break;
		}
		case BINARY: {
			detail = new BinaryDetail();
			break;
		}
		default: {
			logger.error("Field " + fieldName + " not found to be number, string, or binary.");
			return;
		}
		}
		profile.setMainType(mainType.toString());
		profile.setPresence(presence);
		detail.setDetailType(detailType.toString());
		fieldMapping.put(fieldName, profile);
	}

	private int addNumberField(PreparedStatement ppst, int data_sample_or_schema_id, String fieldName,
			Profile profile) {
		NumberDetail nDetail = (NumberDetail) profile.getDetail();
		String field_order = null, count = null, walking_square_sum = null, walking_sum = null, number_min = null,
				number_max = null, number_average = null;
		int number_histogram = -1, detail_type_id = -1, interpretation_id = -1;
		float presence = -1.0f;
		long num_distinct = -1;
		double number_std_dev = -1.0;
		int generatedKey = -1;

		// parameter fieldName
		field_order = null;
		num_distinct = nDetail.getNumDistinctValues();
		count = nDetail.getWalkingCount().toString();
		walking_square_sum = nDetail.getWalkingSquareSum().toString();
		walking_sum = nDetail.getWalkingSum().toString();
		presence = profile.getPresence();
		// parameter data_sample_id
		number_histogram = insertHistogram(nDetail.getFreqHistogram());
		detail_type_id = DetailType.fromString(nDetail.getDetailType()).getIndex();
		interpretation_id = addInterpretation(profile.getInterpretation());
		number_min = nDetail.getMin().toString();
		number_max = nDetail.getMax().toString();
		number_average = nDetail.getAverage().toString();
		number_std_dev = nDetail.getStdDev();

		try {
			ppst.setString(1, fieldName);
			ppst.setNull(2, Types.VARCHAR);
			ppst.setLong(3, num_distinct);
			ppst.setString(4, count);
			ppst.setString(5, walking_square_sum);
			ppst.setString(6, walking_sum);
			ppst.setFloat(7, presence);
			ppst.setInt(8, data_sample_or_schema_id);
			ppst.setInt(9, number_histogram);
			ppst.setInt(10, detail_type_id);
			ppst.setInt(11, interpretation_id);
			ppst.setString(12, number_min);
			ppst.setString(13, number_max);
			ppst.setString(14, number_average);
			ppst.setDouble(15, number_std_dev);

			ppst.execute();

			generatedKey = h2.getGeneratedKey(ppst);
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

		return generatedKey;

	}

	private int addStringField(PreparedStatement ppst, int data_sample_or_schema_id, String fieldName,
			Profile profile) {
		StringDetail sDetail = (StringDetail) profile.getDetail();
		int generatedKey = -1;

		// parameter fieldName
		String field_order = null;
		long num_distinct = sDetail.getNumDistinctValues();
		String count = sDetail.getWalkingCount().toString();
		String walking_square_sum = sDetail.getWalkingSquareSum().toString();
		String walking_sum = sDetail.getWalkingSum().toString();
		float presence = profile.getPresence();
		// parameter data_sample_id
		int string_character_histogram = insertHistogram(sDetail.getCharFreqHistogram());
		int string_term_histogram = insertHistogram(sDetail.getTermFreqHistogram());
		int detail_type_id = DetailType.fromString(sDetail.getDetailType()).getIndex();
		int interpretation_id = addInterpretation(profile.getInterpretation());
		int string_min_length = sDetail.getMinLength();
		int string_max_length = sDetail.getMaxLength();
		double string_average_length = sDetail.getAverageLength();
		double string_std_dev_length = sDetail.getStdDevLength();

		try {
			ppst.setString(1, fieldName);
			ppst.setNull(2, Types.VARCHAR);
			ppst.setLong(3, num_distinct);
			ppst.setString(4, count);
			ppst.setString(5, walking_square_sum);
			ppst.setString(6, walking_sum);
			ppst.setFloat(7, presence);
			ppst.setInt(8, data_sample_or_schema_id);
			ppst.setInt(9, string_character_histogram);
			ppst.setInt(10, string_term_histogram);
			ppst.setInt(11, detail_type_id);
			ppst.setInt(12, interpretation_id);
			ppst.setInt(13, string_min_length);
			ppst.setInt(14, string_max_length);
			ppst.setDouble(15, string_average_length);
			ppst.setDouble(16, string_std_dev_length);

			ppst.execute();

			generatedKey = h2.getGeneratedKey(ppst);
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

		return generatedKey;
	}

	private int addBinaryField(PreparedStatement ppst, int data_sample_or_schema_id, String fieldName,
			Profile profile) {

		logger.error("Received binary field.  Untested!");

		BinaryDetail bDetail = (BinaryDetail) profile.getDetail();
		int generatedKey = -1;

		// parameter fieldName
		String field_order = null;
		long num_distinct = bDetail.getNumDistinctValues();
		String count = null;
		String walking_square_sum = null;
		String walking_sum = null;
		float presence = profile.getPresence();
		// parameter data_sample_id
		int binary_character_histogram = insertHistogram(bDetail.getByteHistogram());
		int detail_type_id = DetailType.fromString(bDetail.getDetailType()).getIndex();
		int interpretation_id = addInterpretation(profile.getInterpretation());
		String binary_mime_type = bDetail.getMimeType();
		long binary_length = bDetail.getLength().longValue();
		String binary_hash = bDetail.getHash();
		double binary_entropy = bDetail.getEntropy();

		try {
			ppst.setString(1, fieldName);
			ppst.setNull(2, Types.VARCHAR);
			ppst.setLong(3, num_distinct);
			ppst.setString(4, count);
			ppst.setString(5, walking_square_sum);
			ppst.setString(6, walking_sum);
			ppst.setFloat(7, presence);
			ppst.setInt(8, data_sample_or_schema_id);
			ppst.setInt(9, binary_character_histogram);
			ppst.setInt(10, detail_type_id);
			ppst.setInt(11, interpretation_id);
			ppst.setString(12, binary_mime_type);
			ppst.setLong(13, binary_length);
			ppst.setString(14, binary_hash);
			ppst.setDouble(15, binary_entropy);

			ppst.execute();

			generatedKey = h2.getGeneratedKey(ppst);
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

		return generatedKey;
	}

	private void putResultSetProfileInFieldMapping(Map<String, Profile> fieldMapping, ResultSet rs, String tableName)
			throws SQLException {
		DetailType detailType = DetailType.getTypeByIndex(rs.getInt(detail_type_id_key));
		MainType mainType = detailType.getMainType();
		float presence = rs.getFloat(presence_key);
		String fieldName = rs.getString(field_name_key);
		Profile profile = new Profile();

		profile.setPresence(presence);
		profile.setMainType(mainType.toString());
		
		Interpretation interpretation = new Interpretation();
		interpretation.setInterpretation(rs.getString(interpretation_name_key));
		profile.setInterpretation(interpretation);

		switch (mainType) {
		case NUMBER: {
			// NumberMetrics nm = resultSetToNumberMetrics(rs, tableName);
			profile.setDetail(resultSetToNumberDetail(rs, tableName));
			break;
		}
		case STRING: {
			// StringMetrics sm = resultSetToStringMetrics(rs, tableName);
			profile.setDetail(resultSetToStringDetail(rs, tableName));
			break;
		}
		case BINARY: {
			// BinaryMetrics bm = resultSetToBinaryMetrics(rs, tableName);
			profile.setDetail(resultSetToBinaryDetail(rs, tableName));
			break;
		}
		default: {
			logger.error("Not retrieved from database as number, string, or binary!");
		}
		}

		if (tableName.equals(schema_field)) {
			List<AliasNameDetails> aliasList = new ArrayList<AliasNameDetails>();

			int schemaFieldId = rs.getInt("schema_field.schema_field_id");

			int dataSampleFieldId = rs.getInt("schema_alias_mapping.data_sample_field_id");
			if (dataSampleFieldId > 0) {
				do {
					int specificRowSchemaFieldId = rs.getInt("schema_field.schema_field_id");
					if (specificRowSchemaFieldId != schemaFieldId) {
						rs.previous();
						break;
					}
					String aliasName = rs.getString("data_sample_field.field_name");
					String aliasDsGuid = rs.getString("data_sample.ds_guid");
					AliasNameDetails aliasNameDetails = new AliasNameDetails();
					aliasNameDetails.setAliasName(aliasName);
					aliasNameDetails.setDsGuid(aliasDsGuid);
					aliasList.add(aliasNameDetails);
				} while (rs.next());
				profile.setAliasNames(aliasList);
			}
		}
		fieldMapping.put(fieldName, profile);
	}

	private Profile resultSetToProfile(ResultSet rs, String tableName) throws SQLException {
		Profile profile = new Profile();

		String detailType = rs.getString(detail_type_id_key);
		MainType mainType = DetailType.fromString(detailType).getMainType();
		switch (mainType) {
		case NUMBER: {
			NumberDetail numberDetail = resultSetToNumberDetail(rs, tableName);
			profile.setDetail(numberDetail);
			break;
		}
		case STRING: {
			StringDetail stringMetrics = resultSetToStringDetail(rs, tableName);
			profile.setDetail(stringMetrics);
			break;
		}
		case BINARY: {
			logger.error("Got field as binary.");
			BinaryDetail binaryDetail = null;
			profile.setDetail(binaryDetail);
			break;
		}
		default: {
			logger.error("Did not retrieve field as number, string, nor binary!");
			return null;
		}
		}

		return profile;
	}

	private void createBuckets(AbstractBucketList freqHistogram, int histogramId) {
		// List<AbstractBucket> list = freqHistogram.getOrderedBuckets();
		List<Integer> dataList = freqHistogram.getData();
		List<String> labelList = freqHistogram.getLabels();
		int size = 0;
		if (dataList.size() != labelList.size()) {
			logger.error("Data and label lists are unequal sizes.");
		} else {
			size = dataList.size();
		}
		String createBuckets = insertBucketsString(freqHistogram.getData().size(), histogramId);
		if (createBuckets == null)
			return;

		PreparedStatement ppst = null;
		try {
			ppst = dbConnection.prepareStatement(createBuckets);
			for (int i = 1; i <= size; i++) {
				String label = labelList.get(i - 1);
				if (label.length() > 255)
					label = label.substring(0, 255);
				ppst.setString((2 * i - 1), label);
				ppst.setString((2 * i), dataList.get(i - 1).toString());
			}
			// H2DataAccessObject.johnOutput.println(H2DataAccessObject.preparedStatementToString(stmt));
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

	private int insertHistogram(AbstractBucketList buckets) {
		PreparedStatement ppst = null;
		int histogramId = -1;

		try {
			ppst = dbConnection.prepareStatement(ADD_HISTOGRAM, PreparedStatement.RETURN_GENERATED_KEYS);
			// H2DataAccessObject.johnOutput.println(H2DataAccessObject.preparedStatementToString(ppst));
			ppst.setNull(1, Types.INTEGER);
			ppst.setNull(2, Types.VARCHAR);
			ppst.setNull(3, Types.VARCHAR);
			ppst.execute();
			histogramId = h2.getGeneratedKey(ppst);
			createBuckets(buckets, histogramId);
			createRegionHistogram(buckets, histogramId);
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
		return histogramId;
	}

	private void createRegionHistogram(AbstractBucketList buckets, int histogramId) {
		RegionData regionData = buckets.getRegionData();
		PreparedStatement ppst = null;
		PreparedStatement ppst2 = null;

		try {
			if (regionData != null) {
				ppst = dbConnection.prepareStatement(ADD_HISTOGRAM, PreparedStatement.RETURN_GENERATED_KEYS);
				ppst.setInt(1, histogramId);
				ppst.setString(2, regionData.getLatitudeKey());
				ppst.setString(3, regionData.getLongitudeKey());
				ppst.execute();
				int regionDataId = h2.getGeneratedKey(ppst);
				String createRegionBuckets = insertBucketsString(regionData.getRows().size(), regionDataId);
				ppst2 = dbConnection.prepareStatement(createRegionBuckets);
				List<RowEntry> rows = regionData.getRows();
				for (int i = 1; i <= rows.size(); i++) {
					String label = rows.get(i - 1).getC().get(0).getV().toString();
					if (label.length() > 255)
						label = label.substring(0, 255);
					ppst2.setString((2 * i - 1), label);
					ppst2.setString((2 * i), rows.get(i - 1).getC().get(1).getV().toString());
				}
				ppst2.execute();
			}
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
			try {
				if (ppst2 != null)
					ppst2.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
	}

	/*
	 * TODO John, this is what I need for the interpretations finish the
	 * addInterpretations() method use it in addNumberMetrics() and
	 * addStringMetrics() add joins to all QUERY_NUMBER_FIELDS and
	 * QUERY_STRING_FIELDS to return the interpretation data and then put the
	 * interpretation data into the bean
	 */

	private int addInterpretation(Interpretation interpretation) {
		PreparedStatement ppst = null;
		int interpretationId = -1;

		try {
			ppst = dbConnection.prepareStatement(ADD_INTERPRETATION, PreparedStatement.RETURN_GENERATED_KEYS);
			ppst.setString(1, interpretation.getInterpretation());
			ppst.execute();
			interpretationId = h2.getGeneratedKey(ppst);
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
		return interpretationId;
	}

	private void addInterpretations(List<AbstractJavaInterpretation> interpretations, int fieldId) throws SQLException {
		// ******************DO NOT IMPLEMENT **********************
		/*
		 * TODO write queries for add and retrieve interpretation and add
		 * histogram
		 * 
		 * queries are ADD_INTERPRETATION use setString(1,
		 * interpretation.getInterpretationName()); h2.getGeneratedKey(^
		 * statement) and ADD_INTERPRETATION_MAPPING ? 1 = interpretation id ? 2
		 * = field_id ? 3 = confidence
		 */
		boolean ready = false;
		if (!ready) {
			return; // TODO finish method
		}

		dbConnection.setAutoCommit(false);
		try {
			List<Integer> interpretationIds = new ArrayList<Integer>();
			for (AbstractJavaInterpretation interpretation : interpretations) {
				PreparedStatement ppst = dbConnection.prepareStatement(ADD_INTERPRETATION,
						PreparedStatement.RETURN_GENERATED_KEYS);
				ppst.setString(1, interpretation.getInterpretationName());
				ppst.execute();
				int interpretationId = h2.getGeneratedKey(ppst);
				interpretationIds.add(interpretationId);
			}
			for (Integer i : interpretationIds) {
				PreparedStatement ppst = dbConnection.prepareStatement(ADD_INTERPRETATION_MAPPING);
				ppst.setInt(1, i);
				ppst.setInt(2, fieldId);
				ppst.setFloat(3, interpretations.get(i).getConfidence().floatValue());
				ppst.execute();
			}
		} catch (SQLException e) {
			logger.error("SQL error added interpretations.");
			logger.error(e);
		}
	}

	private static String insertBucketsString(int bucketSize, int histogramId) {
		String s = "INSERT INTO bucket (histogram_id, bucket_id, b_order, b_definition, b_count) VALUES ";
		// List<AbstractBucket> bucketList = buckets.getOrderedBuckets();
		int size = bucketSize; // bucketList.size();
		if (size > 0) {
			for (int i = 0; i < size - 1; i++) {
				String v = "(" + histogramId + ", NULL, " + i + ", ?, ?) , ";
				s += v;
			}
			s += "(" + histogramId + ", NULL, " + (size - 1) + ", ?, ?);";
		} else {
			return null;
		}
		return s;
	}

	private NumberDetail resultSetToNumberDetail(ResultSet rs, String tableName) throws SQLException {
		NumberDetail nd = new NumberDetail();

		nd.setAverage(new BigDecimal(rs.getString(tableName + dot + number_average_key)));
		nd.setDetailType(DetailType.getTypeByIndex(rs.getInt(tableName + dot + detail_type_id_key)).toString());
		nd.setMin(new BigDecimal(rs.getString(tableName + dot + number_min_key)));
		nd.setMax(new BigDecimal(rs.getString(tableName + dot + number_max_key)));
		nd.setStdDev(rs.getDouble(tableName + dot + number_std_dev_key));
		nd.setNumDistinctValues(rs.getInt(tableName + dot + num_distinct_key));
		nd.setWalkingCount(new BigDecimal(rs.getString(tableName + dot + count_key)));
		nd.setFreqHistogram(getNumberHistogram(rs.getInt(number_histogram_key)));
		nd.setWalkingSquareSum(new BigDecimal(rs.getString(tableName + dot + walking_square_sum_key)));
		nd.setWalkingSum(new BigDecimal(rs.getString(tableName + dot + walking_sum_key)));

		return nd;
	}

	private StringDetail resultSetToStringDetail(ResultSet rs, String tableName) throws SQLException {
		StringDetail sd = new StringDetail();

		sd.setAverageLength(rs.getDouble(tableName + dot + string_average_length_key));
		sd.setDetailType(DetailType.getTypeByIndex(rs.getInt(tableName + dot + detail_type_id_key)).toString());
		sd.setMinLength(rs.getInt(tableName + dot + string_min_length_key));
		sd.setMaxLength(rs.getInt(tableName + dot + string_max_length_key));
		sd.setStdDevLength(rs.getDouble(tableName + dot + string_std_dev_length_key));
		sd.setNumDistinctValues(rs.getInt(tableName + dot + num_distinct_key));
		sd.setWalkingCount(new BigDecimal(rs.getString(tableName + dot + count_key)));
		sd.setTermFreqHistogram(getTermHistogram(rs.getInt(tableName + dot + string_term_histogram_key)));
		sd.setCharFreqHistogram(getCharacterHistogram(rs.getInt(string_character_histogram_key)));
		sd.setWalkingSquareSum(new BigDecimal(rs.getString(tableName + dot + walking_square_sum_key)));
		sd.setWalkingSum(new BigDecimal(rs.getString(tableName + dot + walking_sum_key)));

		return sd;
	}

	private BinaryDetail resultSetToBinaryDetail(ResultSet rs, String tableName) throws SQLException {
		BinaryDetail binaryDetail = new BinaryDetail();
		binaryDetail.setDetailType(DetailType.getTypeByIndex(rs.getInt(detail_type_id_key)).toString());
		binaryDetail.setEntropy(rs.getDouble(binary_entropy_key));
		binaryDetail.setMimeType(rs.getString(binary_mime_type_key));
		binaryDetail.setHash(rs.getString(binary_hash_key));
		binaryDetail.setLength(BigInteger.valueOf(rs.getLong(binary_length_key)));
		binaryDetail.setByteHistogram(getByteFrequencyHistogram(rs.getInt(binary_character_histogram_key)));
		return binaryDetail;
	}

	/**
	 * The the term histogram based on its histogram ID.
	 * 
	 * @param histogramId
	 * @return
	 * @throws SQLException
	 */
	private TermBucketList getTermHistogram(int histogramId) {
		TermBucketList histogram = null;
		PreparedStatement ppst = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_BUCKETS_BY_HISTOGRAM_ID);
			ppst.setInt(1, histogramId);
			ResultSet rs = ppst.executeQuery();

			histogram = new TermBucketList();
			int i = 0;
			while (rs.next()) {
				String definition = rs.getString("b_definition");
				int count = rs.getInt("b_count");
				TermBucket tb = new TermBucket(definition, BigInteger.valueOf(count));
				histogram.getBucketList().add(i, tb);
				i++;
			}

			if (histogramId == h2.getEmptyHistogramId()) {
				histogram = new TermBucketList();
				return histogram;
			}

			RegionData regionData = getRegionData(histogramId);
			if (regionData != null) {
				histogram.setType("map");
			}
			histogram.setRegionData(regionData);
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
		return histogram;
	}

	/**
	 * Get the character histogram based on its ID.
	 * 
	 * @param histogramId
	 * @return
	 * @throws SQLException
	 */
	private CharacterBucketList getCharacterHistogram(int histogramId) {
		CharacterBucketList histogram = null;
		PreparedStatement ppst = null;
		ResultSet rs = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_BUCKETS_BY_HISTOGRAM_ID);
			ppst.setInt(1, histogramId);
			rs = ppst.executeQuery();

			histogram = new CharacterBucketList();
			while (rs.next()) {
				String definition = rs.getString("b_definition");
				int count = rs.getInt("b_count");
				CharacterBucket cb = new CharacterBucket(definition, BigInteger.valueOf(count));
				histogram.getBucketMap().put(Integer.valueOf(definition.charAt(0)), cb);
			}

			if (histogramId == h2.getEmptyHistogramId()) {
				histogram = new CharacterBucketList();
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
		return histogram;
	}

	/**
	 * Get the number histogram based on its ID.
	 * 
	 * @param histogramId
	 * @return
	 * @throws SQLException
	 */
	private NumberBucketList getNumberHistogram(int histogramId) {
		NumberBucketList histogram = null;
		PreparedStatement ppst = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_BUCKETS_BY_HISTOGRAM_ID);
			ppst.setInt(1, histogramId);
			ResultSet rs = ppst.executeQuery();

			histogram = new NumberBucketList();
			int i = 0;
			while (rs.next()) {
				String definition = rs.getString("b_definition");
				int count = rs.getInt("b_count");
				NumberBucket nb = new NumberBucket(definition, BigInteger.valueOf(count));
				histogram.getBucketList().add(i, nb);
				i++;
			}

			if (histogramId == h2.getEmptyHistogramId()) {
				histogram = new NumberBucketList();
				return histogram;
			}

			RegionData regionData = getRegionData(histogramId);
			if (regionData != null) {
				histogram.setType("map");
			}
			histogram.setRegionData(regionData);
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
		return histogram;
	}
	
	private ByteBucketList getByteFrequencyHistogram(int histogramId) {
		ByteBucketList histogram = null;
		PreparedStatement ppst = null;

		try {
			ppst = dbConnection.prepareStatement(QUERY_BUCKETS_BY_HISTOGRAM_ID);
			ppst.setInt(1, histogramId);
			ResultSet rs = ppst.executeQuery();

			histogram = new ByteBucketList();
			int i = 0;
			while (rs.next()) {
				String definition = rs.getString("b_definition");
				byte byteDef = Byte.valueOf(definition);
				int count = rs.getInt("b_count");
				ByteBucket nb = new ByteBucket(byteDef, BigInteger.valueOf(count));
				histogram.getBucketList().set(i, nb);
				i++;
			}

			if (histogramId == h2.getEmptyHistogramId()) {
				histogram = new ByteBucketList();
				return histogram;
			}

			RegionData regionData = getRegionData(histogramId);
			if (regionData != null) {
				histogram.setType("map");
			}
			histogram.setRegionData(regionData);
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
		return histogram;
	}

	private RegionData getRegionData(int baseHistogramId) {
		final String latitudeKey = "latitude_key_for_region_histograms";
		final String longitudeKey = "longitude_key_for_region_histograms";
		RegionData regionData = null;
		PreparedStatement ppst2 = null;
		ResultSet rs2 = null;

		try {
			ppst2 = dbConnection.prepareStatement(QUERY_REGION_HISTOGRAM_BY_PARENT_HISTOGRAM_ID);
			ppst2.setInt(1, baseHistogramId);
			rs2 = ppst2.executeQuery();

			regionData = null;
			if (rs2.next()) {
				regionData = new RegionData();
				List<RowEntry> rows = new ArrayList<RowEntry>();
				regionData.setLatitudeKey(rs2.getString(latitudeKey));
				regionData.setLongitudeKey(rs2.getString(longitudeKey));
				do {
					String country = rs2.getString("b_definition");
					Integer count = Integer.valueOf(rs2.getString("b_count"));
					rows.add(new RowEntry(country, count));
				} while (rs2.next());
				regionData.setRows(rows);
			}
		} catch (SQLException e) {
			logger.error("Error executing query.");
			e.printStackTrace();
		} finally {
			try {
				if (rs2 != null)
					rs2.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
			try {
				if (ppst2 != null)
					ppst2.close();
			} catch (SQLException e) {
				logger.error("Error executing query.");
				e.printStackTrace();
			}
		}
		return regionData;
	}
}
