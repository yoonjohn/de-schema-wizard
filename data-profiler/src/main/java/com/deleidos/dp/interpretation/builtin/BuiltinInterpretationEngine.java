package com.deleidos.dp.interpretation.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.DomainMetaData;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.interpretation.InterpretationEngine;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.deleidos.dp.reversegeocoding.MockReverseGeocoder;
import com.deleidos.dp.reversegeocoding.ReverseGeocoder.ReverseGeocodingWorker;

public class BuiltinInterpretationEngine implements InterpretationEngine {
	private static final Logger logger = Logger.getLogger(BuiltinInterpretationEngine.class);
	private ResourceBundle bundle = ResourceBundle.getBundle("error-messages");
	private BuiltinDomain builtinDomain;
	private ReverseGeocodingWorker mockupReverseGeocoder = null;
	
	public BuiltinInterpretationEngine(boolean fakeGeocode) {
		builtinDomain = new BuiltinDomain();
		if(fakeGeocode) {
			mockupReverseGeocoder = new MockReverseGeocoder();
		}
	}

	@Override
	public JSONArray getAvailableDomains() {
		List<DomainMetaData> domainList = new ArrayList<DomainMetaData>();
		DomainMetaData domain = new DomainMetaData();
		domain.setdName(builtinDomain.getDomainName());
		domainList.add(domain);
		
		JSONArray jsonArr = new JSONArray();
		JSONObject json = new JSONObject();
		domainList.forEach((k) -> json.put("key", k));
		return jsonArr;
		//		return domainList;
	}

	@Override
	public Map<String, Profile> interpret(String domainGuid, Map<String, Profile> profileMap) {
		for(String key : profileMap.keySet()) {
			String comparisonName = key;
			if(key.contains(String.valueOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER))) {
				comparisonName = key.substring(key.lastIndexOf(DefaultProfilerRecord.STRUCTURED_OBJECT_APPENDER)+1, key.length());
			}
			Interpretation iBean = determineInterpretation(builtinDomain, comparisonName, profileMap.get(key), .8f);
			profileMap.get(key).setInterpretation(iBean);
		}
		return profileMap;
	}

	/**
	 * Determine the possible interpretations of a metric.
	 * @param metrics The metrics class to be interpreted.
	 * @param minimumConfidenceLevel The minimum confidence for an interpretation to be considered a possibility.
	 * @return A list of possible interpretations of the given metric.
	 */
	private Interpretation determineInterpretation(BuiltinDomain builtinDomain, String fieldName, Profile profile, float minimumConfidenceLevel) {
		List<AbstractBuiltinInterpretation> possibleInterpretations = new ArrayList<AbstractBuiltinInterpretation>();
		for(AbstractBuiltinInterpretation interpretation : builtinDomain.getInterpretationMap().values()) {
			if(profile.getDetail() instanceof NumberDetail) {
				if(!interpretation.fitsNumberMetrics(Profile.getNumberDetail(profile).getMin()) 
						|| !interpretation.fitsNumberMetrics(Profile.getNumberDetail(profile).getMax())) {
					continue;
				}
			} else if(profile.getDetail() instanceof StringDetail) {

			} else if(profile.getDetail() instanceof BinaryDetail) {

			} else {
				return Interpretation.UNKNOWN;
			}
			double d = 0;
			d = interpretation.matches(fieldName, profile);
			interpretation.setConfidence(Double.valueOf(d));
			if(interpretation.getConfidence() > minimumConfidenceLevel) {
				possibleInterpretations.add(interpretation);
			}
		}

		possibleInterpretations.sort((o1,  o2) -> { 
			double dif = o2.getConfidence() - o1.getConfidence();
			if(dif < 0) return -1;
			else if(Double.doubleToRawLongBits(dif) == 0) return 0;
			else return 1;
		});

		Interpretation iBean = new Interpretation();
		if(possibleInterpretations.size() > 0) {
			iBean.setiName(possibleInterpretations.get(0).getInterpretationName());
			return iBean;
		} else {
			return Interpretation.UNKNOWN;
		}

	}

	@Override
	public List<String> getCountryCodesFromCoordinateList(List<Double[]> latlngs) throws DataAccessException {
		if(this.mockupReverseGeocoder == null) {
			throw new DataAccessException("Interpretation Engine not connected.");
		} else {
			return mockupReverseGeocoder.getCountryCodesFromCoordinateList(latlngs);
		}
	}

	@Override
	public JSONObject getInterpretationListByDomainGuid(String domainGuid) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject createDomain(JSONObject domainJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject createInterpretation(JSONObject interpretationJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject updateDomain(JSONObject domainJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject updateInterpretation(JSONObject interpretationJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject deleteDomain(JSONObject domainJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject deleteInterpretation(JSONObject interpretationJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject validatePythonScript(JSONObject b64Script) {
		return buildNoLiveInterpretationEngineResponse();
	}

	@Override
	public JSONObject testPythonScript(JSONObject iIdJson) {
		return buildNoLiveInterpretationEngineResponse();
	}

	private JSONObject buildNoLiveInterpretationEngineResponse() {
		logger.error("This webapp is using the builtin Interpretation Engine, but it attempted to interact with the "
				+ "HTTP Interpretation Engine.");
		return new JSONObject();
	}

	@Override
	public boolean isLive() {
		return true;
	}
}
