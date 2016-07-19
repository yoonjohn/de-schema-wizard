/**
 * 
 */
package com.deleidos.dp.interpretationEngine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.integration.DataProfilerIntegrationEnvironment;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * @author yoonj1
 *
 */
public class HttpInterpretationEngineIT extends DataProfilerIntegrationEnvironment {
	private static final Logger logger = Logger.getLogger(HttpInterpretationEngineIT.class);

	// Test variables
	private final String domainName = "Farm Animals";
	private final String newDomainName = "Zoo Animals";
	private final String interpretationName = "Cow";
	private final String newInterpretationName = "Pig";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testDomains() {
		try {
			// Create domain
			logger.info("Creating domain: " + domainJson().toString());
			JSONObject createDomainResponse = InterpretationEngineFacade.getInstance().createDomain(domainJson());

			logger.info(createDomainResponse.toString());
			logger.info("");

			JSONObject insertedRecord = new JSONObject();
			String domainGuid = createDomainResponse.getString("returnValue");
			insertedRecord.put("dId", domainGuid);

			// Get domains
			logger.info("Getting domains.");
			JSONArray getDomainResponse = InterpretationEngineFacade.getInstance().getAvailableDomains();

			logger.info(getDomainResponse.toString());
			logger.info("");

			// Update domain
			logger.info("Updating domain name to: " + newDomainName);
			insertedRecord.put("dName", newDomainName);
			JSONObject updateDomainResponse = InterpretationEngineFacade.getInstance().updateDomain(insertedRecord);

			logger.info(updateDomainResponse.toString());
			logger.info("");

			// Get domains (confirm update)
			logger.info("Confirming domain name change to: " + newDomainName);
			JSONArray getDomainResponseAgain = InterpretationEngineFacade.getInstance().getAvailableDomains();

			logger.info(getDomainResponseAgain.toString());
			logger.info("");

			JSONArray domains = new JSONArray(getDomainResponseAgain.toString());

			for (int i = 0; i < domains.length(); i++) {
				JSONObject updatedRecord = domains.getJSONObject(i);

				if (updatedRecord.getString("dId").equals(insertedRecord.get("dId"))) {
					assertTrue(insertedRecord.get("dName").equals(newDomainName));
				}
			}

			// Delete domain
			logger.info("Deleting domain");
			JSONObject deleteDomainResponse = InterpretationEngineFacade.getInstance().deleteDomain(insertedRecord);

			logger.info(deleteDomainResponse.toString());
			logger.info("");

		} catch (DataAccessException e) {
			fail();
		}
	}

	@Test
	public void testInterpretations() throws DataAccessException {
		try {
		// Create domain
		logger.info("Creating domain: " + domainJson());
		JSONObject createDomainResponse = InterpretationEngineFacade.getInstance().createDomain(domainJson());
		
		logger.info(createDomainResponse.toString());
		logger.info("");
		String s = createDomainResponse.toString();
		JSONObject returnedDomainRecord = new JSONObject(s);
		JSONObject domainJson = new JSONObject();
		String domainGuid = returnedDomainRecord.getString("returnValue");
		domainJson.put("dId", domainGuid);

		// Create interpretation
		logger.info("Creating interpretation: " + interpretationJson(domainGuid).toString());
		JSONObject createInterpretationResponse = InterpretationEngineFacade.getInstance()
				.createInterpretation(interpretationJson(domainGuid));
		
		logger.info(createInterpretationResponse.toString());
		logger.info("");

		JSONObject returnedRecord = new JSONObject(createInterpretationResponse.toString());
		JSONObject insertedRecord = new JSONObject();
		insertedRecord.put("iId", returnedRecord.getString("returnValue"));
		insertedRecord.put("iDomainId", domainGuid);

		// Get interpretations
		logger.info("Getting interpretations.");
		JSONObject getInterpretationResponse = InterpretationEngineFacade.getInstance()
				.getInterpretationListByDomainGuid(domainGuid);
		
		logger.info(getInterpretationResponse.toString());
		logger.info("");

		// Update interpretation
		logger.info("Updating interpretation name to: " + newInterpretationName);
		insertedRecord.put("iName", newInterpretationName);
		JSONObject updateInterpretationResponse = InterpretationEngineFacade.getInstance()
				.updateInterpretation(insertedRecord);
		
		logger.info(updateInterpretationResponse.toString());
		logger.info("");

		// Get interpretations (confirm update)
		logger.info("Confirming interpretation name change to: " + newInterpretationName);
		JSONObject getInterpretationResponseAgain = InterpretationEngineFacade.getInstance()
				.getInterpretationListByDomainGuid(domainGuid);
		
		logger.info(getInterpretationResponseAgain.toString());
		logger.info("");

		JSONObject interpretations = new JSONObject(getInterpretationResponseAgain.toString());
		JSONObject targetInterpretation = interpretations.getJSONObject(newInterpretationName);

		assertTrue(targetInterpretation.get("iName").equals(newInterpretationName));

		// Delete interpretation
		logger.info("Deleting interpretation");
		JSONObject deleteInterpretationResponse = InterpretationEngineFacade.getInstance()
				.deleteInterpretation(insertedRecord);

		logger.info(deleteInterpretationResponse.toString());
		logger.info("");

		// Delete domain
		logger.info("Deleting domain.");
		JSONObject deleteDomainResponse = InterpretationEngineFacade.getInstance().deleteDomain(domainJson);

		logger.info(deleteDomainResponse.toString());
		logger.info("");

		} catch(DataAccessException e) {
			fail();
		}
	}

	// Private methods
	private JSONObject domainJson() {
		JSONObject domainJson = new JSONObject();
		domainJson.put("dName", domainName);

		return domainJson;
	}

	private JSONObject interpretationJson(String domainGuid) {
		JSONObject interpretationJson = new JSONObject();
		interpretationJson.put("iName", interpretationName);
		interpretationJson.put("iDomainId", domainGuid);

		return interpretationJson;
	}
}
