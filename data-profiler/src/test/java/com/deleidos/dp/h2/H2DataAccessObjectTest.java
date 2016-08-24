package com.deleidos.dp.h2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.SchemaMetaData;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.environ.DPMockUpEnvironmentTest;
import com.deleidos.dp.exceptions.DataAccessException;

/**
 * Tests the various functionalities of the H2 Data Access Object. These same
 * functions are used by the Service Layer.
 * 
 * @author yoonj1
 *
 */
public class H2DataAccessObjectTest extends DPMockUpEnvironmentTest {
	private static Logger logger = Logger.getLogger(H2DataAccessObjectTest.class);

	@Test
	public void getSchemaByGuidTest() throws DataAccessException {
		Schema schema;
		JSONObject json;

		String schemaGuid = "sad89fuy98a5f-12a3-1231-124sdf31d21f";
		schema = H2DataAccessObject.getInstance().getSchemaByGuid(schemaGuid, false);
		String jsonString = SerializationUtility.serialize(schema);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSchemaByGuid() passed.");
		assertEquals(schemaGuid, json.get("sId"));
		assertTrue(!schema.getsProfile().isEmpty());
	}

	@Test
	public void getSchemaMetaDataByGuidTest() throws DataAccessException {
		SchemaMetaData schemaMetaData;
		JSONObject json;

		String schemaGuid = "sad89fuy98a5f-12a3-1231-124sdf31d21f";
		schemaMetaData = H2DataAccessObject.getInstance().getSchemaMetaDataByGuid(schemaGuid);
		String jsonString = SerializationUtility.serialize(schemaMetaData);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSchemaMetaDataByGuid() passed.");
		assertEquals(schemaGuid, json.get("sId"));
	}

	@Test
	public void getSchemaFieldByGuid() throws DataAccessException {
		Map<String, Profile> map;
		JSONObject json;

		String schemaGuid = "sad89fuy98a5f-12a3-1231-124sdf31d21f";
		map = H2DataAccessObject.getInstance().getSchemaFieldByGuid(schemaGuid, true);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSchemaFieldByGuid() passed.");
	}

	@Test
	public void getSchemaMetaDataFieldByGuid() throws DataAccessException {
		Map<String, Profile> map;
		JSONObject json;

		String schemaGuid = "sad89fuy98a5f-12a3-1231-124sdf31d21f";
		map = H2DataAccessObject.getInstance().getSchemaFieldByGuid(schemaGuid, false);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSchemaMetaDataFieldByGuid() passed.");
	}

	@Test
	public void getSampleByGuidTest() throws DataAccessException {
		DataSample sample;
		JSONObject json;

		String sampleGuid = "fdeb76c6-472a-4c6c-8301-e9cfd63e30fa";
		sample = H2DataAccessObject.getInstance().getSampleByGuid(sampleGuid);
		String jsonString = SerializationUtility.serialize(sample);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSampleByGuid() passed.");
		assertEquals(sampleGuid, json.get("dsId"));
	}

	@Test
	public void getSampleMetaDataByGuidTest() throws DataAccessException {
		DataSample sample;
		JSONObject json;

		String sampleGuid = "fdeb76c6-472a-4c6c-8301-e9cfd63e30fa";
		sample = H2DataAccessObject.getInstance().getSampleByGuid(sampleGuid);
		String jsonString = SerializationUtility.serialize(sample);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSampleMetaDataByGuid() passed.");
		assertEquals(sampleGuid, json.get("dsId"));
	}

	@Test
	public void getSampleFieldByGuid() throws DataAccessException {
		Map<String, Profile> map;
		JSONObject json;

		String sampleGuid = "fdeb76c6-472a-4c6c-8301-e9cfd63e30fa";
		map = H2DataAccessObject.getInstance().getSampleFieldByGuid(sampleGuid, true);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSampleFieldByGuid() passed.");
	}

	@Test
	public void getSampleMetaDataFieldByGuid() throws DataAccessException {
		Map<String, Profile> map;
		JSONObject json;

		String sampleGuid = "fdeb76c6-472a-4c6c-8301-e9cfd63e30fa";
		map = H2DataAccessObject.getInstance().getSampleFieldByGuid(sampleGuid, false);
		String jsonString = SerializationUtility.serialize(map);
		json = new JSONObject(jsonString);

		logger.debug(json.toString());
		logger.info("getSampleFieldByGuid() passed.");
	}

	@Test
	public void getAllSchemaMetaData() throws DataAccessException {
		List<SchemaMetaData> schemaCatalog;

		schemaCatalog = H2DataAccessObject.getInstance().getAllSchemaMetaData();
		schemaCatalog.forEach((k) -> logger.debug(SerializationUtility.serialize(k).toString()));

		logger.info("getAllSchemaMetaData(); passed.");
	}

	@Test
	public void getAllSampleMetaData() throws DataAccessException {
		List<DataSampleMetaData> sampleCatalog;

		sampleCatalog = H2DataAccessObject.getInstance().getAllSampleMetaData();
		sampleCatalog.forEach((k) -> logger.debug(SerializationUtility.serialize(k).toString()));

		logger.info("getAllSchemaMetaData(); passed.");
	}

	@Ignore
	@Test
	public void deleteSchemaByGuid() throws DataAccessException {
		List<SchemaMetaData> schemaList = new ArrayList<SchemaMetaData>();
		List<String> schemaGuids = new ArrayList<String>();
		String schemaGuid = "abfdklgmdklsfngmkldsfjngkdsfngklsdfe";

		// Gets the current list of Schemas
		schemaList = H2DataAccessObject.getInstance().getAllSchemaMetaData();
		logger.debug("Current list of Schema GUIDs:");
		schemaList.forEach((k) -> logger.debug(k.getsGuid()));
		schemaList.forEach((k) -> schemaGuids.add(k.getsGuid()));

		// Assert that the list contains the GUID to be deleted
		assertTrue(schemaGuids.contains(schemaGuid));

		logger.debug("Deleting: " + schemaGuid);
		H2DataAccessObject.getInstance().deleteSchemaByGuid(schemaGuid);

		// Clear the buffer
		schemaList.clear();
		schemaGuids.clear();

		// Retrieves the updated list of Schemas
		schemaList = H2DataAccessObject.getInstance().getAllSchemaMetaData();
		logger.debug("New list of Schema GUIDs:");
		schemaList.forEach((k) -> logger.debug(k.getsGuid()));
		schemaList.forEach((k) -> schemaGuids.add(k.getsGuid()));

		// Assert that the GUID was deleted
		assertFalse(schemaGuids.contains(schemaGuid));

		logger.info("deleteSchemaByGuid(); passed.");
	}

	@Ignore
	@Test
	public void deleteSampleByGuid() throws DataAccessException {
		List<DataSampleMetaData> sampleList = new ArrayList<DataSampleMetaData>();
		List<String> sampleGuids = new ArrayList<String>();
		String sampleGuid = "ghsdkgfdsg-sdfgh-sfdgers-dfghw3e4gfs";

		// Gets the current list of Samples
		sampleList = H2DataAccessObject.getInstance().getAllSampleMetaData();
		logger.debug("Current list of Sample GUIDs:");
		sampleList.forEach((k) -> logger.debug(k.getDsGuid()));
		sampleList.forEach((k) -> sampleGuids.add(k.getDsGuid()));

		// Assert that the list contains the GUID to be deleted
		assertTrue(sampleGuids.contains(sampleGuid));

		logger.debug("Deleting: " + sampleGuid);
		H2DataAccessObject.getInstance().deleteSampleByGuid(sampleGuid);

		// Clear the buffer
		sampleList.clear();
		sampleGuids.clear();

		// Gets the current list of Samples
		sampleList = H2DataAccessObject.getInstance().getAllSampleMetaData();
		logger.debug("New list of Sample GUIDs:");
		sampleList.forEach((k) -> logger.debug(k.getDsGuid()));
		sampleList.forEach((k) -> sampleGuids.add(k.getDsGuid()));

		// Assert that the list contains the GUID to be deleted
		assertFalse(sampleGuids.contains(sampleGuid));

		logger.info("deleteSampleByGuid(); passed.");
	}

	@Test
	public void deleteByGuid() throws DataAccessException {
		// Testing a Data Sample GUID
		List<DataSampleMetaData> sampleList = new ArrayList<DataSampleMetaData>();
		List<String> sampleGuids = new ArrayList<String>();
		String sampleGuid = "ghsdkgfdsg-sdfgh-sfdgers-dfghw3e4gfs";

		// Gets the current list of Samples
		sampleList = H2DataAccessObject.getInstance().getAllSampleMetaData();
		logger.debug("Current list of Sample GUIDs:");
		sampleList.forEach((k) -> logger.debug(k.getDsGuid()));
		sampleList.forEach((k) -> sampleGuids.add(k.getDsGuid()));

		// Assert that the list contains the GUID to be deleted
		assertTrue(sampleGuids.contains(sampleGuid));

		logger.debug("Deleting: " + sampleGuid);
		H2DataAccessObject.getInstance().deleteByGuid(sampleGuid);

		// Clear the buffer
		sampleList.clear();
		sampleGuids.clear();

		// Gets the current list of Samples
		sampleList = H2DataAccessObject.getInstance().getAllSampleMetaData();
		logger.debug("New list of Sample GUIDs:");
		sampleList.forEach((k) -> logger.debug(k.getDsGuid()));
		sampleList.forEach((k) -> sampleGuids.add(k.getDsGuid()));

		// Assert that the list contains the GUID to be deleted
		assertFalse(sampleGuids.contains(sampleGuid));

		logger.info("deleteSampleByGuid(); passed.");

		// Testing a Schema GUID
		List<SchemaMetaData> schemaList = new ArrayList<SchemaMetaData>();
		List<String> schemaGuids = new ArrayList<String>();
		String schemaGuid = "abfdklgmdklsfngmkldsfjngkdsfngklsdfe";

		// Gets the current list of Schemas
		schemaList = H2DataAccessObject.getInstance().getAllSchemaMetaData();
		logger.debug("Current list of Schema GUIDs:");
		schemaList.forEach((k) -> logger.debug(k.getsGuid()));
		schemaList.forEach((k) -> schemaGuids.add(k.getsGuid()));

		// Assert that the list contains the GUID to be deleted
		assertTrue(schemaGuids.contains(schemaGuid));

		logger.debug("Deleting: " + schemaGuid);
		H2DataAccessObject.getInstance().deleteByGuid(schemaGuid);

		// Clear the buffer
		schemaList.clear();
		schemaGuids.clear();

		// Retrieves the updated list of Schemas
		schemaList = H2DataAccessObject.getInstance().getAllSchemaMetaData();
		logger.debug("New list of Schema GUIDs:");
		schemaList.forEach((k) -> logger.debug(k.getsGuid()));
		schemaList.forEach((k) -> schemaGuids.add(k.getsGuid()));

		// Assert that the GUID was deleted
		assertFalse(schemaGuids.contains(schemaGuid));

		logger.info("deleteSchemaByGuid(); passed.");
	}
}
