package com.deleidos.dmf.accessor;

import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public interface ServiceLayer {
	/**
	 * Gets the catalog from the H2 Database.
	 * 
	 * @return 
	 */
	public Response getCatalog();

	/**
	 * Gets a list of interpretations from a domain guid.
	 * 
	 * @param domainGuid
	 * @return 
	 */
	public Response getDomainInterpretations(String domainGuid);

	/**
	 * Creates a domain in the Interpretation Engine MongoDB
	 * 
	 * @param domainJson
	 * @return
	 */
	public Response createDomain(JSONObject domainJson);

	/**
	 * Creates an interpretation in the Interpretation Engine MongoDB
	 * 
	 * @param interpretationJson
	 * @return 
	 */
	public Response createInterpretation(JSONObject interpretationJson);

	/**
	 * Updates a domain in the Interpretation Engine MongoDB
	 * 
	 * @param domainJson
	 * @return
	 */
	public Response updateDomain(JSONObject domainJson);

	/**
	 * Updates an interpretation in the Interpretation Engine MongoDB
	 * 
	 * @param interpretationJson
	 * @return
	 */
	public Response updateInterpretation(JSONObject interpretationJson);

	/**
	 * Deletes a domain in the Interpretation Engine MongoDB
	 * 
	 * @param domainJson
	 * @return
	 */
	public Response deleteDomain(JSONObject domainJson);

	/**
	 * Deletes an interpretation in the Interpretation Engine MongoDB
	 * 
	 * @param interpretationJson
	 * @return 
	 */
	public Response deleteInterpretation(JSONObject interpretationJson);
	
	/**
	 * Validates a Python script that is encoded in base64.
	 * 
	 * @param Python script encoded in base64
	 * @return 
	 */
	public Response validatePythonScript(String iId);
	
	/**
	 * Tests a Python script with example data
	 * 
	 * @param iId
	 * @return
	 */
	public Response testPythonScript(String iId);

	/**
	* 
	*/
	public Response addSchema(JSONObject schemaJson);

	/**
	 * Gets Schema bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Schema bean as a JSON Object
	 */
	public Response getSchemaByGuid(String guid);
	
	/**
	 * Gets Schema bean with no histogram from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Schema bean as a JSON Object
	 */
	public Response getSchemaByGuidNoHistogram(String guid);

	/**
	 * Gets SchemaMetaData bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return SchemaMetaData bean as a JSON Object
	 */
	public Response getSchemaMetaDataByGuid(String guid);

	/**
	 * Gets Data Sample bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Data Sample bean as a JSON object
	 */
	public Response getSampleByGuid(String guid);

	/**
	 * Gets Data Sample Meta Data bean from the H2 Database by GUID.
	 * 
	 * @param guid
	 * @return Data Sample Meta Data bean as a JSON object
	 */
	public Response getSampleMetaDataByGuid(String guid);

	/**
	 * Gets the field of a given Schema.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSchemaFieldByGuid(String guid);

	/**
	 * Gets the field meta data of a given Schema.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSchemaMetaDataFieldByGuid(String guid);

	/**
	 * Gets the field of a given Sample.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSampleFieldByGuid(String guid);

	/**
	 * Gets the field meta data of a given Sample.
	 * 
	 * @param guid
	 * @return Field descriptor
	 */
	public Response getSampleMetaDataFieldByGuid(String guid);

	/**
	 * Deletes a Schema by its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Schema
	 * @return 
	 */
	public Response deleteSchemaByGuid(String guid);

	/**
	 * Deletes a Data Sample by its GUID.
	 * 
	 * @param guid
	 *            The GUID of a Data Sample
	 */
	public Response deleteSampleByGuid(String guid);

	/**
	 * Takes an ambiguous GUID and determinatively deletes it from the database.
	 * 
	 * @param guid
	 *            A GUID from either a Schema or Data Sample
	 */
	public Response deleteByGuid(String guid);

}
