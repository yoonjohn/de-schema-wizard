package com.deleidos.dp.interpretation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.deserializors.SerializationUtility;
import com.deleidos.dp.exceptions.DataAccessException;
import com.fasterxml.jackson.core.type.TypeReference;

public class HttpInterpretationEngine implements InterpretationEngine {
	private static final Logger logger = Logger.getLogger(HttpInterpretationEngine.class);

	private final String ROOT_CONTEXT = "/blueprint";
	private final String CREATE_PATH = "/create";
	private final String UPDATE_PATH = "/update";
	private final String DELETE_PATH = "/delete";
	private final String DOMAIN_PATH = "/domain";
	private final String INTERPRETATION_PATH = "/interpretation";
	private final String GET_DOMAINS_PATH = "/domains";
	private final String GET_INTERPRETATIONS_BY_DOMAIN_PATH_PREFIX = "/domain";
	private final String INTERPRET_PATH = "/interpret";
	private final String REVERSE_GEO_PATH = "/reversegeo";
	private final String VALIDATE_PYTHON_PATH = "/validatePythonScript";
	private final String TEST_PYTHON_PATH = "/testPythonScript";
	private final int CONNECTION_TIMEOUT = 8000;
	private final int READ_TIMEOUT = 8000;
	private String baseUrl;
	private IEConfig config;
	private ResourceBundle bundle = ResourceBundle.getBundle("error-messages");
	private boolean isLive;

	public HttpInterpretationEngine(String url) throws DataAccessException {
		this(IEConfig.dynamicConfig(url));
	}

	public HttpInterpretationEngine(IEConfig config) throws DataAccessException {
		this.config = config;
		this.baseUrl = config.getUrl() + ROOT_CONTEXT;
		if (!testConnection()) {
			throw new DataAccessException("Could not connect to Interpretation Engine at " + config.getUrl() + ".");
		} else {
			logger.info("Initialized Interpretation Engine at " + baseUrl + ".");
		}
	}

	public HttpInterpretationEngine() throws DataAccessException, IOException {
		this(new IEConfig().load());
	}

	@Override
	public JSONObject getInterpretationListByDomainGuid(String domainName) throws DataAccessException {
		logger.debug("Get Interpretations by Domain received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + GET_INTERPRETATIONS_BY_DOMAIN_PATH_PREFIX + "/" + domainName;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON).get();
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.OK) {
				JSONObject json = new JSONObject();
				JSONArray jArray = new JSONArray(body);
				for (int i = 0; i < jArray.length(); i++) {
					json.put(jArray.getJSONObject(i).getString("iName"), jArray.getJSONObject(i));
				}
				return json;
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			throw new DataAccessException(e.toString());
		}
	}

	@Override
	public JSONArray getAvailableDomains() throws DataAccessException {
		logger.debug("Get Domains request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + GET_DOMAINS_PATH;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON).get();
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.OK) {
				JSONArray jArray = new JSONArray(body);
				return jArray;
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public Map<String, Profile> interpret(String domainGuid, Map<String, Profile> profileMap)
			throws DataAccessException {
		logger.debug("Interpret request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + INTERPRET_PATH;
			logger.debug("Connecting with URL " + url);

			JSONObject profileJson = new JSONObject(SerializationUtility.serialize(profileMap));
			final String exampleValuesKey = "example-values";
			for (String key : profileJson.keySet()) {
				JSONArray exampleValues = new JSONArray(
						SerializationUtility.serialize(profileMap.get(key).getExampleValues()));
				profileJson.getJSONObject(key).put(exampleValuesKey, exampleValues);
			}
			JSONObject formContent = new JSONObject();
			formContent.put("domain_guid", domainGuid);
			formContent.put("profile", profileJson);

			logger.debug("Sending object to Interpretation Engine: ");
			logger.debug(formContent);

			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(formContent.toString()), Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.OK) {
				try {
					Map<String, List<Interpretation>> interpretationMapping = SerializationUtility.deserialize(body,
							new TypeReference<Map<String, List<Interpretation>>>() {
							});
					profileMap.forEach((k, v) -> {
						if (interpretationMapping.containsKey(k) && !interpretationMapping.get(k).isEmpty()) {
							v.setInterpretation(interpretationMapping.get(k).get(0));
						} else {
							v.setInterpretation(Interpretation.UNKNOWN);
						}
					});

					return profileMap;
				} catch (Exception e) {
					logger.error("Error using Interpretation Engine: " + e);
					throw new DataAccessException(
							"Data from the Python Interpretation Engine was received, but there was an error retreiving the data. "
									+ e);
				}
			} else if (status == Status.FORBIDDEN) {
				throw new DataAccessException(bundle.getString("create.domain.duplicate.name"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public List<String> getCountryCodesFromCoordinateList(List<Double[]> latlngs) throws DataAccessException {
		logger.debug("Get Country Codes request received. Sending request to the Python Interpretation Engine.");
		List<String> countryList = null;
		try {
			URL url = new URL(baseUrl + REVERSE_GEO_PATH);
			logger.debug("Connecting with URL " + url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			JSONArray coordinateJson = new JSONArray(SerializationUtility.serialize(latlngs));
			JSONObject formContent = new JSONObject();
			formContent.put("coordinates", coordinateJson);

			OutputStream urlOutputStream = conn.getOutputStream();
			String content = formContent.toString();
			byte[] bytes = content.getBytes();
			urlOutputStream.write(bytes);
			urlOutputStream.flush();

			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				logger.error("Received an unsuccessful response code from Interpretation Engine REST interface " + "at "
						+ url + ".  Returning null.");
				logger.error(conn.getResponseCode() + ": " + conn.getResponseMessage());
				throw new DataAccessException(
						"There was an error receiving and interpretation from the Python Interpretation Engine");
			} else {
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String buf;
				while ((buf = br.readLine()) != null) {
					sb.append(buf);
				}
				logger.debug("Received content from Interpretation Engine: ");
				logger.debug(sb.toString());
				try {
					countryList = SerializationUtility.deserialize(sb.toString(), new TypeReference<List<String>>() {
					});
				} catch (Exception e) {
					logger.error("Error using Interpretation Engine: " + e);
					throw new DataAccessException(
							"Data from the Python Interpretation Engine was received, but there was an error retreiving the data. "
									+ e);
				}
			}
			conn.disconnect();
		} catch (MalformedURLException e) {
			logger.error(e);
			throw new DataAccessException("There was a problem with the URL. " + e);
		} catch (IOException e) {
			logger.error(e);
			throw new DataAccessException(
					"There was a problem reading the data from the Python Interpretation Engine " + e);
		}
		return countryList;
	}

	@Override
	public JSONObject createDomain(JSONObject domainJson) throws DataAccessException {
		logger.debug("Create Domain request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + CREATE_PATH + DOMAIN_PATH;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(domainJson.toString()), Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.CREATED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else if (status == Status.FORBIDDEN) {
				throw new DataAccessException(bundle.getString("create.domain.duplicate.name"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public JSONObject createInterpretation(JSONObject interpretationJson) throws DataAccessException {
		logger.debug("Create Interpretation request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + CREATE_PATH + INTERPRETATION_PATH;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(interpretationJson.toString()), Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.CREATED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else if (status == Status.FORBIDDEN) {
				throw new DataAccessException(bundle.getString("create.interpretation.duplicate.name"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public JSONObject updateDomain(JSONObject domainJson) throws DataAccessException {
		logger.debug("Update Interpretation request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + UPDATE_PATH + DOMAIN_PATH;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(domainJson.toString()), Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.ACCEPTED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else if (status == Status.CONFLICT) {
				throw new DataAccessException(bundle.getString("update.domain.many.found"));
			} else if (status == Status.NOT_FOUND) {
				throw new DataAccessException(bundle.getString("update.domain.none.found"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public JSONObject updateInterpretation(JSONObject interpretationJson) throws DataAccessException {
		logger.debug("Update Interpretation request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + UPDATE_PATH + INTERPRETATION_PATH;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(interpretationJson.toString()), Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.ACCEPTED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else if (status == Status.CONFLICT) {
				throw new DataAccessException(bundle.getString("update.interpretation.many.found"));
			} else if (status == Status.NOT_FOUND) {
				throw new DataAccessException(bundle.getString("update.interpretation.none.found"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public JSONObject deleteDomain(JSONObject domainJson) throws DataAccessException {
		logger.debug("Delete Domain request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String dId = domainJson.getString("dId");
			String url = baseUrl + DELETE_PATH + DOMAIN_PATH + "/" + dId;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request().delete();
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.ACCEPTED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else if (status == Status.CONFLICT) {
				throw new DataAccessException(bundle.getString("delete.domain.many.found"));
			} else if (status == Status.NOT_FOUND) {
				throw new DataAccessException(bundle.getString("delete.domain.none.found"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	@Override
	public JSONObject deleteInterpretation(JSONObject interpretationJson) throws DataAccessException {
		logger.debug("Delete Interpretation request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String iId = interpretationJson.getString("iId");
			String url = baseUrl + DELETE_PATH + INTERPRETATION_PATH + "/" + iId;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request().delete();
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.ACCEPTED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else if (status == Status.CONFLICT) {
				throw new DataAccessException(bundle.getString("delete.interpretation.many.found"));
			} else if (status == Status.NOT_FOUND) {
				throw new DataAccessException(bundle.getString("delete.interpretation.none.found"));
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	public JSONObject validatePythonScript(JSONObject iIdJson) throws DataAccessException {
		logger.debug("Validate Python Script request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			String url = baseUrl + VALIDATE_PYTHON_PATH;
			logger.debug("Connecting with URL " + url);
			WebTarget resourceTarget = disposableClient().target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(iIdJson.toString()),
					Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.OK || status == Status.EXPECTATION_FAILED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	public JSONObject testPythonScript(JSONObject iIdJson) throws DataAccessException {
		logger.debug("Test Python Script request received. Sending request to the Python Interpretation Engine.");
		logger.debug("");
		try {
			// Create a JSON Object with an iId and metricsDictionary
			logger.debug("Getting Interpretation from Interpretation Engine to build metrics.");
			Response interpretationResponse = getInterpretationById(iIdJson);

			JSONObject interpretationResponseJson = new JSONObject(interpretationResponse.getEntity().toString());
			JSONArray interpretationJsonArr = new JSONArray(interpretationResponseJson.get("returnValue").toString());
			JSONObject interpretationJson = interpretationJsonArr.getJSONObject(0);
			JSONArray exampleValuesJsonArr = interpretationJson.getJSONArray("iSampleData");
			List<Object> exampleValuesArr = new ArrayList<Object>();
			for (int i = 0; i < exampleValuesJsonArr.length(); i++) {
				String exampleValue = exampleValuesJsonArr.getString(i).toString();
				exampleValuesArr.add(exampleValue);
			}
			Profile exampleProfile = BundleProfileAccumulator.generateProfile("fieldName", exampleValuesArr);
			Map<String, Profile> profileMapping = new HashMap<String, Profile>();
			profileMapping.put("exampleField", exampleProfile);

			JSONObject formContent = new JSONObject();
			formContent.put("iId", iIdJson.getString("iId"));
			formContent.put("profile", new JSONObject(SerializationUtility.serialize(profileMapping)));
			logger.debug(formContent.toString());

			// Send information over rest
			String url = baseUrl + TEST_PYTHON_PATH;
			javax.ws.rs.client.Client client = ClientBuilder.newClient();
			client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
			client.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
			WebTarget resourceTarget = client.target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(formContent.toString()), Response.class);
			Status status = Status.fromStatusCode(response.getStatus());
			String body = response.readEntity(String.class);
			logger.debug("Received content from Interpretation Engine.");
			logger.debug("Response code: " + response.getStatus());
			logger.debug("Content: " + body);

			if (status == Status.OK || status == Status.EXPECTATION_FAILED) {
				JSONObject jObject = new JSONObject();
				jObject.put("returnValue", body);
				return jObject;
			} else {
				throw new DataAccessException(response.getEntity().toString());
			}
		} catch (ProcessingException e) {
			logger.error(bundle.getString("ie.server.timeout"));
			logger.error("Connection timeout is currently set to " + CONNECTION_TIMEOUT + "ms.");
			logger.error("Read timeout is currently set to " + READ_TIMEOUT + "ms.");
			throw new DataAccessException(bundle.getString("ie.server.timeout"));
		} catch (Exception e) {
			logger.error(e.toString());
			throw new DataAccessException(bundle.getString("ie.unexpected.error"));
		}
	}

	public boolean testConnection() {
		logger.debug("Checking if Intepretation Engine is up.");
		logger.debug("");
		boolean connected = false;
		try {
			WebTarget resourceTarget = disposableClient().target(baseUrl);
			Response response = resourceTarget.request(MediaType.TEXT_PLAIN).get();
			Status status = Status.fromStatusCode(response.getStatus());
			connected = status == Status.OK;
		} catch (Exception e) {
			logger.error("Connection attempt with Interpretation Engine unsuccessful.");
			logger.error(e);
			connected = false;
		}
		isLive = connected;
		return isLive;
	}

	@Override
	public boolean isLive() {
		return isLive;
	}

	// Private methods
	private Response getInterpretationById(JSONObject iIdJson) {
		logger.debug("Get interpretation by iId request received. Sending request over REST.");
		logger.debug("");
		try {
			logger.debug("Getting Interpretation from Interpretation Engine.");
			String iId = iIdJson.getString("iId");
			String url = baseUrl + INTERPRETATION_PATH + "/" + iId;
			logger.debug("Connecting with URL " + url);
			javax.ws.rs.client.Client client = ClientBuilder.newClient();
			client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
			client.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
			WebTarget resourceTarget = client.target(url);
			Response response = resourceTarget.request(MediaType.APPLICATION_JSON).get();
			Status status = Status.fromStatusCode(response.getStatus());

			if (status == Status.OK) {
				return buildStandardResponse(status, response, "Interpretation", true);
			} else {
				return buildStandardErrorResponse(response, url);
			}
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(bundle.getString("server.unreachable"))
					.build();
		}
	}

	private Response buildStandardErrorResponse(Response response, String url) {
		return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, response, url, "unexpected.error");
	}

	private Response buildErrorResponse(Status status, Response response, String url, String messageKey) {
		logger.error(
				"Received an unsuccessful response code from Interpretation Engine REST interface at " + url + ".");
		logger.error(bundle.getString(messageKey));
		logger.error("Response code: " + response.getStatus());
		logger.error(response.readEntity(String.class));
		return Response.status(status).entity(bundle.getString(messageKey)).build();
	}

	private Response buildStandardResponse(Status status, Response response, String expectedContentName) {
		return buildStandardResponse(status, response, expectedContentName, false);
	}

	private Response buildStandardResponse(Status status, Response response, String expectedContentName,
			boolean array) {
		String body = response.readEntity(String.class);
		Object bodyContent = body;
		if (response.getMediaType().toString().equals(MediaType.APPLICATION_JSON)) {
			logger.info("Content detected as JSON: " + body);
			if (!array) {
				bodyContent = new JSONObject(body);
			} else {
				bodyContent = new JSONArray(body);
			}
		}
		logger.debug("Received content from Interpretation Engine.");
		logger.debug("Response code: " + response.getStatus());
		logger.debug(expectedContentName + ": " + bodyContent);
		JSONObject json = new JSONObject();
		json.put("returnValue", bodyContent);
		return Response.status(status).entity(json.toString()).build();
	}

	private javax.ws.rs.client.Client disposableClient() {
		javax.ws.rs.client.Client client = ClientBuilder.newClient();
		client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		client.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
		return client;
	}
}
