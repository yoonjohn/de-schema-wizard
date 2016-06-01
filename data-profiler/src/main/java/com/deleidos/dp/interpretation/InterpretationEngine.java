package com.deleidos.dp.interpretation;

import java.util.List;
import java.util.Map;

import com.deleidos.dp.beans.Domain;
import com.deleidos.dp.beans.Profile;

public interface InterpretationEngine {

	public List<Domain> getAvailableDomains();
	
	public Map<String, Profile> interpret(Domain domain, Map<String, Profile> profileMap);
	
}
