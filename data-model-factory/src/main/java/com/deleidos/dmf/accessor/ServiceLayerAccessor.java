package com.deleidos.dmf.accessor;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.SchemaMetaData;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.InterpretationEngine;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;

/**
 * Service layer for Schema Wizard.
 * 
 * @author yoonj1
 *
 */
public class ServiceLayerAccessor implements ServiceLayer {
	public static final Logger logger = Logger.getLogger(ServiceLayerAccessor.class);
	H2DataAccessObject h2Dao;
	InterpretationEngine interpretationEngine;
	private ResourceBundle bundle = ResourceBundle.getBundle("error-messages");

	public ServiceLayerAccessor() {
		try {
			h2Dao = H2DataAccessObject.getInstance();
			interpretationEngine = InterpretationEngineFacade.getInstance();
		} catch (DataAccessException e) {
			logger.error(e);
		}
	}

	/**
	 * Gets the catalog from the H2 Database.
	 * 
	 * @return
	 */
	public Response getCatalog() {
		JSONObject json = new JSONObject();

		try {
			List<SchemaMetaData> schemaList = h2Dao.getAllSchemaMetaData();
			List<DataSampleMetaData> sampleList = h2Dao.getAllSampleMetaData();
			JSONArray domainJson = interpretationEngine.getAvailableDomains();

				json.put("schemaCatalog", new JSONArray(SerializationUtility.serialize(schemaList)));
				json.put("dataSamplesCatalog", new JSONArray(SerializationUtility.serialize(sampleList)));
				json.put("domainsCatalog", new JSONArray(domainJson.toString()));
				return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
		} catch (JSONException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		}  catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets a list of interpretations from a domain guid.
	 * 
	 * @param domainGuid
	 * @return
	 */
	public Response getDomainInterpretations(String domainGuid) {
		try {
			JSONObject jObject = interpretationEngine.getInterpretationListByDomainGuid(domainGuid);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Creates a domain in the Interpretation Engine MongoDB
	 * 
	 * @param domainJson
	 * @return
	 */
	public Response createDomain(JSONObject domainJson) {
		try {
			JSONObject jObject = interpretationEngine.createDomain(domainJson);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Creates an interpretation in the Interpretation Engine MongoDB
	 * 
	 * @param interpretationJson
	 * @return
	 */
	public Response createInterpretation(JSONObject interpretationJson) {
		try {
			JSONObject jObject = interpretationEngine.createInterpretation(interpretationJson);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		}  catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Updates a domain in the Interpretation Engine MongoDB
	 * 
	 * @param domainJson
	 * @return
	 */
	public Response updateDomain(JSONObject domainJson) {
		try {
			JSONObject jObject = interpretationEngine.updateDomain(domainJson);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		}  catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		}  catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Updates an interpretation in the Interpretation Engine MongoDB
	 * 
	 * @param interpretationJson
	 * @return
	 */
	public Response updateInterpretation(JSONObject interpretationJson) {
		try {
			JSONObject jObject = interpretationEngine.updateInterpretation(interpretationJson);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Deletes a domain in the Interpretation Engine MongoDB
	 * 
	 * @param domainJson
	 * @return
	 */
	public Response deleteDomain(JSONObject domainJson) {
		try {
			JSONObject jObject = interpretationEngine.deleteDomain(domainJson);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Deletes an interpretation in the Interpretation Engine MongoDB
	 * 
	 * @param interpretationJson
	 * @return Number of records modified
	 */
	public Response deleteInterpretation(JSONObject interpretationJson) {
		try {
			JSONObject jObject = interpretationEngine.deleteInterpretation(interpretationJson);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Validates a Python script that is encoded in base64.
	 * 
	 * @param Python
	 *            script encoded in base64
	 * @return
	 */
	public Response validatePythonScript(String iId) {
		try {
			JSONObject iIdJson = new JSONObject();
			JSONObject jObject = interpretationEngine.validatePythonScript(iIdJson.put("iId", iId));
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			JSONObject json = new JSONObject();
			json.put("type", "error");
			json.put("row", "0");
			json.put("text", bundle.getString("ie.unexpected.error"));
			return generatedResponse(Status.SERVICE_UNAVAILABLE, json.toString());
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			JSONObject json = new JSONObject();
			json.put("type", "error");
			json.put("row", "0");
			json.put("text", bundle.getString("ie.unexpected.error"));
			return generatedResponse(Status.INTERNAL_SERVER_ERROR, json.toString());
		}
	}

	/**
	 * Tests a Python script with example data
	 * 
	 * @param iId
	 * @return
	 */
	public Response testPythonScript(String iId) {
		try {
			JSONObject iIdJson = new JSONObject();
			JSONObject jObject = interpretationEngine.testPythonScript(iIdJson.put("iId", iId));
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (ProcessingException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.GATEWAY_TIMEOUT);
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("ie.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 
	 */
	public Response addSchema(JSONObject schemaJson) {
		Schema schema = SerializationUtility.deserialize(schemaJson, Schema.class);

		try {
			JSONObject jObject = new JSONObject();
			jObject.put("schemaGuid", h2Dao.addSchema(schema));
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets Schema bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Schema bean as a JSON Object
	 */
	public Response getSchemaByGuid(String guid) {
		try {
			// Show histogram
			Schema schema = h2Dao.getSchemaByGuid(guid, true);
			String jsonString = SerializationUtility.serialize(schema);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets Schema bean with no histogram from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Schema bean as a JSON Object
	 */
	public Response getSchemaByGuidNoHistogram(String guid) {
		try {
			// Do not show histogram
			Schema schema = h2Dao.getSchemaByGuid(guid, false);
			String jsonString = SerializationUtility.serialize(schema);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets SchemaMetaData bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return SchemaMetaData bean as a JSON Object
	 */
	public Response getSchemaMetaDataByGuid(String guid) {
		try {
			SchemaMetaData schemaMetaData = h2Dao.getSchemaMetaDataByGuid(guid);
			String jsonString = SerializationUtility.serialize(schemaMetaData);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets Data Sample bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Data Sample bean as a JSON object
	 */
	public Response getSampleByGuid(String guid) {
		try {
			DataSample sample = h2Dao.getSampleByGuid(guid);
			String jsonString = SerializationUtility.serialize(sample);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets Data Sample Meta Data bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Data Sample Meta Data bean as a JSON object
	 */
	public Response getSampleMetaDataByGuid(String guid) {
		try {
			DataSampleMetaData sampleMetaData = h2Dao.getSampleMetaDataByGuid(guid);
			String jsonString = SerializationUtility.serialize(sampleMetaData);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets the field of a given Schema.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSchemaFieldByGuid(String guid) {
		try {
			Map<String, Profile> map = h2Dao.getSchemaFieldByGuid(guid, true);
			String jsonString = SerializationUtility.serialize(map);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets the field meta data of a given Schema.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSchemaMetaDataFieldByGuid(String guid) {
		try {
			Map<String, Profile> map = h2Dao.getSchemaFieldByGuid(guid, false);
			String jsonString = SerializationUtility.serialize(map);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets the field of a given Sample.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSampleFieldByGuid(String guid) {
		try {
			Map<String, Profile> map = h2Dao.getSampleFieldByGuid(guid, true);
			String jsonString = SerializationUtility.serialize(map);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Gets the field meta data of a given Sample.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSampleMetaDataFieldByGuid(String guid) {
		try {
			Map<String, Profile> map = h2Dao.getSampleFieldByGuid(guid, false);
			String jsonString = SerializationUtility.serialize(map);
			JSONObject jObject = new JSONObject(jsonString);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Deletes a Schema by its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Schema
	 * @return
	 */
	public Response deleteSchemaByGuid(String guid) {
		try {
			h2Dao.deleteSchemaByGuid(guid);
			JSONObject jObject = new JSONObject();
			jObject.put("deleted", guid);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Deletes a Data Sample by its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Data Sample
	 * @return
	 */
	public Response deleteSampleByGuid(String guid) {
		try {
			h2Dao.deleteSampleByGuid(guid);
			JSONObject jObject = new JSONObject();
			jObject.put("deleted", guid);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Takes an ambiguous GUID and determinatively deletes it from the database.
	 * 
	 * @param guid
	 *            A GUID from either a Schema or Data Sample
	 * @return
	 */
	public Response deleteByGuid(String guid) {
		try {
			h2Dao.deleteByGuid(guid);
			JSONObject jObject = new JSONObject();
			jObject.put("deleted", guid);
			return generatedResponse(Status.ACCEPTED, jObject.toString());
		} catch (DataAccessException e) {
			logger.error(e.toString());
			return generatedEmptyJsonErrorResponse(Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error(bundle.getString("h2.unexpected.error"));
			return generatedEmptyJsonErrorResponse(Status.INTERNAL_SERVER_ERROR);
		}
	}

	// Private methods
	private Response generatedResponse(Response.Status status, String message) {
		return Response.status(status).entity(message).build();
	}
	
	private Response generatedEmptyJsonErrorResponse(Response.Status status) {
		JSONObject emptyJson = new JSONObject();
		return Response.status(status).entity(emptyJson.toString()).build();
	}
}
