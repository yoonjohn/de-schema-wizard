package com.deleidos.sw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dmf.accessor.ServiceLayerAccessor;
import com.deleidos.dmf.analyzer.TikaAnalyzer;
import com.deleidos.dmf.exception.AnalyticsUndetectableTypeException;
import com.deleidos.dmf.exception.AnalyticsUnsupportedParserException;
import com.deleidos.dmf.exception.AnalyzerException;
import com.deleidos.dp.exceptions.DataAccessException;

@Path("/")
@SuppressWarnings("unchecked")
public class SchemaWizardController implements ISchemaWizardController {

	private static Logger logger = Logger.getLogger(SchemaWizardController.class);

	private String uploadDirectory = null;

	private ResourceBundle bundle = ResourceBundle.getBundle("error-messages");

	TikaAnalyzer analyzerService;

	ServiceLayerAccessor dataService;

	/**
	 * Constructor
	 *
	 * @param service
	 */
	public SchemaWizardController(TikaAnalyzer analyzerService, ServiceLayerAccessor dataService) {
		super();
		this.analyzerService = analyzerService;
		this.dataService = dataService;
		InputStream inputStream = null;
		try {
			Properties prop = new Properties();
			inputStream = getClass().getClassLoader().getResourceAsStream("config.properties");
			if (inputStream != null) {
				prop.load(inputStream);
				uploadDirectory = prop.getProperty("uploadDirectory");
				inputStream.close();
			} else {
				throw new FileNotFoundException("property file 'config.properties' not found in the classpath");
			}
		} catch (IOException e) {
			logger.error("Exception: " + e);
		}
	} // constructor

	/**
	 * Return the servlet's session id for this session
	 *
	 * @return <sessionId>
	 *
	 *         Only available to Schema Wizard
	 */
	@GET
	@Path("/sessionId")
	public Response getSessionId(@Context HttpServletRequest request) {
		String sessionId = request.getSession().getId();
		logger.debug("");
		logger.debug("sessionId: " + sessionId);
		JSONObject jObject = new JSONObject();
		jObject.put("sessionId", sessionId);
		logger.debug("");
		logger.debug("jObject: " + jObject.toString());
		logger.debug("");
		if (sessionId == null || sessionId.equals("")) {
			return Response.status(Response.Status.EXPECTATION_FAILED).build();
		} else {
			return Response.ok(jObject.toString(), MediaType.APPLICATION_JSON).build();
		}
	} // getSessionId

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
	public Response getCatalog() {
		Response response;
		logger.debug("");
		logger.debug("getCatalog request received");
		logger.debug("");
		response = dataService.getCatalog();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getCatalog

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
	public Response saveSchema(String schema) {
		logger.debug("");
		logger.debug("saveSchema request received");
		logger.debug("");
		JSONObject jObject = new JSONObject(schema);
		logger.debug("");
		logger.debug("schema: " + jObject.toString());
		logger.debug("");
		Response response = dataService.addSchema(jObject);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // saveSchema

	/**
	 * Query the H2 database for the schema with the requested id.
	 *
	 * @param schemaId
	 * @return
	 */
	@GET
	@Path("/schema/{schemaId}")
	public Response getSchema(@PathParam("schemaId") String schemaId) {
		logger.debug("");
		logger.debug("getSchema request received: " + schemaId);
		logger.debug("");
		logger.debug("no histogram");
		Response response = dataService.getSchemaByGuid(schemaId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSchema

	/**
	 * Query the H2 database for the schema with the requested id. Do not
	 * include histogram data in the returned object.
	 *
	 * @param schemaId
	 * @return <schema>
	 */
	@GET
	@Path("/schema/{schemaId}/{nohistogram}")
	public Response getSchema(@PathParam("schemaId") String schemaId, @PathParam("nohistogram") String nohistogram) {
		logger.debug("");
		logger.debug("getSchema request received: " + schemaId + "   " + nohistogram);
		logger.debug("");
		logger.debug("no histogram");
		Response response = dataService.getSchemaByGuidNoHistogram(schemaId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSchema

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
	public Response deleteSchema(@PathParam("schemaId") String schemaId) {
		logger.debug("");
		logger.debug("deleteSchema request received: " + schemaId);
		logger.debug("");
		Response response = dataService.deleteSchemaByGuid(schemaId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // deleteSchema

	/**
	 * Query the H2 database for the schema with the requested id. Return
	 * metadata for the schema
	 *
	 * @param schemaId
	 * @return <schema-meta-data>
	 */
	@GET
	@Path("/schemaMetaData/{schemaId}")
	public Response getSchemaMetaData(@PathParam("schemaId") String schemaId) {
		logger.debug("");
		logger.debug("getSchemaMetaData request received: " + schemaId);
		logger.debug("");
		Response response = dataService.getSchemaMetaDataByGuid(schemaId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSchemaMetaData

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
	public Response addSchemaField(@PathParam("schemaId") String schemaId) {
		logger.debug("");
		logger.debug("addSchemaField request received: " + schemaId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // addSchemaField

	/**
	 * Query the H2 database for the schema field with the requested ids.
	 *
	 * @param schemaId
	 * @param fieldId
	 * @return <field-descriptor>
	 */
	@GET
	@Path("/schemaField/{schemaId}/{fieldId}")
	public Response getSchemaField(@PathParam("schemaId") String schemaId, @PathParam("fieldId") String fieldId) {
		logger.debug("");
		logger.debug("getSchemaField request received: " + schemaId + "   " + fieldId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSchemaField

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
	public Response deleteSchemaField(@PathParam("schemaId") String schemaId, @PathParam("fieldId") String fieldId) {
		logger.debug("");
		logger.debug("deleteSchemaField request received: " + schemaId + "   " + fieldId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // deleteSchemaField

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
			@PathParam("fieldId") String fieldId) {
		logger.debug("");
		logger.debug("getSchemaFieldMetaData request received: " + schemaId + "   " + fieldId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSchemaFieldMetaData

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
	public Response saveSampleData(@PathParam("sampleDataId") String sampleDataId) {
		logger.debug("");
		logger.debug("saveSampleData request received: " + sampleDataId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // saveSampleData

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
	public Response getSampleData(@PathParam("sampleDataId") String sampleDataId) {
		logger.debug("");
		logger.debug("getSampleData request received: " + sampleDataId);
		logger.debug("");
		Response response = dataService.getSampleByGuid(sampleDataId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSampleData

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
	public Response deleteSampleData(@PathParam("sampleDataId") String sampleDataId) {
		logger.debug("");
		logger.debug("deleteSampleData request received: " + sampleDataId);
		logger.debug("");
        Response response = dataService.deleteSampleByGuid(sampleDataId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // deleteSampleData

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
	public Response getSampleDataMetaData(@PathParam("sampleDataId") String sampleDataId) {
		logger.debug("");
		logger.debug("getSampleDataMetaData request received: " + sampleDataId);
		logger.debug("");
		Response response = dataService.getSampleMetaDataByGuid(sampleDataId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSampleDataMetaData

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
			@PathParam("fieldId") String fieldId) {
		logger.debug("");
		logger.debug("getSampleDataField request received: " + sampleDataId + "   " + fieldId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSampleDataField

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
			@PathParam("fieldId") String fieldId) {
		logger.debug("");
		logger.debug("deleteSampleDataField request received: " + sampleDataId + "   " + fieldId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // deleteSampleDataField

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
			@PathParam("fieldId") String fieldId) {
		logger.debug("");
		logger.debug("getSampleDataMetaData request received: " + sampleDataId + "   " + fieldId);
		logger.debug("");
		Response response = Response.status(Response.Status.ACCEPTED).entity("Method not yet implemented").build();
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getSampleDataMetaData

	/**
	 * Upload one or more user modified data samples. Return a JSON Array of the
	 * proposed schema.
	 *
	 * @param request
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/uploadModifiedSamples")
	public Response uploadModifiedSamples(@Context HttpServletRequest request, String schemaAnalysisData) {
		String sessionId = request.getSession().getId();
		String schemaGuid = request.getParameter("schemaGuid");
		String domain = request.getParameter("domain");
		logger.debug("");
		logger.debug("uploadModifiedSamples");
		logger.debug("schemaGuid: " + schemaGuid);
		logger.debug("domain:    " + domain);
		logger.debug("sessionId: " + sessionId);
		logger.debug("schemaAnalysisData");
        logger.debug("");
		logger.debug(schemaAnalysisData.toString());
		logger.debug("");
		JSONObject schemaAnalysisDataJson = new JSONObject(schemaAnalysisData);
		JSONObject jObject = new JSONObject();
		try {
			jObject = analyzerService.analyzeSchema(schemaAnalysisDataJson, domain, sessionId);
			logger.debug("");
			return Response.status(Response.Status.ACCEPTED).entity(jObject.toString()).build();
		} catch (IOException e) {
			logger.error(e);
			// TODO file not found on disk dialog
		} catch (AnalyzerException e) {
			logger.error(e);
			// TODO analysis error dialog
		} catch (DataAccessException e) {
			logger.error(e);
			// TODO data access error dialog
		}
		return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(bundle.getString("analyzer.error")).build();
	} // uploadModifiedSamples

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
	public Response uploadSamples(@Context HttpServletRequest request) {

		logger.debug("Received upload request");

		String domain = request.getParameter("domain");
		String tolerance = request.getParameter("tolerance");
		String schemaGuid = request.getParameter("schemaGuid");
		String sessionId = request.getSession().getId();
		logger.debug("");
		logger.debug("sessionId: " + sessionId);
		logger.debug("domain:    " + domain);
		logger.debug("tolerance: " + tolerance);
		logger.debug("schemaGuid: " + schemaGuid);
		logger.debug("");

		String fileFormatType = null;
		JSONObject jObject = new JSONObject();
		JSONArray jArray = new JSONArray();

		if (ServletFileUpload.isMultipartContent(request)) {
			try {
				List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
				String[] guids = new String[multiparts.size()];
				logger.debug("");
				logger.debug("guids.size(): " + guids.length);
				logger.debug("");
				int guidsIndex = -1;
				for (FileItem item : multiparts) {
					if (!item.isFormField()) {
						// write file to file system
						String name = new File(item.getName()).getName();
						item.write(new File(uploadDirectory + File.separator + name));
						logger.debug("");
						logger.debug("File: " + uploadDirectory + File.separator + name);
						logger.debug("");
						// call service routine to analyse file
						guids[++guidsIndex] = analyzerService.analyzeSample(uploadDirectory + File.separator + name,
								domain, tolerance, sessionId, guidsIndex, multiparts.size());
					}
				}
				jArray = analyzerService.matchAnalyzedFields(schemaGuid, guids);
				logger.debug("");
				logger.debug("jArray.length(): " + jArray.length());
				logger.debug("");
				logger.debug("jArray: " + jArray.toString());
				logger.debug("");
				logger.debug("File uploaded successfully");
			} catch (AnalyticsUndetectableTypeException undetectableType) {
				logger.debug("Undetectable type.");
				logger.error(undetectableType);
				return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(bundle.getString("upload.failed")).build();
			} catch (AnalyticsUnsupportedParserException unsupportedParser) {
				logger.debug("Unsupported parser.");
				logger.error(unsupportedParser);
				return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(bundle.getString("upload.failed")).build();
			} catch (DataAccessException dataAccessError) {
				logger.debug("Data access error.");
				logger.error(dataAccessError);
				return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(bundle.getString("upload.failed")).build();
			} catch (Exception ex) {
				logger.debug("File Upload Failed due to " + ex);
				logger.error(ex);
				return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(bundle.getString("upload.failed")).build();
			}
		} else {
			request.setAttribute("message", "This Servlet only handles file upload requests");
		}
		return Response.status(Response.Status.ACCEPTED).entity(jArray.toString()).build();
	} // uploadSamples

	/**
	 * Create a new domain object
	 *
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@POST
	@Path("/domain")
	public Response createDomain(@Context HttpServletRequest request) {
		Response response;
		logger.debug("");
		logger.debug("createDomain request received");
		JSONObject jObject = new JSONObject(request.getParameter("data"));
		logger.debug("");
		logger.debug(jObject.toString());
		response = dataService.createDomain(jObject);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // createDomain

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
	public void updateDomain(@PathParam("domainId") String domainId) {
		logger.debug("");
		logger.debug("updateDomain request received");
		logger.debug("");
		logger.debug("domainId: " + domainId);
		logger.debug("");
	} // updateDomain

	/**
	 * Delete a domain object
	 *
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@DELETE
	@Path("/domain")
	public Response deleteDomain(@Context HttpServletRequest request) {
		String returnValue = null;
		logger.debug("");
		logger.debug("deleteDomain request received");
		JSONObject jObject = new JSONObject(request.getParameter("data"));
		logger.debug(jObject.toString());
		logger.debug("");
		Response response = dataService.deleteDomain(jObject);
		returnValue = response.getEntity().toString();
		logger.debug("");
		logger.debug("Response code: " + response.getStatus());
		logger.debug("returnValue: " + returnValue);
		logger.debug("");
		return response;
	} // deleteDomain

	/**
	 * Query the Interpretation Engine for interpretations of a given domain.
	 * Return a JSON Object of the list of interpretations.
	 *
	 * @param domainId
	 * @return a map of <interpretation-descriptor> objects
	 */
	@GET
	@Path("/{domainId}/interpretation")
	public Response getInterpretations(@PathParam("domainId") String domainId) {
		Response response;
		logger.debug("");
		logger.debug("getInterpretation request received");
		logger.debug("");
		logger.debug("domainId: " + domainId);
		logger.debug("");
		response = dataService.getDomainInterpretations(domainId);
		JSONObject json = new JSONObject(response.getEntity().toString());
		logger.debug("");
		logger.debug("Num Interpretations: " + json.length());
		logger.debug("");
		logger.debug(response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // getInterpretations

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
	public Response updateInterpretation(@PathParam("domainId") String domainId, @Context HttpServletRequest request) {
		Response response;
		logger.debug("");
		logger.debug("updateInterpretation request received");
		logger.debug("");
		logger.debug("domainId: " + domainId);
		JSONObject jObject = new JSONObject(request.getParameter("data"));
		logger.debug(jObject.toString());
		logger.debug("");
		response = dataService.updateInterpretation(jObject);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // updateInterpretation

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
	public Response createInterpretation(@PathParam("domainId") String domainId, @Context HttpServletRequest request) {
		Response response;
		logger.debug("");
		logger.debug("createInterpretation request received");
		logger.debug("");
		logger.debug("domainId: " + domainId);
		JSONObject jObject = new JSONObject(request.getParameter("data"));
		logger.debug(jObject.toString());
		logger.debug("");
		response = dataService.createInterpretation(jObject);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // createInterpretation

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
			@PathParam("interpretationId") String interpretationId) {
		Response response;
		logger.debug("");
		logger.debug("deleteInterpretation request received");
		logger.debug("");
		logger.debug("domainId: " + domainId);
		logger.debug("");
		logger.debug("interpretationId: " + interpretationId);
		logger.debug("");
		JSONObject jObject = new JSONObject();
		jObject.put("iId", interpretationId);
		logger.debug(jObject.toString());
		logger.debug("");
		response = dataService.deleteInterpretation(jObject);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.getEntity().toString());
		logger.debug("");
		return response;
	} // deleteInterpretation

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
	public Response validatePythonScript(@PathParam("interpretationId") String interpretationId) {
		Response response;
		logger.debug("");
		logger.debug("validatePythonScript request received");
		logger.debug("");
		logger.debug("interpretationId: " + interpretationId);
		response = dataService.validatePythonScript(interpretationId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
//		 logger.debug(response.toString());
//		 logger.debug("entity");
//		 logger.debug(response.getEntity().toString());
		JSONObject entity = new JSONObject(response.getEntity().toString());
//		 logger.debug("entity");
//		 logger.debug(entity);
		JSONArray annotations = new JSONArray(entity.get("returnValue").toString());
//		 logger.debug("annotations");
//		 logger.debug(annotations);
		JSONObject jObject = new JSONObject();
		jObject.put("annotations", annotations);
//		 logger.debug("");
		logger.debug("annotations: " + jObject.toString());
		response = Response.status(response.getStatus()).entity(jObject.toString()).build();
		return response;
	} // validatePythonScript

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
	public Response testPythonScript(@PathParam("interpretationId") String interpretationId) {
		Response response;
		logger.debug("");
		logger.debug("testPythonScript request received");
		logger.debug("");
		logger.debug("interpretationId: " + interpretationId);
		response = dataService.testPythonScript(interpretationId);
		logger.debug("");
		logger.debug("Status code: " + response.getStatus());
		logger.debug(response.toString());
		logger.debug("entity");
		logger.debug(response.getEntity().toString());
		JSONObject entity = new JSONObject(response.getEntity().toString());
		logger.debug("entity");
		logger.debug(entity);
		JSONObject consoleOutput = new JSONObject(entity.get("returnValue").toString());
		logger.debug("consoleOutput");
		logger.debug(consoleOutput);
		JSONObject jObject = new JSONObject();
		jObject.put("consoleOutput", consoleOutput);
		logger.debug("");
		logger.debug("consoleOutput: " + jObject.toString());
		response = Response.status(response.getStatus()).entity(jObject.toString()).build();
		return response;
	} // testPythonScript

	/**
	 * Test this REST interface
	 *
	 * @return
	 *
	 * 		Only available to Schema Wizard
	 */
	@GET
	@Path("/test")
	public String test() {
		logger.debug("");
		logger.debug("Testing the Schema Wizard REST endpoint connection.");
		logger.debug("");
		JSONObject jObject = new JSONObject();
		logger.debug("");
		logger.debug(jObject.toString());
		logger.debug("");
		jObject.put("SchemaWizard", "Up");
		return jObject.toString();
	} // test

} // SchemaWizardController
