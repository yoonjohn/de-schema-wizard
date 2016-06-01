package com.deleidos.dp.interpretation;

import java.util.List;
import java.util.Map;

import com.deleidos.dp.beans.Domain;
import com.deleidos.dp.beans.Profile;

public class InterpretationEngineFacade implements InterpretationEngine {
	private static InterpretationEngine interpretationEngine = null;
	
	public static InterpretationEngine getInstance(InterpretationEngine engine) {
		if(interpretationEngine == null) {
			interpretationEngine = engine;
		}
		return interpretationEngine;
	}
	
	public static InterpretationEngine getInstance() {
		if(interpretationEngine == null) {
			interpretationEngine = new JavaInterpretationEngine(); // java based, for now
		}
		return interpretationEngine;
	}

	@Override
	public List<Domain> getAvailableDomains() {
		return interpretationEngine.getAvailableDomains();
	}

	@Override
	public Map<String, Profile> interpret(Domain domain, Map<String, Profile> profileMap) {
		return interpretationEngine.interpret(domain, profileMap);
	}

}
