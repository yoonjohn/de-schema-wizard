package com.deleidos.sw;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
/*import javax.ws.rs.QueryParam;*/

/**
 * Schema Wizard Controller interface.
 *
 * Receive requests from Schema Wizard and Viz Wiz web applications. Return data
 * as string representation of either a JSON Object or JSON Array
 *
 * The JSON description of a returned value is shown in angle brackets, e.g.,
 * <catalog>. Refer to accompanying documentation for details.
 *
 */
@Path("/")
public interface ISchemaWizardController {

	/**
	 * Return the servlet's session id for this session
	 *
	 * @return <sessionId>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/sessionId")
	public Response getSessionId(@Context HttpServletRequest request);

	/**
	 * Query the H2 database for the entire catalog. Return metadata for catalog
	 * schemas, data samples, and domains.
	 *
	 * @return <catalog>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/catalog")
	public Response getCatalog();

	/**
	 * Save the schema in the H2 database. The schema is sent in the payload of
	 * the request.
	 *
	 * @param schema
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/schema")
	public Response saveSchema(String schema);

	/**
	 * Query the H2 database for the schema with the requested id.
	 *
	 * @param schemaId
	 * @return <schema>
	 */
	@GET
	@Path("/schema/{schemaId}")
	public Response getSchema(@PathParam("schemaId") String schemaId);

	/**
	 * Query the H2 database for the schema with the requested id. Do not
	 * include histogram data in the returned object.
	 *
	 * @param schemaId
	 * @return <schema>
	 */
	@GET
	@Path("/schema/{schemaId}/{nohistogram}")
	public Response getSchema(@PathParam("schemaId") String schemaId, @PathParam("nohistogram") String nohistogram);

	/**
	 * Delete the schema with the requested id from the H2 database.
	 *
	 * @param schemaId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/schema/{schemaId}")
	public Response deleteSchema(@PathParam("schemaId") String schemaId);

	/**
	 * Query the H2 database for the schema with the requested id. Return
	 * metadata for the schema
	 *
	 * @param schemaId
	 * @return <schema-meta-data>
	 */
	@GET
	@Path("/schemaMetaData/{schemaId}")
	public Response getSchemaMetaData(@PathParam("schemaId") String schemaId);

	/**
	 * Add a field to the schema. The field meta data and characterization sent
	 * in the payload of the request. Meta dat is <main-type-value>,
	 * <detail-json<>>, <classification-json>.
	 *
	 * @param schemaId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/schemaField/{schemaId}")
	public Response addSchemaField(@PathParam("schemaId") String schemaId);

	/**
	 * Query the H2 database for the schema field with the requested ids.
	 *
	 * @param schemaId
	 * @param fieldId
	 * @return <field-descriptor>
	 */
	@GET
	@Path("/schemaField/{schemaId}/{fieldId}")
	public Response getSchemaField(@PathParam("schemaId") String schemaId, @PathParam("fieldId") String fieldId);

	/**
	 * Delete the schema field with the requested ids from the H2 database.
	 *
	 * @param schemaId
	 * @param fieldId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/schemaField/{schemaId}/{fieldId}")
	public Response deleteSchemaField(@PathParam("schemaId") String schemaId, @PathParam("fieldId") String fieldId);

	/**
	 * Query the H2 database for the schema field with the requested ids. Return
	 * metadata for the schema field
	 *
	 * @param schemaId
	 * @param fieldId
	 * @return <tbd>
	 */
	@GET
	@Path("/schemaFieldMetaData/{schemaId}/{fieldId}")
	public Response getSchemaFieldMetaData(@PathParam("schemaId") String schemaId,
			@PathParam("fieldId") String fieldId);

	/**
	 * Save the sample data in the H2 database for the sample data with the
	 * requested id. The sample data name is sent in the payload of the request.
	 *
	 * @param sampleDataId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/sampleData/{sampleDataId}")
	public Response saveSampleData(@PathParam("sampleDataId") String sampleDataId);

	/**
	 * Query the H2 database for the sample data with the requested id.
	 *
	 * @param sampleDataId
	 * @return <data-sample>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/sampleData/{sampleDataId}")
	public Response getSampleData(@PathParam("sampleDataId") String sampleDataId);

	/**
	 * Delete the sample data with the requested id from the H2 database.
	 *
	 * @param sampleDataId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/sampleData/{sampleDataId}")
	public Response deleteSampleData(@PathParam("sampleDataId") String sampleDataId);

	/**
	 * Query the H2 database for the sample data with the requested id. Return
	 * metadata for the sample data
	 *
	 * @param sampleDataId
	 * @return <tbd>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/sampleDataMetaData/{sampleDataId}")
	public Response getSampleDataMetaData(@PathParam("sampleDataId") String sampleDataId);

	/**
	 * Query the H2 database for the sample data field with the requested ids.
	 *
	 * @param sampleDataId
	 * @param fieldId
	 * @return <field-descriptor>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/sampleDataField/{sampleDataId}/{fieldId}")
	public Response getSampleDataField(@PathParam("sampleDataId") String sampleDataId,
			@PathParam("fieldId") String fieldId);

	/**
	 * Delete the sample data field with the requested ids from the H2 database.
	 *
	 * @param sampleDataId
	 * @param fieldId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/sampleDataField/{sampleDataId}/{fieldId}")
	public Response deleteSampleDataField(@PathParam("sampleDataId") String sampleDataId,
			@PathParam("fieldId") String fieldId);

	/**
	 * Query the H2 database for the sample data field with the requested ids.
	 * Return metadata for the sample data field
	 *
	 * @param sampleDataId
	 * @param fieldId
	 * @return <tbd>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/sampleDataFieldMetaData/{sampleDataId}/{fieldId}")
	public Response getSampleDataMetaData(@PathParam("sampleDataId") String sampleDataId,
			@PathParam("fieldId") String fieldId);

	/**
	 * Upload one or more user modified data samples. Return a JSON Array of the
	 * proposed schema.
	 *
	 * @param request
	 * @param dataSources
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/uploadModifiedSamples")
	public Response uploadModifiedSamples(@Context HttpServletRequest request, String schemaAnalysisData);

	/**
	 * Upload one or more files for analysis. Return a JSON Array of the Data
	 * Sample Descriptors (JSON Object) for each file.
	 *
	 * @param request
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/upload")
	public Response uploadSamples(@Context HttpServletRequest request);

	/**
	 * Create a new domain object
	 *
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/domain")
	public Response createDomain(@Context HttpServletRequest request);

	/**
	 * Upload an updated domain object
	 *
	 * @param domainId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/domain/{domainId}")
	public void updateDomain(@PathParam("domainId") String domainId);

	/**
	 * Delete a domain object
	 *
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/domain")
	public Response deleteDomain(@Context HttpServletRequest request);

	/**
	 * Query the Interpretation Engine for interpretations of a given domain.
	 * Return a JSON Object of the list of interpretations.
	 *
	 * @param domainId
	 * @return a map of <interpretation-descriptor> objects
	 */
	@GET
	@Path("/{domainId}/interpretation")
	public Response getInterpretations(@PathParam("domainId") String domainId);

	/**
	 * Upload an updated interpretation object
	 *
	 * @param domainId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@PUT
	@Path("/{domainId}/interpretation")
	public Response updateInterpretation(@PathParam("domainId") String domainId, @Context HttpServletRequest request);

	/**
	 * Create an updated interpretation object
	 *
	 * @param domainId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/{domainId}/interpretation")
	public Response createInterpretation(@PathParam("domainId") String domainId, @Context HttpServletRequest request);

	/**
	 * Delete an interpretation object
	 *
	 * @param domainId
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/{domainId}/interpretation/{interpretationId}")
	public Response deleteInterpretation(@PathParam("domainId") String domainId,
			@PathParam("interpretationId") String interpretationId);

	/**
	 * Test a python script
	 *
	 * @param interpretation
	 *            object id
	 * @return true if the script is valid
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/python/validate/{interpretationId}")
	public Response validatePythonScript(@PathParam("interpretationId") String interpretationId);

	/**
	 * Test a python script
	 *
	 * @param interpretation
	 *            object id
	 * @return console output of test execution
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/python/test/{interpretationId}")
	public Response testPythonScript(@PathParam("interpretationId") String interpretationId);

	/**
	 * Test this REST interface
	 *
	 * @return
	 */
	@GET
	@Path("/test")
	public String test();

} // ISchemaWizardController
