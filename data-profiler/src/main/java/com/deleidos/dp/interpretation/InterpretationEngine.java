package com.deleidos.dp.interpretation;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.reversegeocoding.ReverseGeocoder.ReverseGeocodingWorker;

public interface InterpretationEngine extends ReverseGeocodingWorker {
	
	public boolean isLive();
	
	public JSONObject createDomain(JSONObject domainJson) throws DataAccessException;
	
	public JSONObject createInterpretation(JSONObject interpretationJson) throws DataAccessException;

	public JSONArray getAvailableDomains() throws DataAccessException;
	
	public JSONObject getInterpretationListByDomainGuid(String domainGuid) throws DataAccessException;
	
	public JSONObject updateDomain(JSONObject domainJson) throws DataAccessException;
	
	public JSONObject updateInterpretation(JSONObject interpretationJson) throws DataAccessException;
	
	public JSONObject deleteDomain(JSONObject domainJson) throws DataAccessException;
	
	public JSONObject deleteInterpretation(JSONObject interpretationJson) throws DataAccessException;
	
	public Map<String, Profile> interpret(String domainGuid, Map<String, Profile> profileMap) throws DataAccessException;
	
	public JSONObject validatePythonScript(JSONObject b64Script) throws DataAccessException;
	
	public JSONObject testPythonScript(JSONObject iIdJson) throws DataAccessException;
	
}