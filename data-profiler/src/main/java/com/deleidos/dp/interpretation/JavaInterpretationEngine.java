package com.deleidos.dp.interpretation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.Domain;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.domain.JavaDomain;

public class JavaInterpretationEngine implements InterpretationEngine {

	@Override
	public List<Domain> getAvailableDomains() {
		List<Domain> domainList = new ArrayList<Domain>();
		for(String domainName : JavaDomain.getDomainList().keySet()) {
			Domain domain = new Domain();
			domain.setName(domainName);
			domainList.add(domain);
		}
		return domainList;
	}

	@Override
	public Map<String, Profile> interpret(Domain domain, Map<String, Profile> profileMap) {
		String domainName = domain.getName();
		JavaDomain jDomain = JavaDomain.getDomainByName(domainName);
		for(String key : profileMap.keySet()) {
			Interpretation iBean = determineInterpretation(jDomain, key, profileMap.get(key), .8f);
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
	private Interpretation determineInterpretation(JavaDomain javaDomain, String fieldName, Profile profile, float minimumConfidenceLevel) {
		List<AbstractJavaInterpretation> possibleInterpretations = new ArrayList<AbstractJavaInterpretation>();
		for(AbstractJavaInterpretation interpretation : javaDomain.getInterpretationMap().values()) {
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
			iBean.setInterpretation(possibleInterpretations.get(0).getInterpretationName());
			return iBean;
		} else {
			return Interpretation.UNKNOWN;
		}
		
	}

}
