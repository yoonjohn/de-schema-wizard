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
import com.deleidos.dp.beans.Attributes;
import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Histogram;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.enums.DetailType;
import com.deleidos.dp.enums.MainType;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.histogram.AbstractBucketList;
import com.deleidos.dp.interpretation.builtin.AbstractBuiltinInterpretation;

/**
 * Data Access Object meant to handle external communications with the H2
 * database.
 * 
 * @author leegc
 *
 */
public class H2MetricsDataAccessObject {
	private static final int EMPTY_HISTOGRAM_ID = 1;
	public static final Logger logger = H2DataAccessObject.logger;
	private H2DataAccessObject h2;
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
	private static final String display_name_key = "display_name";
	private static final String example_value_key = "example_value";
	private static final String unknown = "Unknown";

	private static final String ADD_SAMPLE_NUMBER_FIELD = "INSERT INTO data_sample_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	data_sample_id, number_histogram, detail_type_id, interpretation_id,"
			+ "	number_min, number_max, number_average, number_std_dev, display_name)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_SAMPLE_EXAMPLE_VALUE = "INSERT INTO data_sample_example_value("
			+ " dsev_id, dsf_id, example_value)" 
			+ " VALUES (NULL, ?, ?);";
	
	private static final String ADD_SAMPLE_ATTRIBUTE = "INSERT INTO field_attributes("
			+ " fa_id, dsf_id, identifier, categorical, quantitative, relational, ordinal)" 
			+ " VALUES (NULL, ?, ?, ?, ?, ?, ?);";

	private static final String ADD_SCHEMA_NUMBER_FIELD = "INSERT INTO schema_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	schema_model_id, number_histogram, detail_type_id, interpretation_id,"
			+ "	number_min, number_max, number_average, number_std_dev, display_name)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_SAMPLE_STRING_FIELD = "INSERT INTO data_sample_field("
			+ " field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence, "
			+ " data_sample_id, string_character_histogram, string_term_histogram, detail_type_id, interpretation_id,"
			+ "	string_min_length, string_max_length, string_average_length, string_std_dev_length, display_name) "
			+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?); ";

	private static final String ADD_SCHEMA_STRING_FIELD = "INSERT INTO schema_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	schema_model_id, string_character_histogram, string_term_histogram, detail_type_id, interpretation_id,"
			+ "	string_min_length, string_max_length, string_average_length, string_std_dev_length, display_name)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?); ";

	private static final String ADD_SAMPLE_BINARY_FIELD = "INSERT INTO data_sample_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	data_sample_id, binary_character_histogram, detail_type_id, interpretation_id,"
			+ "	binary_mime_type, binary_length, binary_hash, binary_entropy, display_name)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_SCHEMA_BINARY_FIELD = "INSERT INTO schema_field("
			+ "	field_name, field_order, num_distinct, count, walking_square_sum, walking_sum, presence,"
			+ "	schema_model_id, binary_character_histogram, detail_type_id, interpretation_id,"
			+ "	binary_mime_type, binary_length, binary_hash, binary_entropy, display_name)"
			+ "	VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);	";

	private static final String ADD_EMPTY_SCHEMA_FIELD = "INSERT INTO schema_field("
			+ " schema_model_id, field_name, detail_type_id, presence, display_name) VALUES (?, ?, ?, ?, ?); ";

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

	private static final String QUERY_REGION_HISTOGRAM_BY_PARENT_HISTOGRAM_ID = "SELECT b_short_definition, b_count, "
			+ "region_histogram.longitude_key_for_region_histograms, region_histogram.latitude_key_for_region_histograms "
			+ "FROM bucket " + "INNER JOIN histogram AS base " + "ON (base.histogram_id = ?)"
			+ "INNER JOIN histogram AS region_histogram "
			+ "ON (base.histogram_id = region_histogram.base_histogram_id_for_region_histograms "
			+ "AND bucket.histogram_id = region_histogram.histogram_id);";

	private static final String QUERY_EXAMPLE_FIELDS_BY_DATA_SAMPLE_FIELD = "SELECT * FROM data_sample_example_value"
			+ " WHERE data_sample_example_value.dsf_id = ?;";

	private static final String QUERY_EXAMPLE_FIELDS_BY_SCHEMA_FIELD = "SELECT * FROM schema_field "
			+ " INNER JOIN schema_alias_mapping ON (schema_field.schema_field_id = schema_alias_mapping.schema_field_id) "
			+ " INNER JOIN data_sample_example_value ON (schema_alias_mapping.data_sample_field_id = data_sample_example_value.dsf_id)"
			+ " WHERE schema_field.schema_field_id = ?;";
	
	private static final String QUERY_ATTRIBUTES_BY_DATA_SAMPLE_FIELD = "SELECT * FROM field_attributes"
			+ " WHERE field_attributes.dsf_id = ?";
	
	private static final String QUERY_ATTRIBUTES_BY_SCHEMA_FIELD = "SELECT * FROM schema_field "
			+ " INNER JOIN schema_alias_mapping ON (schema_field.schema_field_id = schema_alias_mapping.schema_field_id) "
			+ " INNER JOIN field_attributes ON (schema_alias_mapping.data_sample_field_id = field_attributes.dsf_id)"
			+ " WHERE schema_field.schema_field_id = ?;";

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
	 */
	public int addSampleField(Connection dbConnection, int data_sample_id, String fieldName, Profile fieldProfile) throws SQLException {
		int fieldId = -1;
		PreparedStatement ppst = null;

		if (fieldProfile.getDetail().isNumberDetail()) {
			ppst = dbConnection.prepareStatement(ADD_SAMPLE_NUMBER_FIELD);
			fieldId = addNumberField(dbConnection, ppst, data_sample_id, fieldName, fieldProfile);
		} else if (fieldProfile.getDetail().isStringDetail()) {
			ppst = dbConnection.prepareStatement(ADD_SAMPLE_STRING_FIELD);
			fieldId = addStringField(dbConnection, ppst, data_sample_id, fieldName, fieldProfile);
		} else if (fieldProfile.getDetail().isBinaryDetail()) {
			logger.error("Detected as binary.");
			ppst = dbConnection.prepareStatement(ADD_SAMPLE_BINARY_FIELD);
			fieldId = addBinaryField(dbConnection, ppst, data_sample_id, fieldName, fieldProfile);
		} else {
			logger.error("Sample field \"" + fieldName + "\" not detected as number, string, or binary!");
			return -1;
		}
		ppst.close();

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
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public int addSchemaField(Connection dbConnection, int schema_model_id, String fieldName, Profile fieldProfile) throws DataAccessException, SQLException {
		int fieldId = -1;
		PreparedStatement ppst = null;

		if (fieldProfile.getPresence() < 0) {
			ppst = dbConnection.prepareStatement(ADD_EMPTY_SCHEMA_FIELD);
			ppst.setInt(1, schema_model_id);
			ppst.setString(2, fieldName);
			ppst.setInt(3, DetailType.fromString(fieldProfile.getDetail().getDetailType()).getIndex());
			ppst.setDouble(4, fieldProfile.getPresence());
			ppst.setString(5, fieldProfile.getDisplayName());
			ppst.execute();
			int id = h2.getGeneratedKey(ppst);
			ppst.close();
			return id;
		} else if (fieldProfile.getDetail().isNumberDetail()) {
			ppst = dbConnection.prepareStatement(ADD_SCHEMA_NUMBER_FIELD);
			fieldId = addNumberField(dbConnection, ppst, schema_model_id, fieldName, fieldProfile);
		} else if (fieldProfile.getDetail().isStringDetail()) {
			ppst = dbConnection.prepareStatement(ADD_SCHEMA_STRING_FIELD);
			fieldId = addStringField(dbConnection, ppst, schema_model_id, fieldName, fieldProfile);
		} else if (fieldProfile.getDetail().isBinaryDetail()) {
			logger.error("Detected as binary.");
			ppst = dbConnection.prepareStatement(ADD_SCHEMA_BINARY_FIELD);
			fieldId = addBinaryField(dbConnection, ppst, schema_model_id, fieldName, fieldProfile);
		} else {
			logger.error("Schema field \"" + fieldName + "\" not detected as number, string, or binary!");
			throw new DataAccessException("Schema field \"" + fieldName + "\" not detected as number, string, or binary!");
		}

		ppst.close();
		addAliasNames(dbConnection, fieldId, fieldProfile.getAliasNames());

		return fieldId;
	}

	private void addAliasNames(Connection dbConnection, int schemaFieldId, List<AliasNameDetails> aliasNames) throws DataAccessException, SQLException {
		PreparedStatement ppst = null;

		for (AliasNameDetails and : aliasNames) {
			int sampleFieldId = h2.getH2Samples().getSampleFieldIdBySampleGuidAndName(dbConnection, and.getDsGuid(),
					and.getAliasName());
			if (sampleFieldId <= 0) {
				logger.error("Not adding alias name.");
			} else {
				ppst = dbConnection.prepareStatement(ADD_ALIAS_NAME);
				ppst.setInt(1, schemaFieldId);
				ppst.setInt(2, sampleFieldId);
				ppst.execute();
				ppst.close();
			}
		}

	}

	/**
	 * Get the field mapping of a desired sample
	 * 
	 * @param sampleGuid
	 *            the guid of the desired sample
	 * @return a mapping of all fields associated with this sample
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public Map<String, Profile> getFieldMappingBySampleGuid(Connection dbConnection, String sampleGuid) throws DataAccessException, SQLException {
		Map<String, Profile> fieldMapping = new HashMap<String, Profile>();
		PreparedStatement ppst = null;
		ResultSet rs = null;

		ppst = dbConnection.prepareStatement(QUERY_METRICS_BY_SAMPLE_GUID, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		ppst.setString(1, sampleGuid);
		rs = ppst.executeQuery();
		if (rs.next()) {
			do {
				putResultSetProfileInFieldMapping(dbConnection, fieldMapping, rs, data_sample_field, true);
			} while (rs.next());
		} else {
			logger.warn("Empty result set from sample guid: " + sampleGuid);
		}

		ppst.close();
		return fieldMapping;
	}

	/**
	 * Get the field mapping of a desired schema
	 * 
	 * @param schemaGuid
	 *            the guid of the desired schema
	 * @return a mapping of all fields associated with this schema
	 * @throws DataAccessException
	 * @throws SQLException 
	 */
	public Map<String, Profile> getFieldMappingBySchemaGuid(Connection dbConnection, String schemaGuid) throws DataAccessException, SQLException {
		Map<String, Profile> fieldMapping = new HashMap<String, Profile>();
		// h2.queryWithOutput("SELECT * from schema_field; ");
		PreparedStatement ppst = null;
		ResultSet rs = null;
		ResultSet rs2 = null;

		ppst = dbConnection.prepareStatement(QUERY_METRICS_BY_SCHEMA_GUID, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		ppst.setString(1, schemaGuid);
		rs = ppst.executeQuery();
		if (rs.next()) {
			do {
				putResultSetProfileInFieldMapping(dbConnection, fieldMapping, rs, schema_field, false);
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
		ppst.close();
		return fieldMapping;
	}

	// Private methods
	private void putEmptyMetricsInProfileFieldMapping(Map<String, Profile> fieldMapping, ResultSet rs2)
			throws SQLException {
		String fieldName = rs2.getString(field_name_key);
		float presence = rs2.getFloat(presence_key);
		int detailIndex = rs2.getInt(detail_type_id_key);
		String displayName = rs2.getString(display_name_key);
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
		detail.setDetailType(detailType.toString());
		profile.setDetail(detail);
		Interpretation interpretation = new Interpretation();
		interpretation.setiName("Unknown");
		profile.setInterpretation(interpretation);
		profile.setMainType(mainType.toString());
		profile.setPresence(presence);
		profile.setDisplayName(displayName);
		fieldMapping.put(fieldName, profile);
	}

	private int addNumberField(Connection dbConnection, PreparedStatement ppst, int data_sample_or_schema_id, String fieldName,
			Profile profile) throws SQLException {
		NumberDetail nDetail = (NumberDetail) profile.getDetail();
		String field_order = null, count = null, walking_square_sum = null, walking_sum = null, number_min = null,
				number_max = null, number_average = null;
		int number_histogram = -1, detail_type_id = -1, interpretation_id = -1;
		float presence = -1.0f;
		String num_distinct = "-1";
		double number_std_dev = -1.0;
		int generatedKey = -1;
		String displayName = profile.getDisplayName();

		// parameter fieldName
		field_order = null;
		num_distinct = nDetail.getNumDistinctValues();
		count = nDetail.getWalkingCount().toString();
		walking_square_sum = nDetail.getWalkingSquareSum().toString();
		walking_sum = nDetail.getWalkingSum().toString();
		presence = profile.getPresence();
		// parameter data_sample_id
		number_histogram = insertHistogram(dbConnection, nDetail.getFreqHistogram());
		detail_type_id = DetailType.fromString(nDetail.getDetailType()).getIndex();
		interpretation_id = addInterpretation(dbConnection, profile.getInterpretation());
		number_min = nDetail.getMin().toString();
		number_max = nDetail.getMax().toString();
		number_average = nDetail.getAverage().toString();
		number_std_dev = nDetail.getStdDev();

		dbConnection.setAutoCommit(false);
		ppst.setString(1, fieldName);
		ppst.setNull(2, Types.VARCHAR);
		ppst.setString(3, num_distinct);
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
		ppst.setString(16, displayName);

		ppst.execute();

		generatedKey = h2.getGeneratedKey(ppst);

		// Example values
		int dataSampleFieldId = generatedKey;
		List<Object> exampleValuesList = profile.getExampleValues();

		if (exampleValuesList != null) {
			for(Object exampleValue : exampleValuesList) {
				addExampleValue(dbConnection, dataSampleFieldId, exampleValue);	
			}
		}
		
		// Attributes (VizWiz objects)
		Attributes attributes = profile.getAttributes();
		if (attributes != null) {
			addAttributes(dbConnection, dataSampleFieldId, attributes);	
		} else {
			Attributes unknownAttributes = generateUnkownAttributes();
			addAttributes(dbConnection, dataSampleFieldId, unknownAttributes);	
		}

		dbConnection.setAutoCommit(true);

		ppst.close();

		return generatedKey;

	}

	private int addStringField(Connection dbConnection, PreparedStatement ppst, int data_sample_or_schema_id, String fieldName,
			Profile profile) throws SQLException {
		StringDetail sDetail = (StringDetail) profile.getDetail();
		int generatedKey = -1;
		String displayName = profile.getDisplayName();

		// parameter fieldName
		String field_order = null;
		String num_distinct = sDetail.getNumDistinctValues();
		String count = sDetail.getWalkingCount().toString();
		String walking_square_sum = sDetail.getWalkingSquareSum().toString();
		String walking_sum = sDetail.getWalkingSum().toString();
		float presence = profile.getPresence();
		// parameter data_sample_id
		int string_character_histogram = EMPTY_HISTOGRAM_ID;// insertHistogram(sDetail.getCharFreqHistogram());
		int string_term_histogram = insertHistogram(dbConnection, sDetail.getTermFreqHistogram());
		int detail_type_id = DetailType.fromString(sDetail.getDetailType()).getIndex();
		int interpretation_id = addInterpretation(dbConnection, profile.getInterpretation());
		int string_min_length = sDetail.getMinLength();
		int string_max_length = sDetail.getMaxLength();
		double string_average_length = sDetail.getAverageLength();
		double string_std_dev_length = sDetail.getStdDevLength();

		dbConnection.setAutoCommit(false);
		ppst.setString(1, fieldName);
		ppst.setNull(2, Types.VARCHAR);
		ppst.setString(3, num_distinct);
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
		ppst.setString(17, displayName);

		ppst.execute();

		generatedKey = h2.getGeneratedKey(ppst);

		// Example values
		int dataSampleFieldId = generatedKey;
		List<Object> exampleValuesList = profile.getExampleValues();

		if (exampleValuesList != null) {
			for(Object exampleValue : exampleValuesList) {
				addExampleValue(dbConnection, dataSampleFieldId, exampleValue);	
			}
		}
		
		// Attributes (VizWiz objects)
		Attributes attributes = profile.getAttributes();
		if (attributes != null) {
			addAttributes(dbConnection, dataSampleFieldId, attributes);	
		} else {
			Attributes unknownAttributes = generateUnkownAttributes();
			addAttributes(dbConnection, dataSampleFieldId, unknownAttributes);	
		}

		dbConnection.setAutoCommit(true);
		ppst.close();

		return generatedKey;
	}

	private int addBinaryField(Connection dbConnection, PreparedStatement ppst, int data_sample_or_schema_id, String fieldName,
			Profile profile) throws SQLException {

		logger.error("Received binary field.  Untested!");

		BinaryDetail bDetail = (BinaryDetail) profile.getDetail();
		int generatedKey = -1;

		// parameter fieldName
		String field_order = null;
		String num_distinct = bDetail.getNumDistinctValues();
		String count = null;
		String walking_square_sum = null;
		String walking_sum = null;
		float presence = profile.getPresence();
		// parameter data_sample_id
		int binary_character_histogram = insertHistogram(dbConnection, bDetail.getByteHistogram());
		int detail_type_id = DetailType.fromString(bDetail.getDetailType()).getIndex();
		int interpretation_id = addInterpretation(dbConnection, profile.getInterpretation());
		String binary_mime_type = bDetail.getMimeType();
		long binary_length = bDetail.getLength().longValue();
		String binary_hash = bDetail.getHash();
		double binary_entropy = bDetail.getEntropy();
		String displayName = profile.getDisplayName();

		dbConnection.setAutoCommit(false);
		ppst.setString(1, fieldName);
		ppst.setNull(2, Types.VARCHAR);
		ppst.setString(3, num_distinct);
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
		ppst.setString(16, displayName);

		ppst.execute();

		generatedKey = h2.getGeneratedKey(ppst);

		// Example values
		int dataSampleFieldId = generatedKey;
		List<Object> exampleValuesList = profile.getExampleValues();

		if (exampleValuesList != null) {
			for(Object exampleValue : exampleValuesList) {
				addExampleValue(dbConnection, dataSampleFieldId, exampleValue);	
			}		
		}
		
		// Attributes (VizWiz objects)
		Attributes attributes = profile.getAttributes();
		if (attributes != null) {
			addAttributes(dbConnection, dataSampleFieldId, attributes);	
		} else {
			Attributes unknownAttributes = generateUnkownAttributes();
			addAttributes(dbConnection, dataSampleFieldId, unknownAttributes);	
		}

		dbConnection.setAutoCommit(true);
		ppst.close();

		return generatedKey;
	}

	private void putResultSetProfileInFieldMapping(Connection dbConnection, Map<String, Profile> fieldMapping, ResultSet rs, String tableName,
			boolean isSample) throws SQLException {
		DetailType detailType = DetailType.getTypeByIndex(rs.getInt(detail_type_id_key));
		MainType mainType = detailType.getMainType();
		float presence = rs.getFloat(presence_key);
		String fieldName = rs.getString(field_name_key);
		String displayName = rs.getString(display_name_key);
		Profile profile = new Profile();

		profile.setPresence(presence);
		profile.setMainType(mainType.toString());
		profile.setDisplayName(displayName);

		Interpretation interpretation = new Interpretation();
		interpretation.setiName(rs.getString(interpretation_name_key));
		profile.setInterpretation(interpretation);

		switch (mainType) {
		case NUMBER: {
			// NumberMetrics nm = resultSetToNumberMetrics(rs, tableName);
			profile.setDetail(resultSetToNumberDetail(dbConnection, rs, tableName));
			break;
		}
		case STRING: {
			// StringMetrics sm = resultSetToStringMetrics(rs, tableName);
			profile.setDetail(resultSetToStringDetail(dbConnection, rs, tableName));
			break;
		}
		case BINARY: {
			// BinaryMetrics bm = resultSetToBinaryMetrics(rs, tableName);
			profile.setDetail(resultSetToBinaryDetail(dbConnection, rs, tableName));
			break;
		}
		default: {
			logger.error("Not retrieved from database as number, string, or binary!");
		}
		}

		List<Object> exampleValues = null;
		Attributes attributes = null;
		
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

					// Example Values and Attributes
					int sfId = rs.getInt("schema_field.schema_field_id");
					exampleValues = queryExampleValues(dbConnection, QUERY_EXAMPLE_FIELDS_BY_SCHEMA_FIELD, sfId);
					attributes = queryAttributes(dbConnection, QUERY_ATTRIBUTES_BY_SCHEMA_FIELD, sfId);
				} while (rs.next());
				profile.setAliasNames(aliasList);
			}

		} else if (tableName.equals(data_sample_field)) {
			// Example Values and Attributes
			int dsfId = rs.getInt("data_sample_field.data_sample_field_id");
			exampleValues = queryExampleValues(dbConnection, QUERY_EXAMPLE_FIELDS_BY_DATA_SAMPLE_FIELD, dsfId);		
			attributes = queryAttributes(dbConnection, QUERY_ATTRIBUTES_BY_DATA_SAMPLE_FIELD, dsfId);
		}
		profile.setExampleValues(exampleValues);
		profile.setAttributes(attributes);
		fieldMapping.put(fieldName,profile);
	}
	
	private List<Object> queryExampleValues(Connection dbConnection, String query, int id) throws SQLException {
		List<Object> exampleValues = new ArrayList<Object>();
		PreparedStatement queryExampleValues;
		queryExampleValues = dbConnection.prepareStatement(query);
		queryExampleValues.setInt(1, id);
		ResultSet rs = queryExampleValues.executeQuery();
		if (rs.next()) {
			do {
				String exampleValue = rs.getString(example_value_key);
				exampleValues.add(exampleValue);
			} while (rs.next()); 
		}	
		return exampleValues;
	}
	
	private Attributes queryAttributes(Connection dbConnection, String query, int id) throws SQLException {
		PreparedStatement queryExampleValues;
		queryExampleValues = dbConnection.prepareStatement(query);
		queryExampleValues.setInt(1, id);
		ResultSet rs = queryExampleValues.executeQuery();
		Attributes attributes = null;
		if (rs.next()) {
			attributes = new Attributes();
			attributes.setIdentifier(rs.getString("identifier"));
			attributes.setCategorical(rs.getString("categorical"));
			attributes.setQuantitative(rs.getString("quantitative"));
			attributes.setRelational(rs.getString("relational"));
			attributes.setOrdinal(rs.getString("ordinal"));
		}	
		return attributes;
	}

	private Profile resultSetToProfile(Connection dbConnection, ResultSet rs, String tableName) throws SQLException {
		Profile profile = new Profile();

		String detailType = rs.getString(detail_type_id_key);
		MainType mainType = DetailType.fromString(detailType).getMainType();
		switch (mainType) {
		case NUMBER: {
			NumberDetail numberDetail = resultSetToNumberDetail(dbConnection, rs, tableName);
			profile.setDetail(numberDetail);
			break;
		}
		case STRING: {
			StringDetail stringMetrics = resultSetToStringDetail(dbConnection, rs, tableName);
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

	private int addInterpretation(Connection dbConnection, Interpretation interpretation) throws SQLException {
		PreparedStatement ppst = null;
		int interpretationId = -1;

		ppst = dbConnection.prepareStatement(ADD_INTERPRETATION, PreparedStatement.RETURN_GENERATED_KEYS);
		ppst.setString(1, interpretation.getiName());
		ppst.execute();
		interpretationId = h2.getGeneratedKey(ppst);
		ppst.close();

		return interpretationId;
	}

	private static String insertBucketsString(int bucketSize, int histogramId) {
		String s = "INSERT INTO bucket (histogram_id, bucket_id, b_order, b_long_definition, b_short_definition, b_count) VALUES ";
		int size = bucketSize; // bucketList.size();
		if (size > 0) {
			for (int i = 0; i < size - 1; i++) {
				String v = "(" + histogramId + ", NULL, " + i + ", ?, ?, ?) , ";
				s += v;
			}
			s += "(" + histogramId + ", NULL, " + (size - 1) + ", ?, ?, ?);";
		} else {
			return null;
		}
		return s;
	}

	private NumberDetail resultSetToNumberDetail(Connection dbConnection, ResultSet rs, String tableName) throws SQLException {
		NumberDetail nd = new NumberDetail();

		nd.setAverage(new BigDecimal(rs.getString(tableName + dot + number_average_key)));
		nd.setDetailType(DetailType.getTypeByIndex(rs.getInt(tableName + dot + detail_type_id_key)).toString());
		nd.setMin(new BigDecimal(rs.getString(tableName + dot + number_min_key)));
		nd.setMax(new BigDecimal(rs.getString(tableName + dot + number_max_key)));
		nd.setStdDev(rs.getDouble(tableName + dot + number_std_dev_key));
		nd.setNumDistinctValues(rs.getString(tableName + dot + num_distinct_key));
		nd.setWalkingCount(new BigDecimal(rs.getString(tableName + dot + count_key)));
		nd.setFreqHistogram(getHistogram(dbConnection, rs.getInt(number_histogram_key)));
		nd.setWalkingSquareSum(new BigDecimal(rs.getString(tableName + dot + walking_square_sum_key)));
		nd.setWalkingSum(new BigDecimal(rs.getString(tableName + dot + walking_sum_key)));

		return nd;
	}

	private StringDetail resultSetToStringDetail(Connection dbConnection, ResultSet rs, String tableName) throws SQLException {
		StringDetail sd = new StringDetail();

		sd.setAverageLength(rs.getDouble(tableName + dot + string_average_length_key));
		sd.setDetailType(DetailType.getTypeByIndex(rs.getInt(tableName + dot + detail_type_id_key)).toString());
		sd.setMinLength(rs.getInt(tableName + dot + string_min_length_key));
		sd.setMaxLength(rs.getInt(tableName + dot + string_max_length_key));
		sd.setStdDevLength(rs.getDouble(tableName + dot + string_std_dev_length_key));
		sd.setNumDistinctValues(rs.getString(tableName + dot + num_distinct_key));
		sd.setWalkingCount(new BigDecimal(rs.getString(tableName + dot + count_key)));
		sd.setTermFreqHistogram(getHistogram(dbConnection, rs.getInt(tableName + dot + string_term_histogram_key)));
		sd.setWalkingSquareSum(new BigDecimal(rs.getString(tableName + dot + walking_square_sum_key)));
		sd.setWalkingSum(new BigDecimal(rs.getString(tableName + dot + walking_sum_key)));

		return sd;
	}

	private BinaryDetail resultSetToBinaryDetail(Connection dbConnection, ResultSet rs, String tableName) throws SQLException {
		BinaryDetail binaryDetail = new BinaryDetail();
		binaryDetail.setDetailType(DetailType.getTypeByIndex(rs.getInt(detail_type_id_key)).toString());
		binaryDetail.setEntropy(rs.getDouble(binary_entropy_key));
		binaryDetail.setMimeType(rs.getString(binary_mime_type_key));
		binaryDetail.setHash(rs.getString(binary_hash_key));
		binaryDetail.setLength(BigInteger.valueOf(rs.getLong(binary_length_key)));
		binaryDetail.setByteHistogram(getHistogram(dbConnection, rs.getInt(binary_character_histogram_key)));
		return binaryDetail;
	}

	private int insertHistogram(Connection dbConnection, Histogram histogram) throws SQLException {
		PreparedStatement ppst = null;
		int histogramId = -1;

		ppst = dbConnection.prepareStatement(ADD_HISTOGRAM, PreparedStatement.RETURN_GENERATED_KEYS);
		// H2DataAccessObject.johnOutput.println(H2DataAccessObject.preparedStatementToString(ppst));
		ppst.setNull(1, Types.INTEGER);
		ppst.setNull(2, Types.VARCHAR);
		ppst.setNull(3, Types.VARCHAR);
		ppst.execute();
		histogramId = h2.getGeneratedKey(ppst);
		createBuckets(dbConnection, histogram, histogramId);
		createRegionHistogram(dbConnection, histogram.getRegionData(), histogramId);
		ppst.close();

		return histogramId;
	}

	private void createRegionHistogram(Connection dbConnection, RegionData regionData, int histogramId) throws SQLException {
		PreparedStatement ppst = null;
		PreparedStatement ppst2 = null;

		if (regionData != null) {
			ppst = dbConnection.prepareStatement(ADD_HISTOGRAM, PreparedStatement.RETURN_GENERATED_KEYS);
			ppst.setInt(1, histogramId);
			ppst.setString(2, regionData.getLatitudeKey());
			ppst.setString(3, regionData.getLongitudeKey());
			ppst.execute();
			int regionDataId = h2.getGeneratedKey(ppst);
			ppst.close();
			String createRegionBuckets = insertBucketsString(regionData.getRows().size(), regionDataId);
			ppst2 = dbConnection.prepareStatement(createRegionBuckets);
			List<RowEntry> rows = regionData.getRows();
			for (int i = 1; i <= rows.size(); i++) {
				String label = rows.get(i - 1).getC().get(0).getV().toString();
				if (label.length() > 255) {
					label = label.substring(0, 255);
				}
				ppst2.setString((3 * i - 2), label);
				ppst2.setString((3 * i - 1), label);
				ppst2.setString((3 * i), rows.get(i - 1).getC().get(1).getV().toString());
			}
			ppst2.execute();
			ppst2.close();
		}

	}

	private void createBuckets(Connection dbConnection, Histogram histogram, int histogramId) throws SQLException {
		List<Integer> dataList = histogram.getData();
		List<String> labelList = histogram.getLabels();
		List<String> longLabelList = histogram.getLongLabels();
		int size = 0;
		if (dataList.size() != labelList.size()) {
			logger.error("Data and label lists are unequal sizes.");
		} else {
			size = dataList.size();
		}
		String createBuckets = insertBucketsString(histogram.getData().size(), histogramId);
		if (createBuckets == null)
			return;

		PreparedStatement ppst = null;
		ppst = dbConnection.prepareStatement(createBuckets);
		for (int i = 1; i <= size; i++) {
			String longLabel = longLabelList.get(i - 1);
			String label = labelList.get(i - 1);
			if (longLabel.length() > 255) {
				longLabel = longLabel.substring(0, 255);
			}
			ppst.setString((3 * i - 2), longLabel);
			ppst.setString((3 * i - 1), label);
			ppst.setString((3 * i), dataList.get(i - 1).toString());
		}
		// H2DataAccessObject.johnOutput.println(H2DataAccessObject.preparedStatementToString(stmt));
		ppst.execute();
		ppst.close();
	}

	private Histogram getHistogram(Connection dbConnection, int histogramId) throws SQLException {
		Histogram histogram = new Histogram();
		if (histogramId == EMPTY_HISTOGRAM_ID) {
			return histogram;
		}

		PreparedStatement ppst = null;

		ppst = dbConnection.prepareStatement(QUERY_BUCKETS_BY_HISTOGRAM_ID);
		ppst.setInt(1, histogramId);
		ResultSet rs = ppst.executeQuery();

		histogram = new Histogram();
		int i = 0;
		List<String> shortLabels = new ArrayList<String>();
		List<String> longLabels = new ArrayList<String>();
		List<Integer> data = new ArrayList<Integer>();
		while (rs.next()) {
			String definition = rs.getString("b_short_definition");
			String longDefinition = rs.getString("b_long_definition");
			int count = rs.getInt("b_count");
			shortLabels.add(definition);
			longLabels.add(longDefinition);
			data.add(count);
		}

		histogram.setLabels(shortLabels);
		histogram.setLongLabels(longLabels);
		histogram.setData(data);

		RegionData regionData = getRegionData(dbConnection, histogramId);
		if (regionData != null) {
			histogram.setType("map");
		}
		histogram.setRegionData(regionData);
		ppst.close();

		return histogram;
	}

	private RegionData getRegionData(Connection dbConnection, int baseHistogramId) throws NumberFormatException, SQLException {
		final String latitudeKey = "latitude_key_for_region_histograms";
		final String longitudeKey = "longitude_key_for_region_histograms";
		RegionData regionData = null;
		PreparedStatement ppst2 = null;
		ResultSet rs2 = null;

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
				String country = rs2.getString("b_short_definition");
				Integer count = Integer.valueOf(rs2.getString("b_count"));
				rows.add(new RowEntry(country, count));
			} while (rs2.next());
			regionData.setRows(rows);
		}
		ppst2.close();
		return regionData;
	}

	private void addExampleValue(Connection dbConnection, int dsfId, Object exampleValue) throws SQLException {
		PreparedStatement addExampleValues = dbConnection.prepareStatement(ADD_SAMPLE_EXAMPLE_VALUE)
				;
		addExampleValues.setInt(1, dsfId);
		addExampleValues.setString(2, delimitStr(exampleValue.toString(), 1024));
		
		addExampleValues.execute();
		addExampleValues.close();
	}
	
	private void addAttributes(Connection dbConnection, int dsfId, Attributes attribute) throws SQLException {
		PreparedStatement addAttributes = dbConnection.prepareStatement(ADD_SAMPLE_ATTRIBUTE);

		addAttributes.setInt(1, dsfId);		
		addAttributes.setString(2, delimitStr(attribute.getIdentifier(), 24));
		addAttributes.setString(3, delimitStr(attribute.getCategorical(), 24));
		addAttributes.setString(4, delimitStr(attribute.getQuantitative(), 24));
		addAttributes.setString(5, delimitStr(attribute.getRelational(), 24));
		addAttributes.setString(6, delimitStr(attribute.getOrdinal(), 24));
		
		addAttributes.execute();
		addAttributes.close();
	}
	
	private String delimitStr(String str, int delimiter) {
		return (str.length() >= delimiter) ? str.substring(0, delimiter) : str;
	}
	
	private Attributes generateUnkownAttributes() {
		Attributes unknownAttribute = new Attributes();
		unknownAttribute.setIdentifier(unknown);
		unknownAttribute.setCategorical(unknown);
		unknownAttribute.setQuantitative(unknown);
		unknownAttribute.setRelational(unknown);
		unknownAttribute.setOrdinal(unknown);
		return unknownAttribute;
	}
}
