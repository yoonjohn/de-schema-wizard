package com.deleidos.ie.webapp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.deleidos.ie.mongodb.MongoHandler;

@Path("/")
public class InterpretationEngineController {
	private static Logger logger = Logger.getLogger(InterpretationEngineController.class);

	/**
	 * Constructor
	 *
	 * @param service
	 */
	public InterpretationEngineController() {

	} // constructor

	@GET
	@Path("/reversegeo/{coordinates}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getReverseGeo(@PathParam("coordinates") String coordinates) {
		/*
		 * 
		 * Basic implementation of reverse geocoding is here.
		 * 
		 * // TODO Ability to reverse geocode several latitude/longitude fields
		 * in one JSON object
		 * 
		 */		
		JSONObject json = new JSONObject(coordinates);
		double lat = Double.parseDouble(json.getString("lat"));
		double lng = Double.parseDouble(json.getString("lng"));

		String countryCode = MongoHandler.getCountryCodeFromCoordinates(lat, lng, 100);
		
		if (countryCode == null) {countryCode = "Invalid Input Format.";}

		logger.debug("");
		logger.debug("getReverseGeo request received");
		logger.debug("");
		JSONObject geoLocation = new JSONObject();
		geoLocation.put(countryCode, coordinates);
		logger.debug("");
		logger.debug(geoLocation.toString());
		logger.debug("");
		return geoLocation.toString();
	} // getReverseGeo

	@GET
	@Path("/interpret/{profile}")
	@Produces(MediaType.TEXT_PLAIN)
	public String interpret(@PathParam("profile") String profile) {
		/*
		 * 
		 * Profile interpretation logic goes here
		 * 
		 */

		logger.debug("");
		logger.debug("interpret request received");
		logger.debug("");
		JSONObject interpretation = new JSONObject();
		interpretation.put("profile", profile);
		logger.debug("");
		logger.debug(interpretation.toString());
		logger.debug("");
		return interpretation.toString();
	} // interpret

	@GET
	@Path("/test")
	@Produces(MediaType.TEXT_PLAIN)
	public String test() {
		logger.debug("");
		logger.debug("test request received");
		logger.debug("");
		JSONObject testObject = new JSONObject();
		testObject.put("test", "value");
		logger.debug("");
		logger.debug(testObject.toString());
		logger.debug("");
		return testObject.toString();
	} // test
}
